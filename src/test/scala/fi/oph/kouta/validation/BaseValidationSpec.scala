package fi.oph.kouta.validation

import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def assertRight(e:E) = e.validate() should equal (Right(()))

  def assertLeft(e:E, expected:List[String]):Assertion = e.validate() match {
    case Right(_) => assert(false, "Expecting left, got right")
    case Left(s) => s should contain theSameElementsAs(expected)
  }

  def assertLeft(e:E, expected:String):Assertion = assertLeft(e, List(expected))
}
