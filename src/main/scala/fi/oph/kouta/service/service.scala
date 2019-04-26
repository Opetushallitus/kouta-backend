package fi.oph.kouta.service

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.validation.Validatable

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e:E, f:(E) => R) = e.validate() match {
    case Right(_) => f(e)
    case Left(list) => throw new KoutaValidationException(list)
  }
}

case class KoutaValidationException(errorMessages:List[String]) extends RuntimeException

trait AuthorizationService {

  def withAuthorizedChildAndParentOrganizationOids[R](oid:OrganisaatioOid, f: Seq[OrganisaatioOid] => R): R =
    OrganisaatioClient.getAllParentAndChildOidsFlat(oid) match {
      case oids if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
      case oids => f(oids)
    }

  def withAuthorizedChildOrganizationOids[R](oid:OrganisaatioOid, f: Seq[OrganisaatioOid] => R): R =
    OrganisaatioClient.getAllChildOidsFlat(oid) match {
      case oids if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
      case oids => f(oids)
    }
}

case class OrganizationAuthorizationFailedException(oid:OrganisaatioOid) extends RuntimeException
