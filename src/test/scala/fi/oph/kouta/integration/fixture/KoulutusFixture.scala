package fi.oph.kouta.integration.fixture

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.service.KoulutusService
import fi.oph.kouta.servlet.KoulutusServlet
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}

object KoulutusServiceIgnoringIndexing extends KoulutusService(SqsInTransactionServiceIgnoringIndexing)

trait KoulutusFixture { this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  protected lazy val koulutusService: KoulutusService = KoulutusServiceIgnoringIndexing

  addServlet(new KoulutusServlet(koulutusService), KoulutusPath)

  val koulutus = TestData.AmmKoulutus

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = tila)
  def koulutus(julkinen:Boolean, organisaatioOid:String, tila:Julkaisutila):Koulutus =
    koulutus.copy(julkinen = julkinen, organisaatioOid = OrganisaatioOid(organisaatioOid), tila = tila)

  def put(koulutus:Koulutus):String = put(KoulutusPath, koulutus, oid(_))

  def get(oid: String, expected: Koulutus): String =
    get(KoulutusPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "koulutukset"))))

  def get(oid: String, headers: Seq[(String, String)], expected: Koulutus): String =
    get(KoulutusPath, oid, headers, expected.copy(modified = Some(readModifiedByOid(oid, "koulutukset"))))

  def update(koulutus: Koulutus, lastModified: String, expectUpdate: Boolean): Unit =
    update(KoulutusPath, koulutus, lastModified, expectUpdate)

  def update(koulutus: Koulutus, lastModified: String): Unit =
    update(koulutus, lastModified, expectUpdate = true)

  def addToList(koulutus:Koulutus) = {
    val oid = put(koulutus)
    val modified = readModifiedByOid(oid, "koulutukset")
    new KoulutusListItem(KoulutusOid(oid), koulutus.nimi, koulutus.tila, koulutus.organisaatioOid, koulutus.muokkaaja, modified)
  }
}
