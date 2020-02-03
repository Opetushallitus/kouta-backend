package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Sv}
import org.scalatra.test.scalatest.ScalatraFlatSpec

class ValidationsSpec extends ScalatraFlatSpec with Validations {

  "findPuuttuvatKielet" should "return all kielet when kielistetty is an empty map" in {
    findPuuttuvatKielet(Seq(Fi, Sv), Map()) should contain theSameElementsAs Seq(Fi, Sv)
  }

  it should "return the missing kielet when there are some texts" in {
    findPuuttuvatKielet(Seq(Fi, Sv), Map(Fi -> "text")) should contain theSameElementsAs Seq(Sv)
  }

  it should "return the missing kielet when there are some empty texts" in {
    findPuuttuvatKielet(Seq(Fi, Sv), Map(Fi -> "text", Sv -> "")) should contain theSameElementsAs Seq(Sv)
  }

  "validateKielistetty" should "return all kielet when kielistetty is an empty map" in {
    validateKielistetty(Seq(Fi, Sv), Map(), "test") should contain(invalidKielistetty("test", Seq(Fi, Sv)))
  }

  it should "return the missing kielet when there are some texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text"), "test") should contain(invalidKielistetty("test", Seq(Sv)))
  }

  it should "return the missing kielet when there are some empty texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text", Sv -> ""), "test") should contain(invalidKielistetty("test", Seq(Sv)))
  }

  "assertValidEmail" should "accept an email with a plus" in {
    assertValidEmail("foo+bar@example.com") shouldEqual NoErrors
  }

  it should "fail an email without a TLD" in {
    assertValidEmail("foo@bar") should contain theSameElementsAs Seq(invalidEmail("foo@bar"))
  }

  it should "accept an email with a funny TLD" in {
    assertValidEmail("foo@bar.pics") shouldEqual NoErrors
  }
}
