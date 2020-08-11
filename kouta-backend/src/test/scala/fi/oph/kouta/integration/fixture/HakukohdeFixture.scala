package fi.oph.kouta.integration.fixture

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.JulkaistuHakukohde
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.repository.{HakukohdeDAO, SQLHelpers}
import fi.oph.kouta.service.{HakukohdeService, OrganisaatioServiceImpl}
import fi.oph.kouta.servlet.HakukohdeServlet
import fi.oph.kouta.util.TimeUtils

trait HakukohdeFixture extends SQLHelpers with KoutaIntegrationSpec with AccessControlSpec {

  val HakukohdePath = "/hakukohde"

  def hakukohdeService: HakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new HakukohdeService(SqsInTransactionServiceIgnoringIndexing, new AuditLog(MockAuditLogger), organisaatioService)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new HakukohdeServlet(hakukohdeService), HakukohdePath)
  }
  val hakukohde = JulkaistuHakukohde

  def getIds(hakukohde:Hakukohde): Hakukohde = {
    import slick.jdbc.PostgresProfile.api._
    hakukohde.copy(
      liitteet = hakukohde.liitteet.map(l => l.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_liitteet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${l.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString))),
      valintakokeet = hakukohde.valintakokeet.map(vk => vk.copy(id = db.runBlocking(
        sql"""select id from hakukohteiden_valintakokeet where hakukohde_oid = ${hakukohde.oid} and tyyppi_koodi_uri = ${vk.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString)))
    )}

  def hakukohde(toteutusOid: String, hakuOid: String, valintaperusteId: UUID): Hakukohde = hakukohde.copy(
    toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid:String, toteutusOid:String, hakuOid:String, valintaperusteId:UUID):Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId))

  def hakukohde(oid: String, toteutusOid: String, hakuOid: String, valintaperusteId: UUID, tila: Julkaisutila): Hakukohde = hakukohde.copy(
    oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid), valintaperusteId = Some(valintaperusteId), tila = tila)

  def hakukohde(toteutusOid: ToteutusOid, hakuOid: HakuOid, valintaperusteId: UUID, organisaatioOid: OrganisaatioOid): Hakukohde = hakukohde.copy(
    toteutusOid = toteutusOid, hakuOid = hakuOid, valintaperusteId = Some(valintaperusteId), organisaatioOid = organisaatioOid)

  def put(hakukohde: Hakukohde): String = put(HakukohdePath, hakukohde, oid)
  def put(hakukohde: Hakukohde, sessionId: UUID): String = put(HakukohdePath, hakukohde, sessionId, oid)

  def get(oid: String, expected: Hakukohde): String = get(HakukohdePath, oid, expected.copy(modified = Some(readHakukohdeModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Hakukohde): String = get(HakukohdePath, oid, sessionId, expected.copy(modified = Some(readHakukohdeModified(oid))))

  def update(haku: Hakukohde, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(HakukohdePath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(HakukohdePath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Hakukohde, lastModified: String, expectUpdate: Boolean): Unit = update(HakukohdePath, haku, lastModified, expectUpdate)
  def update(haku: Hakukohde, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(hakukohde: Hakukohde) = {
    val oid = put(hakukohde)
    val modified = readHakukohdeModified(oid)
    HakukohdeListItem(HakukohdeOid(oid), hakukohde.toteutusOid, hakukohde.hakuOid, hakukohde.valintaperusteId,
      hakukohde.nimi, hakukohde.tila, hakukohde.organisaatioOid, hakukohde.muokkaaja, modified)
  }

  def readHakukohdeModified(oid: String): LocalDateTime = readHakukohdeModified(HakukohdeOid(oid))
  def readHakukohdeModified(oid: HakukohdeOid): LocalDateTime =
    TimeUtils.instantToModifiedAt(db.runBlocking(HakukohdeDAO.selectLastModified(oid)).get)
}
