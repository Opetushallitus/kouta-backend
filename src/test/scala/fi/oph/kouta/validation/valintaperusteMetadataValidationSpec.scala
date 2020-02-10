package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.Validations._

class ValintaperusteMetadataValidationSpec extends SubEntityValidationSpec[ValintaperusteMetadata] {

  "ValintaperusteMetadata validation" should "validate valintatavat" in {
    val metadata = AmmValintaperusteMetadata.copy(valintatavat = Seq(Valintatapa1.copy(valintatapaKoodiUri = Some("virhe"))))
    failsValidation(Tallennettu, metadata, validationMsg("virhe"))
  }

  it should "validate kielitaitovaatimukset" in {
    val metadata = AmmValintaperusteMetadata.copy(kielitaitovaatimukset = Seq(Kielitaitovaatimus1.copy(kieliKoodiUri = Some("virhe"))))
    failsValidation(Tallennettu, metadata, validationMsg("virhe"))
  }

  it should "validate kuvaus if julkaistu" in {
    passesValidation(Tallennettu, AmmValintaperusteMetadata.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, AmmValintaperusteMetadata.copy(kuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("kuvaus", Seq(Sv)))
  }

  it should "validate osaamistaustaKoodiUrit for KorkeakoulutusValintaperusteMetadata" in {
    val metadata = YoValintaperusteMetadata.copy(osaamistaustaKoodiUrit = Seq("virhe"))
    failsValidation(Tallennettu, metadata, validationMsg("virhe"))
  }
}

class ValintaperusteKielitaitovaatimusValidationSpec extends SubEntityValidationSpec[ValintaperusteKielitaitovaatimus] {
  "ValintaperusteKielitaitovaatimus validation" should "pass a valid valintaperusteKielitaitovaatimus" in {
    passesValidation(Julkaistu, Kielitaitovaatimus1)
  }

  it should "fail if kieliKoodiUri is invalid" in {
    failsValidation(Tallennettu, Kielitaitovaatimus1.copy(kieliKoodiUri = Some("virhe")), validationMsg("virhe"))
  }

  it should "validate kielitaidonVoiOsoittaa" in {
    failsValidation(Tallennettu, Kielitaitovaatimus1.copy(kielitaidonVoiOsoittaa = Seq(Kielitaito(Some("virhe")))), validationMsg("virhe"))
  }

  it should "fail if kieliKoodiUri is missing in a julkaistu valintaperusteKielitaitovaatimus" in {
    passesValidation(Tallennettu, Kielitaitovaatimus1.copy(kieliKoodiUri = None))
    failsValidation(Julkaistu, Kielitaitovaatimus1.copy(kieliKoodiUri = None), missingMsg("kieliKoodiUri"))
  }
}

class KielitaitoValidationSpec extends SubEntityValidationSpec[Kielitaito] {
  val kielitaito = Kielitaito(kielitaitoKoodiUri = Some("kielitaidonosoittaminen_03#1"), lisatieto = Map(Fi -> "muu", Sv -> "muu sv"))

  "Kielitaito validation" should "pass a valid kielitaito" in {
    passesValidation(Julkaistu, kielitaito)
  }

  it should "fail if kielitaitoKoodiUri is invalid" in {
    failsValidation(Tallennettu, kielitaito.copy(kielitaitoKoodiUri = Some("virhe")), validationMsg("virhe"))
  }

  it should "fail if a julkaistu kielitaito has invalid lisatieto" in {
    passesValidation(Tallennettu, kielitaito.copy(lisatieto = Map(Fi -> "lisatieto")))
    failsValidation(Julkaistu, kielitaito.copy(lisatieto = Map(Fi -> "lisatieto")), invalidKielistetty("lisatieto", Seq(Sv)))
  }
}

class KielitaitovaatimusValidationSpec extends SubEntityValidationSpec[Kielitaitovaatimus] {
  val kielitaitovaatimus = Kielitaitovaatimus(
    kielitaitovaatimusKoodiUri = Some("kielitaitovaatimustyypit_01#1"),
    kielitaitovaatimusKuvaukset = Seq(
      KielitaitovaatimusKuvaus(
        kielitaitovaatimusKuvausKoodiUri = Some("kielitaitovaatimustyypitkuvaus_01#1"),
        kielitaitovaatimusTaso = Some("1"))))

  "Kielitaitovaatimus validation" should "pass a valid kielitaitovaatimus" in {
    passesValidation(Julkaistu, kielitaitovaatimus)
  }

  it should "fail if kielitaitovaatimusKoodiUri is invalid" in {
    failsValidation(Tallennettu, kielitaitovaatimus.copy(kielitaitovaatimusKoodiUri = Some("virhe")), validationMsg("virhe"))
  }

  it should "pass if kielitaitovaatimusKoodiUri is missing" in {
    passesValidation(Julkaistu, kielitaitovaatimus.copy(kielitaitovaatimusKoodiUri = None))
  }

  it should "validate kielitaitovaatimusKuvaukset" in {
    val kielitaitovaatimusKuvaukset = Seq(KielitaitovaatimusKuvaus(kielitaitovaatimusKuvausKoodiUri = Some("virhe")))
    failsValidation(Tallennettu, kielitaitovaatimus.copy(kielitaitovaatimusKuvaukset = kielitaitovaatimusKuvaukset), validationMsg("virhe"))
  }

  it should "pass if kielitaitovaatimusKuvaukset is empty" in {
    passesValidation(Julkaistu, kielitaitovaatimus.copy(kielitaitovaatimusKuvaukset = Seq()))
  }
}

class KielitaitovaatimusKuvausValidationSpec extends SubEntityValidationSpec[KielitaitovaatimusKuvaus] {
  val kielitaitovaatimusKuvaus = KielitaitovaatimusKuvaus(
    kielitaitovaatimusKuvausKoodiUri = Some("kielitaitovaatimustyypitkuvaus_01#1"),
    kielitaitovaatimusTaso = Some("1"))

  "KielitaitovaatimusKuvaus validation" should "pass a valid kielitaitovaatimusKuvaus" in {
    passesValidation(Julkaistu, kielitaitovaatimusKuvaus)
  }

  it should "fail if kielitaitovaatimusKuvausKoodiUri is invalid" in {
    failsValidation(Tallennettu, kielitaitovaatimusKuvaus.copy(kielitaitovaatimusKuvausKoodiUri = Some("virhe")), validationMsg("virhe"))
  }

  it should "pass if kielitaitovaatimusKuvausKoodiUri is invalid" in {
    passesValidation(Julkaistu, kielitaitovaatimusKuvaus.copy(kielitaitovaatimusKuvausKoodiUri = None))
  }

  it should "not care about the value of kielitaitovaatimusTaso" in {
    passesValidation(Julkaistu, kielitaitovaatimusKuvaus.copy(kielitaitovaatimusTaso = None))
    passesValidation(Julkaistu, kielitaitovaatimusKuvaus.copy(kielitaitovaatimusTaso = Some("virhe")))
    passesValidation(Julkaistu, kielitaitovaatimusKuvaus.copy(kielitaitovaatimusTaso = Some("42")))
  }
}
