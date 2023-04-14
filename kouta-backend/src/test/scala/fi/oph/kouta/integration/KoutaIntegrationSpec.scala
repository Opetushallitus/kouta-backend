package fi.oph.kouta.integration

import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.Diff
import fi.oph.kouta.TestOids._
import fi.oph.kouta.TestSetups
import fi.oph.kouta.client.KoutaSearchClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{AmkKoulutusKoodit, AmmatillisetKoulutusKoodit, YoKoulutusKoodit}
import fi.oph.kouta.integration.fixture.{DatabaseFixture, Id, Oid, Oids, Updated}
import fi.oph.kouta.mocks._
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.validation.{ErrorMessage, ValidationError}
import fi.vm.sade.utils.cas.CasClient.SessionCookie
import org.json4s.jackson.Serialization.read
import org.mockito.scalatest.MockitoSugar
import org.scalactic.Equality
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.util.UUID
import scala.collection.mutable

case class TestUser(oid: UserOid, username: String, sessionId: UUID) {
  val ticket: SessionCookie = MockSecurityContext.ticketFor(TestSetups.defaultServiceIdentifier, username)
}

trait DefaultTestImplicits {
  implicit val organisaatioOidOrdering: Ordering[OrganisaatioOid] = (a: OrganisaatioOid, b: OrganisaatioOid) =>
    a.s compare b.s
}

trait KoutaIntegrationSpec
    extends ScalatraFlatSpec
    with SpecWithMocks
    with UrlProperties
    with HttpSpec
    with DatabaseFixture
    with DefaultTestImplicits
    with DefaultMocks
    with MockitoSugar {

  System.setProperty("kouta-backend.useSecureCookies", "false")
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)
  TestSetups.setupPostgres()

  val serviceIdentifier: String = TestSetups.defaultServiceIdentifier
  val defaultAuthorities: Set[Authority] = TestSetups.defaultAuthorities

  val testUser: TestUser = TestUser(TestUserOid, "testuser", defaultSessionId)

  val mockOppijanumerorekisteriClient: MockOppijanumerorekisteriClient = new MockOppijanumerorekisteriClient()

  val casUrl                                         = "testCasUrl"
  val securityContext: SecurityContext               = MockSecurityContext(casUrl, serviceIdentifier, defaultAuthorities)
  val mockKayttooikeusClient: MockKayttooikeusClient = new MockKayttooikeusClient(securityContext, defaultAuthorities)
  val mockKoutaSearchClient = mock[KoutaSearchClient]

  val mockHakemusPalveluClient = new MockHakemusPalveluClient()

  def addDefaultSession(): Unit = {
    SessionDAO.store(CasSession(ServiceTicket(testUser.ticket), testUser.oid.s, defaultAuthorities), testUser.sessionId)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    TestSetups.setupAwsKeysForSqs()
    addDefaultSession()
    addDefaultMocks()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    db.clean()
    db.migrate()
  }
}

trait AccessControlSpec extends ScalatraFlatSpec {
  this: HttpSpec =>

  protected val roleEntities: Seq[RoleEntity] = Seq.empty

  override def beforeAll() = {
    super.beforeAll()
    addTestSessions()
  }

  val crudSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty
  val readSessions: mutable.Map[OrganisaatioOid, UUID] = mutable.Map.empty

  var ophSession: UUID         = _
  var ophSession2: UUID         = _
  var indexerSession: UUID     = _
  var fakeIndexerSession: UUID = _
  var otherRoleSession: UUID = _
  var yliopistotSession: UUID = _
  var ammAndChildSession: UUID = _

  private def storeTestSession(authorities: Set[Authority] = Set(), userOid: Option[UserOid] = None, session: Option[UUID] = None): UUID = {
    val sessionId = session.getOrElse(UUID.randomUUID())
    val oid       = userOid.getOrElse(userOidForTestSessionId(sessionId))
    val user      = TestUser(oid, s"user-$oid", sessionId)
    SessionDAO.store(CasSession(ServiceTicket(user.ticket), user.oid.s, authorities), user.sessionId)
    sessionId
  }

  private val userOidLength = "1.2.246.562.24.12345678901".length
  def userOidForTestSessionId(sessionId: UUID): UserOid =
    UserOid(f"1.2.246.562.24.${math.abs(sessionId.getLeastSignificantBits)}%011d".substring(0, userOidLength))

  def addTestSession(): UUID = storeTestSession(Set(), None, None)

  def addTestSession(roles: Seq[Role], organisaatioOids: Seq[OrganisaatioOid], userOid: Option[UserOid], session: Option[UUID]): UUID = {
    val authorities = organisaatioOids.map(org => roles.map(Authority(_, org))).flatten
    storeTestSession(authorities.toSet, userOid, session)
  }

  def addTestSession(
      roles: Seq[Role],
      organisaatioOids: Seq[OrganisaatioOid],
      userOid: Option[UserOid] = None,
  ): UUID = {
    addTestSession(roles, organisaatioOids, userOid, None)
  }

  def addTestSession(
      role: Role,
      organisaatioOid: OrganisaatioOid,
  ): UUID = {
    addTestSession(Seq(role), Seq(organisaatioOid), None, None)
  }

  def addTestSessions(): Unit = {
    Seq(ChildOid, EvilChildOid, GrandChildOid, ParentOid, LonelyOid, YoOid, AmmOid).foreach { org =>
      crudSessions.update(org, addTestSession(roleEntities.map(_.Crud.asInstanceOf[Role]), Seq(org), None, crudSessions.get(org)))
    }

    Seq(ChildOid, YoOid, AmmOid).foreach { org =>
      readSessions.update(org, addTestSession(roleEntities.map(_.Read.asInstanceOf[Role]), Seq(org), None, readSessions.get(org)))
    }

    yliopistotSession = addTestSession(roleEntities.map(_.Crud.asInstanceOf[Role]), Seq(YoOid, HkiYoOid, LutYoOid), None, Option(yliopistotSession))
    ammAndChildSession = addTestSession(roleEntities.map(_.Crud.asInstanceOf[Role]), Seq(AmmOid, ChildOid), None, Option(ammAndChildSession))
    ophSession = addTestSession(Seq(Role.Paakayttaja), Seq(OphOid), Some(OphUserOid), Option(ophSession))
    ophSession2 = addTestSession(Seq(Role.Paakayttaja), Seq(OphOid), Some(OphUserOid2), Option(ophSession2))
    indexerSession = addTestSession(Seq(Role.Indexer), Seq(OphOid), None, Option(indexerSession))
    fakeIndexerSession = addTestSession(Seq(Role.Indexer), Seq(ChildOid), None, Option(fakeIndexerSession))
    otherRoleSession = addTestSession(Seq(Role.UnknownRole("APP_OTHER")), Seq(ChildOid), None, Option(otherRoleSession))
  }
}

trait DefaultMocks extends OrganisaatioServiceMock with DefaultKoodistoMocks {
  def addDefaultMocks(): Unit = {
    mockOrganisaatioResponse()
    addDefaultKoodistoMockResponsesForKoulutus()
    addDefaultKoodistoMockResponsesForToteutus()
    addDefaultKoodistoMockResponsesForHakukohde()
    addDefaultKoodistoMockResponsesForHaku()
    addDefaultKoodistoMockResponsesForValintaperuste()
    addDefaultKoodistoMockResponsesForOppilaitos()
  }
}

trait DefaultKoodistoMocks extends KoodistoServiceMock {
  def addDefaultKoodistoMockResponsesForKoulutus(): Unit = {
    mockKoodiUriVersionResponse("koulutus_371101", 1)
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_00", 1)
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 1)
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso2_091", 1)
    mockLatestKoodiUriResponse("koulutus_201101", 12)
    mockLatestKoodiUriResponse("koulutus_999907", 1)
    mockLatestKoodiUriResponse("opintojenlaajuusyksikko_2", 1)
    mockLatestKoodiUriResponse("opintojenlaajuusyksikko_6", 1)
    mockLatestKoodiUriResponse("opintojenlaajuusyksikko_8", 1)
    mockKoodistoResponse("koulutuksenlisatiedot", Seq(("koulutuksenlisatiedot_03", 1, None)))
    mockKoulutustyyppiResponse(AmmatillisetKoulutusKoodit.koulutusTyypit.last, Seq(("koulutus_371101", 12, None)), AmmatillisetKoulutusKoodit.koulutusTyypit.init)
    mockKoulutustyyppiResponse(YoKoulutusKoodit.koulutusTyypit.last, Seq(("koulutus_201000", 12, None),("koulutus_371101", 12, None)), YoKoulutusKoodit.koulutusTyypit.init)
    mockKoulutustyyppiResponse(AmkKoulutusKoodit.koulutusTyypit.last, Seq(("koulutus_201000", 12, None),("koulutus_371101", 12, None)), AmkKoulutusKoodit.koulutusTyypit.init)
    mockKoodistoResponse("koulutus", Seq(("koulutus_000002", 12, None), ("koulutus_301101", 1, None), ("koulutus_371101", 1, None), ("koulutus_775101", 1, None), ("koulutus_301104", 1, None)))
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101"))
    mockKoulutusKoodiUritForEPerusteResponse(123L, None, Seq("koulutus_371101"))
    mockKoodistoResponse("tutkintonimikekk", Seq(("tutkintonimikekk_110", 3, None)))
    mockKoodistoResponse("opintojenlaajuusyksikko", Seq(
      ("opintojenlaajuusyksikko_2", 1, None), ("opintojenlaajuusyksikko_6", 1, None), ("opintojenlaajuusyksikko_8", 1, None)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    mockKoodistoResponse(
      "kansallinenkoulutusluokitus2016koulutusalataso2",
      Seq(
        ("kansallinenkoulutusluokitus2016koulutusalataso2_080", 2, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_054", 2, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_020", 2, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso1_001", 2, None)
      )
    )
    mockKoodistoResponse("erikoistumiskoulutukset", Seq(("erikoistumiskoulutukset_001", 2, None)))
  }

  def addDefaultKoodistoMockResponsesForToteutus(): Unit = {
    mockKoodistoResponse(
      "oppilaitoksenopetuskieli",
      Seq(("oppilaitoksenopetuskieli_1", 1, None), ("oppilaitoksenopetuskieli_4", 1, None))
    )
    mockKoodistoResponse("opetusaikakk", Seq(("opetusaikakk_1", 1, None)))
    mockKoodistoResponse("opetuspaikkakk", Seq(("opetuspaikkakk_1", 1, None), ("opetuspaikkakk_2", 1, None)))
    mockKoodistoResponse("osaamisala", Seq(("osaamisala_0001", 1, None)))
    mockKoodistoResponse("kausi", Seq(("kausi_k", 1, None)))
    mockKoodistoResponse(
      "kieli",
      Seq(
        ("kieli_EN", 1, None),
        ("kieli_DE", 1, None),
        ("kieli_SV", 1, None),
        ("kieli_FR", 1, None),
        ("kieli_ES", 1, None),
        ("kieli_FI", 1, None),
        ("kieli_ET", 1, None)
      )
    )
    mockKoodistoResponse("lukiopainotukset", Seq(("lukiopainotukset_1", 1, None)))
    mockKoodistoResponse("lukiolinjaterityinenkoulutustehtava", Seq(("lukiolinjaterityinenkoulutustehtava_1", 1, None)))
    mockKoodistoResponse("moduulikoodistolops2021", Seq(("moduulikoodistolops2021_kald3", 1, None)))
  }

  def addDefaultKoodistoMockResponsesForHakukohde(): Unit = {
    mockKoodistoResponse(
      "pohjakoulutusvaatimuskouta",
      Seq(
        ("pohjakoulutusvaatimuskouta_pk", 1, None),
        ("pohjakoulutusvaatimuskouta_yo", 1, None),
        ("pohjakoulutusvaatimuskouta_104", 1, None),
        ("pohjakoulutusvaatimuskouta_109", 1, None)
      )
    )
    mockKoodistoResponse(
      "liitetyypitamm",
      Seq(("liitetyypitamm_1", 1, None), ("liitetyypitamm_2", 1, None), ("liitetyypitamm_10", 1, None))
    )
    mockKoodistoResponse("posti", Seq(("posti_04230", 2, None), ("posti_61100", 2, None)))
    mockKoodistoResponse(
      "valintakokeentyyppi",
      Seq(
        ("valintakokeentyyppi_1", 1, None),
        ("valintakokeentyyppi_11", 1, None),
        ("valintakokeentyyppi_42", 2, None),
        ("valintakokeentyyppi_57", 2, None),
        ("valintakokeentyyppi_66", 6, None)
      )
    )
    mockKoodistoResponse(
      "hakukohteetperusopetuksenjalkeinenyhteishaku",
      Seq(("hakukohteetperusopetuksenjalkeinenyhteishaku_101", 1, None))
    )

    mockKoodistoResponse("painotettavatoppiaineetlukiossa", Seq(("painotettavatoppiaineetlukiossa_b3pt",1,None), ("painotettavatoppiaineetlukiossa_b1lt",1,None)))
    mockKoodistoResponse("kieli", Seq(("kieli_fi",1,None), ("kieli_sv",1,None)))
    mockKoodiUriVersionResponse("lukiopainotukset_1", 1)
  }

  def addDefaultKoodistoMockResponsesForHaku(): Unit = {
    mockKoodistoResponse("hakutapa", Seq(("hakutapa_01", 1, None), ("hakutapa_02", 1, None), ("hakutapa_03", 1, None)))
    mockKoodistoResponse("haunkohdejoukko", Seq(("haunkohdejoukko_17", 1, None), ("haunkohdejoukko_15", 1, None), ("haunkohdejoukko_05", 3, None)))
    mockKoodistoResponse("haunkohdejoukontarkenne", Seq(("haunkohdejoukontarkenne_1", 1, None)))
  }

  def addDefaultKoodistoMockResponsesForValintaperuste(): Unit = {
    mockKoodistoResponse("valintatapajono", Seq(("valintatapajono_av", 1, None), ("valintatapajono_tv", 1, None)))
  }

  def addDefaultKoodistoMockResponsesForOppilaitos(): Unit = {
    mockKoodistoResponse("organisaationkuvaustiedot", Seq(("organisaationkuvaustiedot_03", 1, None)))
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

  def validationErrorBody(expected: ErrorMessage, path: String): String = validationErrorBody(
    List(ValidationError(path, expected))
  )

  def jsonHeader: (String, String) = "Content-Type" -> "application/json; charset=utf-8"

  def headersIfUnmodifiedSince(lastModified: String, sessionHeader: (String, String) = defaultSessionHeader) =
    List(jsonHeader, sessionHeader, KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)

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

  def post[E <: scala.AnyRef, R](path: String, entity: E, lastModified: String, sessionId: UUID, expectedStatus: Int, result: String => R): R = {
    post(path, bytes(entity), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
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

  def put[E <: AnyRef, M <: AnyRef](path: String, entity: E, expectedStatus: Int, errorMessage: M)(implicit
      equality: Equality[M],
      mf: Manifest[M]
  ): Unit =
    put(path, bytes(entity), defaultHeaders) {
      withClue(body) {
        status should equal(expectedStatus)
        read[M](body) shouldEqual errorMessage
      }
    }

  def put[E <: AnyRef](
      path: String,
      entity: E,
      sessionId: UUID,
      expectedStatus: Int,
      errors: List[ValidationError]
  ): Unit =
    put(path, bytes(entity), headers = Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(expectedStatus)
        body shouldEqual validationErrorBody(errors)
      }
    }

  def get[E <: scala.AnyRef, I](path: String, id: I, expected: E)(implicit
      equality: Equality[E],
      mf: Manifest[E]
  ): String =
    get(path, id, defaultSessionId, expected)

  def get[E <: scala.AnyRef, I](path: String, id: I, sessionId: UUID, expected: E)(implicit
      equality: Equality[E],
      mf: Manifest[E]
  ): String = {
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

  def update[E <: scala.AnyRef](
      path: String,
      entity: E,
      lastModified: String,
      expectUpdate: Boolean,
      sessionId: UUID
  ): Unit =
    update(
      path,
      entity,
      Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId)),
      expectUpdate
    )

  def update[E <: scala.AnyRef](
      path: String,
      entity: E,
      headers: Iterable[(String, String)],
      expectUpdate: Boolean
  ): Unit = {
    post(path, bytes(entity), headers) {
      withClue(body) {
        status should equal(200)
      }
      updated(body) should equal(expectUpdate)
    }
  }

  def update[E <: scala.AnyRef](
      path: String,
      entity: E,
      lastModified: String,
      sessionId: UUID,
      expectedStatus: Int
  ): Unit = {
    post(
      path,
      bytes(entity),
      Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId))
    ) {
      withClue(body) {
        status should equal(expectedStatus)
      }
    }
  }

  def update[E <: AnyRef](
      path: String,
      entity: E,
      lastModified: String,
      expectedStatus: Int,
      errorPath: String,
      errorMsg: ErrorMessage
  ): Unit =
    update(path, entity, lastModified, expectedStatus, Seq(ValidationError(errorPath, errorMsg)))

  def update[E <: AnyRef, M <: AnyRef](
      path: String,
      entity: E,
      lastModified: String,
      expectedStatus: Int,
      errorMessage: M
  )(implicit equality: Equality[M], mf: Manifest[M]): Unit =
    post(
      path,
      bytes(entity),
      Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, defaultSessionHeader)
    ) {
      withClue(body) {
        status should equal(expectedStatus)
        read[M](body) shouldEqual errorMessage
      }
    }

  def update[E <: AnyRef](
      path: String,
      entity: E,
      sessionId: UUID,
      lastModified: String,
      expectedStatus: Int,
      errors: List[ValidationError]
  ): Unit =
    post(
      path,
      bytes(entity),
      Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified, jsonHeader, sessionHeader(sessionId))
    ) {
      withClue(body) {
        status should equal(expectedStatus)
        body shouldEqual validationErrorBody(errors)
      }
    }

  def list[R](path: String, params: Map[String, String], expected: List[R])(implicit mf: Manifest[R]): Seq[R] =
    list(path, params, expected, defaultSessionId)

  def list[R](path: String, params: Map[String, String], expected: List[R], sessionId: UUID)(implicit
     mf: Manifest[R]
  ): Seq[R] = {
    get(s"$path/list", params, Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(200)
      }
      val result = read[List[R]](body)
      result should contain theSameElementsAs expected
      result
    }
  }

  // theSameElementsAs-matcherin diffi on todella hankalaa luettavaa vähänkään isomman tai monimutkaisia elementtejä
  // sisältävän listan kanssa. Diffx-kirjastolla saadaan huomattavasti miellyttävämpi diffi.
  def listDiffx[R: Diff](path: String, params: Map[String, String], expected: List[R], sessionId: UUID = defaultSessionId)(implicit
      mf: Manifest[R]
  ): Seq[R] = {

    get(s"$path/list", params, Seq(sessionHeader(sessionId))) {
      withClue(body) {
        status should equal(200)
      }
      val result = read[List[R]](body)
      result shouldMatchTo expected
      result
    }
  }

  def list(path: String, params: Map[String, String], expectedStatus: Int): Unit =
    list(path, params, expectedStatus, defaultSessionId)

  def list(path: String, params: Map[String, String], expectedStatus: Int, sessionId: UUID): Unit =
    list(path, params, expectedStatus, Seq(sessionHeader(sessionId)))

  def list(
      path: String,
      params: Map[String, String],
      expectedStatus: Int,
      headers: Iterable[(String, String)]
  ): Unit = {
    get(s"$path/list", params, headers) {
      status should equal(expectedStatus)
    }
  }
}
