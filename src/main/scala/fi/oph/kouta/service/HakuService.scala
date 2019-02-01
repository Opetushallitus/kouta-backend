package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO}

object HakuService extends ValidatingService[Haku] with AuthorizationService {
  
  def put(haku: Haku): Option[HakuOid] = withValidation(haku, HakuDAO.put(_))

  def update(haku: Haku, notModifiedSince:Instant): Boolean = withValidation(haku, HakuDAO.update(_, notModifiedSince))

  def get(oid: HakuOid): Option[(Haku, Instant)] = HakuDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[HakuListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, HakuDAO.listByOrganisaatioOids)

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, HakukohdeDAO.listByHakuOidAndOrganisaatioOids(hakuOid, _))

  def listHakukohteet(hakuOid: HakuOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByHakuOid(hakuOid)

  def listKoulutukset(hakuOid: HakuOid): Seq[KoulutusListItem] =
    KoulutusDAO.listByHakuOid(hakuOid)
}
