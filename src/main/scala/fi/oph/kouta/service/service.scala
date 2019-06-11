package fi.oph.kouta.service

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.Validatable
import fi.vm.sade.utils.slf4j.Logging

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e:E, f: E => R): R = e.validate() match {
    case Right(_) => f(e)
    case Left(list) => throw KoutaValidationException(list)
  }
}

case class KoutaValidationException(errorMessages:List[String]) extends RuntimeException

trait AuthorizationService extends Logging {

  private lazy val rootOrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

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

  def authorize(role: Role, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Unit =
    authorizeAll(role, Seq(organisaatioOid))

  def authorizeAll(role: Role, organisaatioOids: Iterable[OrganisaatioOid])(implicit authenticated: Authenticated): Unit = {
    withChildren(role) { children =>
      organisaatioOids
        .find(oid => !children.contains(oid))
        .foreach(oid => throw OrganizationAuthorizationFailedException(oid))
    }
  }

  def authorizeAny(role: Role, organisaatioOids: Iterable[OrganisaatioOid])(implicit authenticated: Authenticated): Unit =
    withChildren(role) { children =>
      organisaatioOids
        .find(children.contains)
        .getOrElse(throw OrganizationAuthorizationFailedException(organisaatioOids))
    }

  private def withChildren(role: Role)(authorization: Set[OrganisaatioOid] => Unit)(implicit authenticated: Authenticated): Unit =
    orgsForRole(role, authenticated) match {
      case oids if oids.contains(rootOrganisaatioOid) =>
      case oids =>
        val childOids = oids ++ lazyFlatChildren(oids)

        authorization(childOids)
    }

  private def orgsForRole(role: Role, authenticated: Authenticated): Set[OrganisaatioOid] =
    authenticated.session.roleMap
      .getOrElse(role, throw RoleAuthorizationFailedException(Seq(role), authenticated.session.roles))
      .flatten

  private def lazyFlatChildren(orgs: Set[OrganisaatioOid]): Set[OrganisaatioOid] =
    orgs.view
      .flatMap(oid => OrganisaatioClient.getAllChildOidsFlat(oid).toSet)
      .toSet

  def authorize(acceptedRoles: Role*)(implicit authenticated: Authenticated): Unit = {
    if (!authenticated.session.hasAnyRole(acceptedRoles.toSet)) {
      throw RoleAuthorizationFailedException(acceptedRoles, authenticated.session.roles)
    }
  }
}

case class OrganizationAuthorizationFailedException(oids: Iterable[OrganisaatioOid]) extends RuntimeException

object OrganizationAuthorizationFailedException {
  def apply(oid: OrganisaatioOid): OrganizationAuthorizationFailedException = OrganizationAuthorizationFailedException(Seq(oid))
}

case class RoleAuthorizationFailedException(acceptedRoles: Seq[Role], existingRoles: Iterable[Role])
  extends RuntimeException(s"Authorization failed, missing role. Accepted roles: ${acceptedRoles.map(_.name).mkString(",")}. Existing roles: ${existingRoles.map(_.name).mkString(",")}.")
