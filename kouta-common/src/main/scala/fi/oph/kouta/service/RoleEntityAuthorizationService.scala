package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioOidsAndOppilaitostyypitFlat
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, UserOid}
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}

trait RoleEntityAuthorizationService[E <: AuthorizableEntity[E]] extends AuthorizationService {
  protected val roleEntity: RoleEntity

  private def authorizeJulkinen(entity: AuthorizableMaybeJulkinen[E],
                                organisaatioOids: Seq[OrganisaatioOid],
                                oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat): Boolean = {

    def isCorrectKoulutustyyppi(): Boolean = oidsAndOppilaitostyypit._2.contains(entity.koulutustyyppi)

    DefaultAuthorizationRule(entity, organisaatioOids, oidsAndOppilaitostyypit) match {
      case false => entity.julkinen && isCorrectKoulutustyyppi()
      case true if entity.organisaatioOid == RootOrganisaatioOid => isCorrectKoulutustyyppi()
      case _ => true
    }
  }

  private def authorizeKoulutustyyppi(entity: AuthorizableByKoulutustyyppi[E],
                                      organisaatioOids: Seq[OrganisaatioOid],
                                      oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat): Boolean = {

    def isCorrectKoulutustyyppi(): Boolean = oidsAndOppilaitostyypit._2.contains(entity.koulutustyyppi)

    DefaultAuthorizationRule(entity, organisaatioOids, oidsAndOppilaitostyypit) match {
      case _ => isCorrectKoulutustyyppi()
    }
  }

  val AuthorizationRuleForJulkinen: AuthorizationRule =
    (entity: Authorizable, organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat) => entity match {
      case e: AuthorizableMaybeJulkinen[E] => authorizeJulkinen(e, organisaatioOids, oidsAndOppilaitostyypit)
      case _ => throw new RuntimeException("AuthorizationRuleForJulkinen kutsuttu entitylla, joka ei ole AuthorizableMaybeJulkinen")
    }

  val authorizationRuleByKoulutustyyppi: AuthorizationRule =
    (entity: Authorizable, organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat) => entity match {
      case e: AuthorizableByKoulutustyyppi[E] => authorizeKoulutustyyppi(e, organisaatioOids, oidsAndOppilaitostyypit)
      case _ => throw new RuntimeException("authorizationRuleByKoulutustyyppi kutsuttu entitylla, joka ei ole AuthorizableByKoulutustyyppi")
    }


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
