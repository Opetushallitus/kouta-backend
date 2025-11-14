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

  def hikariConfig: HikariConfig = {
    val config = new HikariConfig()
    config.setJdbcUrl(settings.url)
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
    val executor = AsyncExecutor("kouta", hikariConfig.getMaximumPoolSize, 1000)
    logger.info(
      s"Configured Hikari with ${classOf[HikariConfig].getSimpleName} " +
        s"${ToStringBuilder.reflectionToString(hikariConfig).replaceAll("password=.*?,", "password=<HIDDEN>,")}" +
        s" and executor ${ToStringBuilder.reflectionToString(executor)}"
    )
    Database.forDataSource(
      new HikariDataSource(hikariConfig),
      maxConnections = Some(hikariConfig.getMaximumPoolSize),
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
  private val flywayConfig = Flyway.configure.dataSource(settings.url, settings.username, settings.password).cleanDisabled(false)

  logger.warn(settings.username)

  migrate()

  def init(): Unit = {}

  val dataSource: javax.sql.DataSource = {
    new HikariDataSource(hikariConfig)
  }

  def migrate(target: String = "latest"): MigrateResult = flywayConfig.target(target).load.migrate

  def clean(): CleanResult = flywayConfig.load.clean
}

object SimpleDatabaseAccessor extends KoutaDatabaseAccessor {
  override def hikariConfig: HikariConfig = {
    val config = super.hikariConfig
    config.setMaximumPoolSize(1)
    config.setMinimumIdle(0)
    config
  }
}
