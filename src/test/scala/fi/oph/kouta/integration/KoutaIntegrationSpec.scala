package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.KoutaBackendSwagger
import fi.oph.kouta.TestSetups.{setupAwsKeysForSqs, setupWithEmbeddedPostgres, setupWithTemplate}
import fi.oph.kouta.integration.fixture.{Id, Oid, Updated}
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{CasSession, Role, ServiceTicket}
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import org.scalactic.Equality
import org.scalatra.test.scalatest.ScalatraFlatSpec

import scala.reflect.Manifest

trait KoutaIntegrationSpec extends ScalatraFlatSpec with HttpSpec with DatabaseSpec {
  val testUserOid = "test-user-oid"
  val testUserTicket = ServiceTicket("test-user-ticket")

  def addDefaultSession(): Unit = SessionDAO.store(CasSession(testUserTicket, testUserOid, Role.all.values.toSet), defaultSessionId)

  implicit val swagger = new KoutaBackendSwagger

  override def beforeAll(): Unit = {
    super.beforeAll()
    Option(System.getProperty("kouta-backend.test-postgres-port")) match {
      case Some(port) => setupWithTemplate(port.toInt)
      case None => setupWithEmbeddedPostgres
    }
    setupAwsKeysForSqs()

    addDefaultSession()
  }
}

sealed trait HttpSpec extends KoutaJsonFormats { this: ScalatraFlatSpec =>
  val defaultSessionId = UUID.randomUUID()

  val DebugJson = false

  def debugJson[E <: AnyRef](body: String)(implicit mf: Manifest[E]) = {
    if(DebugJson) {
      import org.json4s.jackson.Serialization.writePretty
      println(writePretty[E](read[E](body)))
    }
  }

  import org.json4s.jackson.Serialization.{read, write}

  def paramString(params: List[(String, String)]) =
    if (params.isEmpty) ""
    else
      s"""?${params.map(p => s"${p._1}=${p._2}").mkString("&")}"""

  def errorBody(expected: String): String = s"""{"error":"${expected}"}"""

  def validateErrorBody(expected: List[String]): String = s"""[${expected.map(s => s""""$s"""").mkString(",")}]"""

  def validateErrorBody(expected: String): String = validateErrorBody(List(expected))

  def jsonHeader = "Content-Type" -> "application/json; charset=utf-8"

  def headersIfUnmodifiedSince(lastModified: String) = List(jsonHeader, sessionHeader, "If-Unmodified-Since" -> lastModified)

  def sessionHeader(sessionId: String): (String, String) = "Cookie" -> s"session=$sessionId"

  def sessionHeader: (String, String) = sessionHeader(defaultSessionId.toString)

  def defaultHeaders: Seq[(String, String)] = Seq(sessionHeader, jsonHeader)

  def bytes(o: AnyRef) = write(o).getBytes

  val oid = (body: String) => read[Oid](body).oid

  def id(body: String) = (read[Id](body)).id

  def updated(body: String) = read[Updated](body).updated

  def put[E <: scala.AnyRef, R](path: String, entity: E, result: String => R): R = {
    put(path, bytes(entity), defaultHeaders) {
      withClue(body) {
        status should equal(200)
      }
      result(body)
    }
  }

  def get[E <: scala.AnyRef, I](path: String, id: I, expected: E)(implicit equality: Equality[E], mf: Manifest[E]): String =
    get(path, id, defaultHeaders, expected)

  def get[E <: scala.AnyRef, I](path: String, id: I, headers: Iterable[(String, String)], expected: E)(implicit equality: Equality[E], mf: Manifest[E]): String = {
    get(s"$path/${id.toString}", headers = headers) {
      status should equal(200)
      debugJson(body)
      read[E](body) should equal(expected)
      header("Last-Modified")
    }
  }

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, expectUpdate: Boolean): Unit =
    update(path, entity, headersIfUnmodifiedSince(lastModified), expectUpdate)

  def update[E <: scala.AnyRef](path: String, entity: E, headers: Iterable[(String, String)], expectUpdate: Boolean): Unit = {
    post(path, bytes(entity), headers) {
      status should equal(200)
      updated(body) should equal(expectUpdate)
    }
  }

  def list[R](path: String, params: Map[String, String], expected: List[R], headers: Iterable[(String, String)])
             (implicit mf: Manifest[R]): Seq[R] = {
    get(s"$path/list", params, headers) {
      status should equal(200)
      val result = read[List[R]](body)
      result should contain theSameElementsAs expected
      result
    }
  }

  def list[R](path: String, params: Map[String, String], expected: List[R])(implicit mf: Manifest[R]): Seq[R] =
    list(path, params, expected, defaultHeaders)

  def list(path: String, params: Map[String, String], expectedStatus: Int, headers: Iterable[(String, String)]): Unit = {
    get(s"$path/list", params, headers) {
      status should equal(expectedStatus)
    }
  }

  def list(path: String, params: Map[String, String], expectedStatus: Int): Unit =
    list(path, params, expectedStatus, defaultHeaders)
}

sealed trait DatabaseSpec {

  import fi.oph.kouta.repository.KoutaDatabase

  lazy val db = KoutaDatabase

  def truncateDatabase() = {
    import slick.jdbc.PostgresProfile.api._
    db.runBlocking(sqlu"""delete from hakukohteiden_valintakokeet""")
    db.runBlocking(sqlu"""delete from hakukohteiden_liitteet""")
    db.runBlocking(sqlu"""delete from hakukohteiden_hakuajat""")
    db.runBlocking(sqlu"""delete from hakukohteet""")
    db.runBlocking(sqlu"""delete from hakujen_hakuajat""")
    db.runBlocking(sqlu"""delete from haut""")
    db.runBlocking(sqlu"""delete from valintaperusteet""")
    db.runBlocking(sqlu"""delete from toteutusten_tarjoajat""")
    db.runBlocking(sqlu"""delete from toteutukset""")
    db.runBlocking(sqlu"""delete from koulutusten_tarjoajat""")
    db.runBlocking(sqlu"""delete from koulutukset""")

    db.runBlocking(sqlu"""delete from hakukohteiden_valintakokeet_history""")
    db.runBlocking(sqlu"""delete from hakukohteiden_liitteet_history""")
    db.runBlocking(sqlu"""delete from hakukohteiden_hakuajat_history""")
    db.runBlocking(sqlu"""delete from hakukohteet_history""")
    db.runBlocking(sqlu"""delete from hakujen_hakuajat_history""")
    db.runBlocking(sqlu"""delete from haut_history""")
    db.runBlocking(sqlu"""delete from valintaperusteet_history""")
    db.runBlocking(sqlu"""delete from toteutusten_tarjoajat_history""")
    db.runBlocking(sqlu"""delete from toteutukset_history""")
    db.runBlocking(sqlu"""delete from koulutusten_tarjoajat_history""")
    db.runBlocking(sqlu"""delete from koulutukset_history""")

    db.runBlocking(sqlu"""delete from roles""")
    db.runBlocking(sqlu"""delete from sessions""")

    deleteAsiasanat()
  }

  def deleteAsiasanat() = {
    import slick.jdbc.PostgresProfile.api._
    db.runBlocking(sqlu"""delete from asiasanat""")
    db.runBlocking(sqlu"""delete from ammattinimikkeet""")
  }

  import slick.jdbc.PostgresProfile.api._
  import java.time._

  implicit val getInstant = slick.jdbc.GetResult[LocalDateTime](r =>
    LocalDateTime.ofInstant(r.nextTimestamp().toInstant, ZoneId.of("Europe/Helsinki")).withNano(0).withSecond(0))

  def readModifiedByOid(oid:String, table:String):LocalDateTime = db.runBlocking(
    sql"""select lower(system_time) from #${table} where oid = $oid""".as[LocalDateTime].head)

  def readModifiedById(id:UUID, table:String):LocalDateTime = db.runBlocking(
    sql"""select lower(system_time) from #${table} where id = ${id.toString}::uuid""".as[LocalDateTime].head)
}
