package fi.oph.kouta.integration.fixture

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ToteutusServlet
import org.json4s.jackson.Serialization.read

trait ToteutusFixture extends CommonFixture { this: KoutaIntegrationSpec =>

  addServlet(new ToteutusServlet(), "/toteutus")

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

  def putToteutusOk(toteutus:Toteutus) = {
    put("/toteutus", bytes(toteutus)) {
      status should equal(200)
      oid(body)
    }
  }

  def getToteutusOk(oid:String, expected:Toteutus) = {
    get(s"/toteutus/$oid") {
      status should equal (200)
      read[Toteutus](body) should equal (expected)
      header.get("Last-Modified").get
    }
  }

  def updateToteutusOk(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean = true) = {
    post("/toteutus", bytes(toteutus), headersIfUnmodifiedSince(lastModified)) {
      status should equal (200)
      updated(body) should equal (expectUpdate)
    }
  }
}