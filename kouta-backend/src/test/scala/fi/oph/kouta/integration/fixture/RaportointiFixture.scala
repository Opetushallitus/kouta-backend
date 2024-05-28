package fi.oph.kouta.integration.fixture

import fi.oph.kouta.client.OrganisaatioServiceClient
import fi.oph.kouta.domain.raportointi.RaportointiDateTimeFormat
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{LokalisointiServiceMock, MockSiirtotiedostoPalveluClient}
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.service.RaportointiService
import fi.oph.kouta.servlet.RaportointiServlet
import org.json4s.JValue
import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.Assertion

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant, LocalDateTime}
import java.time.temporal.ChronoUnit

trait RaportointiFixture
    extends KoulutusFixture
    with ToteutusFixture
    with HakukohdeFixture
    with HakuFixture
    with SorakuvausFixture
    with ValintaperusteFixture
    with OppilaitosFixture
    with OppilaitoksenOsaFixture
    with KeywordFixture {

  this: KoutaIntegrationSpec =>

  override val mockOrganisaatioServiceClient = mock[OrganisaatioServiceClient]

  val RaportointiPath = "/raportointi"
  val dayBefore       = Some(LocalDateTime.now.minus(Duration.of(1, ChronoUnit.DAYS)))
  val twoDaysBefore   = Some(LocalDateTime.now.minus(Duration.of(2, ChronoUnit.DAYS)))
  val dayAfter        = Some(LocalDateTime.now.plus(Duration.of(1, ChronoUnit.DAYS)))

  val siirtotiedostoPalveluClient = new MockSiirtotiedostoPalveluClient()
  val maxNumberOfItemsInOneWrite = 40
  def raportointiService: RaportointiService =
    new RaportointiService(
      RaportointiDAO,
      koulutusService,
      toteutusService,
      hakukohdeService,
      mockOppijanumerorekisteriClient,
      siirtotiedostoPalveluClient
    ) {
      override val maxNumberOfItemsInFile: Int = maxNumberOfItemsInOneWrite
    }


  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new RaportointiServlet(raportointiService), RaportointiPath)
  }

  def get(
      entityPath: String,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      expectedStatusCode: Int
  ): String = {
    val queryParams = (startTime, endTime) match {
      case (Some(startTime), Some(endTime)) => s"?startTime=${dateParam(startTime)}&endTime=${dateParam(endTime)}"
      case (Some(startTime), None)          => s"?startTime=${dateParam(startTime)}"
      case (None, Some(endTime))            => s"?endTime=${dateParam(endTime)}"
      case (_, _)                           => ""
    }
    get(s"$RaportointiPath/$entityPath$queryParams", headers = Seq(sessionHeader(raportointiSession))) {
      status should equal(expectedStatusCode)
      body
    }
  }

  def verifyLatestContents(checkedIds: Seq[String], idFieldName: String): Assertion = {
    val reportJson = parse(lastRaporttiContent())
    verifyContents(reportJson, checkedIds, idFieldName)
  }

  def verifyFirstContents(checkedIds: Seq[String], idFieldName: String): Assertion = {
    val reportJson = parse(firstRaporttiContent())
    verifyContents(reportJson, checkedIds, idFieldName)
  }

  private def verifyContents(json: JValue, checkedIds: Seq[String], idFieldName: String): Assertion = {
    val contentIds =
      json.asInstanceOf[JArray].values.indices.map(idx => (json(idx) \ idFieldName).extract[String])
    contentIds should contain theSameElementsAs checkedIds
  }

  def verifyKeywordContents(rawList: List[String]): Assertion = {
    val allLowercase     = rawList.map(_.toLowerCase)
    val sv               = allLowercase.map(k => s"${k}_sv")
    val searchedKeywords = allLowercase ::: sv
    val reportJson       = parse(lastRaporttiContent())
    val contentIds =
      reportJson.asInstanceOf[JArray].values.indices.map(idx => (reportJson(idx) \ "arvo").extract[String])
    contentIds.intersect(searchedKeywords) should contain theSameElementsAs searchedKeywords
  }

  def nbrOfContentItems(): Int      = siirtotiedostoPalveluClient.numberOfContentItems
  def lastRaporttiContent(): String = siirtotiedostoPalveluClient.last()
  def firstRaporttiContent(): String = siirtotiedostoPalveluClient.head()
  def clearRaporttiContents(): Unit = siirtotiedostoPalveluClient.clearContents()

  def dateParam(dateTime: LocalDateTime): String =
    URLEncoder.encode(RaportointiDateTimeFormat.format(dateTime), StandardCharsets.UTF_8)
}
