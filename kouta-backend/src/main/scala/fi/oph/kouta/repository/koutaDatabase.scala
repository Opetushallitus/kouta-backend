package fi.oph.kouta.repository

import java.util.concurrent.TimeUnit
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.kouta.config.{KoutaConfigurationFactory, KoutaDatabaseConfiguration}
import fi.oph.kouta.logging.Logging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}
import org.postgresql.jdbc.AutoSave
import org.postgresql.util.PSQLException
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation
import slick.jdbc.TransactionIsolation.Serializable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

abstract class KoutaDatabaseAccessor extends Logging {
  val settings: KoutaDatabaseConfiguration = KoutaConfigurationFactory.configuration.databaseConfiguration

  // Poolikohtainen nimi, jotta Hikarin lokit ja MBeanit erottelevat poolit toisistaan. Nimien on oltava
  // uniikkeja, koska registerMbeans=true rekisteröi MBeanin poolin nimellä.
  protected def poolName: String = getClass.getSimpleName.stripSuffix("$")

  def hikariConfig: HikariConfig = {
    val config = new HikariConfig()
    config.setPoolName(poolName)
    // Yhteyden odottamisen on aikakatkaistava selvästi ennen kuin kutsujan Await ehtii aikakatkaista
    // (ks. runBlockingTransactionally, 20 s + 1 s). Hikarin oletus on 30 s, jolloin pooliruuhka näkyi
    // lokissa pelkkänä TimeoutExceptionina eikä Hikarin omana "Connection is not available" -virheenä.
    config.setConnectionTimeout(5000)
    config.setValidationTimeout(3000)
    if (settings.useAwsJdbcWrapper) {
      config.setDriverClassName("software.amazon.jdbc.Driver")
      config.setJdbcUrl(settings.url.replace("jdbc:postgresql:", "jdbc:aws-wrapper:postgresql:"))
    } else {
      config.setJdbcUrl(settings.url)
    }
    config.setUsername(settings.username)
    config.setPassword(settings.password)
    val maxPoolSize = settings.maxConnections.getOrElse(10)
    config.setMaximumPoolSize(maxPoolSize)
    if (KoutaConfigurationFactory.isTesting) {
      // Prevent db error "cached plan must not change result type" in migration tests
      config.addDataSourceProperty("autosave", AutoSave.CONSERVATIVE)
    }
    settings.minConnections.foreach(config.setMinimumIdle)
    settings.registerMbeans.foreach(config.setRegisterMbeans)
    //settings.initializationFailTimeout.foreach(hikariConfig.setI)
    //hikariConfig.setLeakDetectionThreshold(settings.leakDetectionThresholdMillis.getOrElse(settings.getMaxLifetime))
    config
  }

  val db = {
    val config = hikariConfig
    // Slick vaatii queueSize > 0 -tapauksessa, että säiemäärä == maxConnections.
    val executor = AsyncExecutor(poolName, config.getMaximumPoolSize, 1000)
    logger.info(
      s"Configured Hikari with ${classOf[HikariConfig].getSimpleName} " +
        s"${ToStringBuilder.reflectionToString(config).replaceAll("password=.*?,", "password=<HIDDEN>,")}" +
        s" and executor ${ToStringBuilder.reflectionToString(executor)}"
    )
    Database.forDataSource(
      new HikariDataSource(config),
      maxConnections = Some(config.getMaximumPoolSize),
      executor
    )
  }

  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(10, TimeUnit.MINUTES)): R = {
    Await.result(
      db.run(operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))),
      timeout + Duration(1, TimeUnit.SECONDS)
    )
  }

  def runBlockingTransactionally[R](operations: DBIO[R]):Try[R]=
    runBlockingTransactionally()(operations)

  def runBlockingTransactionally[R](timeout: Duration = Duration(20, TimeUnit.SECONDS),
                                    isolation: TransactionIsolation = Serializable,
                                    retries: Int = 2)(operations: DBIO[R]): Try[R] = {
    val SERIALIZATION_VIOLATION = "40001"
    try {
      Success[R](runBlocking(operations.transactionally.withTransactionIsolation(isolation), timeout))
    } catch {
      case e: PSQLException if e.getSQLState == SERIALIZATION_VIOLATION && retries > 0 =>
        logger.warn("Failed due to serialization violation, retrying")
        runBlockingTransactionally(timeout, isolation, retries - 1)(operations)
      case e: Exception =>
        logger.error("Error in transactional db query", e)
        Failure(e)
    }
  }

  def destroy(): Unit = {
    db.executor.close()
    db.close()
  }
}

object KoutaDatabase extends KoutaDatabaseAccessor with Logging {
  private val flywayConfig = Flyway.configure.dataSource(settings.url, settings.username, settings.password)
  private val isTestEnvironment = KoutaConfigurationFactory.configuration.isTestEnvironment

  logger.warn(settings.username)

  migrate()

  def init(): Unit = {}

  // Oma, tarkoituksella pieni pooli db-schedulerille (SchedulerConfig ajaa yhdellä säikeellä). Aiemmin tämä varasi
  // saman maxConnections-määrän kuin sovelluksen pääpooli, eli instanssi otti kantaan tuplasti yhteyksiä.
  val dataSource: javax.sql.DataSource = {
    val config = hikariConfig
    config.setPoolName("kouta-scheduler")
    config.setMaximumPoolSize(4)
    config.setMinimumIdle(0)
    new HikariDataSource(config)
  }

  def migrate(target: String = "latest"): MigrateResult = flywayConfig.target(target).load.migrate

  def clean(): CleanResult = flywayConfig.cleanDisabled(!isTestEnvironment).load.clean
}

object SimpleDatabaseAccessor extends KoutaDatabaseAccessor {
  override def hikariConfig: HikariConfig = {
    val config = super.hikariConfig
    config.setMaximumPoolSize(1)
    config.setMinimumIdle(0)
    config
  }
}
