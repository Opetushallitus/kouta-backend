package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.client.OrganisaatioClient.OrganisaatioOidsAndOppilaitostyypitFlat
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{HasPrimaryId, HasTeemakuvaMetadata, Koulutustyyppi, MaybeJulkinen, TeemakuvaMetadata}
import fi.oph.kouta.indexing.S3Service
import fi.oph.kouta.security.{Authorizable, Role, RoleEntity}
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

trait TeemakuvaService[ID, T <: HasTeemakuvaMetadata[T, M] with HasPrimaryId[ID, T], M <: TeemakuvaMetadata[M]] extends Logging {
  val s3Service: S3Service

  def teemakuvaPrefix: String

  def checkTeemakuvaInPut(entity: T, put: T => ID, update: (T, Instant) => Boolean): ID =
    entity.metadata.flatMap(_.teemakuva) match {
      case Some(s3Service.tempUrl(filename)) =>
        val id = put(entity.withMetadata(entity.metadata.get.withTeemakuva(None)))
        val url = s3Service.copyImage(s3Service.getTempKey(filename), s"$teemakuvaPrefix/$id/$filename")
        update(entity.withPrimaryID(id).withMetadata(entity.metadata.get.withTeemakuva(Some(url))), Instant.now())
        s3Service.deleteImage(s3Service.getTempKey(filename))
        id
      case Some(s3Service.publicUrl(_)) =>
        put(entity)
      case None =>
        put(entity)
      case Some(other) =>
        logger.warn(s"Theme image outside the bucket: $other")
        put(entity)
    }

  def checkTeemakuvaInUpdate(entity: T, update: T => Boolean): Boolean =
    entity.metadata.flatMap(_.teemakuva) match {
      case Some(s3Service.tempUrl(filename)) =>
        val url = s3Service.copyImage(s3Service.getTempKey(filename), s"$teemakuvaPrefix/${entity.primaryId.get}/$filename")
        val changed = update(entity.withMetadata(entity.metadata.get.withTeemakuva(Some(url))))
        s3Service.deleteImage(s3Service.getTempKey(filename))
        changed
      case _ => update(entity)
    }
}

case class KoutaValidationException(errorMessages:List[String]) extends RuntimeException

trait RoleEntityAuthorizationService extends AuthorizationService {
  protected val roleEntity: RoleEntity

  lazy val ophOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  def getAuthorizationRuleForMaybeJulkinen(entity: MaybeJulkinen)(organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat): Boolean = {
    super.isAuthorized(organisaatioOids, oidsAndOppilaitostyypit) match {
      case false => entity.julkinen && oidsAndOppilaitostyypit._2.contains(entity.koulutustyyppi)
      case true if entity.organisaatioOid == ophOid => oidsAndOppilaitostyypit._2.contains(entity.koulutustyyppi)
      case _ => true
    }
  }

  def authorizeGet[E <: Authorizable](entityWithTime: Option[(E, Instant)],
                                      authorizationRules: AutorizationRules = AutorizationRules(roleEntity.readRoles))
                                     (implicit authenticated: Authenticated): Option[(E, Instant)] =
    entityWithTime.map {
      case (e, t) => ifAuthorizedOrganizations(Seq(e.organisaatioOid), authorizationRules) {
        (e, t)
      }
    }

  def authorizePut[E <: Authorizable, I](entity: E)(f: => I)(implicit authenticated: Authenticated): I =
    ifAuthorizedOrganizations(Seq(entity.organisaatioOid), AutorizationRules(roleEntity.createRoles)) {
      f
    }

  def authorizeUpdate[E <: Authorizable, I](entityForUpdate: => Option[(E, Instant)])(f: => I)(implicit authenticated: Authenticated): I =
    entityForUpdate match {
      case None         => throw new NoSuchElementException
      case Some((e, _)) => ifAuthorizedOrganizations(Seq(e.organisaatioOid), AutorizationRules(roleEntity.updateRoles)) {
        f
      }
    }

  def isAuthorizationException(exception: Throwable) = exception match {
    case t:OrganizationAuthorizationFailedException => true
    case t:RoleAuthorizationFailedException => true
    case _ => false
  }
}

case class AutorizationRules(requiredRoles: Seq[Role],
                             withParents: Boolean = false,
                             alternativeRules: Seq[(Seq[OrganisaatioOid], OrganisaatioOidsAndOppilaitostyypitFlat) => Boolean] = Seq())

trait AuthorizationService extends Logging {

  private lazy val rootOrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  protected lazy val indexerRoles: Seq[Role] = Seq(Role.Indexer)

  type OrganisaatioOidsAndOppilaitostyypitFlatView = IterableView[OrganisaatioOidsAndOppilaitostyypitFlat, Iterable[_]]

  def isAuthorized(organisaatioOids: Seq[OrganisaatioOid], oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlat):  Boolean =
    oidsAndOppilaitostyypit._1.exists(x => organisaatioOids.exists(_ == x))

  def ifAuthorizedOrganizations[R](organisaatioOids: Seq[OrganisaatioOid],
                                   autorizationRules: AutorizationRules)
                                  (f: => R)
                                  (implicit authenticated: Authenticated): R = {

    val AutorizationRules(requiredRoles, withParents, alternativeRules) = autorizationRules

    def authorized(oidsAndOppilaitostyypit: OrganisaatioOidsAndOppilaitostyypitFlatView): Boolean = alternativeRules match {
      case Nil   => oidsAndOppilaitostyypit.exists(isAuthorized(organisaatioOids, _))
      case rules => oidsAndOppilaitostyypit.exists(oo => rules.exists(_(organisaatioOids, oo)))
    }

    val authorizedOrgs = authenticated.session.getOrganizationsForRoles(requiredRoles)
    lazy val allAuthorizedOidsAndOppilaitostyypit = if(withParents) lazyFlatChildrenAndParents(authorizedOrgs) else lazyFlatChildren(authorizedOrgs)

    authorizedOrgs match {
      case _ if authorizedOrgs.isEmpty => throw RoleAuthorizationFailedException(requiredRoles, authenticated.session.roles)
      case _ if authorizedOrgs.contains(rootOrganisaatioOid) => f
      case _ if authorized(allAuthorizedOidsAndOppilaitostyypit) => f
      case _ => throw OrganizationAuthorizationFailedException(organisaatioOids, authorizedOrgs)
    }
  }

  /** Checks if the the authenticated user has access to the given organization, then calls f with a sequence of descendants of that organization. */
  def withAuthorizedChildOrganizationOids[R](oid: OrganisaatioOid, roles: Seq[Role])(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOids(oid, AutorizationRules(roles)){f}

  def withAuthorizedChildOrganizationOidsAndOppilaitostyypit[R](oid: OrganisaatioOid, roles: Seq[Role])(f: OrganisaatioOidsAndOppilaitostyypitFlat => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, AutorizationRules(roles)){f}

  def withAuthorizedOrganizationOidsAndOppilaitostyypit[R](oid: OrganisaatioOid, autorizationRules: AutorizationRules)(f: OrganisaatioOidsAndOppilaitostyypitFlat => R)(implicit authenticated: Authenticated): R =
    ifAuthorizedOrganizations(Seq(oid), autorizationRules) {

      val requestedOrganizations = if( autorizationRules.withParents ) OrganisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(oid)
                                   else                                OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid)

      requestedOrganizations match {
        case (oids, _) if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
        case (oids, t) => f(oids, t)
      }
    }

  def withAuthorizedOrganizationOids[R](oid: OrganisaatioOid, autorizationRules: AutorizationRules)(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(oid, autorizationRules) { case (oids, _) =>
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
