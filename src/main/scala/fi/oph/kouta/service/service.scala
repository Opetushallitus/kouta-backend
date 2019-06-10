package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Perustiedot
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.Validatable
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.IterableView

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e:E, f: E => R): R = e.validate() match {
    case Right(_) => f(e)
    case Left(list) => throw KoutaValidationException(list)
  }
}

case class KoutaValidationException(errorMessages:List[String]) extends RuntimeException

trait AuthorizationService extends Logging {

  private lazy val rootOrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  protected val roleEntity: RoleEntity
  protected lazy val indexerRoles: Seq[Role] = Seq(Role.Indexer)

  @deprecated("Should use Authenticated instead of a single given oid", "2019-05-23")
  def withAuthorizedChildAndParentOrganizationOids[R](oid: OrganisaatioOid, f: Seq[OrganisaatioOid] => R): R =
    OrganisaatioClient.getAllParentAndChildOidsFlat(oid) match {
      case oids if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
      case oids                 => f(oids)
    }

  def authorizeGet[R <: Perustiedot](thingWithTime: Option[(R, Instant)])(implicit authenticated: Authenticated): Option[(R, Instant)] =
    thingWithTime.map {
      case (thing, lastModified) =>
        withAuthorizedChildOrganizationOids(roleEntity.readRoles) { authorizedOrganizations =>
          authorize(thing.organisaatioOid, authorizedOrganizations) {
            (thing, lastModified)
          }
        }
    }


  def authorizePut[R <: Perustiedot, I](thing: R)(f: => I)(implicit authenticated: Authenticated): I =
    withAuthorizedChildOrganizationOids(roleEntity.createRoles) { authorizedOrganizations =>
      authorize(thing.organisaatioOid, authorizedOrganizations) {
        f
      }
    }

  def authorizeUpdate[R <: Perustiedot, I](maybeExisting: => Option[(R, Instant)])(f: => I)(implicit authenticated: Authenticated): I = {
    withAuthorizedChildOrganizationOids(roleEntity.updateRoles) { authorizedOrganizations =>
      maybeExisting match {
        case None => throw new NoSuchElementException()
        case Some(existing) =>
          authorize(existing._1.organisaatioOid, authorizedOrganizations) {
            f
          }
      }
    }
  }

  /**
    * Checks if the the authenticated user has access to the given organization, then calls f with a sequence of descendants of that organization.
    * @param oid Organisaatio oid of the organization
    * @param roles The user needs to have access to the organization with at least one of these roles
    * @param f Function to call with the descendant organizations
    * @param authenticated Authentication of the user
    * @tparam R Return type of f
    * @return Whatever f returns
    */
  def withAuthorizedChildOrganizationOids[R](oid: OrganisaatioOid, roles: Seq[Role])(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedChildOrganizationOids(roles) { children =>
      authorize(oid, children) {
        OrganisaatioClient.getAllChildOidsFlat(oid) match {
          case oids if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
          case oids => f(oids)
        }
      }
    }

  def withAuthorizedChildOrganizationOids[R](roles: Seq[Role])(f: IterableView[OrganisaatioOid, Iterable[_]] => R)(implicit authenticated: Authenticated): R =
    orgsForRoles(roles) match {
      case oids if oids.isEmpty => throw RoleAuthorizationFailedException(roles, authenticated.session.roles)
      case oids                 => f(oids.view ++ lazyFlatChildren(oids))
    }

  def authorize[R](allowedOrganization: OrganisaatioOid, authorizedOrganizations: Iterable[OrganisaatioOid])(r: R): R =
    authorizeRootOrAny(Set(allowedOrganization), authorizedOrganizations)(r)

/*
  def authorizeAny[R](allowedOrganizations: Set[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid])(r: R): R =
    if (authorizedOrganizations.exists(allowedOrganizations.contains)) r
    else throw OrganizationAuthorizationFailedException(allowedOrganizations)
  */

  def authorizeRootOrAny[R](allowedOrganizations: Set[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid])(r: R): R =
    if (authorizedOrganizations.exists(authorized => authorized == rootOrganisaatioOid || allowedOrganizations.contains(authorized))) r
    else throw OrganizationAuthorizationFailedException(allowedOrganizations)

  /*
  def authorizeRootOrAll[R](requiredOrganizations: Set[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid])(r: => R): R = {
    if (authorizedOrganizations.exists(_ == rootOrganisaatioOid)) r
    else authorizeAll(requiredOrganizations, authorizedOrganizations.toSet)(r)
  }

  def authorizeAll[R](requiredOrganizations: Set[OrganisaatioOid], authorizedOrganizations: Set[OrganisaatioOid])(r: => R): R = {
    if (requiredOrganizations.diff(authorizedOrganizations).isEmpty) r
    else throw OrganizationAuthorizationFailedException(requiredOrganizations)
  }
       */

  private def orgsForRoles(roles: Seq[Role])(implicit authenticated: Authenticated): Set[OrganisaatioOid] =
    roles.flatMap { role =>
      authenticated.session.roleMap.get(role)
    }.fold(Set())(_ union _)

  private def lazyFlatChildren(orgs: Set[OrganisaatioOid]): IterableView[OrganisaatioOid, Iterable[_]] =
    orgs.view.flatMap(oid => OrganisaatioClient.getAllChildOidsFlat(oid).toSet)

/*
  def authorize(acceptedRoles: Role*)(implicit authenticated: Authenticated): Unit = {
    if (!authenticated.session.hasAnyRole(acceptedRoles.toSet)) {
      throw RoleAuthorizationFailedException(acceptedRoles, authenticated.session.roles)
    }
  }
*/

  def hasRootAccess(roles: Seq[Role])(implicit authenticated: Authenticated): Boolean =
    roles.exists { role =>
      authenticated.session.roleMap.get(role).exists(_.contains(rootOrganisaatioOid))
    }

  def withRootAccess[R](roles: Seq[Role])(f: => R)(implicit authenticated: Authenticated): R =
    if (hasRootAccess(roles)) {
      f
    } else {
      throw OrganizationAuthorizationFailedException()
    }
}

case class OrganizationAuthorizationFailedException(oids: Iterable[OrganisaatioOid]) extends RuntimeException

object OrganizationAuthorizationFailedException {
  def apply(oid: OrganisaatioOid): OrganizationAuthorizationFailedException = OrganizationAuthorizationFailedException(Seq(oid))

  def apply(): OrganizationAuthorizationFailedException = OrganizationAuthorizationFailedException(Seq.empty)
}

case class RoleAuthorizationFailedException(acceptedRoles: Seq[Role], existingRoles: Iterable[Role])
  extends RuntimeException(s"Authorization failed, missing role. Accepted roles: ${acceptedRoles.map(_.name).mkString(",")}. Existing roles: ${existingRoles.map(_.name).mkString(",")}.")
