package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Julkaisutila, Sv}
import fi.oph.kouta.service.{KoutaValidationException, ValidatingService}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, equal}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}

import scala.util.{Failure, Try}

abstract class BaseValidationSpec[E <: Validatable] extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {

  def validator: ValidatingService[E] = null

  override def afterEach = {
    super.afterEach()
    reset()
  }

  def passValidation(e: E): Unit          = validator.withValidation(e, None)(e => e)
  def passValidation(e: E, oldE: E): Unit = validator.withValidation(e, Some(oldE))(e => e)

  def failValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failValidation(e, Seq(ValidationError(path, message)))
  def failStageChangeValidation(e: E, oldE: E, message: ErrorMessage): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs Seq(ValidationError("tila", message))
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  def failStageChangeValidation(e: E, oldE: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs expected
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  def failValidation(e: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, None)(e => e)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  def failModifyValidation(e: E, oldE: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  def passesValidation(e: E): Assertion = e.validate() should equal(NoErrors)

  def failsValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failsValidation(e, ValidationError(path, message))

  def failsValidation(e: E, expected: ValidationError, moreExpected: ValidationError*): Assertion =
    failsValidation(e, expected +: moreExpected)

  def failsValidation(e: E, expected: Seq[ValidationError]): Assertion = e.validate() match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors   => errors should contain theSameElementsAs expected
  }

  def passesOnJulkaisuValidation(e: E): Assertion = e.validateOnJulkaisu() shouldEqual NoErrors

  def failsOnJulkaisuValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failsOnJulkaisuValidation(e, List(ValidationError(path, message)))

  def failsOnJulkaisuValidation(e: E, expected: Seq[ValidationError]): Assertion =
    e.validateOnJulkaisu() match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }
}

abstract class SubEntityValidationSpec[E <: ValidatableSubEntity]
    extends AnyFlatSpec
    with BeforeAndAfterEach
    with MockitoSugar {

  val DefaultKielivalinta = Seq(Fi, Sv)
  val pathPrefix          = "path"

  def failsValidation(tila: Julkaisutila, e: E, path: String, expected: ErrorMessage): Assertion =
    failsValidation(tila, e, (path, expected))

  def failsValidation(
      tila: Julkaisutila,
      e: E,
      expected: (String, ErrorMessage),
      moreExpected: (String, ErrorMessage)*
  ): Assertion =
    failsValidation(
      tila,
      e,
      (expected +: moreExpected).map { case (path, error) => ValidationError(s"$pathPrefix.$path", error) }
    )

  def failsValidation(tila: Julkaisutila, e: E, expected: Seq[ValidationError]): Assertion =
    e.validate(tila, DefaultKielivalinta, pathPrefix) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  def passesValidation(tila: Julkaisutila, e: E): Assertion =
    e.validate(tila, DefaultKielivalinta, pathPrefix) shouldEqual NoErrors

  def passesOnJulkaisuValidation(e: E): Assertion = e.validateOnJulkaisu(pathPrefix) shouldEqual NoErrors

  def failsOnJulkaisuValidation(e: E, path: String, expected: ErrorMessage): Assertion =
    failsOnJulkaisuValidation(e, (path, expected))

  def failsOnJulkaisuValidation(
      e: E,
      expected: (String, ErrorMessage),
      moreExpected: (String, ErrorMessage)*
  ): Assertion =
    failsOnJulkaisuValidation(
      e,
      (expected +: moreExpected).map { case (path, error) => ValidationError(s"$pathPrefix.$path", error) }
    )

  def failsOnJulkaisuValidation(e: E, expected: Seq[ValidationError]): Assertion =
    e.validateOnJulkaisu(pathPrefix) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }
}
