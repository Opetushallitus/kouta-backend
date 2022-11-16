package fi.oph.kouta.integration

import java.time.Instant
import java.util.UUID
import fi.oph.kouta.client.{CallerId, HttpClient, OidAndChildren}
import fi.oph.kouta.domain.{En, Fi, Haku, Hakukohde, KieliModel, Kielistetty, Koulutus, Organisaatio, Sv, TilaFilter, Toteutus}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, KoulutusOid, Oid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.integration.fixture.AuthFixture
import fi.oph.kouta.service.{HakuService, HakukohdeService, KoulutusService, OrganisaatioServiceImpl, ToteutusService}
import fi.oph.kouta.servlet.{Authenticated, LookupDb, MigrationServlet}
import fi.oph.kouta.validation.NoErrors
import fi.vm.sade.properties.OphProperties
import org.apache.commons.lang3.StringUtils
import org.mockito.captor.ArgCaptor
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.dsl.MatcherWords
import org.scalatra.test.scalatest.ScalatraFlatSpec
import org.scalatra.test.scalatest._

import scala.io.Source
import scala.util.{Failure, Success, Try}

class UrlToResourceClient extends HttpClient with CallerId {
  override def get[T](url: String, errorHandler: (String, Int, String) => Nothing, followRedirects: Boolean)(parse: String => T): T = {
    val source = Source.fromInputStream(UrlToResourceClient.this.getClass.getClassLoader.getResourceAsStream(url))
    Try(try source.mkString finally source.close()) match {
      case Success(text) => parse(text)
      case Failure(x) => throw new RuntimeException(s"Resource $url not found!")
    }
  }
}
class TrailingZeroesLookupDb extends LookupDb {
  override def findMappedOid(oldOid: String): Option[String] =
    if(StringUtils.isNumeric(oldOid)) {
      None
    } else {
      Some(oldOid + "0000")
    }

  override def insertOidMapping(oldOld: String, newOid: String): Unit = {

  }

  override def updateAllowed(oldOid: String): Option[Boolean] = None
}
class MigrationSpec extends KoutaIntegrationSpec with AuthFixture with BeforeAndAfterEach with ScalatraSuite with ScalatraFlatSpec with MatcherWords with MockitoSugar with ArgumentMatchersSugar {

  private def properties = new OphProperties()
    .addOverride("tarjonta-service.haku.oid", "tarjonta/$1.json")
    .addOverride("tarjonta-service.hakukohde.oid", "tarjonta/$1.json")
    .addOverride("tarjonta-service.koulutus.oid", "tarjonta/$1.json")
    .addOverride("tarjonta-service.toteutus.oid", "tarjonta/$1.json")
    .addOverride("tarjonta-service.komo.oid", "tarjonta/$1.json")
    .addOverride("koodisto-service.codeelement", "tarjonta/$1#$2.json")

  val koulutusService: KoulutusService = mock[KoulutusService]
  val toteutusService: ToteutusService = mock[ToteutusService]
  val hakuService: HakuService = mock[HakuService]
  val hakukohdeService: HakukohdeService = mock[HakukohdeService]
  val organisaatioServiceImpl: OrganisaatioServiceImpl = mock[OrganisaatioServiceImpl]

  addServlet(
    new MigrationServlet(
      koulutusService = koulutusService,
      toteutusService = toteutusService,
      hakuService = hakuService,
      hakukohdeService = hakukohdeService,
      organisaatioServiceImpl = organisaatioServiceImpl,
      urlProperties = properties,
      client = new UrlToResourceClient,
      db = new TrailingZeroesLookupDb), "/*")

  "Migrate haku by oid" should "return 200" in {

    val hakuCaptor = ArgCaptor[Haku]
    when(hakuService.update(hakuCaptor, any[Instant])(any[Authenticated])).thenReturn(true)

    post("/haku/1.2.246.562.29.98666182252", headers = defaultHeaders) {
      verify(hakuService).update(hakuCaptor, any[Instant])(any[Authenticated])
      status should equal(200)

      val haku = hakuCaptor.value
      haku.validate() should equal(NoErrors)
      haku.oid should equal(Some(HakuOid("1.2.246.562.29.986661822520000")))
    }

  }
  "Migrate hakukohde by oid" should "return 200" in {

    val organisaatio = Organisaatio(
      oid = "1.2.246.562.10.24790222608",
      children = List(),
      parentOidPath = "",
      oppilaitostyyppi = None,
      organisaatiotyypit = List("organisaatiotyyppi_02"),
      nimi = Map(Fi -> "", Sv -> "", En -> "")
    )
    when(organisaatioServiceImpl.getOrganisaatio(OrganisaatioOid("1.2.246.562.10.24790222608"))).thenReturn(organisaatio)

    val hakukohdeCaptor = ArgCaptor[Hakukohde]
    when(hakukohdeService.update(hakukohdeCaptor, any[Instant])(any[Authenticated])).thenReturn(true)

    when(hakukohdeService.get(any[HakukohdeOid], any[TilaFilter])(any[Authenticated])).thenAnswer((_: HakukohdeOid) =>
      Some(hakukohdeCaptor.value
      .copy(valintakokeet = hakukohdeCaptor.value.valintakokeet
        .map(vk => vk.copy(id = Some(UUID.randomUUID())))), Instant.now()): Option[(Hakukohde, Instant)])

    val koulutusCaptor = ArgCaptor[Koulutus]
    when(koulutusService.update(koulutusCaptor, any[Instant])(any[Authenticated])).thenReturn(true)

    val toteutusCaptor = ArgCaptor[Toteutus]
    when(toteutusService.update(toteutusCaptor, any[Instant])(any[Authenticated])).thenReturn(true)

    post("/hakukohde/1.2.246.562.20.20804704698", headers = defaultHeaders) {
      verify(koulutusService).update(koulutusCaptor, any[Instant])(any[Authenticated])
      verify(toteutusService).update(toteutusCaptor, any[Instant])(any[Authenticated])
      verify(hakukohdeService).update(hakukohdeCaptor, any[Instant])(any[Authenticated])
      status should equal(200)

      val hakukohde = hakukohdeCaptor.value
      hakukohde.validate() should equal(NoErrors)
      hakukohde.oid should equal(Some(HakukohdeOid("1.2.246.562.20.208047046980000")))

      val koulutus = koulutusCaptor.value
      koulutus.validate() should equal(NoErrors)
      koulutus.oid should equal(Some(KoulutusOid("1.2.246.562.13.343266279310000")))

      val toteutus = toteutusCaptor.value
      toteutus.validate() should equal(NoErrors)
      toteutus.oid should equal(Some(ToteutusOid("1.2.246.562.17.433105724580000")))
    }

  }

  "Migrate hakukohde without aloituspaikat" should "return 200" in {

    val organisaatio = Organisaatio(
      oid = "1.2.246.562.10.24790222608",
      children = List(),
      parentOidPath = "",
      oppilaitostyyppi = None,
      organisaatiotyypit = List("organisaatiotyyppi_02"),
      nimi = Map(Fi -> "", Sv -> "", En -> "")
    )
    when(organisaatioServiceImpl.getOrganisaatio(OrganisaatioOid("1.2.246.562.10.24790222608"))).thenReturn(organisaatio)

    reset(koulutusService, toteutusService, hakukohdeService)
    val hakukohdeCaptor = ArgCaptor[Hakukohde]
    when(hakukohdeService.update(hakukohdeCaptor, any[Instant])(any[Authenticated])).thenReturn(true)

    when(hakukohdeService.get(any[HakukohdeOid], any[TilaFilter])(any[Authenticated])).thenAnswer((_: HakukohdeOid) =>
      Some(hakukohdeCaptor.value
      .copy(valintakokeet = hakukohdeCaptor.value.valintakokeet
        .map(vk => vk.copy(id = Some(UUID.randomUUID())))), Instant.now()): Option[(Hakukohde, Instant)])

    when(koulutusService.update(any[Koulutus], any[Instant])(any[Authenticated])).thenReturn(true)
    when(toteutusService.update(any[Toteutus], any[Instant])(any[Authenticated])).thenReturn(true)

    post("/hakukohde/1.2.246.562.20.20804704699", headers = defaultHeaders) {
      verify(hakukohdeService).update(hakukohdeCaptor, any[Instant])(any[Authenticated])
      status should equal(200)

      val hakukohde = hakukohdeCaptor.value
      hakukohde.validate() should equal(NoErrors)
      hakukohde.oid should equal(Some(HakukohdeOid("1.2.246.562.20.208047046990000")))
      hakukohde.metadata.get.aloituspaikat.get.lukumaara should equal(Some(0))
    }

  }

  override def header = ???

}
