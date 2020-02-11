package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Julkaisutila, Sv}
import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def passesValidation(e: E) = e.validate() should equal(NoErrors)

  def failsValidation(e: E, path: String, expected: String): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs Seq(ValidationError(path, expected))
  }

  def failsValidation(e: E, expected: (String, String), moreExpected: (String, String)*): Assertion =
    failsValidation(e, (expected +: moreExpected).map(ValidationError.tupled))

  def failsValidation(e: E, expected: Seq[ValidationError]): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs expected
  }
}


abstract class SubEntityValidationSpec[E <: ValidatableSubEntity] extends ScalatraFlatSpec {

  val DefaultKielivalinta = Seq(Fi, Sv)

  def failsValidation(tila: Julkaisutila, e: E, path: String, expected: String): Assertion =
    e.validate(tila, DefaultKielivalinta, "path") match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs Seq(ValidationError("path." + path, expected))
    }

  def failsValidation(tila: Julkaisutila, e: E, expected: (String, String), moreExpected: (String, String)*): Assertion =
    failsValidation(tila, e, (expected +: moreExpected).map { case (path, value) => ValidationError("path." + path, value) })

  def failsValidation(tila: Julkaisutila, e: E, expected: Seq[ValidationError]): Assertion =
    e.validate(tila, DefaultKielivalinta, "path") match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs expected
    }

  def passesValidation(tila: Julkaisutila, e: E): Assertion = e.validate(tila, DefaultKielivalinta, "path") should equal(NoErrors)
}
