package fi.oph.kouta.validation

import org.scalatest.DoNotDiscover
import org.scalatra.test.scalatest.ScalatraFlatSpec

@DoNotDiscover
abstract class BaseValidationSpec[E <: Validatable] extends ScalatraFlatSpec {

  def assertRight(e:E) = e.validate() should equal (Right())

  def assertLeft(e:E, expected:String) = e.validate() match {
    case Right(_) => assert(false, "Expecting left, got right")
    case Left(s) => s should equal (expected)
  }
}
