package fi.oph.kouta.service

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.validation.Validatable

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e:E, f:(E) => R) = e.validate() match {
    case Right(_) => f(e)
    case Left(list) => throw new KoutaValidationException(list)
  }
}

case class KoutaValidationException(errorMessages:List[String]) extends RuntimeException

trait AuthorizationService {

  def withAuthorizedOrganizationOids[R](oid:String, f:(Seq[String]) => R) =
    OrganisaatioClient.getAllParentAndChildOidsFlat(oid) match {
      case oids if oids.isEmpty => throw new KoutaAuthorizationFailedException(oid)
      case oids => f(oids)
    }
}

case class KoutaAuthorizationFailedException(oid:String) extends RuntimeException