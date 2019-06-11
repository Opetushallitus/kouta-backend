package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestSetups.{setupAwsKeysForSqs, setupWithEmbeddedPostgres, setupWithTemplate}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.{Id, Oid, Updated}
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security._
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.{KoutaBackendSwagger, MockSecurityContext, OrganisaatioServiceMock}
import org.json4s.jackson.Serialization.read
import org.scalactic.Equality
import org.scalatra.test.scalatest.ScalatraFlatSpec

import scala.collection.mutable
import scala.reflect.Manifest

case class TestUser(oid: String, username: String, sessionId: UUID) {
  val ticket = MockSecurityContext.ticketFor(KoutaIntegrationSpec.serviceIdentifier, username)
}

trait KoutaIntegrationSpec extends ScalatraFlatSpec with HttpSpec with DatabaseSpec {
  implicit val swagger: KoutaBackendSwagger = new KoutaBackendSwagger

  val serviceIdentifier = KoutaIntegrationSpec.serviceIdentifier
  val rootOrganisaatio = KoutaIntegrationSpec.rootOrganisaatio
  val defaultAuthorities = KoutaIntegrationSpec.defaultAuthorities

  val testUser = TestUser("test-user-oid", "testuser", defaultSessionId)

  def addDefaultSession(): Unit =  {
    SessionDAO.store(CasSession(ServiceTicket(testUser.ticket), testUser.oid, defaultAuthorities), testUser.sessionId)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Option(System.getProperty("kouta-backend.test-postgres-port")) match {
      case Some(port) => setupWithTemplate(port.toInt)
      case None => setupWithEmbeddedPostgres()
    }
    setupAwsKeysForSqs()

    addDefaultSession()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    truncateDatabase()
  }
}

object KoutaIntegrationSpec {
  val serviceIdentifier = "testService"

  val rootOrganisaatio = OrganisaatioOid("1.2.246.562.10.00000000001")
  val defaultAuthorities: Set[Authority] = RoleEntity.all.map(re => Authority(re.Crud, rootOrganisaatio)).toSet
}

trait AccessControlSpec extends OrganisaatioServiceMock { this: HttpSpec =>
  val LonelyOid = OrganisaatioOid("1.2.246.562.10.99999999999")
  val UnknownOid = OrganisaatioOid("1.2.246.562.10.99999999998")

  //val testSessions: mutable.Map[Symbol, (String, String)] = mutable.Map.empty
  val crudSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty
  val readSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty

  var indexerSession: UUID = _
  var fakeIndexerSession: UUID = _
  var otherRoleSession: UUID = _

  def addTestSession(authorities: Authority*): UUID = {
    val sessionId = UUID.randomUUID()
    val oid = s"1.2.246.562.24.${math.abs(sessionId.getLeastSignificantBits.toInt)}"
    val user = TestUser(oid, s"user-$oid", sessionId)
    SessionDAO.store(CasSession(ServiceTicket(user.ticket), user.oid, authorities.toSet), user.sessionId)
    sessionId
  }

  def addTestSession(role: Role, organisaatioOid: OrganisaatioOid): UUID =
    addTestSession(Seq(role), organisaatioOid)

  def addTestSession(roles: Seq[Role], organisaatioOid: OrganisaatioOid): UUID = {
    val authorities = roles.map(Authority(_, organisaatioOid))
    addTestSession(authorities: _*)
  }

  def addTestSessions(roleEntities: RoleEntity*): Unit = {
    mockOrganisaatioResponses(EvilChildOid, ChildOid, ParentOid, GrandChildOid)
    mockSingleOrganisaatioResponses(LonelyOid)

    Seq(ChildOid, EvilChildOid, GrandChildOid, ParentOid, LonelyOid).foreach { org =>
      crudSessions.update(org, addTestSession(roleEntities.map(re => re.Crud.asInstanceOf[Role]), org))
    }

    Seq(ChildOid).foreach { org =>
      readSessions.update(org, addTestSession(roleEntities.map(_.Read.asInstanceOf[Role]), org))
    }

    indexerSession = addTestSession(Role.Indexer, OphOid)
    fakeIndexerSession = addTestSession(Role.Indexer, ChildOid)
    otherRoleSession = addTestSession(Role.UnknownRole("APP_OTHER"), ChildOid)
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

  def headersIfUnmodifiedSince(lastModified: String) = List(jsonHeader, defaultSessionHeader, "If-Unmodified-Since" -> lastModified)

  def sessionHeader(sessionId: String): (String, String) = "Cookie" -> s"session=$sessionId"
  def sessionHeader(sessionId: UUID): (String, String) = sessionHeader(sessionId.toString)

  def defaultSessionHeader: (String, String) = sessionHeader(defaultSessionId)

  def defaultHeaders: Seq[(String, String)] = Seq(defaultSessionHeader, jsonHeader)

  def bytes(o: AnyRef) = write(o).getBytes

  val oid = (body: String) => read[Oid](body).oid

  def id(body: String) = (read[Id](body)).id

  def updated(body: String) = read[Updated](body).updated

  def put[E <: scala.AnyRef, R](path: String, entity: E, sessionId: UUID, result: String => R): R = {
    put(path, bytes(entity), headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(200)
      }
      result(body)
    }
  }

  def put[E <: scala.AnyRef, R](path: String, entity: E, result: String => R): R =
    put(path, entity, defaultSessionId, result)

  def put[E <: scala.AnyRef](path: String, entity: E, sessionId: UUID, expectedStatus: Int): Unit = {
    put(path, bytes(entity), headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
      }
    }
  }

  def get[E <: scala.AnyRef, I](path: String, id: I, expected: E)(implicit equality: Equality[E], mf: Manifest[E]): String =
    get(path, id, defaultSessionId, expected)

  def get[E <: scala.AnyRef, I](path: String, id: I, sessionId: UUID, expected: E)(implicit equality: Equality[E], mf: Manifest[E]): String = {
    get(s"$path/${id.toString}", headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(200)
      }
      debugJson(body)
      read[E](body) should equal(expected)
      header("Last-Modified")
    }
  }

  def get(path: String, sessionId: UUID, expectedStatus: Int): Unit = {
    get(path, headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
      }
    }
  }

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, expectUpdate: Boolean): Unit =
    update(path, entity, lastModified, expectUpdate, defaultSessionId)

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(path, entity, Seq("If-Unmodified-Since" -> lastModified, jsonHeader, sessionHeader(sessionId)), expectUpdate)

  def update[E <: scala.AnyRef](path: String, entity: E, headers: Iterable[(String, String)], expectUpdate: Boolean): Unit = {
    post(path, bytes(entity), headers) {
      status should equal(200)
      updated(body) should equal(expectUpdate)
    }
  }

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, sessionId: UUID, expectedStatus: Int): Unit = {
    post(path, bytes(entity), Seq("If-Unmodified-Since" -> lastModified, jsonHeader, sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
      }
    }
  }

  def list[R](path: String, params: Map[String, String], expected: List[R])(implicit mf: Manifest[R]): Seq[R] =
    list(path, params, expected, defaultSessionId)

  def list[R](path: String, params: Map[String, String], expected: List[R], sessionId: UUID)(implicit mf: Manifest[R]): Seq[R] = {
    get(s"$path/list", params, Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(200)
      }
      val result = read[List[R]](body)
      result should contain theSameElementsAs expected
      result
    }
  }

  def list(path: String, params: Map[String, String], expectedStatus: Int): Unit =
    list(path, params, expectedStatus, defaultSessionId)

  def list(path: String, params: Map[String, String], expectedStatus: Int, sessionId: UUID): Unit =
    list(path, params, expectedStatus, Seq(sessionHeader(sessionId)))

  def list(path: String, params: Map[String, String], expectedStatus: Int, headers: Iterable[(String, String)]): Unit = {
    get(s"$path/list", params, headers) {
      status should equal(expectedStatus)
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

    db.runBlocking(sqlu"""delete from authorities""")
    db.runBlocking(sqlu"""delete from sessions""")

    deleteAsiasanat()
  }

  def deleteAsiasanat() = {
    import slick.jdbc.PostgresProfile.api._
    db.runBlocking(sqlu"""delete from asiasanat""")
    db.runBlocking(sqlu"""delete from ammattinimikkeet""")
  }

  import java.time._

  import slick.jdbc.PostgresProfile.api._

  implicit val getInstant = slick.jdbc.GetResult[LocalDateTime](r =>
    LocalDateTime.ofInstant(r.nextTimestamp().toInstant, ZoneId.of("Europe/Helsinki")).withNano(0).withSecond(0))

  def readModifiedByOid(oid:String, table:String):LocalDateTime = db.runBlocking(
    sql"""select lower(system_time) from #${table} where oid = $oid""".as[LocalDateTime].head)

  def readModifiedById(id:UUID, table:String):LocalDateTime = db.runBlocking(
    sql"""select lower(system_time) from #${table} where id = ${id.toString}::uuid""".as[LocalDateTime].head)
}
