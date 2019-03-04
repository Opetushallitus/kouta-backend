package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}

object KoulutusService extends ValidatingService[Koulutus] with AuthorizationService {

    def put(koulutus: Koulutus): Option[KoulutusOid] = {
        val oid = withValidation(koulutus, KoulutusDAO.put)
        IndexingService.index(koulutus.copy(oid = oid))
        oid
    }

    def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean = {
        val updated = withValidation(koulutus, KoulutusDAO.update(_, notModifiedSince))
        if (updated) IndexingService.index(koulutus)
        updated
    }

    def get(oid: KoulutusOid): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)

    def list(organisaatioOid: OrganisaatioOid): Seq[KoulutusListItem] =
        withAuthorizedChildAndParentOrganizationOids(organisaatioOid, KoulutusDAO.listByOrganisaatioOids)

    def toteutukset(oid: KoulutusOid, vainJulkaistut: Option[Boolean] = None): Seq[Toteutus] = vainJulkaistut match {
        case Some(true) => ToteutusDAO.getJulkaistutByKoulutusOid(oid)
        case _ => ToteutusDAO.getByKoulutusOid(oid)
    }

    def hakutiedot(oid: KoulutusOid): Seq[Hakutieto] =
        HakutietoDAO.getByKoulutusOid(oid)

    def listToteutukset(oid: KoulutusOid): Seq[OidListItem] =
        ToteutusDAO.listByKoulutusOid(oid)

    def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] =
        withAuthorizedChildOrganizationOids(organisaatioOid, ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _))
}