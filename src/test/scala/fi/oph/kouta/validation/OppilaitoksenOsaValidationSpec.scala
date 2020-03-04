package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Fi, Julkaistu, Lisatieto, OppilaitoksenOsa, OppilaitoksenOsaMetadata, OppilaitosMetadata, Sv, Tallennettu, Yhteystieto}
import fi.oph.kouta.validation.Validations.{invalidKielistetty, invalidUrl, missingMsg, notNegativeMsg, validationMsg}

class OppilaitoksenOsaValidationSpec extends BaseValidationSpec[OppilaitoksenOsa] {

  val min = TestData.MinOppilaitoksenOsa
  val max = TestData.JulkaistuOppilaitoksenOsa

  it should "fail if perustiedot are invalid" in {
    failsValidation(max.copy(oid = OrganisaatioOid("virhe")), "oid", validationMsg("virhe"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(max.copy(organisaatioOid = OrganisaatioOid("virhe")), "organisaatioOid", validationMsg("virhe"))
  }

  it should "pass a valid oppilaitos" in {
    passesValidation(max)
  }

  it should "fail if oppilaitosOid is invalid" in {
    failsValidation(min.copy(oppilaitosOid = OrganisaatioOid("virhe")), "oppilaitosOid", validationMsg("virhe"))
  }

  it should "validate metadata" in {
    val oppilaitos = min.copy(metadata = Some(OppilaitoksenOsaMetadata(opiskelijoita = Some(-1))))
    failsValidation(oppilaitos, "metadata.opiskelijoita", notNegativeMsg)
  }

  it should "fail if teemakuva is not a proper url" in {
    failsValidation(min.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
  }
}

class OppilaitoksenOsaMetadataValidationSpec extends SubEntityValidationSpec[OppilaitoksenOsaMetadata] {

  val min = OppilaitoksenOsaMetadata()
  val max = TestData.JulkaistuOppilaitoksenOsa.metadata.get

  it should "pass a valid metadata" in {
    passesValidation(Julkaistu, max)
  }

  it should "validate yhteystiedot" in {
    val metadata = min.copy(yhteystiedot = Some(Yhteystieto(wwwSivu = Map(Fi -> "http://testi.fi", Sv -> "urli"))))
    failsValidation(Tallennettu, metadata, "yhteystiedot.wwwSivu.sv", invalidUrl("urli"))
  }

  it should "fail if esittely is present only for some languages in a julkaistu oppilaitoksenOsa" in {
    val metadata = min.copy(esittely = Map(Fi -> "esittely"))
    passesValidation(Tallennettu, metadata)
    failsValidation(Julkaistu, metadata, "esittely", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if kampus is present only for some languages in a julkaistu oppilaitoksenOsa" in {
    val metadata = min.copy(kampus = Map(Fi -> "kampus"))
    passesValidation(Tallennettu, metadata)
    failsValidation(Julkaistu, metadata, "kampus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if opiskelijoita is negative" in {
    failsValidation(Tallennettu, min.copy(opiskelijoita = Some(-1)), "opiskelijoita", notNegativeMsg)
  }
}
