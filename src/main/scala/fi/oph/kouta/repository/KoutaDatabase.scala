package fi.oph.kouta.repository

import java.util.concurrent.TimeUnit

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.apache.commons.lang3.builder.ToStringBuilder
import org.flywaydb.core.Flyway
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object KoutaDatabase {

  val flyway = new Flyway()
  flyway.setDataSource(System.getProperty("kouta-backend.db.url"),
                       System.getProperty("kouta-backend.db.user"),
                       System.getProperty("kouta-backend.db.password"))
  flyway.migrate()

  val db = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(System.getProperty("kouta-backend.db.url"))
    hikariConfig.setUsername(System.getProperty("kouta-backend.db.user"))
    hikariConfig.setPassword(System.getProperty("kouta-backend.db.password"))
    //config.maxConnections.foreach(c.setMaximumPoolSize)
    //config.minConnections.foreach(c.setMinimumIdle)
    //config.registerMbeans.foreach(c.setRegisterMbeans)
    //config.initializationFailTimeout.foreach(c.setInitializationFailTimeout)
    //c.setLeakDetectionThreshold(config.leakDetectionThresholdMillis.getOrElse(c.getMaxLifetime))
    //val maxConnections = config.numThreads.getOrElse(20)
    val executor = AsyncExecutor("kouta", 10, 1000)
    //logger.info(s"Configured Hikari with ${classOf[HikariConfig].getSimpleName} ${ToStringBuilder.reflectionToString(c).replaceAll("password=.*?,", "password=<HIDDEN>,")}" +
    //  s" and executor ${ToStringBuilder.reflectionToString(executor)}")


    Database.forDataSource(new HikariDataSource(hikariConfig), maxConnections = Some(10), executor)
  }

  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(10, TimeUnit.MINUTES)): R = {
    Await.result(
      db.run(operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))),
      timeout + Duration(1, TimeUnit.SECONDS)
    )
  }
}
