package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.{Koulutus, OidListItem, Toteutus}
import fi.oph.kouta.repository.{KoulutusDAO, ToteutusDAO}

object KoulutusService extends ValidatingService[Koulutus] with AuthorizationService {

    def put(koulutus:Koulutus): Option[String] = withValidation(koulutus, KoulutusDAO.put(_))

    def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean =
        withValidation(koulutus, KoulutusDAO.update(_, notModifiedSince))

    def get(oid:String): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)

    def list(organisaatioOid:String):Seq[OidListItem] =
        withAuthorizedChildAndParentOrganizationOids(organisaatioOid, KoulutusDAO.listByOrganisaatioOids)

    def toteutukset(oid:String): Seq[Toteutus] = ToteutusDAO.getByKoulutusOid(oid)

    def listToteutukset(oid:String, organisaatioOid:String):Seq[OidListItem] =
        withAuthorizedChildOrganizationOids(organisaatioOid, ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _))
}