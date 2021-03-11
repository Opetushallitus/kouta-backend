package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Julkaisutila, Sv}
import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def passesValidation(e: E) = e.validate() should equal(NoErrors)

  def failsValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failsValidation(e, ValidationError(path, message))

  def failsValidation(e: E, expected: ValidationError, moreExpected: ValidationError*): Assertion =
    failsValidation(e, expected +: moreExpected)

  def failsValidation(e: E, expected: Seq[ValidationError]): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs expected
  }

  def passesOnJulkaisuValidation(e: E): Assertion = e.validateOnJulkaisu() shouldEqual NoErrors

  def failsOnJulkaisuValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failsOnJulkaisuValidation(e, List(ValidationError(path, message)))

  def failsOnJulkaisuValidation( e: E, expected: Seq[ValidationError]): Assertion =
    e.validateOnJulkaisu() match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs expected
    }
}

abstract class SubEntityValidationSpec[E <: ValidatableSubEntity] extends ScalatraFlatSpec {

  val DefaultKielivalinta = Seq(Fi, Sv)
  val pathPrefix = "path"

  def failsValidation(tila: Julkaisutila, e: E, path: String, expected: ErrorMessage): Assertion =
    failsValidation(tila, e, (path, expected))

  def failsValidation(tila: Julkaisutila, e: E, expected: (String, ErrorMessage), moreExpected: (String, ErrorMessage)*): Assertion =
    failsValidation(tila, e, (expected +: moreExpected).map { case (path, value) => ValidationError(s"$pathPrefix.$path", value) })

  def failsValidation(tila: Julkaisutila, e: E, expected: Seq[ValidationError]): Assertion =
    e.validate(tila, DefaultKielivalinta, pathPrefix) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs expected
    }

  def passesValidation(tila: Julkaisutila, e: E): Assertion = e.validate(tila, DefaultKielivalinta, pathPrefix) shouldEqual NoErrors

  def passesOnJulkaisuValidation(e: E) = e.validateOnJulkaisu(pathPrefix) shouldEqual NoErrors

  def failsOnJulkaisuValidation(e: E, path: String, expected: ErrorMessage): Assertion =
    failsOnJulkaisuValidation(e, (path, expected))

  def failsOnJulkaisuValidation( e: E, expected: (String, ErrorMessage), moreExpected: (String, ErrorMessage)*): Assertion =
    failsOnJulkaisuValidation(e, (expected +: moreExpected).map { case (path, value) => ValidationError(s"$pathPrefix.$path", value) })

  def failsOnJulkaisuValidation( e: E, expected: Seq[ValidationError]): Assertion =
    e.validateOnJulkaisu(pathPrefix) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs expected
    }
}
