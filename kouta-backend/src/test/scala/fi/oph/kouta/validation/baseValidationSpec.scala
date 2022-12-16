package fi.oph.kouta.validation

import fi.oph.kouta.domain.{En, Fi, Julkaistu, Julkaisutila, Sv, Tallennettu}
import fi.oph.kouta.service.{KoutaValidationException, ValidatingService, ValidatingSubService}
import fi.oph.kouta.validation.Validations.invalidKielistetty
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, equal}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}

import scala.util.{Failure, Success, Try}

trait ValidationConstants {
  val vainSuomeksi  = Map(Fi -> "vain suomeksi", Sv -> "")
  val fullKielistetty      = Map(Fi -> "suomeksi", Sv -> "på svenska")
  val kielistettyWoSvenskaError = invalidKielistetty(Seq(Sv))
  val defaultName     = Map(Fi -> "nimi", Sv -> "nimi sv", En -> "nimi en")
  val defaultCtx = ValidationContext(Julkaistu, Seq(Fi, Sv), CrudOperations.create)
}

abstract class BaseServiceValidationSpec[E <: Validatable] extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar with ValidationConstants {

  def validator: ValidatingService[E] = null

  def passesValidation(e: E): Unit          = validator.withValidation(e, None)(e => e)
  def passesValidation(e: E, oldE: E): Unit = validator.withValidation(e, Some(oldE))(e => e)

  def failsValidation(e: E, path: String, message: ErrorMessage): Assertion =
    failsValidation(e, Seq(ValidationError(path, message)))
  def failsStageChangeValidation(e: E, oldE: E, message: ErrorMessage): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs Seq(ValidationError("tila", message))
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  def failsStageChangeValidation(e: E, oldE: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs expected
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  def failsValidation(e: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, None)(e => e)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  def failsModifyValidation(e: E, oldE: E, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(e, Some(oldE))(e => e)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }
}

abstract class BaseSubServiceValidationSpec[E <: Validatable] extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar with ValidationConstants {
  def validator: ValidatingSubService[E]

  def passesValidation(e: E, vCtx: ValidationContext = defaultCtx): Unit = validator.validate(e, None, vCtx)
  def passesModifyValidation(e: E, oldE: E, vCtx: ValidationContext = defaultCtx): Unit = validator.validate(e, Some(oldE), vCtx)

  def failsSingleValidation(e: E, path: String, message: ErrorMessage, vCtx: ValidationContext = defaultCtx): Assertion =
    failsValidation(e, Seq(ValidationError(path, message)), vCtx)

  def failsValidation(e: E, expected: Seq[ValidationError], vCtx: ValidationContext = defaultCtx): Assertion =
    validator.validate(e, None, vCtx) should contain theSameElementsAs(expected)

  def failsModifyValidation(e: E, oldE: E, expected: Seq[ValidationError], vCtx: ValidationContext = ValidationContext(Tallennettu, Seq(Fi, Sv), CrudOperations.update)): Assertion =
    validator.validate(e, Some(oldE), vCtx) should contain theSameElementsAs(expected)
}