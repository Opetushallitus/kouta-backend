package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.validation.Validations._

class ValintaperusteValidationSpec extends BaseValidationSpec[Valintaperuste] {

  val max = YoValintaperuste
  val min = MinYoValintaperuste

  it should "fail if perustiedot is invalid" in {
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
  }

  it should "pass incomplete valintaperuste if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if any valintaperuste is invalid" in {
    failsValidation(min.copy(hakutapaKoodiUri = Some("korppi")), "hakutapaKoodiUri", validationMsg("korppi"))
    failsValidation(min.copy(kohdejoukkoKoodiUri = Some("kerttu")), "kohdejoukkoKoodiUri", validationMsg("kerttu"))
    failsValidation(min.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), "kohdejoukonTarkenneKoodiUri", validationMsg("tonttu"))
  }

  it should "validate valintakokeet" in {
    failsValidation(min.copy(valintakokeet = List(Valintakoe1.copy(tyyppiKoodiUri = Some("koodi")))), "valintakokeet[0].tyyppiKoodiUri", validationMsg("koodi"))
  }

  it should "validate metadata" in {
    val invalidMetadata = YoValintaperusteMetadata.copy(valintatavat = Seq(Valintatapa1.copy(valintatapaKoodiUri = Some("virhe"))))
    failsValidation(min.copy(metadata = Some(invalidMetadata)), "metadata.valintatavat[0].valintatapaKoodiUri", validationMsg("virhe"))
  }

  it should "fail if julkaistu valintaperuste is invalid" in {
    failsValidation(max.copy(hakutapaKoodiUri = None), "hakutapaKoodiUri", missingMsg)
    failsValidation(max.copy(kohdejoukkoKoodiUri = None), "kohdejoukkoKoodiUri", missingMsg)
    failsValidation(max.copy(sorakuvausId = None), "sorakuvausId", missingMsg)
  }

  it should "pass valid julkaistu valintaperuste" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(max.copy(hakutapaKoodiUri = None, kohdejoukkoKoodiUri = None),
      ("hakutapaKoodiUri", missingMsg), ("kohdejoukkoKoodiUri", missingMsg))
  }
}

