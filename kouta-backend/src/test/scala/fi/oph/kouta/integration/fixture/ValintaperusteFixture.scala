package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, LokalisointiClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.repository.{HakukohdeDAO, ValintaperusteDAO}
import fi.oph.kouta.service.{HakukohdeUtil, KoodistoService, OrganisaatioServiceImpl, ValintaperusteService, ValintaperusteServiceValidation}
import fi.oph.kouta.servlet.ValintaperusteServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality
import slick.jdbc.GetResult

import java.util.UUID

trait ValintaperusteFixture extends AccessControlSpec {
  this: KoutaIntegrationSpec =>

  val ValintaperustePath = "/valintaperuste"
  val koodistoService                 = new KoodistoService(new KoodistoClient(urlProperties.get))

  def valintaperusteService: ValintaperusteService = {
    val organisaatioService             = new OrganisaatioServiceImpl(urlProperties.get)
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val valintaperusteServiceValidation = new ValintaperusteServiceValidation(koodistoService, HakukohdeDAO)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val hakukohdeUtil = new HakukohdeUtil(mockOppijanumerorekisteriClient, koodistoService, lokalisointiClient)

    new ValintaperusteService(
      SqsInTransactionServiceIgnoringIndexing,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      valintaperusteServiceValidation,
      koutaIndeksoijaClient,
      hakukohdeUtil
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new ValintaperusteServlet(valintaperusteService), ValintaperustePath)
  }

  val valintaperuste: Valintaperuste = TestData.AmmValintaperuste
  val tuvaValintaperuste: Valintaperuste =
    TestData.AmmValintaperuste.copy(koulutustyyppi = Tuva, metadata = Some(TestData.TuvaValintaperusteMetadata))

  def getIds(valintaperuste: Valintaperuste): Valintaperuste = {
    import slick.jdbc.PostgresProfile.api._

    implicit val getUUIDResult: GetResult[UUID] = GetResult(r => {
      UUID.fromString(r.nextString())
    })

    valintaperuste.copy(
      valintakokeet = valintaperuste.valintakokeet.map(valintakoe =>
        valintakoe.copy(id =
          db.runBlocking(sql"""select id from valintaperusteiden_valintakokeet
              where valintaperuste_id = ${valintaperuste.id.map(_.toString)}::uuid
                and tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri}""".as[String])
            .headOption
            .map(UUID.fromString)
        )
      )
    )
  }

  def tallennettuValintaperuste(id: UUID): Valintaperuste = getIds(valintaperuste.copy(id = Some(id)))

  def valintaperuste(id: UUID): Valintaperuste                     = valintaperuste.copy(id = Some(id))
  def valintaperuste(id: UUID, tila: Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), tila = tila)

  def put(valintaperuste: Valintaperuste): UUID = put(ValintaperustePath, valintaperuste, id)
  def put(valintaperuste: Valintaperuste, sessionId: UUID): UUID =
    put(ValintaperustePath, valintaperuste, sessionId, id)

  implicit val valintaperusteEquality: Equality[Valintaperuste] = (a: Valintaperuste, b: Any) =>
    b match {
      case v: Valintaperuste =>
        val that  = a.copy(valintakokeet = a.valintakokeet.map(_.copy(id = None)).sortBy(_.nimi(Fi)))
        val other = v.copy(valintakokeet = v.valintakokeet.map(_.copy(id = None)).sortBy(_.nimi(Fi)))
        Equality.default[Valintaperuste].areEqual(that, other)
      case _ => false
    }

  def getLastModified(id: UUID): String = get(ValintaperustePath, id).toString

  def get(id: UUID, expected: Valintaperuste): String =
    get(ValintaperustePath, id, expected.copy(modified = Some(readValintaperusteModified(id))))
  def get(id: UUID, sessionId: UUID, expected: Valintaperuste): String =
    get(ValintaperustePath, id, sessionId, expected.copy(modified = Some(readValintaperusteModified(id))))

  def update(valintaperuste: Valintaperuste, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(ValintaperustePath, valintaperuste, lastModified, sessionId, expectedStatus)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(ValintaperustePath, valintaperuste, lastModified, expectUpdate, sessionId)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean): Unit =
    update(ValintaperustePath, valintaperuste, lastModified, expectUpdate)
  def update(valintaperuste: Valintaperuste, lastModified: String): Unit =
    update(valintaperuste, lastModified, expectUpdate = true)

  def valintaperuste(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Valintaperuste =
    valintaperuste.copy(organisaatioOid = organisaatioOid, tila = tila)

  def addToList(valintaperuste: Valintaperuste): ValintaperusteListItem = {
    val id       = put(valintaperuste)
    val modified = readValintaperusteModified(id)
    ValintaperusteListItem(
      id,
      valintaperuste.nimi,
      valintaperuste.tila,
      valintaperuste.organisaatioOid,
      valintaperuste.muokkaaja,
      modified
    )
  }

  def readValintaperusteMuokkaaja(id: String): String = {
    getStringColumnValue("valintaperusteet", "muokkaaja", "id", id)
  }
  def readValintaperusteModified(id: String): Modified = readValintaperusteModified(UUID.fromString(id))
  def readValintaperusteModified(id: UUID): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(ValintaperusteDAO.selectLastModified(id)).get)
}
