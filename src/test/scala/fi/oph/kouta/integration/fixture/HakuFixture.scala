package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.JulkaistuHaku
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.service.HakuService
import fi.oph.kouta.servlet.HakuServlet

object HakuServiceIgnoringIndexing extends HakuService(SqsInTransactionServiceIgnoringIndexing)

trait HakuFixture { this: KoutaIntegrationSpec =>

  val HakuPath = "/haku"

  protected lazy val hakuService: HakuService = HakuServiceIgnoringIndexing

  addServlet(new HakuServlet(hakuService), HakuPath)

  val haku = JulkaistuHaku

  def haku(oid: String): Haku = haku.copy(oid = Some(HakuOid(oid)))
  def haku(oid: String, tila: Julkaisutila): Haku = haku.copy(oid = Some(HakuOid(oid)), tila = tila)
  def haku(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Haku =
    haku.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(haku: Haku): String = put(HakuPath, haku, oid)
  def put(haku: Haku, sessionId: UUID): String = put(HakuPath, haku, sessionId, oid)
  def get(oid: String, expected: Haku): String = get(HakuPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "haut"))))
  def get(oid: String, sessionId: UUID, expected: Haku): String = get(HakuPath, oid, sessionId, expected.copy(modified = Some(readModifiedByOid(oid, "haut"))))
  def update(haku: Haku, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(HakuPath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(HakuPath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean): Unit = update(HakuPath, haku, lastModified, expectUpdate)
  def update(haku: Haku, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(haku: Haku) = {
    val oid = put(haku)
    val modified = readModifiedByOid(oid, "haut")
    HakuListItem(HakuOid(oid), haku.nimi, haku.tila, haku.organisaatioOid, haku.muokkaaja, modified)
  }
}
