package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ToteutusServlet

trait ToteutusFixture { this: KoutaIntegrationSpec =>

  val ToteutusPath = "/toteutus"

  addServlet(new ToteutusServlet(), ToteutusPath)

  val opetus = ToteutuksenOpetus
  val ammMetatieto = AmmToteutuksenMetatieto
  val toteutus = JulkaistuAmmToteutus

  def toteutus(koulutusOid:String): Toteutus = toteutus.copy(koulutusOid = koulutusOid)
  def toteutus(oid:String, koulutusOid:String): Toteutus = toteutus.copy(oid = Some(oid), koulutusOid = koulutusOid)
  def toteutus(oid:String, koulutusOid:String, tila:Julkaisutila): Toteutus = toteutus.copy(oid = Some(oid), koulutusOid = koulutusOid, tila = tila)

  def put(toteutus:Toteutus):String = put(ToteutusPath, toteutus, oid(_))
  def get(oid:String, expected:Toteutus): String = get(ToteutusPath, oid, expected)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus:Toteutus, lastModified:String): Unit = update(toteutus, lastModified, true)
}