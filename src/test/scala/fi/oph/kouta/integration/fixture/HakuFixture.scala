package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData.JulkaistuHaku
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakuServlet

trait HakuFixture { this: KoutaIntegrationSpec =>

  val HakuPath = "/haku"

  addServlet(new HakuServlet(), HakuPath)

  val haku = JulkaistuHaku

  def haku(oid: String): Haku = haku.copy(oid = Some(HakuOid(oid)))
  def haku(oid: String, tila: Julkaisutila): Haku = haku.copy(oid = Some(HakuOid(oid)), tila = tila)
  def haku(tila: Julkaisutila, organisaatioOid: String): Haku =
    haku.copy(organisaatioOid = OrganisaatioOid(organisaatioOid), tila = tila)

  def put(haku: Haku): String = put(HakuPath, haku, defaultSessionId, oid)

  def get(oid: String, expected: Haku): String = get(HakuPath, oid, defaultSessionId, expected.copy(modified = Some(readModifiedByOid(oid, "haut"))))

  def update(haku: Haku, lastModified: String, expectUpdate: Boolean): Unit = update(HakuPath, haku, lastModified, expectUpdate, defaultSessionId)
  def update(haku: Haku, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(haku: Haku) = {
    val oid = put(haku)
    val modified = readModifiedByOid(oid, "haut")
    HakuListItem(HakuOid(oid), haku.nimi, haku.tila, haku.organisaatioOid, haku.muokkaaja, modified)
  }
}
