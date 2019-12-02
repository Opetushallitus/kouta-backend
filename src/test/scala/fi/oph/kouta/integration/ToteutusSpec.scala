package fi.oph.kouta.integration

import java.time.LocalDateTime

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture._
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations
import org.json4s.jackson.JsonMethods

class ToteutusSpec extends KoutaIntegrationSpec
  with AccessControlSpec with KoulutusFixture with ToteutusFixture with KeywordFixture with UploadFixture with Validations {

  override val roleEntities = Seq(Role.Toteutus)

  var koulutusOid = ""

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus)
  }

  "Get toteutus by oid" should "return 404 if toteutus not found" in {
    get(s"$ToteutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown toteutus oid")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$ToteutusPath/123") {
      status should equal (401)
    }
  }

  it should "allow a user of the toteutus organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(toteutus.organisaatioOid), toteutus(oid, koulutusOid))
  }

  it should "allow a user of the tarjoaja organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid).copy(tarjoajat = List(LonelyOid)))
    get(oid, crudSessions(LonelyOid), toteutus(oid, koulutusOid).copy(tarjoajat = List(LonelyOid)))
  }

  it should "deny a user without access to the toteutus organization" in {
    val oid = put(toteutus(koulutusOid))
    get(s"$ToteutusPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(ParentOid), toteutus(oid, koulutusOid))
  }

  it should "allow a user with only access to a descendant organization" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(GrandChildOid), toteutus(oid, koulutusOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(toteutus(koulutusOid))
    get(s"$ToteutusPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, indexerSession, toteutus(oid, koulutusOid))
  }

  "Create toteutus" should "store toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, toteutus(oid, koulutusOid))
  }

  it should "store korkeakoulutus toteutus" in {
    val oid = put(TestData.JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid(koulutusOid)))
    get(oid, TestData.JulkaistuYoToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid)))
  }


  it should "write create toteutus to audit log" in {
    MockAuditLogger.clean()
    val oid = put(toteutus(koulutusOid).withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "toteutus_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    put(ToteutusPath, bytes(toteutus(koulutusOid))) {
      status should equal (401)
    }
  }

  it should "validate new toteutus" in {
    put(ToteutusPath, bytes(toteutus(koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(invalidOidsMsg(List("katkarapu").map(OrganisaatioOid))))
    }
  }

  it should "allow a user of the toteutus organization to create the toteutus" in {
    put(toteutus(koulutusOid), crudSessions(toteutus.organisaatioOid))
  }

  it should "deny a user without access to the toteutus organization" in {
    put(ToteutusPath, toteutus(koulutusOid), crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the toteutus" in {
     put(toteutus(koulutusOid), crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(ToteutusPath, toteutus(koulutusOid), crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(ToteutusPath, toteutus(koulutusOid), readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(ToteutusPath, toteutus(koulutusOid), indexerSession, 403)
  }

  it should "copy a temporary image to a permanent location while creating the toteutus" in {
    saveLocalPng("temp/image.png")
    val oid = put(toteutus(koulutusOid).copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))))

    get(oid, toteutus(oid, koulutusOid).copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/toteutus-teemakuva/$oid/image.png")))))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"toteutus-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
    MockS3Client.reset()
  }

  it should "not touch an image that's not in the temporary location" in {
    val toteutusWithImage = toteutus(koulutusOid).copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))))
    val oid = put(toteutusWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, toteutusWithImage.copy(oid = Some(ToteutusOid(oid))))
    MockS3Client.reset()
  }

  "Update toteutus" should "update toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    get(oid, toteutus(oid, koulutusOid, Arkistoitu))
  }

  it should "not update toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    MockAuditLogger.clean()
    update(thisToteutus, lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(oid, thisToteutus)
  }

  it should "write toteutus update to audit log" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    MockAuditLogger.clean()
    update(toteutus(oid, koulutusOid, Arkistoitu).copy(modified = Some(LocalDateTime.parse("1000-01-01T12:00:00"))), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "toteutus_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    post(ToteutusPath, bytes(thisToteutus), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the toteutus organization to update the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, false, crudSessions(toteutus.organisaatioOid))
  }

  it should "deny a user without access to the toteutus organization" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, false, crudSessions(ParentOid))
  }

  it should "allow a user with only access to a descendant organization" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, false, crudSessions(GrandChildOid))
  }

  it should "allow a user of the tarjoaja organization to update the toteutus" in {
    val oid = put(toteutus(koulutusOid).copy(tarjoajat = List(LonelyOid)))
    val thisToteutus = toteutus(oid, koulutusOid).copy(tarjoajat = List(LonelyOid))
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, false, crudSessions(LonelyOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, readSessions(toteutus.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    post(ToteutusPath, bytes(thisToteutus), headers = defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    post(ToteutusPath, bytes(thisToteutus), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update toteutuksen nimi, metadata ja tarjoajat" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    val uusiToteutus = thisToteutus.copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = Some(thisToteutus.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(kuvaus = Map(Fi -> "kuvaus", En -> "description"))),
      tarjoajat = List("2.2", "3.2", "4.2").map(OrganisaatioOid))
    update(uusiToteutus, lastModified, true)
    get(oid, uusiToteutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    val uusiToteutus = thisToteutus.copy(tarjoajat = List())
    update(uusiToteutus, lastModified, true)
    get(oid, uusiToteutus) should not equal (lastModified)
  }

  it should "store and update unfinished toteutus" in {
    val unfinishedToteutus = new Toteutus(muokkaaja = UserOid("5.4.3.2"), koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = OrganisaatioOid("1.2"), modified = None)
    val oid = put(unfinishedToteutus)
    val lastModified = get(oid, unfinishedToteutus.copy(oid = Some(ToteutusOid(oid))))
    val newKoulutusOid = put(koulutus)
    val newUnfinishedToteutus = unfinishedToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(newKoulutusOid))
    update(newUnfinishedToteutus, lastModified)
    get(oid, newUnfinishedToteutus)
  }

  it should "validate updated toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    post(ToteutusPath, bytes(toteutus(oid, koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(invalidOidsMsg(List("katkarapu").map(OrganisaatioOid))))
    }
  }

  it should "copy a temporary image to a permanent location while updating the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))

    saveLocalPng("temp/image.png")
    val toteutusWithImage = toteutus(oid, koulutusOid).copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))))

    update(toteutusWithImage, lastModified)
    get(oid, toteutusWithImage.copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/toteutus-teemakuva/$oid/image.png")))))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"toteutus-teemakuva/$oid/image.png"))
    MockS3Client.reset()
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val toteutusWithImage = toteutus(oid, koulutusOid).copy(metadata = toteutus.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))))

    update(toteutusWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, toteutusWithImage.copy(oid = Some(ToteutusOid(oid))))
    MockS3Client.reset()
  }

  object ToteutusJsonMethods extends JsonMethods {
    def extractJsonString(s: String): Toteutus = {
      parse(s).extract[Toteutus]
    }
  }

  it should "extract toteutus from JSON of correct form" in {
    val toteutus: Toteutus = ToteutusJsonMethods.extractJsonString(correctJson)
    toteutus.metadata.isDefined shouldBe(true)
  }

  it should "fail to extract toteutus from JSON of incorrect form" in {
    an [org.json4s.MappingException] shouldBe thrownBy(ToteutusJsonMethods.extractJsonString(incorrectJson))
  }

  val correctJson: String = """{
    "oid": "1.2.246.562.17.00000000000000000067",
    "koulutusOid": "1.2.246.562.13.00000000000000000167",
    "tila": "tallennettu",
    "tarjoajat": [],
    "nimi": {
      "fi": "Metalliseppäalan osaamisala, pk (Taideteollisuusalan perustutkinto)"
    },
    "metadata": {
      "tyyppi": "amm",
      "kuvaus": {},
      "osaamisalat": [],
      "opetus": {
      "opetuskieliKoodiUrit": [
      "oppilaitoksenopetuskieli_1#1",
      "oppilaitoksenopetuskieli_2#1"
      ],
      "opetuskieletKuvaus": {},
      "opetusaikaKoodiUrit": [
      "opetusaikakk_1#1"
      ],
      "opetusaikaKuvaus": {},
      "opetustapaKoodiUrit": [],
      "opetustapaKuvaus": {},
      "onkoMaksullinen": false,
      "maksullisuusKuvaus": {},
      "alkamisaikaKuvaus": {},
      "lisatiedot": [],
      "onkoStipendia": false,
      "stipendinKuvaus": {}
    },
      "asiasanat": [],
      "ammattinimikkeet": []
    },
    "muokkaaja": "1.2.246.562.24.87917166937",
    "organisaatioOid": "1.2.246.562.10.53642770753",
    "kielivalinta": [
    "fi",
    "sv",
    "en"
    ],
    "modified": "2019-10-29T15:21"
  }"""

  val incorrectJson: String = """{
    "oid": "1.2.246.562.17.00000000000000000067",
    "koulutusOid": "1.2.246.562.13.00000000000000000167",
    "tila": "tallennettu",
    "tarjoajat": [],
    "nimi": {
      "fi": "Metalliseppäalan osaamisala, pk (Taideteollisuusalan perustutkinto)"
    },
    "metadata": {
      "tyyppi": "amm",
      "kuvaus": {},
      "osaamisalat": [],
      "opetus": {
      "opetuskieliKoodiUrit": [
      "oppilaitoksenopetuskieli_1#1",
      "oppilaitoksenopetuskieli_2#1"
      ],
      "opetuskieletKuvaus": {},
      "opetusaikaKoodiUrit": [
      "opetusaikakk_1#1"
      ],
      "opetusaikaKuvaus": {},
      "opetustapaKoodiUrit": [],
      "opetustapaKuvaus": {},
      "onkoMaksullinen": false,
      "maksullisuusKuvaus": {},
      "maksunMaara": {"a": "b"},
      "alkamisaikaKuvaus": {},
      "lisatiedot": [],
      "onkoStipendia": false,
      "stipendinKuvaus": {}
    },
      "asiasanat": [],
      "ammattinimikkeet": []
    },
    "muokkaaja": "1.2.246.562.24.87917166937",
    "organisaatioOid": "1.2.246.562.10.53642770753",
    "kielivalinta": [
    "fi",
    "sv",
    "en"
    ],
    "modified": "2019-10-29T15:21"
  }"""
}
