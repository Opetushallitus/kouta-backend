package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.{toteutus, _}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ToteutusService extends ToteutusService(SqsInTransactionService, S3Service)

class ToteutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService with TeemakuvaService[ToteutusOid, Toteutus, ToteutusMetadata] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] =
    authorizeGet(ToteutusDAO.get(oid), AuthorizationRules(roleEntity.readRoles, withParents = true, withTarjoajat = true))

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing))
    }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(ToteutusDAO.get(toteutus.oid.get), AuthorizationRules(roleEntity.updateRoles, withParents = true, withTarjoajat = true)) {
      withValidation(toteutus, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(roleEntity.readRoles, withParents = true))(ToteutusDAO.listByAllowedOrganisaatiot)

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

  private def putWithIndexing(toteutus: Toteutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getPutActions(toteutus))

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getUpdateActions(toteutus, notModifiedSince),
      toteutus.oid.get.toString)
}
