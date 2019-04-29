package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.JulkaistuHakukohde
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.repository.SQLHelpers
import fi.oph.kouta.service.HakukohdeService
import fi.oph.kouta.servlet.HakukohdeServlet

object HakukohdeServiceWithoutIndexing extends HakukohdeService(SqsInTransactionServiceIgnoringIndexing)

trait HakukohdeFixture extends SQLHelpers { this: KoutaIntegrationSpec =>

  val HakukohdePath = "/hakukohde"

  protected lazy val hakukohdeService: HakukohdeService = HakukohdeServiceWithoutIndexing

  addServlet(new HakukohdeServlet(hakukohdeService), HakukohdePath)

  val hakukohde = JulkaistuHakukohde

  def getIds(hakukohde:Hakukohde): Hakukohde = {
    import slick.jdbc.PostgresProfile.api._
    hakukohde.copy(
      liitteet = hakukohde.liitteet.map(l => l.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_liitteet where hakukohde_oid = ${hakukohde.oid} and tyyppi = ${l.tyyppi}""".as[String]).headOption.map(UUID.fromString))),
      valintakokeet = hakukohde.valintakokeet.map(l => l.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_valintakokeet where hakukohde_oid = ${hakukohde.oid} and tyyppi = ${l.tyyppi}""".as[String]).headOption.map(UUID.fromString))),
    )}

  def hakukohde(toteutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid:String, toteutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid:String, toteutusOid:String, hakuOid:String, valintaperusteId:UUID, tila:Julkaisutila):Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), tila = tila)

  def put(hakukohde:Hakukohde):String = put(HakukohdePath, hakukohde, oid(_))
  def get(oid:String, expected:Hakukohde):String = get(HakukohdePath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "hakukohteet"))))
  def update(haku:Hakukohde, lastModified:String, expectUpdate:Boolean):Unit = update(HakukohdePath, haku, lastModified, expectUpdate)
  def update(haku:Hakukohde, lastModified:String):Unit = update(haku, lastModified, true)


  def hakukohde(toteutusOid:String, hakuOid:String, valintaperusteId:UUID, organisaatioOid:String):Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), organisaatioOid = OrganisaatioOid(organisaatioOid))

  def addToList(hakukohde:Hakukohde) = {
    val oid = put(hakukohde)
    val modified = readModifiedByOid(oid, "hakukohteet")
    new HakukohdeListItem(HakukohdeOid(oid), hakukohde.toteutusOid, hakukohde.hakuOid, hakukohde.valintaperusteId,
      hakukohde.nimi, hakukohde.tila, hakukohde.organisaatioOid, hakukohde.muokkaaja, modified)
  }
}
