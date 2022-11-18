package fi.oph.kouta.service

import fi.oph.kouta.client.OrganisaatioOidsAndOppilaitostyypitFlat
import fi.oph.kouta.domain.{Enum, EnumType, Koulutustyyppi}
import fi.oph.kouta.domain.Koulutustyyppi.isKorkeakoulu
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.security.{Authorizable, AuthorizableMaybeJulkinen, Role}
import fi.oph.kouta.service.AuthorizedToAnyOfGivenOrganizationsRule.authorizedOrganisations
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.IterableView

sealed trait OrganizationsAuthorizationMode extends EnumType

object OrganizationsAuthorizationMode extends Enum[OrganizationsAuthorizationMode] {
  override def name: String = "organizations authorization mode"
  def values = List(Any, All)
}

case object Any extends OrganizationsAuthorizationMode { val name = "rights needed to any"}
case object All extends OrganizationsAuthorizationMode { val name = "rights needed to all"}

trait AuthorizationRule {
  def isAuthorized(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean
  def organizationsAuthorizationMode(): OrganizationsAuthorizationMode
  def authorizedOrganisations(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid]): Seq[OrganisaatioOid] =
    additionalOrganisaatioOids :+ entity.organisaatioOid
  def isMemberOfOwnerOrganization(entity: Authorizable, usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView): Boolean =
    usersOidsAndOppilaitostyypit.exists(_._1.exists(_ == entity.organisaatioOid))
}

case object AuthorizedToAnyOfGivenOrganizationsRule extends AuthorizationRule {

  override def isAuthorized(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean = {
    val organisaatiot = authorizedOrganisations(entity, additionalOrganisaatioOids)
    usersOidsAndOppilaitostyypit.exists(_._1.exists(x => organisaatiot.exists(_ == x)))
  }

  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = Any
}

case object AuthorizedToAllOfGivenOrganizationsRule extends AuthorizationRule with Logging {

  override def isAuthorized(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean = {
    val organisaatiot = authorizedOrganisations(entity, additionalOrganisaatioOids)
    var theList: Seq[Seq[OrganisaatioOid]] = Seq()
    val iter = usersOidsAndOppilaitostyypit.map(_._1).iterator
    while(iter.hasNext) theList = theList :+ iter.next()
    logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!! all users organisations " + theList.flatten)
    organisaatiot.forall(org => usersOidsAndOppilaitostyypit.exists(_._1.exists(_ == org)))
  }
  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = All
}

case class AuthorizationRules(requiredRoles: Seq[Role],
                              allowAccessToParentOrganizations: Boolean = false,
                              overridingAuthorizationRule: Option[AuthorizationRule] = None,
                              additionalAuthorizedOrganisaatioOids: Seq[OrganisaatioOid] = Seq())

trait AuthorizationService extends Logging {

  def organisaatioService: OrganisaatioService

  protected lazy val indexerRoles: Seq[Role] = Seq(Role.Indexer)



  def ifAuthorized[R](authorizable: Authorizable,
                      authorizationRules: AuthorizationRules)
                     (f: => R)
                     (implicit authenticated: Authenticated): R = {

    val AuthorizationRules(requiredRoles, allowAccessToParentOrganizations, overridingAuthorizationRule, additionalAuthorizedOrganisaatioOids) =
      authorizationRules
    val rule = overridingAuthorizationRule match {
      case Some(rule) => rule
      case _ => AuthorizedToAnyOfGivenOrganizationsRule
    }

    val authorizedOrgs = authenticated.session.getOrganizationsForRoles(requiredRoles)

    def authorized(oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView): Boolean =
      rule.isAuthorized(authorizable, additionalAuthorizedOrganisaatioOids, oidsAndOppilaitostyypit,
        authorizedOrgs.map(org => organisaatioService.getAllChildOidsFlat(org)).flatten.contains(authorizable.organisaatioOid))


    def allAuthorizedOidsAndOppilaitostyypit(): OrganisaatioOidsAndOppilaitostyypitFlatView =
      if(allowAccessToParentOrganizations) lazyFlatChildrenAndParents(authorizedOrgs) else lazyFlatChildren(authorizedOrgs)

    if(authorizedOrgs.isEmpty) {
      throw RoleAuthorizationFailedException(requiredRoles, authenticated.session.roles)
    } else if(authorizedOrgs.contains(RootOrganisaatioOid) || authorized(allAuthorizedOidsAndOppilaitostyypit())) {
      f
    } else {
      throw OrganizationAuthorizationFailedException(rule.authorizedOrganisations(authorizable, additionalAuthorizedOrganisaatioOids).distinct, authorizedOrgs, rule.organizationsAuthorizationMode())
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
          organisaatioService.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid)
        } else {
          organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(oid)
        }

      requestedOrganizations match {
        case (oids, _) if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
        case (oids, t) => f(oids, t)
      }
    }

  def withAuthorizedOrganizationOidsAndRelevantKoulutustyyppis[R](oid: OrganisaatioOid, authorizationRules: AuthorizationRules)(f: OrganisaatioOidsAndOppilaitostyypitFlat => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, authorizationRules)(oidsAndTyypit =>
      f(oidsAndTyypit))

  def withAuthorizedOrganizationOids[R](oid: OrganisaatioOid, authorizationRules: AuthorizationRules)(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, authorizationRules) { case (oids, _) =>
      f(oids)
    }

  protected def lazyFlatChildrenAndParents(orgs: Set[OrganisaatioOid]): OrganisaatioOidsAndOppilaitostyypitFlatView =
    orgs.view.map(oid => organisaatioService.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid))

  protected def lazyFlatChildren(orgs: Set[OrganisaatioOid]): OrganisaatioOidsAndOppilaitostyypitFlatView =
    orgs.view.map(oid => organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(oid))

  def hasRootAccess(roles: Seq[Role])(implicit authenticated: Authenticated): Boolean =
    roles.exists(role => authenticated.session.roleMap.get(role).exists(_.contains(RootOrganisaatioOid)))

  def withRootAccess[R](roles: Seq[Role])(f: => R)(implicit authenticated: Authenticated): R =
    if (hasRootAccess(roles)) {
      f
    } else {
      throw OrganizationAuthorizationFailedException(Seq(RootOrganisaatioOid), Seq.empty)
    }
}
