package fi.oph.kouta.integration.fixture

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.KoulutusServlet
import org.json4s.jackson.Serialization.read

trait KoulutusFixture { this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  addServlet(new KoulutusServlet(), KoulutusPath)

  val koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Some(Amm),
    koulutusKoodiUri = Some("koulutus_123#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(new KoulutusMetadata()),
    tarjoajat = List("1.2", "2.2", "3.2"),
    muokkaaja = "Mörkö Muokkaaja",
    kielivalinta = List(Fi, Sv))

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(oid))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(oid), tila = tila)

  def put(koulutus:Koulutus):String = put(KoulutusPath, koulutus, oid(_))
  def get(oid:String, expected:Koulutus):String = get(KoulutusPath, oid, expected)
  def update(koulutus:Koulutus, lastModified:String, expectUpdate:Boolean):Unit = update(KoulutusPath, koulutus, lastModified, expectUpdate)
  def update(koulutus:Koulutus, lastModified:String):Unit = update(koulutus, lastModified, true)

  def list(params:List[(String, String)], expected:List[OidListResponse]):List[OidListResponse] = list(KoulutusPath, params, expected)
}