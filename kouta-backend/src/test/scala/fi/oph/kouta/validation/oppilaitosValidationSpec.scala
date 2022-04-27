package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Fi, Julkaistu, NimettyLinkki, Oppilaitos, OppilaitosMetadata, Sv, Tallennettu, TietoaOpiskelusta, Yhteystieto}
import fi.oph.kouta.validation.Validations._

class OppilaitosValidationSpec extends BaseValidationSpec[Oppilaitos] {

  val min: Oppilaitos = TestData.MinOppilaitos
  val max: Oppilaitos = TestData.JulkaistuOppilaitos

  it should "fail if perustiedot are invalid" in {
    failsValidation(max.copy(oid = OrganisaatioOid("virhe")), "oid", validationMsg("virhe"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(max.copy(organisaatioOid = OrganisaatioOid("virhe")), "organisaatioOid", validationMsg("virhe"))
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

  it should "fail if logo is not a proper url" in {
    failsValidation(min.copy(logo = Some("ftp://url.fi/ftp-logo")), "logo", invalidUrl("ftp://url.fi/ftp-logo"))
  }
}

class OppilaitosMetadataValidationSpec extends SubEntityValidationSpec[OppilaitosMetadata] {

  val min: OppilaitosMetadata = OppilaitosMetadata()
  val max: OppilaitosMetadata = TestData.JulkaistuOppilaitos.metadata.get

  it should "pass a valid metadata" in {
    passesValidation(Julkaistu, max)
  }

  it should "validate tietoaOpiskelusta" in {
    val metadata = min.copy(tietoaOpiskelusta = Seq(TietoaOpiskelusta(otsikkoKoodiUri = "virhe", teksti = Map())))
    failsValidation(Tallennettu, metadata, "tietoaOpiskelusta[0].otsikkoKoodiUri", validationMsg("virhe"))
  }

  it should "validate wwwSivu" in {
    val metadata = min.copy(wwwSivu = Some(NimettyLinkki(url = Map(Fi -> "http://testi.fi", Sv -> "urli"))))
    failsValidation(Tallennettu, metadata, "wwwSivu.url.sv", invalidUrl("urli"))
  }

  it should "fail if wwwSivu not present in a julkaistu oppilaitos" in {
    val metadata = max.copy(wwwSivu = None)
    passesValidation(Tallennettu, metadata)
    failsValidation(Julkaistu, metadata, "wwwSivu", missingMsg)
  }

  it should "validate hakijapalveluidenYhteystiedot" in {
    val metadata = min.copy(hakijapalveluidenYhteystiedot = Some(Yhteystieto(sahkoposti = Map(Fi -> "validi@eemeli.fi", Sv -> "epavalidi@eemeli"))))
    failsValidation(Tallennettu, metadata, "hakijapalveluidenYhteystiedot.sahkoposti.sv", invalidEmail("epavalidi@eemeli"))
  }

  it should "fail if esittely is present only for some languages in a julkaistu oppilaitos" in {
    val metadata = max.copy(esittely = Map(Fi -> "esittely"))
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

