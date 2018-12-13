package fi.oph.kouta.repository

import java.sql.{JDBCType, PreparedStatement, ResultSet, Timestamp}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}

import fi.oph.kouta.domain.{Ajanjakso, OidListItem}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import slick.jdbc.{PositionedParameters, SetParameter}
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlStreamingAction

import scala.util.Try

trait SQLHelpers extends KoutaJsonFormats with Logging {

  def createInParams(x:Seq[String]) = if(x.isEmpty) s"''" else x.map(s => s"'$s'").mkString(",")
  def createRangeInParams(x:Seq[Ajanjakso]) = if(x.isEmpty) s"''" else x.map(s => s"${toTsrangeString(s)}").mkString(",")

  def formatTimestampParam(value:Option[LocalDateTime]) = value.map(ISO_LOCAL_DATE_TIME_FORMATTER.format).getOrElse(null)

  def toJsonParam(value:AnyRef) = Option(toJson(value)) match {
    case Some(s) if !s.isEmpty & !"{}".equals(s) => s
    case _ => null
  }

  def toTsrangeString(a:Ajanjakso) = s"'[${ISO_LOCAL_DATE_TIME_FORMATTER.format(a.alkaa)}, ${ISO_LOCAL_DATE_TIME_FORMATTER.format(a.paattyy)})'"

  implicit object SetInstant extends SetParameter[Instant] {
    def apply(v: Instant, pp: PositionedParameters): Unit = {
      pp.setObject(OffsetDateTime.ofInstant(v, ZoneId.of("Europe/Helsinki")), JDBCType.TIMESTAMP_WITH_TIMEZONE.getVendorTypeNumber)
    }
  }

  /*TODO: params:Array[AnyRef]
  def query[T](sql:String, params:Array[String], toResult:(ResultSet) => T): List[T] = {
    KoutaDatabase.runBlocking(SimpleDBIO { session =>
      var statement:Option[PreparedStatement] = None
      var resultSet:Option[ResultSet] = None
      try {
        statement = Some(session.connection.prepareStatement(sql))
        statement.foreach(s => {
          params.zip(Stream from 1).map { case (x, i) => s.setString(i, x)}
          resultSet = Some(s.executeQuery())
        })
        new Iterator[T] {
          def hasNext = resultSet.map(_.next()).getOrElse(false)
          def next() = toResult(resultSet.get)
        }.toList
      } catch {
        case t:Throwable => {
          logger.error(s"Query failed with exception ${t.getMessage}", t)
          throw t
        }
      } finally {
        Try(resultSet.foreach(_.close))
        Try(statement.foreach(_.close))
      }
    })
  }*/
}
