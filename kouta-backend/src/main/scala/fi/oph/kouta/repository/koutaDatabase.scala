package fi.oph.kouta.repository

import java.util.concurrent.TimeUnit
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation
import slick.jdbc.TransactionIsolation.Serializable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object KoutaDatabase extends Logging {

  val settings = KoutaConfigurationFactory.configuration.databaseConfiguration

  val hikariConfig: HikariConfig = {
    val config = new HikariConfig()
    config.setJdbcUrl(settings.url)
    config.setUsername(settings.username)
    config.setPassword(settings.password)
    val maxPoolSize = settings.maxConnections.getOrElse(10)
    config.setMaximumPoolSize(maxPoolSize)
    settings.minConnections.foreach(config.setMinimumIdle)
    settings.registerMbeans.foreach(config.setRegisterMbeans)
    //settings.initializationFailTimeout.foreach(hikariConfig.setI)
    //hikariConfig.setLeakDetectionThreshold(settings.leakDetectionThresholdMillis.getOrElse(settings.getMaxLifetime))
    config
  }

  logger.warn(settings.username)

  migrate()

  val db = initDb(hikariConfig)

  def init() = {}

  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(10, TimeUnit.MINUTES)): R = {
    Await.result(
      db.run(operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))),
      timeout + Duration(1, TimeUnit.SECONDS)
    )
  }

  def runBlockingTransactionally[R](operations: DBIO[R], timeout: Duration = Duration(20, TimeUnit.SECONDS), isolation: TransactionIsolation = Serializable, retries: Int = 2): Try[R] = {
    val SERIALIZATION_VIOLATION = "40001"
    try {
      Success[R](runBlocking(operations.transactionally.withTransactionIsolation(isolation), timeout))
    } catch {
      case e: PSQLException if e.getSQLState == SERIALIZATION_VIOLATION && retries > 0 =>
        logger.warn("Failed due to serialization violation, retrying")
        runBlockingTransactionally(operations, timeout, isolation, retries - 1)
      case e: Exception =>
        logger.error("Error in transactional db query", e)
        Failure(e)
    }
  }

  def destroy() = {
    db.close()
  }

  private def initDb(config: HikariConfig) = {
    val executor = AsyncExecutor("kouta", config.getMaximumPoolSize, 1000)
    logger.info(s"Configured Hikari with ${classOf[HikariConfig].getSimpleName} " +
      s"${ToStringBuilder.reflectionToString(config).replaceAll("password=.*?,", "password=<HIDDEN>,")}" +
      s" and executor ${ToStringBuilder.reflectionToString(executor)}")
    Database.forDataSource(new HikariDataSource(config), maxConnections = Some(config.getMaximumPoolSize), executor)
  }

  val dataSource: javax.sql.DataSource = {
    new HikariDataSource(hikariConfig)
  }

  private def migrate() = {
    val flyway = Flyway.configure.dataSource(settings.url, settings.username, settings.password).load
    Thread.sleep(2000)
    flyway.migrate
  }
}
