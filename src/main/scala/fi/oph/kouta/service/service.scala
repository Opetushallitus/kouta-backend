package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.client.OrganisaatioClient.OrganisaatioOidsAndOppilaitostyypitFlat
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Julkaistu
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.{Authorizable, AuthorizableEntity, AuthorizableMaybeJulkinen, Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.IterableView

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e: E, oldE: Option[E], f: E => R): R = {
    val errors = if (!oldE.contains(Julkaistu) && e.tila == Julkaistu) {
      e.validate() ++ e.validateOnJulkaisu()
    } else {
      e.validate()
    }

    errors match {
      case NoErrors => f(e)
      case errors => throw KoutaValidationException(errors)
    }
  }
}

case class KoutaValidationException(errorMessages: IsValid) extends RuntimeException

trait RoleEntityAuthorizationService[E <: AuthorizableEntity[E]] extends AuthorizationService {
  protected val roleEntity: RoleEntity

  lazy val ophOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  private def authorizeJulkinen(entity: AuthorizableMaybeJulkinen[E], organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat): Boolean = {

    def isCorrectKoulutustyyppi(): Boolean = oidsAndOppilaitostyypit._2.contains(entity.koulutustyyppi)

    DefaultAuthorizationRule(entity, organisaatioOids, oidsAndOppilaitostyypit) match {
      case false => entity.julkinen && isCorrectKoulutustyyppi()
      case true if entity.organisaatioOid == ophOid => isCorrectKoulutustyyppi()
      case _ => true
    }
  }

  val AuthorizationRuleForJulkinen: AuthorizationRule = (entity: Authorizable, organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat) => entity match {
    case e: AuthorizableMaybeJulkinen[E] => authorizeJulkinen(e, organisaatioOids, oidsAndOppilaitostyypit)
    case _ => false
  }

  def authorizeGet(entityWithTime: Option[(E, Instant)],
                   authorizationRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles))
                  (implicit authenticated: Authenticated): Option[(E, Instant)] =
    entityWithTime.map {
      case (e, t) => ifAuthorized(e, authorizationRules) {
        (e, t)
      }
    }

  private def withUpdatedMuokkaaja(entity: E)(implicit authenticated: Authenticated) =
    entity.withMuokkaaja(UserOid(authenticated.session.personOid))

  def authorizePut[I](entity: E,
                      authorizationRules: AuthorizationRules = AuthorizationRules(roleEntity.createRoles))
                     (f: E => I)(implicit authenticated: Authenticated): I =
    ifAuthorized(entity, authorizationRules) {
      f(withUpdatedMuokkaaja(entity))
    }

  def authorizeUpdate[I](oldEntity: => Option[(E, Instant)],
                         updatedEntity: E,
                         authorizationRules: AuthorizationRules = AuthorizationRules(roleEntity.updateRoles))
                        (f: (E, E) => I)(implicit authenticated: Authenticated): I =
    oldEntity match {
      case None => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
      case Some((e, _)) => ifAuthorized(e, authorizationRules) {
        f(e, withUpdatedMuokkaaja(updatedEntity))
      }
    }

  def authorizeUpdate[I](oldEntity: => Option[(E, Instant)],
                         updatedEntity: E,
                         authorizationRules: List[AuthorizationRules])
                        (f: (E, E) => I)(implicit authenticated: Authenticated): I = {
    def checkRules(rule: AuthorizationRules, rules: List[AuthorizationRules], e: E): I = {
      if (rules.isEmpty) {
        ifAuthorized(e, rule)(f(e, withUpdatedMuokkaaja(updatedEntity)))
      } else {
        ifAuthorized(e, rule)(checkRules(rules.head, rules.tail, withUpdatedMuokkaaja(e)))
      }
    }

    oldEntity match {
      case None => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
      case Some((e, _)) =>
        authorizationRules match {
          case Nil => throw EntityNotFoundException(s"Ei päivitettävää")
          case rule :: rules => checkRules(rule, rules, e)
        }
    }
  }

}

trait AuthorizationService extends Logging {

  private lazy val rootOrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

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
    } else if(authorizedOrgs.contains(rootOrganisaatioOid) || authorized(allAuthorizedOidsAndOppilaitostyypit())) {
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
          OrganisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid)
        } else {
          OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid)
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
    orgs.view.map(oid => OrganisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid))

  protected def lazyFlatChildren(orgs: Set[OrganisaatioOid]): OrganisaatioOidsAndOppilaitostyypitFlatView =
    orgs.view.map(oid => OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid))

  def hasRootAccess(roles: Seq[Role])(implicit authenticated: Authenticated): Boolean =
    roles.exists(role => authenticated.session.roleMap.get(role).exists(_.contains(rootOrganisaatioOid)))

  def withRootAccess[R](roles: Seq[Role])(f: => R)(implicit authenticated: Authenticated): R =
    if (hasRootAccess(roles)) {
      f
    } else {
      throw OrganizationAuthorizationFailedException(Seq(rootOrganisaatioOid), Seq.empty)
    }
}

case class OrganizationAuthorizationFailedException(message:String)
extends RuntimeException(message)

object OrganizationAuthorizationFailedException {
def apply(allowedOrganizationOids: Iterable[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid]): OrganizationAuthorizationFailedException =
  OrganizationAuthorizationFailedException(s"Authorization failed, missing organization. " +
    s"User asked right to these organizations: ${allowedOrganizationOids.map(_.s).mkString(",")}. " +
    s"Instead, authorized organizations are: ${authorizedOrganizations.map(_.s).mkString(",")}.")

def apply(missingOrganizationOid: OrganisaatioOid): OrganizationAuthorizationFailedException =
  OrganizationAuthorizationFailedException(s"Authorization failed, unknown organization oid ${missingOrganizationOid.s}")
}

case class RoleAuthorizationFailedException(acceptedRoles: Seq[Role], existingRoles: Iterable[Role])
extends RuntimeException(s"Authorization failed, missing role. Accepted roles: ${acceptedRoles.map(_.name).mkString(",")}. Existing roles: ${existingRoles.map(_.name).mkString(",")}.")
