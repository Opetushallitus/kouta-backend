package fi.oph.kouta.integration

import fi.oph.kouta.KonfoIndexingQueues
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.{AmmatillinenToteutusMetadata, Fi}
import fi.oph.kouta.integration.fixture.{KeywordFixture, KoulutusFixture, ToteutusFixture}
import org.scalatest.BeforeAndAfterEach

class KeywordSpec extends KoutaIntegrationSpec with KeywordFixture
  with KoulutusFixture with ToteutusFixture with KonfoIndexingQueues with BeforeAndAfterEach {

  var koulutusOid = ""

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    deleteAsiasanat()
    storeAsiasanat()
    storeAmmattinimikkeet()
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

  "Update toteutus" should "store ammattinimikkeet ja asiasanat in toteutus" in {
    searchAsiasanat("robo", List())
    searchAmmattinimikkeet("insinööri", List())
    put(toteutus(koulutusOid))
    searchAsiasanat("robo", toteutus.metadata.get.asiasanat.map(_.arvo))
    searchAmmattinimikkeet("insinööri", toteutus.metadata.get.ammattinimikkeet.map(_.arvo))
  }

  "Create toteutus" should "update ammattinimikkeet ja asiasanat in toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val updatedToteutus = toteutus(oid, koulutusOid).copy(metadata = Some(AmmatillinenToteutusMetadata(
      asiasanat = List(Keyword(Fi, "robotti")),
      ammattinimikkeet = List(Keyword(Fi, "robotti-insinööri"))
    )))
    update(updatedToteutus, lastModified)
    searchAsiasanat("robo", List("robotiikka", "robotti", "robottiautomatiikka"))
    searchAmmattinimikkeet("insinööri", List("insinööri", "koneinsinööri", "robotti-insinööri"))
  }

  "Create ammattinimike" should "not mind if ammattinimike exists" in {
    import slick.jdbc.PostgresProfile.api._
    val value = ammattinimikkeet.head.toLowerCase
    db.runBlocking(sql"""select count(*) from ammattinimikkeet where ammattinimike = ${value}""".as[Int].head) should be(1)
    post(AmmattinimikePath, bytes(List(value)), headers = Seq(sessionHeader)) {
      status should equal(200)
    }
    db.runBlocking(sql"""select count(*) from ammattinimikkeet where ammattinimike = ${value}""".as[Int].head) should be(1)
  }

  it should "return 401 without a valid session" in {
    val value = ammattinimikkeet.head.toLowerCase
    post(AmmattinimikePath, bytes(List(value)), headers = Seq()) {
      status should equal(401)
    }
  }

  "Create asiasana" should "not mind if asiasana exists" in {
    import slick.jdbc.PostgresProfile.api._
    val value = asiasanat.head.toLowerCase
    db.runBlocking(sql"""select count(*) from asiasanat where asiasana = ${value}""".as[Int].head) should be(1)
    post(AsiasanaPath, bytes(List(value)), headers = Seq(sessionHeader)) {
      withClue(body) {
        status should equal(200)
      }
    }
    db.runBlocking(sql"""select count(*) from asiasanat where asiasana = ${value}""".as[Int].head) should be(1)
  }

  it should "return 401 without a valid session" in {
    val value = asiasanat.head.toLowerCase
    post(AsiasanaPath, bytes(List(value)), headers = Seq()) {
      withClue(body) {
        status should equal(401)
      }
    }
  }
}
