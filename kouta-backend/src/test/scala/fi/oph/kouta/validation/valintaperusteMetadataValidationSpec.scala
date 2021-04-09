package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.Validations._

class ValintaperusteMetadataValidationSpec extends SubEntityValidationSpec[ValintaperusteMetadata] {

  "ValintaperusteMetadata validation" should "validate valintatavat" in {
    val metadata = AmmValintaperusteMetadata.copy(valintatavat = Seq(Valintatapa1.copy(valintatapaKoodiUri = Some("virhe"))))
    failsValidation(Tallennettu, metadata, "valintatavat[0].valintatapaKoodiUri", validationMsg("virhe"))
  }

  it should "validate kuvaus if julkaistu" in {
    passesValidation(Tallennettu, AmmValintaperusteMetadata.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, AmmValintaperusteMetadata.copy(kuvaus = Map(Fi -> "kuvaus")), "kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "validate hakukelpoisuus if julkaistu" in {
    passesValidation(Tallennettu, AmmValintaperusteMetadata.copy(hakukelpoisuus = Map(Fi -> "hakukelpoisuus")))
    failsValidation(Julkaistu, AmmValintaperusteMetadata.copy(hakukelpoisuus = Map(Fi -> "hakukelpoisuus")), "hakukelpoisuus", invalidKielistetty(Seq(Sv)))
  }

  it should "validate lisatiedot if julkaistu" in {
    passesValidation(Tallennettu, AmmValintaperusteMetadata.copy(lisatiedot = Map(Fi -> "lisatiedot")))
    failsValidation(Julkaistu, AmmValintaperusteMetadata.copy(lisatiedot = Map(Fi -> "lisatiedot")), "lisatiedot", invalidKielistetty(Seq(Sv)))
  }
}
