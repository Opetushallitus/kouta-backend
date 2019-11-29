package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.{toteutus, _}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ToteutusService extends ToteutusService(SqsInTransactionService, S3Service)

class ToteutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService with TeemakuvaService[ToteutusOid, Toteutus, ToteutusMetadata] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid)
    authorizeGet(toteutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing))
    }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get)
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime))
    authorizeUpdate(toteutusWithTime, rules) { oldToteutus =>
      withValidation(toteutus, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
    }
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true))(ToteutusDAO.listByAllowedOrganisaatiot)

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(indexerRoles)(HakuDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByToteutusOidAndAllowedOrganisaatiot(oid, _)
    }
  }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ToteutusSearchResult = {

    def assocHakukohdeCounts(r: ToteutusSearchResult): ToteutusSearchResult =
      r.copy(result = r.result.map {
        t => t.copy(hakukohteet = listHakukohteet(t.oid, organisaatioOid).size)
      })

    list(organisaatioOid).map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaIndexClient.searchToteutukset(toteutusOids, params))
    }
  }

  private def getTarjoajat(maybeToteutusWithTime: Option[(Toteutus, Instant)]): Seq[OrganisaatioOid] =
    maybeToteutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def putWithIndexing(toteutus: Toteutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getPutActions(toteutus),
      (added: Toteutus) => added.oid.get.toString)

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getUpdateActions(toteutus, notModifiedSince),
      toteutus.oid.get.toString)
}
