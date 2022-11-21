package fi.oph.kouta.service

import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, UserOid}
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}

import java.time.Instant

trait AuthorizationRuleForJulkinen extends AuthorizationRule {
  def isJulkinenAuthorized(entity: AuthorizableMaybeJulkinen[_], additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean

  def assertUserAuthorizedToKoulutustyyppi(entity: AuthorizableMaybeJulkinen[_], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView): Boolean = {
    if (!usersOidsAndOppilaitostyypit.exists(_._2.contains(entity.koulutustyyppi)))
      throw KoulutustyyppiAuthorizationFailedException(entity.koulutustyyppi, usersOidsAndOppilaitostyypit.map(_._2).toSeq.flatten.distinct)
    true
  }

  override def isAuthorized(authorizable: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean =
    authorizable match {
      case entity: AuthorizableMaybeJulkinen[_] =>
        isJulkinenAuthorized(entity, additionalOrganisaatioOids, usersOidsAndOppilaitostyypit, userIsMemberOfOwnerOrg)
      case _ => throw new RuntimeException("AuthorizationRuleForJulkinen kutsuttu entitylla, joka ei ole AuthorizableMaybeJulkinen")
    }
}

case object AuthorizationRuleByOrganizationAndKoulutustyyppi extends AuthorizationRuleForJulkinen {
  override def isJulkinenAuthorized(entity: AuthorizableMaybeJulkinen[_], additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean = {
    authorizedOrganisations(entity, additionalOrganisaatioOids).forall(org => usersOidsAndOppilaitostyypit.exists(_._1.exists(_ == org))) match {
      case true => assertUserAuthorizedToKoulutustyyppi(entity, usersOidsAndOppilaitostyypit)
      case _ => false
    }
  }

  override def authorizedOrganisations(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid]): Seq[OrganisaatioOid] =
    Seq(entity.organisaatioOid)

  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = All
}

case object AuthorizationRuleForReadJulkinen extends AuthorizationRuleForJulkinen {
  override def isJulkinenAuthorized(entity: AuthorizableMaybeJulkinen[_], additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean =
    AuthorizedToAnyOfGivenOrganizationsRule.isAuthorized(entity, additionalOrganisaatioOids, usersOidsAndOppilaitostyypit, userIsMemberOfOwnerOrg: Boolean) match {
      case false => entity.julkinen && assertUserAuthorizedToKoulutustyyppi(entity, usersOidsAndOppilaitostyypit)
      case true if entity.organisaatioOid == RootOrganisaatioOid => entity.julkinen && assertUserAuthorizedToKoulutustyyppi(entity, usersOidsAndOppilaitostyypit)
      case _ => true
    }

  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = Any
}

case object AuthorizationRuleForUpdateTarjoajat extends AuthorizationRuleForJulkinen {
  override def isJulkinenAuthorized(entity: AuthorizableMaybeJulkinen[_], additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean =
    authorizedOrganisations(entity, additionalOrganisaatioOids).forall(org => usersOidsAndOppilaitostyypit.exists(_._1.exists(_ == org))) match {
      case true if userIsMemberOfOwnerOrg => true
      case true if entity.julkinen => assertUserAuthorizedToKoulutustyyppi(entity, usersOidsAndOppilaitostyypit)
      case _ => false
    }

  override def authorizedOrganisations(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid]): Seq[OrganisaatioOid] =
    additionalOrganisaatioOids

  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = All
}

case object AuthorizationRuleByKoulutustyyppi extends AuthorizationRule {
  override def isAuthorized(authorizable: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid], usersOidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView, userIsMemberOfOwnerOrg: Boolean): Boolean =
    authorizable match {
      case entity: AuthorizableByKoulutustyyppi[_] =>
        if (!usersOidsAndOppilaitostyypit.exists(_._2.contains(entity.koulutustyyppi)))
          throw KoulutustyyppiAuthorizationFailedException(entity.koulutustyyppi, usersOidsAndOppilaitostyypit.map(_._2).toSeq.flatten.distinct)
        true
      case _ => throw new RuntimeException("authorizationRuleByKoulutustyyppi kutsuttu entitylla, joka ei ole AuthorizableByKoulutustyyppi")
    }
  override def organizationsAuthorizationMode(): OrganizationsAuthorizationMode = Any
  override def authorizedOrganisations(entity: Authorizable, additionalOrganisaatioOids: Seq[OrganisaatioOid]): Seq[OrganisaatioOid] = Seq()
}

trait RoleEntityAuthorizationService[E <: AuthorizableEntity[E]] extends AuthorizationService {
  protected val roleEntity: RoleEntity

  def authorizeGet(entityWithTime: Option[(E, Instant)])
                  (implicit authenticated: Authenticated): Option[(E, Instant)] =
    authorizeGet(entityWithTime, AuthorizationRules(roleEntity.readRoles))

  def authorizeGet(entityWithTime: Option[(E, Instant)], authorizationRules: AuthorizationRules)
                  (implicit authenticated: Authenticated): Option[(E, Instant)] =
    entityWithTime.map {
      case (e, t) => (authorizeGet(e, authorizationRules), t)
    }

  def authorizeGet(entity: E)(implicit authenticated: Authenticated): E =
    authorizeGet(entity, AuthorizationRules(roleEntity.readRoles))

  def authorizeGet(entity: E, authorizationRules: AuthorizationRules)
                  (implicit authenticated: Authenticated): E =
    ifAuthorized(entity, authorizationRules)(entity)

  private def withUpdatedMuokkaaja(entity: E)(implicit authenticated: Authenticated) =
    entity.withMuokkaaja(UserOid(authenticated.session.personOid))

  private def checkRuleList[I](rule: AuthorizationRules, rules: List[AuthorizationRules], entity: E) (f: => I)(implicit authenticated: Authenticated): I = {
    if (rules.isEmpty) {
      ifAuthorized(entity, rule)(f)
    } else {
      ifAuthorized(entity, rule)(checkRuleList(rules.head, rules.tail, entity)(f))
    }
  }

  def authorizePut[I](entity: E,
                      authorizationRules: AuthorizationRules = AuthorizationRules(roleEntity.createRoles))
                     (f: E => I)(implicit authenticated: Authenticated): I =
    ifAuthorized(entity, authorizationRules) {
      f(withUpdatedMuokkaaja(entity))
    }

  def authorizePut[I](entity: E,
                      authorizationRules: List[AuthorizationRules])
                     (f: E => I)(implicit authenticated: Authenticated): I =
    authorizationRules match {
      case Nil =>
        f(withUpdatedMuokkaaja(entity))
      case rules =>
        checkRuleList(rules.head, rules.tail, entity)(f(withUpdatedMuokkaaja(entity)))
    }

  def authorizeUpdate[I](oldEntity: => Option[(E, Instant)],
                         updatedEntity: E,
                         authorizationRules: AuthorizationRules = AuthorizationRules(roleEntity.updateRoles))
                        (f: (E, E) => I)(implicit authenticated: Authenticated): I =
    oldEntity match {
      case None => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
      case Some((e, _)) =>
        doAuthorizeUpdate(e, updatedEntity, List(authorizationRules))(f)
    }

  def authorizeUpdate[I](oldEntity: => Option[(E, Instant)],
                         updatedEntity: E,
                         authorizationRules: List[AuthorizationRules])
                        (f: (E, E) => I)(implicit authenticated: Authenticated): I = {
    oldEntity match {
      case None => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
      case Some((e, _)) =>
        doAuthorizeUpdate(e, updatedEntity, authorizationRules) (f)
    }
  }

  private def doAuthorizeUpdate[I](oldEntity: E,
                                   updatedEntity: E,
                                   authorizationRules: List[AuthorizationRules])
                                  (f: (E, E) => I)(implicit authenticated: Authenticated): I = {
    def organisaatioOidChanged() = oldEntity.organisaatioOid != updatedEntity.organisaatioOid
    def organisaatioOidChangeRules() =
      AuthorizationRules(
        roleEntity.updateRoles,
        additionalAuthorizedOrganisaatioOids = Seq(updatedEntity.organisaatioOid),
        overridingAuthorizationRule = Some(AuthorizedToAllOfGivenOrganizationsRule)
      )

    authorizationRules match {
      case Nil =>
        if (organisaatioOidChanged())
          checkRuleList(organisaatioOidChangeRules(), List(), oldEntity)(f(oldEntity, withUpdatedMuokkaaja(updatedEntity)))
        else throw EntityNotFoundException(s"Ei päivitettävää")
      case rules =>
        val rulesForUpdate =
          if (organisaatioOidChanged()) rules :+ organisaatioOidChangeRules()
          else rules
        checkRuleList(rulesForUpdate.head, rulesForUpdate.tail, oldEntity)(f(oldEntity, withUpdatedMuokkaaja(updatedEntity)))
    }
  }
}
