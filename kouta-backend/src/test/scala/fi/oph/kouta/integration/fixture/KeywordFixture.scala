package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.OrganisaatioServiceImpl
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.service.KeywordService
import fi.oph.kouta.servlet.{AmmattinimikeServlet, AsiasanaServlet}
import org.json4s.jackson.Serialization.read

trait KeywordFixture extends KoutaIntegrationSpec with AccessControlSpec {
  val AsiasanaPath = "/asiasana"
  val AmmattinimikePath = "/ammattinimike"

  override def beforeAll(): Unit = {
    super.beforeAll()
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val keywordService = new KeywordService(new AuditLog(MockAuditLogger), organisaatioService)
    addServlet(new AsiasanaServlet(keywordService), AsiasanaPath)
    addServlet(new AmmattinimikeServlet(keywordService), AmmattinimikePath)
  }

  val asiasanat = List(
    "aaMu",
    "aarre",
    "aikainen",
    "armas",
    "armias",
    "avara",
    "harava",
    "hauras",
    "hauva",
    "kaipAA",
    "kaipaus",
    "karvas",
    "kauna",
    "kaura",
    "varvas",
    "vauras",
    "vauva"
  )

  val ammattinimikkeet = List(
    "kippari",
    "kuppari",
    "länkkäri",
    "LÄäkäri",
    "maalari",
    "muurari",
    "pappi",
    "perämies",
    "piika",
    "puTkimies",
    "pyöveli",
    "pääri",
    "räätäli",
    "sähkömies",
    "talonpoika",
    "yleislääkäri"
  )

  def storeAsiasanat() = {
    post(AsiasanaPath, bytes(asiasanat), headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    post(s"$AsiasanaPath?kieli=sv", bytes(asiasanat.map(a => s"${a}_sv")), headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
  }

  def storeAmmattinimikkeet() = {
    post(AmmattinimikePath, bytes(ammattinimikkeet), headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    post(s"$AmmattinimikePath?kieli=sv", bytes(ammattinimikkeet.map(a => s"${a}_sv")), headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
  }

  def searchAsiasanat(term:String, expected:List[String], params:List[(String,String)] = List(), sessionId: UUID = defaultSessionId) = {
    get(s"$AsiasanaPath/search/$term${paramString(params)}", headers = Seq(sessionHeader(sessionId))) {
      status should equal(200)
      read[List[String]](body) should equal(expected)
    }
  }

  def searchAmmattinimikkeet(term:String, expected:List[String], params:List[(String,String)] = List(), sessionId: UUID = defaultSessionId) = {
    get(s"$AmmattinimikePath/search/$term${paramString(params)}", headers = Seq(sessionHeader(sessionId))) {
      status should equal(200)
      read[List[String]](body) should equal(expected)
    }
  }
}
