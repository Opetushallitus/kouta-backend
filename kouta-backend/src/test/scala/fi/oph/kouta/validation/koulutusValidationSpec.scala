package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

class KoulutusValidationSpec extends BaseValidationSpec[Koulutus] {

  val amm = AmmKoulutus
  val yo = YoKoulutus
  val min = MinKoulutus

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(KoulutusOid("1.2.3"))), "oid", validationMsg("1.2.3"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "pass incomplete koulutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(oid = Some(KoulutusOid("1.2.3"))), "oid", validationMsg("1.2.3"))
  }

  it should "fail if julkaistu koulutus is invalid" in {
    failsValidation(amm.copy(johtaaTutkintoon = false), "johtaaTutkintoon", invalidTutkintoonjohtavuus("amm"))
    failsValidation(amm.copy(koulutusKoodiUri = None), "koulutusKoodiUri", missingMsg)
    failsValidation(amm.copy(koulutusKoodiUri = Some("mummo")), "koulutusKoodiUri", validationMsg("mummo"))
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)),
      ("tarjoajat[0]", validationMsg("mummo")), ("tarjoajat[1]", validationMsg("varis")), ("tarjoajat[2]", validationMsg("1.2.3")))
    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
    failsValidation(amm.copy(teemakuva = Some("mummo")), "teemakuva", invalidUrl("mummo"))
  }

  it should "validate metadata" in {
    val metadata = amm.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    failsValidation(amm.copy(metadata = Some(metadata.copy(kuvaus = Map(Fi -> "kuvaus")))), "metadata.kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if the tyyppi of the metadata differs from the tyyppi of the koulutus" in {
    val metadata = amm.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    failsValidation(amm.copy(metadata = Some(metadata.copy(tyyppi = Muu))), "metadata.tyyppi", InvalidMetadataTyyppi)
  }

  it should "fail if ePeruste ID is negative or missing for a ammatillinen koulutus" in {
    failsValidation(amm.copy(ePerusteId = Some(-3)), "ePerusteId", notNegativeMsg)
    passesValidation(yo.copy(ePerusteId = None))
    failsValidation(amm.copy(ePerusteId = None), "ePerusteId", missingMsg)
  }

  it should "fail if korkeakoulutus metadata is invalid" in {
    val metadata = yo.metadata.get.asInstanceOf[YliopistoKoulutusMetadata]
    failsValidation(yo.copy(metadata = Some(metadata.copy(kuvauksenNimi = Map(Fi -> "lisatieto")))), "metadata.kuvauksenNimi", invalidKielistetty(Seq(Sv)))
    passesValidation(yo.copy(tila = Tallennettu).copy(metadata = Some(metadata.copy(kuvauksenNimi = Map(Fi -> "lisatieto")))))

    failsValidation(yo.copy(metadata = Some(metadata.copy(tutkintonimikeKoodiUrit = Seq("mummo")))), "metadata.tutkintonimikeKoodiUrit[0]", validationMsg("mummo"))
    failsValidation(yo.copy(metadata = Some(metadata.copy(opintojenLaajuusKoodiUri = Some("mummo")))), "metadata.opintojenLaajuusKoodiUri", validationMsg("mummo"))
  }

  it should "pass valid ammatillinen koulutus" in {
    passesValidation(amm)
  }

  it should "pass valid korkeakoulutus koulutus" in {
    passesValidation(yo)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(koulutusKoodiUri = Some("ankka"), oid = Some(KoulutusOid("2017"))),
      ("koulutusKoodiUri", validationMsg("ankka")), ("oid", validationMsg("2017")))
  }
}

class KoulutusMetadataValidationSpec extends SubEntityValidationSpec[KoulutusMetadata] {

  val amm = AmmKoulutus.metadata.get
  val yo = YoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata]
  val min = AmmatillinenKoulutusMetadata()

  "Koulutus metadata validator" should "pass a valid metadata" in {
    passesValidation(Julkaistu, amm)
  }

  it should "fail if kuvaus has missing languages in a julkaistu koulutus" in {
    passesValidation(Tallennettu, min.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, min.copy(kuvaus = Map(Fi -> "kuvaus")), "kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if any koulutusalaKoodiUrit are invalid" in {
    failsValidation(Tallennettu, yo.copy(koulutusalaKoodiUrit = Seq("mummo")), "koulutusalaKoodiUrit[0]", validationMsg("mummo"))
  }

  it should "validate lisatiedot" in {
    val missingKielivalintaLisatiedot = Seq(Lisatieto(otsikkoKoodiUri = "invalid", teksti = Map(Fi -> "lisatieto", Sv -> "lisatieto")))
    failsValidation(Tallennettu, min.copy(lisatiedot = missingKielivalintaLisatiedot), "lisatiedot[0].otsikkoKoodiUri", validationMsg("invalid"))
  }

  "Korkeakoulutus koulutus metadata validator" should "pass a valid korkeakoulutus metadata" in {
    passesValidation(Julkaistu, yo)
  }

  it should "fail if kuvauksen nimi has missing languages in a julkaistu koulutus" in {
    passesValidation(Tallennettu, yo.copy(kuvauksenNimi = Map(Fi -> "kuvauksenNimi")))
    failsValidation(Julkaistu, yo.copy(kuvauksenNimi = Map(Fi -> "kuvauksenNimi")), "kuvauksenNimi", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if any tutkintonimikeKoodiUrit are invalid" in {
    failsValidation(Tallennettu, yo.copy(tutkintonimikeKoodiUrit = Seq("mummo")), "tutkintonimikeKoodiUrit[0]", validationMsg("mummo"))
  }

  it should "succeed with any valid tutkintonimikeKoodiUri" in {
    passesValidation(Tallennettu, yo.copy(tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_y19#2")))
    passesValidation(Tallennettu, yo.copy(tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_19#2")))
  }

  it should "fail if any opintojenLaajuusKoodiUri is invalid" in {
    failsValidation(Tallennettu, yo.copy(opintojenLaajuusKoodiUri = Some("mummo")), "opintojenLaajuusKoodiUri", validationMsg("mummo"))
  }
}
