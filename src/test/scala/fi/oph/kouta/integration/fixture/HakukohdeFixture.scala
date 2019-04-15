package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.TestData.JulkaistuHakukohde
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakukohdeServlet

trait HakukohdeFixture { this: KoutaIntegrationSpec =>

  val HakukohdePath = "/hakukohde"

  addServlet(new HakukohdeServlet(), HakukohdePath)

  val hakukohde = JulkaistuHakukohde

  def hakukohde(toteutusOid: String, hakuOid: String, valintaperusteId: UUID): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid:String, toteutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid: String, toteutusOid: String, hakuOid: String, valintaperusteId: UUID, tila: Julkaisutila): Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), tila = tila)

  def hakukohde(toteutusOid: String, hakuOid: String, valintaperusteId: UUID, organisaatioOid: String): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), organisaatioOid = OrganisaatioOid(organisaatioOid))

  def put(hakukohde: Hakukohde): String = put(HakukohdePath, hakukohde, defaultSessionId, oid)

  def get(oid: String, expected: Hakukohde): String = get(HakukohdePath, oid, defaultSessionId, expected.copy(modified = Some(readModifiedByOid(oid, "hakukohteet"))))

  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean): Unit = update(HakukohdePath, haku, lastModified, expectUpdate, defaultSessionId)
  def update(haku: Hakukohde, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(hakukohde: Hakukohde) = {
    val oid = put(hakukohde)
    val modified = readModifiedByOid(oid, "hakukohteet")
    HakukohdeListItem(HakukohdeOid(oid), hakukohde.toteutusOid, hakukohde.hakuOid, hakukohde.valintaperusteId,
      hakukohde.nimi, hakukohde.tila, hakukohde.organisaatioOid, hakukohde.muokkaaja, modified)
  }
}
