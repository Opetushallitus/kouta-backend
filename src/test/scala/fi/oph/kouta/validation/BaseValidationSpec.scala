package fi.oph.kouta.validation

import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def passesValidation(e: E) = e.validate() should equal(NoErrors)

  def failsValidation(e: E, expected: List[String]): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs (expected)
  }

  def failsValidation(e: E, expected: String): Assertion = failsValidation(e, List(expected))
}
