package fi.oph.kouta.integration.fixture

import java.util.UUID
import fi.oph.kouta.TestData.{JulkaistuHakukohde, Liite1, Liite2, Valintakoe1}
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoClient, LokalisointiClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.TestData.LukioHakukohteenLinja
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{IndexType, IndexTypeHakukohde, IndexTypeKoulutus, Priority}
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, SQLHelpers, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{HakukohdeCopyResultObject, HakukohdeService, HakukohdeServiceValidation, HakukohdeTilaChangeResultObject, KeywordService, KoodistoService, OrganisaatioServiceImpl, ToteutusService, ToteutusServiceValidation}
import fi.oph.kouta.servlet.HakukohdeServlet
import fi.oph.kouta.util.TimeUtils
import org.junit.Assert
import org.scalactic.Equality
import slick.jdbc.PostgresProfile.api._

object SqsInTransactionServiceCheckingRowExistsWhenIndexing extends SqsInTransactionService {
  override def toSQSQueue(priority: Priority, index: IndexType, value: String): List[String] = {
    index match {
      case IndexTypeHakukohde =>
        HakukohdeDAO.get(HakukohdeOid(value), TilaFilter.all()) match {
          case None => Assert.fail("Hakukohde not found in db")
          case Some(h) => Assert.assertEquals(value, h._1.oid.get.s)
        }
      case IndexTypeKoulutus =>
        KoulutusDAO.get(KoulutusOid(value)) match {
          case None => Assert.fail("Koulutus not found in db")
          case Some(k) => Assert.assertEquals(value, k.oid.get.s)
        }
      case _ =>
    }
    List.empty
  }
}

trait HakukohdeFixture extends SQLHelpers with AccessControlSpec with ToteutusFixture {
  this: KoutaIntegrationSpec with KoulutusFixture =>

  val HakukohdePath           = "/hakukohde"
  val HakukohdeCopyPath       = s"/hakukohde/copy/"
  val HakukohdeChangeTilaPath = s"/hakukohde/tila/"

  def hakukohdeService: HakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoService     = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val mockEPerusteKoodiClient = mock[EPerusteKoodiClient]

    val toteutusServiceValidation = new ToteutusServiceValidation(
      koodistoService,
      organisaatioService,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO,
      mockEPerusteKoodiClient
    )
    val hakukohdeServiceValidation = new HakukohdeServiceValidation(
      koodistoService,
      mockHakemusPalveluClient,
      organisaatioService,
      lokalisointiClient,
      HakukohdeDAO,
      HakuDAO
    )

    new HakukohdeService(
      SqsInTransactionServiceCheckingRowExistsWhenIndexing,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      lokalisointiClient,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      koodistoService,
      new ToteutusService(
        SqsInTransactionServiceCheckingRowExistsWhenIndexing,
        MockS3ImageService,
        auditLog,
        new KeywordService(auditLog, organisaatioService),
        organisaatioService,
        koulutusService,
        lokalisointiClient,
        koodistoService,
        mockOppijanumerorekisteriClient,
        mockKayttooikeusClient,
        toteutusServiceValidation,
        koutaIndeksoijaClient
      ),
      hakukohdeServiceValidation,
      koutaIndeksoijaClient
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new HakukohdeServlet(hakukohdeService), HakukohdePath)
  }

  val hakukohde: Hakukohde = JulkaistuHakukohde
  val hakukohdeWoValintaperusteenValintakokeet = JulkaistuHakukohde.copy(metadata =
    Some(JulkaistuHakukohde.metadata.get.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
  )

  def withValintaperusteenValintakokeet(hakukohde: Hakukohde): Hakukohde = {
    var metadata =
      hakukohde.metadata.get
    if (hakukohde.valintaperusteId.isDefined) {
      val lisatilaisuudet =
        metadata.valintaperusteenValintakokeidenLisatilaisuudet.map(
          _.copy(id =
            db.runBlocking(
              sql"""select id from valintaperusteiden_valintakokeet where valintaperuste_id = ${hakukohde.valintaperusteId.get}"""
                .as[String]
            ).headOption
              .map(UUID.fromString)
          )
        )
      metadata = metadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = lisatilaisuudet)
    }
    hakukohde.copy(metadata = Some(metadata))
  }
  def getIds(hakukohde: Hakukohde): Hakukohde = {

    hakukohde.copy(
      liitteet = hakukohde.liitteet.map(l =>
        l.copy(id =
          db.runBlocking(
            sql"""select id from hakukohteiden_liitteet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${l.tyyppiKoodiUri}"""
              .as[String]
          ).headOption
            .map(UUID.fromString)
        )
      ),
      valintakokeet = hakukohde.valintakokeet.map(vk =>
        vk.copy(id =
          db.runBlocking(
            sql"""select id from hakukohteiden_valintakokeet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${vk.tyyppiKoodiUri}"""
              .as[String]
          ).headOption
            .map(UUID.fromString)
        )
      )
    )
  }

  def tallennettuTuvaHakukohde(oid: String, toteutusOid: String, hakuOid: String, valintaperusteId: UUID) =
    withValintaperusteenValintakokeet(getIds(hakukohde(oid, toteutusOid, hakuOid, valintaperusteId))).copy(
      nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = Map(
            Fi -> s"""nimi (yleiset.vaativanaErityisenaTukena fi)""".stripMargin,
            Sv -> s"""nimi sv (yleiset.vaativanaErityisenaTukena sv)""".stripMargin
          ),
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      )
    )

  def tallennettuLukioHakukohde(
      oid: String,
      toteutusOid: String,
      hakuOid: String,
      nimi: Kielistetty,
      linja: Option[String]
  ) =
    getIds(hakukohde(oid, toteutusOid, hakuOid)).copy(
      nimi = nimi,
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = nimi,
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      ),
      metadata = Some(
        hakukohde.metadata.get
          .copy(
            hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = linja)),
            valintaperusteenValintakokeidenLisatilaisuudet = Seq()
          )
      )
    )

  def tallennettuLukioEBDIAHakukohde(oid: String, toteutusOid: String, hakuOid: String) =
    getIds(hakukohde(oid, toteutusOid, hakuOid)).copy(
      nimi = Map(Fi -> "EB", Sv -> "EB sv"),
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = Map(
            Fi -> "EB",
            Sv -> "EB sv"
          ),
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      ),
      metadata = Some(hakukohde.metadata.get.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )

  implicit val hakukohdeEquality: Equality[Hakukohde] = (a: Hakukohde, b: Any) =>
    b match {
      case v: Hakukohde =>
        val that  = a.copy(valintakokeet = a.valintakokeet.sortBy(_.nimi(Fi)))
        val other = v.copy(valintakokeet = v.valintakokeet.sortBy(_.nimi(Fi)))
        Equality.default[Hakukohde].areEqual(that, other)
      case _ => false
    }

  def hakukohde(toteutusOid: String, hakuOid: String): Hakukohde =
    hakukohde.copy(
      toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid),
      valintaperusteId = None,
      tila = Tallennettu,
      metadata = Some(hakukohde.metadata.get.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )

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

  def hakukohde(toteutusOid: String, hakuOid: String, valintaperusteId: UUID): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid),
    hakuOid = HakuOid(hakuOid),
    valintaperusteId = Some(valintaperusteId)
  )

  def hakukohde(oid: String, toteutusOid: String, hakuOid: String, valintaperusteId: UUID): Hakukohde =
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

  def hakukohde(
      oid: String,
      toteutusOid: String,
      hakuOid: String,
      valintaperusteId: UUID,
      tila: Julkaisutila
  ): Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)),
    toteutusOid = ToteutusOid(toteutusOid),
    hakuOid = HakuOid(hakuOid),
    valintaperusteId = Some(valintaperusteId),
    tila = tila
  )

  def hakukohde(
      toteutusOid: ToteutusOid,
      hakuOid: HakuOid,
      valintaperusteId: UUID,
      organisaatioOid: OrganisaatioOid
  ): Hakukohde = hakukohde.copy(
    toteutusOid = toteutusOid,
    hakuOid = hakuOid,
    valintaperusteId = Some(valintaperusteId),
    organisaatioOid = organisaatioOid
  )

  def put(hakukohde: Hakukohde): String                  = put(HakukohdePath, hakukohde, oid)
  def put(hakukohde: Hakukohde, sessionId: UUID): String = put(HakukohdePath, hakukohde, sessionId, oid)
  def put(hakukohteet: List[String], hakuOid: String): List[HakukohdeCopyResultObject] =
    put(s"$HakukohdeCopyPath$hakuOid", hakukohteet, listResponse[HakukohdeCopyResultObject])

  def changeTila(
      hakukohteet: List[String],
      tila: String,
      lastModified: String,
      sessionId: UUID,
      expectedStatus: Int
  ): List[HakukohdeTilaChangeResultObject] =
    post(
      s"$HakukohdeChangeTilaPath$tila",
      hakukohteet,
      lastModified,
      sessionId,
      expectedStatus,
      listResponse[HakukohdeTilaChangeResultObject]
    )

  def get(oid: String, expected: Hakukohde): String =
    get(HakukohdePath, oid, expected.copy(modified = Some(readHakukohdeModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Hakukohde): String =
    get(HakukohdePath, oid, sessionId, expected.copy(modified = Some(readHakukohdeModified(oid))))

  def update(haku: Hakukohde, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(HakukohdePath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(HakukohdePath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean): Unit =
    update(HakukohdePath, haku, lastModified, expectUpdate)
  def update(haku: Hakukohde, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(hakukohde: Hakukohde): HakukohdeListItem = {
    val oid      = put(withValintaperusteenValintakokeet(hakukohde))
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

  def readHakukohdeMuokkaaja(oid: String): String = {
    getStringColumnValue("hakukohteet", "muokkaaja", "oid", oid)
  }
  def readHakukohdeModified(oid: String): Modified = readHakukohdeModified(HakukohdeOid(oid))
  def readHakukohdeModified(oid: HakukohdeOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(HakukohdeDAO.selectLastModified(oid)).get)
}
