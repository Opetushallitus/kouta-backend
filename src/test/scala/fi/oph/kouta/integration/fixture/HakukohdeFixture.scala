package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.TestData.JulkaistuHakukohde
import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakukohdeServlet

trait HakukohdeFixture { this: KoutaIntegrationSpec =>

  val HakukohdePath = "/hakukohde"

  addServlet(new HakukohdeServlet(), HakukohdePath)

  val hakukohde = JulkaistuHakukohde

  def hakukohde(koulutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    koulutusOid = koulutusOid, hakuOid = hakuOid, valintaperuste = Some(valintaperusteId))

  def hakukohde(oid:String, koulutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    oid = Some(oid), koulutusOid = koulutusOid, hakuOid = hakuOid, valintaperuste = Some(valintaperusteId))

  def hakukohde(oid:String, koulutusOid:String, hakuOid:String, valintaperusteId:UUID, tila:Julkaisutila):Hakukohde = hakukohde.copy(
    oid = Some(oid), koulutusOid = koulutusOid, hakuOid = hakuOid, valintaperuste = Some(valintaperusteId), tila = tila)

  def put(hakukohde:Hakukohde):String = put(HakukohdePath, hakukohde, oid(_))
  def get(oid:String, expected:Hakukohde):String = get(HakukohdePath, oid, expected)
  def update(haku:Hakukohde, lastModified:String, expectUpdate:Boolean):Unit = update(HakukohdePath, haku, lastModified, expectUpdate)
  def update(haku:Hakukohde, lastModified:String):Unit = update(haku, lastModified, true)
}
