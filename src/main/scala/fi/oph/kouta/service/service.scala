package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{HasPrimaryId, HasTeemakuvaMetadata, Koulutustyyppi, TeemakuvaMetadata}
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

  def authorizeGet[E <: Authorizable](entityWithTime: Option[(E, Instant)])(implicit authenticated: Authenticated): Option[(E, Instant)] =
    entityWithTime.map {
      case (entity, lastModified) =>
        withAuthorizedChildOrganizationOids(roleEntity.readRoles) { authorizedOrganizations =>
          authorize(entity.organisaatioOid, authorizedOrganizations) {
            (entity, lastModified)
          }
        }
    }

  def authorizePut[E <: Authorizable, I](entity: E)(f: => I)(implicit authenticated: Authenticated): I =
    withAuthorizedChildOrganizationOids(roleEntity.createRoles) { authorizedOrganizations =>
      authorize(entity.organisaatioOid, authorizedOrganizations) {
        f
      }
    }

  def authorizeUpdate[E <: Authorizable, I](entityForUpdate: => Option[(E, Instant)])(f: => I)(implicit authenticated: Authenticated): I = {
    withAuthorizedChildOrganizationOids(roleEntity.updateRoles) { authorizedOrganizations =>
      entityForUpdate match {
        case None => throw new NoSuchElementException()
        case Some(existing) =>
          authorize(existing._1.organisaatioOid, authorizedOrganizations) {
            f
          }
      }
    }
  }
}

trait AuthorizationService extends Logging {

  private lazy val rootOrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  protected lazy val indexerRoles: Seq[Role] = Seq(Role.Indexer)

  /** Checks if the the authenticated user has access to the given organization, then calls f with a sequence of descendants of that organization. */
  def withAuthorizedChildOrganizationOids[R](oid: OrganisaatioOid, roles: Seq[Role])(f: Seq[OrganisaatioOid] => R)(implicit authenticated: Authenticated): R =
    withAuthorizedChildOrganizationOidsAndOppilaitostyypit(oid, roles) { case (oids, _) =>
      f(oids)
    }

  def withAuthorizedChildOrganizationOidsAndOppilaitostyypit[R](oid: OrganisaatioOid, roles: Seq[Role])(f: (Seq[OrganisaatioOid], Seq[Koulutustyyppi]) => R)(implicit authenticated: Authenticated): R =
    withAuthorizedChildOrganizationOids(roles) { children =>
      authorize(oid, children) {
        OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid) match {
          case (oids, _) if oids.isEmpty => throw OrganizationAuthorizationFailedException(oid)
          case (oids, t) => f(oids, t)
        }
      }
    }

  def withAuthorizedChildOrganizationOids[R](roles: Seq[Role])(f: IterableView[OrganisaatioOid, Iterable[_]] => R)(implicit authenticated: Authenticated): R =
    organizationsForRoles(roles) match {
      case oids if oids.isEmpty => throw RoleAuthorizationFailedException(roles, authenticated.session.roles)
      case oids                 => f(oids.view ++ lazyFlatChildren(oids).flatMap(_._1))
    }

  def authorize[R](allowedOrganization: OrganisaatioOid, authorizedOrganizations: Iterable[OrganisaatioOid])(r: R): R =
    authorizeRootOrAny(Set(allowedOrganization), authorizedOrganizations)(r)

  def authorizeRootOrAny[R](allowedOrganizations: Set[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid])(r: R): R =
    if (authorizedOrganizations.exists(authorized => authorized == rootOrganisaatioOid || allowedOrganizations.contains(authorized))) {
      r
    }
    else {
      throw OrganizationAuthorizationFailedException(allowedOrganizations, authorizedOrganizations)
    }

  protected def organizationsForRoles(roles: Seq[Role])(implicit authenticated: Authenticated): Set[OrganisaatioOid] =
    roles.flatMap { role =>
      authenticated.session.roleMap.get(role)
    }.fold(Set())(_ union _)

  protected def lazyFlatChildren(orgs: Set[OrganisaatioOid]): IterableView[(Seq[OrganisaatioOid], Seq[Koulutustyyppi]), Iterable[_]] =
    orgs.view.map(oid => OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(oid))

  def hasRootAccess(roles: Seq[Role])(implicit authenticated: Authenticated): Boolean =
    roles.exists { role =>
      authenticated.session.roleMap.get(role).exists(_.contains(rootOrganisaatioOid))
    }

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
      s"Allowed organizations: ${allowedOrganizationOids.map(_.s).mkString(",")}. " +
      s"Authorized organizations: ${authorizedOrganizations.map(_.s).mkString(",")}.")

  def apply(missingOrganizationOid: OrganisaatioOid): OrganizationAuthorizationFailedException =
    OrganizationAuthorizationFailedException(s"Authorization failed, unknown organization oid ${missingOrganizationOid.s}")
}

case class RoleAuthorizationFailedException(acceptedRoles: Seq[Role], existingRoles: Iterable[Role])
  extends RuntimeException(s"Authorization failed, missing role. Accepted roles: ${acceptedRoles.map(_.name).mkString(",")}. Existing roles: ${existingRoles.map(_.name).mkString(",")}.")
