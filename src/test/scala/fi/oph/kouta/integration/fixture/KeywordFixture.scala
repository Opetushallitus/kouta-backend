package fi.oph.kouta.integration.fixture

import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.{AmmattinimikeServlet, AsiasanaServlet}
import org.json4s.jackson.Serialization.read

trait KeywordFixture { this:KoutaIntegrationSpec =>
  val AsiasanaPath = "/asiasana"
  val AmmattinimikePath = "/ammattinimike"

  addServlet(new AsiasanaServlet(), AsiasanaPath)
  addServlet(new AmmattinimikeServlet(), AmmattinimikePath)

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
    post(AsiasanaPath, bytes(asiasanat)) {
      status should equal(200)
    }
    post(s"$AsiasanaPath?kieli=sv", bytes(asiasanat.map(a => s"${a}_sv"))) {
      status should equal(200)
    }
  }

  def storeAmmattinimikkeet() = {
    post(AmmattinimikePath, bytes(ammattinimikkeet)) {
      status should equal(200)
    }
    post(s"$AmmattinimikePath?kieli=sv", bytes(ammattinimikkeet.map(a => s"${a}_sv"))) {
      status should equal(200)
    }
  }

  def searchAsiasanat(term:String, expected:List[String], params:List[(String,String)] = List()) = {
    get(s"$AsiasanaPath/search/$term${paramString(params)}" ) {
      status should equal(200)
      read[List[String]](body) should equal(expected)
    }
  }

  def searchAmattinimikkeet(term:String, expected:List[String], params:List[(String,String)] = List()) = {
    get(s"$AmmattinimikePath/search/$term${paramString(params)}" ) {
      status should equal(200)
      read[List[String]](body) should equal(expected)
    }
  }
}
