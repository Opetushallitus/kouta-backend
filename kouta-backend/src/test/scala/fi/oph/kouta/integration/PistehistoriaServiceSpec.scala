package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{LukioHakukohteenLinja, LukioKoulutus}
import fi.oph.kouta.TestOids.OphOid
import fi.oph.kouta.client.{JononAlimmatPisteet, ValintaTulosServiceClient, ValintaperusteetServiceClient, ValintatapajonoDTO}
import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.integration.fixture.{HakuFixture, HakukohdeFixture, ValintaperusteFixture}
import fi.oph.kouta.mocks.{LokalisointiServiceMock, SpecWithMocks}
import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket}
import fi.oph.kouta.service.PistehistoriaService
import fi.oph.kouta.servlet.Authenticated
import slick.jdbc.PostgresProfile.api._

import java.net.InetAddress
import java.util.UUID

class PistehistoriaServiceSpec
    extends KoutaIntegrationSpec
    with SpecWithMocks
    with AccessControlSpec
    with HakuFixture
    with HakukohdeFixture
    with ValintaperusteFixture
    with LokalisointiServiceMock {

  var pistehistoriaService: PistehistoriaService                           = _
  val mockValintaTulosServiceClient: MockValintaTulosServiceClient2        = new MockValintaTulosServiceClient2()
  val mockValintaperusteetServiceClient: MockValintaperusteetServiceClient = new MockValintaperusteetServiceClient()
  val authenticatedPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set(
        "APP_KOUTA",
        "APP_KOUTA_OPHPAAKAYTTAJA",
        s"APP_KOUTA_OPHPAAKAYTTAJA_${OphOid}",
        s"APP_KOUTA_INDEKSOINTI_${OphOid}"
      ).map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )
  var (koulutusOid, toteutusOid, hakuOid, yhteisHakuOid, hakukohdeOid) = ("", "", "", "", "")
  var valintaperusteId: UUID                                           = _

  class MockValintaTulosServiceClient2 extends ValintaTulosServiceClient {
    override def fetchPisteet(hakuOid: HakuOid): List[JononAlimmatPisteet] = {
      List(JononAlimmatPisteet("1679913449161-2797179735267609907", hakukohdeOid, 19.0, 1692695308344L))
    }
  }

  class MockValintaperusteetServiceClient extends ValintaperusteetServiceClient {
    override def getValintatapajono(valintatapajonoOid: String): ValintatapajonoDTO = {
      ValintatapajonoDTO(aloituspaikat = 16, nimi = "Ammatillinen koulutus", kuvaus = null, tyyppi = "valintatapajono_yp", siirretaanSijoitteluun = true,
        tasapistesaanto = "ARVONTA", true, false, true, false, false, 0, 0, true, false, None, None, None, true, null, oid = "1679913449161-2797179735267609907", true, -1)
    }
  }

  override def beforeAll() = {
    super.beforeAll()
    pistehistoriaService = new PistehistoriaService(mockValintaTulosServiceClient, mockValintaperusteetServiceClient)
    hakuOid = put(haku)
    val lkToteutusOid = put(lukioToteutus(put(LukioKoulutus, ophSession)))
    val lkHakukohde = hakukohde(lkToteutusOid, hakuOid).copy(
      nimi = Map(),
      metadata = Some(
        hakukohde.metadata.get
          .copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq(), hakukohteenLinja = Some(LukioHakukohteenLinja))
      )
    )
    mockLokalisointiResponse("hakukohdelomake.lukionYleislinja")
    hakukohdeOid = put(lkHakukohde)
  }

  override def afterEach() = {
    clearServiceMocks()
  }

  "Pisteet and valintajonotyyppi for hakukohde" should "be saved to pistehistoria" in {
    pistehistoriaService.syncPistehistoriaForHaku(HakuOid(hakuOid))(
      authenticatedPaakayttaja
    )
    db.runBlocking(sql"""select count(*) from pistehistoria""".as[Int].head) should be(1)
    db.runBlocking(
      sql"""select pisteet from pistehistoria where hakukohde_oid = ${hakukohdeOid}"""
        .as[Double]
    ).headOption should equal(Some(19.0))
    db.runBlocking(
      sql"""select valintatapajono_tyyppi from pistehistoria where hakukohde_oid = ${hakukohdeOid}"""
        .as[String]
    ).headOption should equal(Some("valintatapajono_yp"))
  }
}
