package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Julkaisutila, Sv}
import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def passesValidation(e: E) = e.validate() should equal(NoErrors)

  def failsValidation(e: E, expected: String, moreExpected: String*): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs (expected +: moreExpected)
  }
}


abstract class SubEntityValidationSpec[E <: ValidatableSubEntity] extends ScalatraFlatSpec {

  val DefaultKielivalinta = Seq(Fi, Sv)

  def failsValidation(tila: Julkaisutila, e: E, expected: String, moreExpected: String*): Assertion = e.validate(tila, DefaultKielivalinta) match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs (expected +: moreExpected)
  }

  def passesValidation(tila: Julkaisutila, e: E) = e.validate(tila, DefaultKielivalinta) should equal(NoErrors)
}
