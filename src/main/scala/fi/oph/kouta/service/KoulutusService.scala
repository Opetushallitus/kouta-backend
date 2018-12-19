package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain.{Koulutus, OidListItem, Toteutus}
import fi.oph.kouta.repository.{KoulutusDAO, ToteutusDAO}

object KoulutusService extends ValidatingService[Koulutus] with AuthorizationService {

    def put(koulutus:Koulutus): Option[KoulutusOid] = withValidation(koulutus, KoulutusDAO.put(_))

    def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean =
        withValidation(koulutus, KoulutusDAO.update(_, notModifiedSince))

    def get(oid:KoulutusOid): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)

    def list(organisaatioOid:OrganisaatioOid):Seq[OidListItem] =
        withAuthorizedChildAndParentOrganizationOids(organisaatioOid, KoulutusDAO.listByOrganisaatioOids)

    def toteutukset(oid:KoulutusOid): Seq[Toteutus] = ToteutusDAO.getByKoulutusOid(oid)

    def listToteutukset(oid:KoulutusOid, organisaatioOid:OrganisaatioOid):Seq[OidListItem] =
        withAuthorizedChildOrganizationOids(organisaatioOid, ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _))
}