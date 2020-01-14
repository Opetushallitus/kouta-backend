package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.{AmmatillinenToteutusMetadata, Fi}
import fi.oph.kouta.integration.fixture.{KeywordFixture, KoulutusFixture, ToteutusFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import org.scalatest.BeforeAndAfterEach

class KeywordSpec extends KoutaIntegrationSpec with AccessControlSpec with KeywordFixture
  with KoulutusFixture with ToteutusFixture with BeforeAndAfterEach {

  var koulutusOid = ""
  var rolelessSession: UUID = _
  var parentSession: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus)
    rolelessSession = addTestSession()
    parentSession = addTestSession(Role.Koulutus.Crud, ParentOid)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    deleteAsiasanat()
    storeAsiasanat()
    storeAmmattinimikkeet()
    MockAuditLogger.clean()
  }

  "Asiasana search" should "search asiasanat" in {
    searchAsiasanat("aa", List("aamu", "aarre", "kaipaa"))
  }

  it should "search asiasanat ignore case" in {
    searchAsiasanat("Aa", List("aamu", "aarre", "kaipaa"))
  }

  it should "return 401 without a valid session" in {
    get(s"$AsiasanaPath/search/aa", headers = Seq()) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "allow access to any authenticated user" in {
    searchAsiasanat("aa", List("aamu", "aarre", "kaipaa"), sessionId = rolelessSession)
  }

  it should "search asiasanat with given kieli" in {
    searchAsiasanat("aa", List("aamu_sv", "aarre_sv", "kaipaa_sv"), List(("kieli", "sv")))
  }

  it should "limit asiasana result to 15 by default" in {
    searchAsiasanat("a", asiasanat.map(_.toLowerCase).take(15))
  }

  it should "limit asiasana result by given value" in {
    searchAsiasanat("aa", List("aamu", "aarre"), List(("limit", "2")))
  }

  it should "order asiasana search correctly" in {
    searchAsiasanat("kai", List("kaipaa", "kaipaus", "aikainen"))
  }

  "Ammattinimike search" should "search ammattinimikkeet" in {
    searchAmmattinimikkeet("lääk", List("lääkäri", "yleislääkäri"))
  }

  it should "search ammattinimikkeet ignore case" in {
    searchAmmattinimikkeet("Lääk", List("lääkäri", "yleislääkäri"))
  }

  it should "return 401 without a valid session" in {
    get(s"$AmmattinimikePath/search/lääk", headers = Seq()) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "allow access to any authenticated user" in {
    searchAmmattinimikkeet("lääk", List("lääkäri", "yleislääkäri"), sessionId = rolelessSession)
  }

  it should "search ammattinimikkeet with given kieli" in {
    searchAmmattinimikkeet("lääk", List("lääkäri_sv", "yleislääkäri_sv"), List(("kieli", "sv")))
  }

  it should "limit ammattinimike result to 15 by default" in {
    searchAmmattinimikkeet("i", ammattinimikkeet.map(_.toLowerCase).take(15))
  }

  it should "limit ammattinimike result by given value" in {
    searchAmmattinimikkeet("mies", List("perämies", "putkimies"), List(("limit", "2")))
  }

  it should "order ammattinimike search correctly" in {
    searchAmmattinimikkeet("pa", List("pappi", "kippari", "kuppari"))
  }

  "Create toteutus" should "store ammattinimikkeet ja asiasanat in toteutus" in {
    searchAsiasanat("robo", List())
    searchAmmattinimikkeet("insinööri", List())
    put(toteutus(koulutusOid))
    MockAuditLogger.find("asiasana", "fi", "robotiikka", "asiasana_create") shouldBe defined
    MockAuditLogger.find("ammattinimike", "fi", "koneinsinööri", "ammattinimike_create") shouldBe defined
    searchAsiasanat("robo", toteutus.metadata.get.asiasanat.map(_.arvo))
    searchAmmattinimikkeet("insinööri", toteutus.metadata.get.ammattinimikkeet.map(_.arvo))
  }

  "Update toteutus" should "update ammattinimikkeet ja asiasanat in toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val updatedToteutus = toteutus(oid, koulutusOid).copy(metadata = Some(ammMetatieto.copy(
      asiasanat = List(Keyword(Fi, "robotti")),
      ammattinimikkeet = List(Keyword(Fi, "robotti-insinööri"))
    )))
    MockAuditLogger.clean()
    update(updatedToteutus, lastModified)
    MockAuditLogger.find("asiasana", "fi", "robotti", "asiasana_create") shouldBe defined
    MockAuditLogger.find("ammattinimike", "fi", "robotti-insinööri", "ammattinimike_create") shouldBe defined
    searchAsiasanat("robo", List("robotiikka", "robotti", "robottiautomatiikka"))
    searchAmmattinimikkeet("insinööri", List("insinööri", "koneinsinööri", "robotti-insinööri"))
  }

  "Create ammattinimike" should "not mind if ammattinimike exists" in {
    import slick.jdbc.PostgresProfile.api._
    val value = ammattinimikkeet.head.toLowerCase
    db.runBlocking(sql"""select count(*) from ammattinimikkeet where ammattinimike = ${value}""".as[Int].head) should be(1)
    post(AmmattinimikePath, bytes(List(value)), headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    MockAuditLogger.logs shouldBe empty
    db.runBlocking(sql"""select count(*) from ammattinimikkeet where ammattinimike = ${value}""".as[Int].head) should be(1)
  }

  it should "write to audit log" in {
    post(AmmattinimikePath, bytes(List("lisätty-ammattinimike")), headers = Seq(defaultSessionHeader)) {
      withClue(body) {
        status should equal(200)
      }
    }
    MockAuditLogger.find("ammattinimike", "fi", "lisätty-ammattinimike", "ammattinimike_create") shouldBe defined
  }

  it should "return 401 without a valid session" in {
    val value = ammattinimikkeet.head.toLowerCase
    post(AmmattinimikePath, bytes(List(value)), headers = Seq()) {
      status should equal(401)
    }
  }

  it should "deny access without root access" in {
    val value = ammattinimikkeet.head.toLowerCase
    post(AmmattinimikePath, bytes(List(value)), headers = Seq(sessionHeader(parentSession))) {
      status should equal(403)
    }
  }

  "Create asiasana" should "not mind if asiasana exists" in {
    import slick.jdbc.PostgresProfile.api._
    val value = asiasanat.head.toLowerCase
    db.runBlocking(sql"""select count(*) from asiasanat where asiasana = ${value}""".as[Int].head) should be(1)
    post(AsiasanaPath, bytes(List(value)), headers = Seq(defaultSessionHeader)) {
      withClue(body) {
        status should equal(200)
      }
    }
    MockAuditLogger.logs shouldBe empty
    db.runBlocking(sql"""select count(*) from asiasanat where asiasana = ${value}""".as[Int].head) should be(1)
  }

  it should "write to audit log" in {
    post(AsiasanaPath, bytes(List("lisätty-asiasana")), headers = Seq(defaultSessionHeader)) {
      withClue(body) {
        status should equal(200)
      }
    }
    MockAuditLogger.find("asiasana", "fi", "lisätty-asiasana", "asiasana_create") shouldBe defined
  }

  it should "return 401 without a valid session" in {
    val value = asiasanat.head.toLowerCase
    post(AsiasanaPath, bytes(List(value)), headers = Seq()) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "deny access without root access" in {
    val value = asiasanat.head.toLowerCase
    post(AsiasanaPath, bytes(List(value)), headers = Seq(sessionHeader(parentSession))) {
      status should equal(403)
    }
  }
}
