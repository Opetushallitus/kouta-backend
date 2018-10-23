package fi.oph.kouta.repository

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import slick.jdbc.PostgresProfile.api._

import scala.util.Try

trait SQLHelpers extends KoutaJsonFormats with Logging {

  def toTimestampParam(value:Option[Instant]) = value.map(Timestamp.from).getOrElse(null)

  def toJsonParam(value:AnyRef) = Option(toJson(value)) match {
    case Some(s) if !s.isEmpty & !"{}".equals(s) => s
    case _ => null
  }

  //TODO: params:Array[AnyRef]
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
  }
}
