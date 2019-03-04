package fi.oph.kouta.integration

import java.util.UUID
import scala.reflect.Manifest

import fi.oph.kouta.KoutaBackendSwagger
import fi.oph.kouta.TestSetups.{setupWithEmbeddedPostgres, setupWithTemplate}
import fi.oph.kouta.integration.fixture.{Id, Oid, Updated}
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import org.scalactic.Equality
import org.scalatra.test.scalatest.ScalatraFlatSpec

trait KoutaIntegrationSpec extends ScalatraFlatSpec with HttpSpec with DatabaseSpec {
  implicit val swagger = new KoutaBackendSwagger

  override def beforeAll() = {
    super.beforeAll()
    Option(System.getProperty("kouta-backend.test-postgres-port")) match {
      case Some(port) => setupWithTemplate(port.toInt)
      case None => setupWithEmbeddedPostgres
    }
  }
}

sealed trait HttpSpec extends KoutaJsonFormats { this: ScalatraFlatSpec =>
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

  def jsonHeader = ("Content-Type", "application/json; charset=utf-8")

  def headersIfUnmodifiedSince(lastModified: String) = List(jsonHeader, ("If-Unmodified-Since", lastModified))

  def bytes(o: AnyRef) = write(o).getBytes

  def oid(body: String) = (read[Oid](body)).oid

  def id(body: String) = (read[Id](body)).id

  def updated(body: String) = read[Updated](body).updated

  def put[E <: scala.AnyRef, R](path: String, entity: E, result: (String) => R): R = {
    put(path, bytes(entity), List(jsonHeader)) {
      withClue(body) {
        status should equal(200)
      }
      result(body)
    }
  }

  def get[E <: scala.AnyRef, I](path: String, id: I, expected: E)(implicit equality: Equality[E], mf: Manifest[E]): String = {
    get(s"$path/${id.toString}") {
      status should equal(200)
      debugJson(body)
      read[E](body) should equal(expected)
      header.get("Last-Modified").get
    }
  }

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, expectUpdate: Boolean) = {
    post(path, bytes(entity), headersIfUnmodifiedSince(lastModified)) {
      status should equal(200)
      updated(body) should equal(expectUpdate)
    }
  }

  def list[R](path: String, params: Map[String, String], expected: List[R])(implicit mf: Manifest[R]) = {
    get(s"$path/list", params) {
      status should equal(200)
      val result = read[List[R]](body)
      result should contain theSameElementsAs (expected)
      result
    }
  }

  def list(path: String, params: Map[String, String], expectedStatus: Int) = {
    get(s"$path/list", params) {
      status should equal(expectedStatus)
      body
    }
  }
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