package fi.oph.kouta.integration.fixture

import java.util.UUID
import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.{JulkaistuHakukohde, Liite1, Liite2, Valintakoe1}
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, KoodistoKaannosClient, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.{HakukohdeDAO, SQLHelpers}
import fi.oph.kouta.service.HakukohdeCopyResultObject
import fi.oph.kouta.service.{HakukohdeService, KeywordService, OrganisaatioServiceImpl, ToteutusService}
import fi.oph.kouta.servlet.HakukohdeServlet
import fi.oph.kouta.util.TimeUtils
import org.scalactic.Equality

trait HakukohdeFixture extends SQLHelpers with KoutaIntegrationSpec with AccessControlSpec with ToteutusFixture  {
  this: KoulutusFixture =>

  val HakukohdePath = "/hakukohde"
  val HakukohdeCopyPath = s"/hakukohde/copy/"

  def hakukohdeService: HakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new KoodistoKaannosClient(urlProperties.get)
    new HakukohdeService(
      SqsInTransactionServiceIgnoringIndexing,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      lokalisointiClient,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      new ToteutusService(SqsInTransactionServiceIgnoringIndexing, MockS3ImageService, auditLog,
        new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient,
        koodistoClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient)
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new HakukohdeServlet(hakukohdeService), HakukohdePath)
  }

  val hakukohde: Hakukohde = JulkaistuHakukohde

  def getIds(hakukohde:Hakukohde): Hakukohde = {
    import slick.jdbc.PostgresProfile.api._
    hakukohde.copy(
      liitteet = hakukohde.liitteet.map(l => l.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_liitteet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${l.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString))),
      valintakokeet = hakukohde.valintakokeet.map(vk => vk.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_valintakokeet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${vk.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString)))
    )}

  implicit val hakukohdeEquality: Equality[Hakukohde] = (a: Hakukohde, b: Any) => b match {
    case v: Hakukohde =>
      val that = a.copy(valintakokeet = a.valintakokeet.sortBy(_.nimi(Fi)))
      val other = v.copy(valintakokeet = v.valintakokeet.sortBy(_.nimi(Fi)))
      Equality.default[Hakukohde].areEqual(that, other)
    case _ => false
  }

  def hakukohde(toteutusOid: String, hakuOid: String): Hakukohde =
    hakukohde.copy(
      toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid),
      valintaperusteId = None,
      tila = Tallennettu)

  def hakukohde(oid: String, toteutusOid: String, hakuOid: String): Hakukohde =
    hakukohde.copy(
      oid = Some(HakukohdeOid(oid)),
      toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid),
      valintaperusteId = None,
      tila = Tallennettu,
      liitteet = List(Liite1, Liite2),
      valintakokeet = List(
        Valintakoe1
      )
    )

  def julkaistuHakukohde(toteutusOid: String, hakuOid: String): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = None, tila = Julkaistu)

  def hakukohde(toteutusOid: String, hakuOid: String, valintaperusteId: UUID): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid:String, toteutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde =
    hakukohde.copy(
      oid = Some(HakukohdeOid(oid)),
      toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid),
      valintaperusteId = Some(valintaperusteId),
      liitteet = List(Liite1, Liite2),
      valintakokeet = List(
        Valintakoe1
      )
    )

  def hakukohde(oid: String, toteutusOid: String, hakuOid: String, valintaperusteId: UUID, tila: Julkaisutila): Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), tila = tila)

  def hakukohde(toteutusOid: ToteutusOid, hakuOid: HakuOid, valintaperusteId: UUID, organisaatioOid: OrganisaatioOid): Hakukohde = hakukohde.copy(
    toteutusOid = toteutusOid, hakuOid = hakuOid, valintaperusteId = Some(valintaperusteId), organisaatioOid = organisaatioOid)

  def put(hakukohde: Hakukohde): String = put(HakukohdePath, hakukohde, oid)
  def put(hakukohde: Hakukohde, sessionId: UUID): String = put(HakukohdePath, hakukohde, sessionId, oid)
  def put(hakukohteet: List[String], hakuOid: String): List[HakukohdeCopyResultObject] = put(s"$HakukohdeCopyPath$hakuOid", hakukohteet, listResponse[HakukohdeCopyResultObject])

  def get(oid: String, expected: Hakukohde): String = get(HakukohdePath, oid, expected.copy(modified = Some(readHakukohdeModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Hakukohde): String = get(HakukohdePath, oid, sessionId, expected.copy(modified = Some(readHakukohdeModified(oid))))

  def update(haku: Hakukohde, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(HakukohdePath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(HakukohdePath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean): Unit = update(HakukohdePath, haku, lastModified, expectUpdate)
  def update(haku: Hakukohde, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(hakukohde: Hakukohde): HakukohdeListItem = {
    val oid = put(hakukohde)
    val modified = readHakukohdeModified(oid)
    HakukohdeListItem(
      HakukohdeOid(oid),
      hakukohde.toteutusOid,
      hakukohde.hakuOid,
      hakukohde.valintaperusteId,
      hakukohde.nimi,
      hakukohde.hakukohdeKoodiUri,
      hakukohde.tila,
      hakukohde.jarjestyspaikkaOid,
      hakukohde.organisaatioOid,
      hakukohde.muokkaaja,
      modified,
      None
    )
  }

  def readHakukohdeModified(oid: String): Modified = readHakukohdeModified(HakukohdeOid(oid))
  def readHakukohdeModified(oid: HakukohdeOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(HakukohdeDAO.selectLastModified(oid)).get)
}
