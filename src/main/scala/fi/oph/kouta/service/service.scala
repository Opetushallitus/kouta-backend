package fi.oph.kouta.service

import fi.oph.kouta.validation.{Validatable}

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e:E, f:(E) => R) = e.validate() match {
    case Right(_) => f(e)
    case Left(s) => throw new IllegalArgumentException(s)
  }
}
