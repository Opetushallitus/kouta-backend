package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.SqsInTransactionService._
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}

object KoulutusService extends ValidatingService[Koulutus] with AuthorizationService {

  def put(koulutus: Koulutus): KoulutusOid =
    withValidation(koulutus, putWithIndexing)

  def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean =
    withValidation(koulutus, updateWithIndexing(_, notModifiedSince))

  def get(oid: KoulutusOid): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[KoulutusListItem] = {
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, KoulutusDAO.listByOrganisaatioOids)
  }

  def toteutukset(oid: KoulutusOid, vainJulkaistut: Option[Boolean] = None): Seq[Toteutus] = vainJulkaistut match {
    case Some(true) => ToteutusDAO.getJulkaistutByKoulutusOid(oid)
    case _ => ToteutusDAO.getByKoulutusOid(oid)
  }

  def hakutiedot(oid: KoulutusOid): Seq[Hakutieto] = HakutietoDAO.getByKoulutusOid(oid)

  def listToteutukset(oid: KoulutusOid): Seq[OidListItem] = ToteutusDAO.listByKoulutusOid(oid)

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _))
  }

  private def putWithIndexing(koulutus: Koulutus) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeKoulutus,
      () => KoulutusDAO.getPutActions(koulutus))

  private def updateWithIndexing(koulutus: Koulutus, notModifiedSince: Instant) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeKoulutus,
      () => KoulutusDAO.getUpdateActions(koulutus, notModifiedSince),
      koulutus.oid.get.toString)
}