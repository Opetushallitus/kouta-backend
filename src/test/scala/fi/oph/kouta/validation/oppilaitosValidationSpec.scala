package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Fi, Julkaistu, Lisatieto, Oppilaitos, OppilaitosMetadata, Osoite, Sv, Tallennettu, Yhteystieto}
import fi.oph.kouta.validation.Validations._

class OppilaitosValidationSpec extends BaseValidationSpec[Oppilaitos] {

  val min = TestData.MinOppilaitos
  val max = TestData.JulkaistuOppilaitos

  it should "fail if perustiedot are invalid" in {
    failsValidation(max.copy(oid = OrganisaatioOid("virhe")), "oid", validationMsg("virhe"))
    failsValidation(max.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(max.copy(organisaatioOid = OrganisaatioOid("virhe")), "organisaatioOid", validationMsg("virhe"))
    failsValidation(max.copy(muokkaaja = UserOid("moikka")), "muokkaaja", validationMsg("moikka"))
  }

  it should "pass a valid oppilaitos" in {
    passesValidation(max)
  }

  it should "validate metadata" in {
    val oppilaitos = min.copy(metadata = Some(OppilaitosMetadata(opiskelijoita = Some(-1))))
    failsValidation(oppilaitos, "metadata.opiskelijoita", notNegativeMsg)
  }

  it should "fail if teemakuva is not a proper url" in {
    failsValidation(min.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
  }
}

class OppilaitosMetadataValidationSpec extends SubEntityValidationSpec[OppilaitosMetadata] {

  val min = OppilaitosMetadata()
  val max = TestData.JulkaistuOppilaitos.metadata.get

  it should "pass a valid metadata" in {
    passesValidation(Julkaistu, max)
  }

  it should "validate tietoaOpiskelusta" in {
    val metadata = min.copy(tietoaOpiskelusta = Seq(Lisatieto(otsikkoKoodiUri = "virhe", teksti = Map())))
    failsValidation(Tallennettu, metadata, "tietoaOpiskelusta[0].otsikkoKoodiUri", validationMsg("virhe"))
  }

  it should "validate yhteystiedot" in {
    val metadata = min.copy(yhteystiedot = Some(Yhteystieto(wwwSivu = Map(Fi -> "http://testi.fi", Sv -> "urli"))))
    failsValidation(Tallennettu, metadata, "yhteystiedot.wwwSivu.sv", invalidUrl("urli"))
  }

  it should "fail if esittely is present only for some languages in a julkaistu oppilaitos" in {
    val metadata = min.copy(esittely = Map(Fi -> "esittely"))
    passesValidation(Tallennettu, metadata)
    failsValidation(Julkaistu, metadata, "esittely", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if any of the numeric fields are negative" in {
    failsValidation(Tallennettu, min.copy(opiskelijoita = Some(-1)), "opiskelijoita", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(korkeakouluja = Some(-1)), "korkeakouluja", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(tiedekuntia = Some(-1)), "tiedekuntia", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(kampuksia = Some(-1)), "kampuksia", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(yksikoita = Some(-1)), "yksikoita", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(toimipisteita = Some(-1)), "toimipisteita", notNegativeMsg)
    failsValidation(Tallennettu, min.copy(akatemioita = Some(-1)), "akatemioita", notNegativeMsg)
  }
}

class YhteystietoValidationSpec extends SubEntityValidationSpec[Yhteystieto] {
  val max = TestData.JulkaistuOppilaitos.metadata.get.yhteystiedot.get

  it should "pass valid yhteystiedot" in {
     passesValidation(Julkaistu, max)
  }

  it should "validate osoite" in {
    val yhteystiedot = max.copy(osoite = Some(Osoite(postinumeroKoodiUri = Some("virhe"))))
    failsValidation(Tallennettu, yhteystiedot, "osoite.postinumeroKoodiUri", validationMsg("virhe"))
  }

  it should "fail on an invalid sahkoposti" in {
    val yhteystiedot = max.copy(sahkoposti = Map(Fi -> "henkilo@example.com", Sv -> "email"))
    failsValidation(Tallennettu, yhteystiedot, "sahkoposti.sv", invalidEmail("email"))
  }

  it should "fail on an invalid wwwSivu" in {
    val yhteystiedot = max.copy(wwwSivu = Map(Fi -> "https://url.fi", Sv -> "url"))
    failsValidation(Tallennettu, yhteystiedot, "wwwSivu.sv", invalidUrl("url"))
  }

  it should "fail if a julkaistu yhteystieto is missing some languages in kielistetty fields" in {

    passesValidation(Tallennettu, max.copy(wwwSivu = Map(Fi -> "https://url.fi")))
    failsValidation(Julkaistu, max.copy(wwwSivu = Map(Fi -> "https://url.fi")), "wwwSivu", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, max.copy(puhelinnumero = Map(Fi -> "puh")))
    failsValidation(Julkaistu, max.copy(puhelinnumero = Map(Fi -> "puh")), "puhelinnumero", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, max.copy(sahkoposti = Map(Fi -> "henkilo@example.com")))
    failsValidation(Julkaistu, max.copy(sahkoposti = Map(Fi -> "henkilo@example.com")), "sahkoposti", invalidKielistetty(Seq(Sv)))
  }
}
