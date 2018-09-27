package fi.oph.kouta.repository

import java.util.concurrent.TimeUnit

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import org.apache.commons.lang3.builder.ToStringBuilder
import org.flywaydb.core.Flyway
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation.Serializable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object KoutaDatabase extends Logging {

  val settings = KoutaConfigurationFactory.configuration.databaseConfiguration

  logger.warn(settings.username)

  migrate()

  val db = initDb()

  def init() = {}

  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(10, TimeUnit.MINUTES)): R = {
    Await.result(
      db.run(operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))),
      timeout + Duration(1, TimeUnit.SECONDS)
    )
  }

  def runBlockingTransactionally[R](operations: DBIO[R], timeout: Duration = Duration(20, TimeUnit.SECONDS)): Either[Throwable, R] = {
    Try(runBlocking(operations.transactionally.withTransactionIsolation(Serializable), timeout)) match {
      case Success(r) => Right(r)
      case Failure(t) => Left(t)
    }
  }

  def destroy() = {
    db.close()
  }

  private def initDb() = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(settings.url)
    hikariConfig.setUsername(settings.username)
    hikariConfig.setPassword(settings.password)
    settings.maxConnections.foreach(hikariConfig.setMaximumPoolSize)
    settings.minConnections.foreach(hikariConfig.setMinimumIdle)
    settings.registerMbeans.foreach(hikariConfig.setRegisterMbeans)
    //settings.initializationFailTimeout.foreach(hikariConfig.setI)
    //hikariConfig.setLeakDetectionThreshold(settings.leakDetectionThresholdMillis.getOrElse(settings.getMaxLifetime))
    val maxConnections = settings.numThreads.getOrElse(20)
    val executor = AsyncExecutor("kouta", 10, 1000)
    logger.info(s"Configured Hikari with ${classOf[HikariConfig].getSimpleName} " +
      s"${ToStringBuilder.reflectionToString(hikariConfig).replaceAll("password=.*?,", "password=<HIDDEN>,")}" +
      s" and executor ${ToStringBuilder.reflectionToString(executor)}")

    Database.forDataSource(new HikariDataSource(hikariConfig), maxConnections = Some(10), executor)
  }

  private def migrate() = {
    val flyway = new Flyway()
    flyway.setDataSource(settings.url, settings.username, settings.password)
    flyway.migrate()
  }
}

import slick.jdbc.{GetResult, SetParameter}

trait PgSetters {

  implicit val pgStringArraySetter:SetParameter[List[String]] = SetParameter[List[String]] {
    case (list, params) => params.setObject(list.toArray, java.sql.Types.ARRAY)
  }

}

trait Extractable[T] {

  val extractor:GetResult[T]

  def extractArray[U](o: Object): List[U] = {
    o.asInstanceOf[org.postgresql.jdbc.PgArray].getArray.asInstanceOf[Array[U]].toList
  }

  def extractPGObject(o: Object): String = {
    o.asInstanceOf[org.postgresql.util.PGobject].getValue
  }

  import fi.oph.kouta.domain.Kieli
  def extractKielistys(o: Object): Map[Kieli.Kieli, String] = {
    implicit val formats = fi.oph.kouta.servlet.KoutaServlet.koutaFormats
    import org.json4s.jackson.Serialization.read
    read[Map[Kieli.Kieli, String]](extractPGObject(o)).filter(p => p._2 != null)
  }
}
