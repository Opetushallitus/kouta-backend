package fi.oph.kouta.integration

import fi.oph.kouta.integration.fixture.KeywordFixture
import org.scalatest.BeforeAndAfterEach

class KeywordSpec extends KoutaIntegrationSpec with KeywordFixture with BeforeAndAfterEach {

  lazy val initData = {
    storeAsiasanat()
    storeAmmattinimikkeet()
    true
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    initData
  }

  it should "search asiasanat" in {
    searchAsiasanat("aa", List("aamu", "aarre", "kaipaa"))
  }

  it should "search ammattinimikkeet" in {
    searchAmattinimikkeet("lääk", List("lääkäri", "yleislääkäri"))
  }

  it should "search asiasanat with given kieli" in {
    searchAsiasanat("aa", List("aamu_sv", "aarre_sv", "kaipaa_sv"), List(("kieli", "sv")))
  }

  it should "search ammattinimikkeet with given kieli" in {
    searchAmattinimikkeet("lääk", List("lääkäri_sv", "yleislääkäri_sv"), List(("kieli", "sv")))
  }

  it should "limit asiasana result to 15 by default" in {
    searchAsiasanat("a", asiasanat.map(_.toLowerCase).take(15))
  }

  it should "limit ammattinimike result to 15 by default" in {
    searchAmattinimikkeet("i", ammattinimikkeet.map(_.toLowerCase).take(15))
  }

  it should "limit asiasana result by given value" in {
    searchAsiasanat("aa", List("aamu", "aarre"), List(("limit", "2")))
  }

  it should "limit ammattinimike result by given value" in {
    searchAmattinimikkeet("mies", List("perämies", "putkimies"), List(("limit", "2")))
  }

  it should "order asiasana search correctly" in {
    searchAsiasanat("kai", List("kaipaa", "kaipaus", "aikainen"))
  }

  it should "order ammattinimike search correctly" in {
    searchAmattinimikkeet("pa", List("pappi", "kippari", "kuppari"))
  }
}
