package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Julkaisutila, Sv}
import fi.oph.kouta.service.{KoutaValidationException, ValidatingService}
import fi.oph.kouta.validation.Validations.invalidKielistetty
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, equal}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}

import scala.util.{Failure, Try}

abstract class BaseServiceValidationSpec[E <: Validatable] extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {

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

  val vainSuomeksi  = Map(Fi -> "vain suomeksi", Sv -> "")
  val fullKielistetty      = Map(Fi -> "suomeksi", Sv -> "på svenska")
  val kielistettyWoSvenskaError = invalidKielistetty(Seq(Sv))
}