package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}
import fi.oph.kouta.util.WithSideEffect

object KoulutusService extends ValidatingService[Koulutus] with AuthorizationService with WithSideEffect {

  def put(koulutus: Koulutus): Option[KoulutusOid] = {
    withSideEffect(withValidation(koulutus, KoulutusDAO.put)) {
      case oid => IndexingService.index(koulutus.copy(oid = oid))
    }
  }

  def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean = {
    withSideEffect(withValidation(koulutus, KoulutusDAO.update(_, notModifiedSince))) {
      case true => IndexingService.index(koulutus)
    }
  }

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
}