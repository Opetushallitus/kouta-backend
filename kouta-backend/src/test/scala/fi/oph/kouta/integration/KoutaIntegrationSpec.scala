package fi.oph.kouta.integration

import com.softwaremill.diffx.scalatest.DiffMatcher._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.TestSetups.{setupAwsKeysForSqs, setupWithEmbeddedPostgres, setupWithTemplate}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.integration.fixture.{Id, Oid, Oids, Updated}
import fi.oph.kouta.mocks.{KoodistoServiceMock, MockHakemusPalveluClient, MockKayttooikeusClient, MockOppijanumerorekisteriClient, MockOrganisaatioServiceClient, MockSecurityContext, OrganisaatioServiceMock}
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.validation.{ErrorMessage, ValidationError, ammatillisetKoulutustyypit, yoKoulutustyypit}
import fi.vm.sade.utils.cas.CasClient.SessionCookie
import org.json4s.jackson.Serialization.read
import org.scalactic.Equality
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.util.UUID
import scala.collection.mutable
import scala.reflect.Manifest

case class TestUser(oid: UserOid, username: String, sessionId: UUID) {
  val ticket: SessionCookie = MockSecurityContext.ticketFor(KoutaIntegrationSpec.serviceIdentifier, username)
}

trait DefaultTestImplicits {

  implicit val organisaatioOidOrdering: Ordering[OrganisaatioOid] = (a: OrganisaatioOid, b: OrganisaatioOid) => a.s compare b.s
}

trait KoutaIntegrationSpec extends ScalatraFlatSpec with HttpSpec with DatabaseSpec with DefaultTestImplicits {

  val serviceIdentifier: String = KoutaIntegrationSpec.serviceIdentifier
  val defaultAuthorities: Set[Authority] = KoutaIntegrationSpec.defaultAuthorities

  val testUser: TestUser = TestUser(TestUserOid, "testuser", defaultSessionId)

  val mockOppijanumerorekisteriClient: MockOppijanumerorekisteriClient = new MockOppijanumerorekisteriClient()

  val casUrl = "testCasUrl"
  val securityContext: SecurityContext = MockSecurityContext(casUrl, serviceIdentifier, defaultAuthorities)
  val mockKayttooikeusClient: MockKayttooikeusClient = new MockKayttooikeusClient(securityContext, defaultAuthorities)
  val mockOrganisaatioClient: MockOrganisaatioServiceClient = new MockOrganisaatioServiceClient(securityContext, defaultAuthorities)

  val mockHakemusPalveluClient = new MockHakemusPalveluClient()

  def addDefaultSession(): Unit =  {
    SessionDAO.store(CasSession(ServiceTicket(testUser.ticket), testUser.oid.s, defaultAuthorities), testUser.sessionId)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    System.setProperty("kouta-backend.useSecureCookies", "false")
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
  val defaultAuthorities: Set[Authority] = RoleEntity.all.map(re => Authority(re.Crud, OphOid)).toSet
}

trait AccessControlSpec extends ScalatraFlatSpec with OrganisaatioServiceMock with KoodistoServiceMock { this: HttpSpec =>

  protected val roleEntities: Seq[RoleEntity] = Seq.empty

  override def startServiceMocking(): Unit = {
    super.startServiceMocking()
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort"))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServiceMocking()
    addTestSessions()

    mockOrganisaatioResponse()
    addDefaultKoodistoMockResponsesForKoulutus()
    addDefaultKoodistoMockResponsesForToteutus()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
  }

  val crudSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty
  val readSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty

  var ophSession: UUID = _
  var indexerSession: UUID = _
  var fakeIndexerSession: UUID = _
  var otherRoleSession: UUID = _

  private def storeTestSession(authorities: Set[Authority] = Set(), userOid: Option[UserOid] = None): UUID = {
    val sessionId = UUID.randomUUID()
    val oid = userOid.getOrElse(userOidForTestSessionId(sessionId))
    val user = TestUser(oid, s"user-$oid", sessionId)
    SessionDAO.store(CasSession(ServiceTicket(user.ticket), user.oid.s, authorities), user.sessionId)
    sessionId
  }

  private val userOidLength = "1.2.246.562.24.12345678901".length
  def userOidForTestSessionId(sessionId: UUID): UserOid =
    UserOid(f"1.2.246.562.24.${math.abs(sessionId.getLeastSignificantBits)}%011d".substring(0, userOidLength))

  def addTestSession(): UUID = storeTestSession()

  def addTestSession(role: Role, organisaatioOid: OrganisaatioOid): UUID =
    addTestSession(Seq(role), organisaatioOid)

  def addTestSession(role: Role, organisaatioOid: OrganisaatioOid, userOid: UserOid): UUID =
    addTestSession(Seq(role), organisaatioOid, Some(userOid))

  def addTestSession(roles: Seq[Role], organisaatioOid: OrganisaatioOid, userOid: Option[UserOid] = None): UUID = {
    val authorities: Seq[Authority] = roles.map(Authority(_, organisaatioOid))
    storeTestSession(authorities.toSet, userOid)
  }

  def addTestSessions(): Unit = {
    Seq(ChildOid, EvilChildOid, GrandChildOid, ParentOid, LonelyOid, YoOid, AmmOid).foreach { org =>
      crudSessions.update(org, addTestSession(roleEntities.map(_.Crud.asInstanceOf[Role]), org))
    }

    Seq(ChildOid, YoOid, AmmOid).foreach { org =>
      readSessions.update(org, addTestSession(roleEntities.map(_.Read.asInstanceOf[Role]), org))
    }

    ophSession = addTestSession(Role.Paakayttaja, OphOid, OphUserOid)
    indexerSession = addTestSession(Role.Indexer, OphOid)
    fakeIndexerSession = addTestSession(Role.Indexer, ChildOid)
    otherRoleSession = addTestSession(Role.UnknownRole("APP_OTHER"), ChildOid)
  }

  def addDefaultKoodistoMockResponsesForKoulutus(): Unit = {
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_00", 1)
    mockLatestKoodiUriResponse("koulutus_201101", 12)
    mockKoodistoResponse("koulutuksenlisatiedot", Seq(("koulutuksenlisatiedot_03", 1, None)))
    mockKoulutustyyppiResponse(ammatillisetKoulutustyypit.last, Seq(("koulutus_371101", 12, None)), ammatillisetKoulutustyypit.init)
    mockKoulutustyyppiResponse(yoKoulutustyypit.last, Seq(("koulutus_201000", 12, None),("koulutus_371101", 12, None)), yoKoulutustyypit.init)
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101"))
    mockKoulutusKoodiUritForEPerusteResponse(123L, None, Seq("koulutus_371101"))
    mockKoodistoResponse("tutkintonimikekk", Seq(("tutkintonimikekk_110", 3, None)))
    mockKoodistoResponse("opintojenlaajuus", Seq(
      ("opintojenlaajuus_40", 1, None), ("opintojenlaajuus_v53", 1, None)))
    mockKoodistoResponse("koulutus", Seq(("koulutus_301101", 1, None)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    mockKoodistoResponse("kansallinenkoulutusluokitus2016koulutusalataso2",Seq(
      ("kansallinenkoulutusluokitus2016koulutusalataso2_080", 2, None),
      ("kansallinenkoulutusluokitus2016koulutusalataso2_020", 2, None),
      ("kansallinenkoulutusluokitus2016koulutusalataso1_001", 2, None)))
    mockKoodistoResponse("opintojenlaajuusyksikko", Seq(("opintojenlaajuusyksikko_6", 1, None)))
  }

  def addDefaultKoodistoMockResponsesForToteutus(): Unit = {
    mockKoodistoResponse("oppilaitoksenopetuskieli", Seq(("oppilaitoksenopetuskieli_1", 1, None)))
    mockKoodistoResponse("opetusaikakk", Seq(("opetusaikakk_1", 1, None)))
    mockKoodistoResponse("opetuspaikkakk", Seq(("opetuspaikkakk_1", 1, None), ("opetuspaikkakk_2", 1, None)))
    mockKoodistoResponse("osaamisala", Seq(("osaamisala_0001", 1, None)))
    mockKoodistoResponse("kausi", Seq(("kausi_k", 1, None)))
    mockKoodistoResponse("kieli", Seq(("kieli_EN", 1, None), ("kieli_DE", 1, None), ("kieli_SV", 1, None),
      ("kieli_FR", 1, None), ("kieli_ES", 1, None), ("kieli_FI", 1, None), ("kieli_ET", 1, None)))
    mockKoodistoResponse("lukiopainotukset", Seq(("lukiopainotukset_1", 1, None)))
    mockKoodistoResponse("lukiolinjaterityinenkoulutustehtava", Seq(("lukiolinjaterityinenkoulutustehtava_1", 1, None)))
    mockKoodistoResponse("moduulikoodistolops2021", Seq(("moduulikoodistolops2021_kald3", 1, None)))
  }

}

sealed trait HttpSpec extends KoutaJsonFormats {
  this: ScalatraFlatSpec =>
  val defaultSessionId: UUID = UUID.randomUUID()

  val DebugJson = false

  def debugJson[E <: AnyRef](body: String)(implicit mf: Manifest[E]): Unit = {
    if (DebugJson) {
      import org.json4s.jackson.Serialization.writePretty
      println(writePretty[E](read[E](body)))
    }
  }

  import org.json4s.jackson.Serialization.{read, write}

  def paramString(params: List[(String, String)]): String =
    if (params.isEmpty) ""
    else
      s"""?${params.map(p => s"${p._1}=${p._2}").mkString("&")}"""

  def errorBody(expected: String): String = s"""{"error":"$expected"}"""

  def validationErrorBody(expected: List[ValidationError]): String = "[" + expected.map(_.toString).mkString(",") + "]"

  def validationErrorBody(expected: ErrorMessage, path: String): String = validationErrorBody(List(ValidationError(path, expected)))

  def jsonHeader: (String, String) = "Content-Type" -> "application/json; charset=utf-8"

  def headersIfUnmodifiedSince(lastModified: String, sessionHeader: (String, String) = defaultSessionHeader) = List(jsonHeader, sessionHeader, KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)

  def sessionHeader(sessionId: String): (String, String) = "Cookie" -> s"session=$sessionId"

  def sessionHeader(sessionId: UUID): (String, String) = sessionHeader(sessionId.toString)

  def defaultSessionHeader: (String, String) = sessionHeader(defaultSessionId)

  def defaultHeaders: Seq[(String, String)] = Seq(defaultSessionHeader, jsonHeader)

  def customHeaders(sessionId: UUID): Seq[(String, String)] = Seq(sessionHeader(sessionId), jsonHeader)

  def bytes(o: AnyRef): Array[Byte] = write(o).getBytes

  val oid: String => String = (body: String) => read[Oid](body).oid

  val oids: String => List[String] = (body: String) => read[Oids](body).oids

  def listResponse[E](body: String)(implicit m: Manifest[E]): List[E] = read[List[E]](body)

  def id(body: String): UUID = read[Id](body).id

  def updated(body: String): Boolean = read[Updated](body).updated

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

  def put[E <: AnyRef](path: String, entity: E, expectedStatus: Int, errorPath: String, errorMsg: ErrorMessage): Unit =
    put(path, entity, expectedStatus, Seq(ValidationError(errorPath, errorMsg)))

  def put[E <: AnyRef, M <: AnyRef](path: String, entity: E, expectedStatus: Int, errorMessage: M)(implicit equality: Equality[M], mf: Manifest[M]): Unit =
    put(path, bytes(entity), defaultHeaders) {
      withClue(body) {
        status should equal(expectedStatus)
        read[M](body) shouldEqual errorMessage
      }
    }

  def put[E <: AnyRef](path: String, entity: E, sessionId: UUID, expectedStatus: Int, errors: List[ValidationError]): Unit =
    put(path, bytes(entity), headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
        body shouldEqual validationErrorBody(errors)
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
      header(KoutaServlet.LastModifiedHeader)
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
    update(path, entity, Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId)), expectUpdate)

  def update[E <: scala.AnyRef](path: String, entity: E, headers: Iterable[(String, String)], expectUpdate: Boolean): Unit = {
    post(path, bytes(entity), headers) {
      withClue(body) {
        status should equal(200)
      }
      updated(body) should equal(expectUpdate)
    }
  }

  def update[E <: scala.AnyRef](path: String, entity: E, lastModified: String, sessionId: UUID, expectedStatus: Int): Unit = {
    post(path, bytes(entity), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
      }
    }
  }

  def update[E <: AnyRef](path: String, entity: E, lastModified: String, expectedStatus: Int, errorPath: String, errorMsg: ErrorMessage): Unit =
    update(path, entity, lastModified, expectedStatus, Seq(ValidationError(errorPath, errorMsg)))

  def update[E <: AnyRef, M <: AnyRef](path: String, entity: E, lastModified: String, expectedStatus: Int, errorMessage: M)(implicit equality: Equality[M], mf: Manifest[M]): Unit =
    post(path, bytes(entity), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, defaultSessionHeader)) {
      withClue(body) {
        status should equal(expectedStatus)
        read[M](body) shouldEqual errorMessage
      }
    }

  def update[E <: AnyRef](path: String, entity: E, sessionId: UUID, lastModified: String, expectedStatus: Int, errors: List[ValidationError]): Unit =
    post(path, bytes(entity), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
        body shouldEqual validationErrorBody(errors)
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
      // theSameElementsAs-matcherin diffi on todella hankalaa luettavaa vähänkään isomman tai monimutkaisia elementtejä
      // sisältävän listan kanssa. Tällä tavalla vertailemalla saadaan huomattavasti parempi diffi.
      result.sortBy(_.hashCode).toString should matchTo(expected.sortBy(_.hashCode).toString)
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
  import slick.jdbc.PostgresProfile.api._

  lazy val db: KoutaDatabase.type = KoutaDatabase

  def truncateDatabase(): Int = {
    db.runBlocking(sqlu"""delete from hakukohteiden_valintakokeet""")
    db.runBlocking(sqlu"""delete from hakukohteiden_liitteet""")
    db.runBlocking(sqlu"""delete from hakukohteiden_hakuajat""")
    db.runBlocking(sqlu"""delete from hakukohteet""")
    db.runBlocking(sqlu"""delete from hakujen_hakuajat""")
    db.runBlocking(sqlu"""delete from haut""")
    db.runBlocking(sqlu"""delete from valintaperusteiden_valintakokeet""")
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
    db.runBlocking(sqlu"""delete from hakujen_valintakokeet_history""")
    db.runBlocking(sqlu"""delete from haut_history""")
    db.runBlocking(sqlu"""delete from valintaperusteet_history""")
    db.runBlocking(sqlu"""delete from toteutusten_tarjoajat_history""")
    db.runBlocking(sqlu"""delete from toteutukset_history""")
    db.runBlocking(sqlu"""delete from koulutusten_tarjoajat_history""")
    db.runBlocking(sqlu"""delete from koulutukset_history""")

    db.runBlocking(sqlu"""delete from sorakuvaukset""")
    db.runBlocking(sqlu"""delete from sorakuvaukset_history""")

    db.runBlocking(sqlu"""delete from oppilaitosten_osat""")
    db.runBlocking(sqlu"""delete from oppilaitosten_osat_history""")
    db.runBlocking(sqlu"""delete from oppilaitokset""")
    db.runBlocking(sqlu"""delete from oppilaitokset_history""")

    db.runBlocking(sqlu"""delete from authorities""")
    db.runBlocking(sqlu"""delete from sessions""")

    deleteAsiasanat()
  }

  def deleteAsiasanat(): Int = {
    db.runBlocking(sqlu"""delete from asiasanat""")
    db.runBlocking(sqlu"""delete from ammattinimikkeet""")
  }
}
