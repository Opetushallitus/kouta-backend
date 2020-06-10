package fi.oph.kouta.service

import fi.oph.kouta.client.{OrganisaatioClient, OrganisaatioOidsAndOppilaitostyypitFlat}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.security.{Authorizable, Role}
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.IterableView


trait AuthorizationService extends Logging {

  def organisaatioClient: OrganisaatioClient

  protected lazy val indexerRoles: Seq[Role] = Seq(Role.Indexer)

  type OrganisaatioOidsAndOppilaitostyypitFlatView = IterableView[OrganisaatioOidsAndOppilaitostyypitFlat, Iterable[_]]
  type AuthorizationRule = (Authorizable, Seq[OrganisaatioOid], OrganisaatioOidsAndOppilaitostyypitFlat) => Boolean

  case class AuthorizationRules(requiredRoles: Seq[Role],
                                allowAccessToParentOrganizations: Boolean = false,
                                overridingAuthorizationRules: Seq[AuthorizationRule] = Seq(),
                                additionalAuthorizedOrganisaatioOids: Seq[OrganisaatioOid] = Seq())

  private def isAuthorized(entity: Authorizable,
                           organisaatioOids: Seq[OrganisaatioOid],
                           oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat) =
    oidsAndOppilaitostyypit._1.exists(x => organisaatioOids.exists(_ == x))

  val DefaultAuthorizationRule: AuthorizationRule = isAuthorized

  def ifAuthorized[R](authorizable: Authorizable,
                      authorizationRules: AuthorizationRules)
                     (f: => R)
                     (implicit authenticated: Authenticated): R = {

    val AuthorizationRules(requiredRoles, allowAccessToParentOrganizations, overridingAuthorizationRules, additionalAuthorizedOrganisaatioOids) = authorizationRules
    val organisaatioOids = Seq(authorizable.organisaatioOid) ++ additionalAuthorizedOrganisaatioOids

    def authorized(oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView): Boolean = overridingAuthorizationRules match {
      case Nil   => oidsAndOppilaitostyypit.exists(DefaultAuthorizationRule(authorizable, organisaatioOids, _))
      case rules => oidsAndOppilaitostyypit.exists(oo => rules.exists(_(authorizable, organisaatioOids, oo)))
    }

    val authorizedOrgs = authenticated.session.getOrganizationsForRoles(requiredRoles)
    def allAuthorizedOidsAndOppilaitostyypit(): OrganisaatioOidsAndOppilaitostyypitFlatView =
      if(allowAccessToParentOrganizations) lazyFlatChildrenAndParents(authorizedOrgs) else lazyFlatChildren(authorizedOrgs)

    if(authorizedOrgs.isEmpty) {
      throw RoleAuthorizationFailedException(requiredRoles, authenticated.session.roles)
    } else if(authorizedOrgs.contains(RootOrganisaatioOid) || authorized(allAuthorizedOidsAndOppilaitostyypit())) {
      f
    } else {
      throw OrganizationAuthorizationFailedException(organisaatioOids, authorizedOrgs)
    }
  }

  /** Checks if the the authenticated user has access to the given organization, then calls f with a sequence of descendants of that organization. */
  def withAuthorizedChildOrganizationOids[R](oid: OrganisaatioOid, roles: Seq[Role])(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOids(oid, AuthorizationRules(roles)){f}

  def withAuthorizedChildOrganizationOidsAndOppilaitostyypit[R](oid: OrganisaatioOid, roles: Seq[Role])(f: OrganisaatioOidsAndOppilaitostyypitFlat => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, AuthorizationRules(roles)){f}

  def withAuthorizedOrganizationOidsAndOppilaitostyypit[R](oid: OrganisaatioOid, authorizationRules: AuthorizationRules)(f: OrganisaatioOidsAndOppilaitostyypitFlat => R)(implicit authenticated: Authenticated): R =
    ifAuthorized(new Authorizable {
      override val organisaatioOid: OrganisaatioOid = oid
    }, authorizationRules) {

      val requestedOrganizations =
        if( authorizationRules.allowAccessToParentOrganizations ) {
          organisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid)
        } else {
          organisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid)
        }

      requestedOrganizations match {
        case (oids, _) if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
        case (oids, t) => f(oids, t)
      }
    }

  def withAuthorizedOrganizationOids[R](oid: OrganisaatioOid, authorizationRules: AuthorizationRules)(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, authorizationRules) { case (oids, _) =>
      f(oids)
    }

  protected def lazyFlatChildrenAndParents(orgs: Set[OrganisaatioOid]): OrganisaatioOidsAndOppilaitostyypitFlatView =
    orgs.view.map(oid => organisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid))

  protected def lazyFlatChildren(orgs: Set[OrganisaatioOid]): OrganisaatioOidsAndOppilaitostyypitFlatView =
    orgs.view.map(oid => organisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid))

  def hasRootAccess(roles: Seq[Role])(implicit authenticated: Authenticated): Boolean =
    roles.exists(role => authenticated.session.roleMap.get(role).exists(_.contains(RootOrganisaatioOid)))

  def withRootAccess[R](roles: Seq[Role])(f: => R)(implicit authenticated: Authenticated): R =
    if (hasRootAccess(roles)) {
      f
    } else {
      throw OrganizationAuthorizationFailedException(Seq(RootOrganisaatioOid), Seq.empty)
    }
}
