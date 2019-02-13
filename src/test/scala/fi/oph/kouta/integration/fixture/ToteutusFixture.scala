package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ToteutusServlet
import org.scalactic.Equality

trait ToteutusFixture { this: KoutaIntegrationSpec =>

  val ToteutusPath = "/toteutus"

  addServlet(new ToteutusServlet(), ToteutusPath)

  val opetus = ToteutuksenOpetus
  val ammMetatieto = AmmToteutuksenMetatieto
  val toteutus = JulkaistuAmmToteutus

  def toteutus(koulutusOid:String): Toteutus = toteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid:String, koulutusOid:String): Toteutus = toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid:String, koulutusOid:String, tila:Julkaisutila): Toteutus = toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid), tila = tila)

  def put(toteutus:Toteutus):String = put(ToteutusPath, toteutus, oid(_))
  def get(oid:String, expected:Toteutus): String = get(ToteutusPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "toteutukset"))))
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus:Toteutus, lastModified:String): Unit = update(toteutus, lastModified, true)



  def toteutus(koulutusOid:String, tila:Julkaisutila, organisaatioOid:String): Toteutus =
    toteutus.copy(koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = OrganisaatioOid(organisaatioOid), tila = tila)

  def addToList(toteutus:Toteutus) = {
    val oid = put(toteutus)
    val modified = readModifiedByOid(oid, "toteutukset")
    new ToteutusListItem(ToteutusOid(oid), toteutus.koulutusOid, toteutus.nimi, toteutus.tila,
      toteutus.organisaatioOid, toteutus.muokkaaja, modified)
  }
}