package fi.oph.kouta.integration.fixture

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ToteutusServlet
import org.json4s.jackson.Serialization.read

trait ToteutusFixture { this: KoutaIntegrationSpec =>

  val ToteutusPath = "/toteutus"

  addServlet(new ToteutusServlet(), ToteutusPath)

  val opetus = Opetus(
    opetuskielet = List("fi"),
    lahiopetus = Some(true),
    opetusajat = List(Iltaopetus, Viikonloppuopetus),
    maksullisuus = None,
    kuvaus = Map())

  val ammMetatieto = ToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(Osaamisala("osaamisala_koodi_uri#1")),
    opetus = Some(opetus),
    asiasanat = List("robotiikka"),
    ammattinimikkeet = List("koneinsinööri"),
    yhteystieto = Some(Yhteystieto(nimi = "Aku Ankka", puhelinnumero = Some("123"))))

  val toteutus = Toteutus(
    oid = None,
    koulutusOid = "",
    tila = Julkaistu,
    tarjoajat = List("1.2.3.3", "1.2.3.4"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(ammMetatieto),
    muokkaaja = "Hirmu kiva muokkaaja",
    kielivalinta = Seq(Fi, Sv))

  def toteutus(koulutusOid:String): Toteutus = toteutus.copy(koulutusOid = koulutusOid)
  def toteutus(oid:String, koulutusOid:String): Toteutus = toteutus.copy(oid = Some(oid), koulutusOid = koulutusOid)
  def toteutus(oid:String, koulutusOid:String, tila:Julkaisutila): Toteutus = toteutus.copy(oid = Some(oid), koulutusOid = koulutusOid, tila = tila)

  def put(toteutus:Toteutus):String = put(ToteutusPath, toteutus, oid(_))
  def get(oid:String, expected:Toteutus): String = get(ToteutusPath, oid, expected)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus:Toteutus, lastModified:String): Unit = update(toteutus, lastModified, true)
}