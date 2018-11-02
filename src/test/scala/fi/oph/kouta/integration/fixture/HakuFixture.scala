package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData.JulkaistuHaku
import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakuServlet

trait HakuFixture { this: KoutaIntegrationSpec =>

  val HakuPath = "/haku"

  addServlet(new HakuServlet(), HakuPath)
  
  val haku = JulkaistuHaku

  def haku(oid:String): Haku = haku.copy(oid = Some(oid))
  def haku(oid:String, tila:Julkaisutila): Haku = haku.copy(oid = Some(oid), tila = tila)

  def put(haku:Haku):String = put(HakuPath, haku, oid(_))
  def get(oid:String, expected:Haku):String = get(HakuPath, oid, expected)
  def update(haku:Haku, lastModified:String, expectUpdate:Boolean):Unit = update(HakuPath, haku, lastModified, expectUpdate)
  def update(haku:Haku, lastModified:String):Unit = update(haku, lastModified, true)
}
