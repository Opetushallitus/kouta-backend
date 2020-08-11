package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Sv}
import fi.oph.kouta.validation.Validations._
import org.scalatra.test.scalatest.ScalatraFlatSpec

class ValidationsSpec extends ScalatraFlatSpec {

  "findMissingKielet" should "return all kielet when kielistetty is an empty map" in {
    findMissingKielet(Seq(Fi, Sv), Map()) should contain theSameElementsAs Seq(Fi, Sv)
  }

  it should "return the missing kielet when there are some texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text")) should contain theSameElementsAs Seq(Sv)
  }

  it should "return the missing kielet when there are some empty texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text", Sv -> "")) should contain theSameElementsAs Seq(Sv)
  }

  "validateKielistetty" should "return all kielet when kielistetty is an empty map" in {
    validateKielistetty(Seq(Fi, Sv), Map(), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Fi, Sv))))
  }

  it should "return the missing kielet when there are some texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text"), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Sv))))
  }

  it should "return the missing kielet when there are some empty texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text", Sv -> ""), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Sv))))
  }

  "assertValidEmail" should "accept an email with a plus" in {
    assertValidEmail("foo+bar@example.com", "test") shouldEqual NoErrors
  }

  it should "fail an email without a TLD" in {
    assertValidEmail("foo@bar", "test") should contain theSameElementsAs Seq(ValidationError("test", invalidEmail("foo@bar")))
  }

  it should "accept an email with a funny TLD" in {
    assertValidEmail("foo@bar.pics", "test") shouldEqual NoErrors
  }

  "assertValidUrl" should "accept a valid url" in {
    assertValidUrl("https://www.google.fi", "url") shouldEqual NoErrors
  }

  it should "fail an invalid url" in {
    assertValidUrl("urli", "url") should contain theSameElementsAs Seq(ValidationError("url", invalidUrl("urli")))
  }

  it should "fail an url without the protocol" in {
    assertValidUrl("www.url.fi", "url") should contain theSameElementsAs Seq(ValidationError("url", invalidUrl("www.url.fi")))
  }


}
