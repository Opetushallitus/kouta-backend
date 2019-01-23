package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.KoulutusServlet
import org.json4s.jackson.Serialization.read
import org.scalactic.Equality

trait KoulutusFixture { this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  addServlet(new KoulutusServlet(), KoulutusPath)

  val koulutus = TestData.AmmKoulutus

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = tila)

  def put(koulutus:Koulutus):String = put(KoulutusPath, koulutus, oid(_))
  def get(oid:String, expected:Koulutus):String = get(KoulutusPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "koulutukset"))))
  def update(koulutus:Koulutus, lastModified:String, expectUpdate:Boolean):Unit = update(KoulutusPath, koulutus, lastModified, expectUpdate)
  def update(koulutus:Koulutus, lastModified:String):Unit = update(koulutus, lastModified, true)

  def koulutus(julkinen:Boolean, organisaatioOid:String, tila:Julkaisutila):Koulutus =
    koulutus.copy(julkinen = julkinen, organisaatioOid = OrganisaatioOid(organisaatioOid), tila = tila)

  def addToList(koulutus:Koulutus) = {
    val oid = put(koulutus)
    val modified = readModifiedByOid(oid, "koulutukset")
    new KoulutusListItem(KoulutusOid(oid), koulutus.nimi, koulutus.tila, koulutus.organisaatioOid, koulutus.muokkaaja, modified)
  }
}