package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

class KoulutusValidationSpec extends BaseValidationSpec[Koulutus] {

  val amm: Koulutus = AmmKoulutus
  val yo: Koulutus = YoKoulutus
  val min: Koulutus = MinKoulutus
  val ammTk: Koulutus = AmmTutkinnonOsaKoulutus
  val ammOa: Koulutus = AmmOsaamisalaKoulutus

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
    failsValidation(amm.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(yo.copy(koulutuksetKoodiUri = Seq("mummo", "väärä")),
      ValidationError("koulutuksetKoodiUri[0]", validationMsg("mummo")),
      ValidationError("koulutuksetKoodiUri[1]", validationMsg("väärä")))
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)),
      ValidationError("tarjoajat[0]", validationMsg("mummo")),
      ValidationError("tarjoajat[1]", validationMsg("varis")),
      ValidationError("tarjoajat[2]", validationMsg("1.2.3")))
    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
    failsValidation(amm.copy(teemakuva = Some("mummo")), "teemakuva", invalidUrl("mummo"))
  }

  it should "contain only one koulutusKoodiUri if not korkeakoulutus" in {
    failsValidation(amm.copy(koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201000#1")), "koulutuksetKoodiUri", tooManyKoodiUris)
  }

  it should "require koulutuksetKoodiUri for julkaistu korkeakoulutus" in {
    failsValidation(yo.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
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

  it should "pass amm tutkinnon osa koulutus" in {
    passesValidation(ammTk)
  }

  it should "fail with wrong tutkintoon johtavuus" in {
    failsValidation(amm.copy(johtaaTutkintoon = false), "johtaaTutkintoon", invalidTutkintoonjohtavuus(amm.koulutustyyppi.toString))
    failsValidation(yo.copy(johtaaTutkintoon = false), "johtaaTutkintoon", invalidTutkintoonjohtavuus(yo.koulutustyyppi.toString))
    failsValidation(ammTk.copy(johtaaTutkintoon = true), "johtaaTutkintoon", invalidTutkintoonjohtavuus(ammTk.koulutustyyppi.toString))
    failsValidation(ammOa.copy(johtaaTutkintoon = true), "johtaaTutkintoon", invalidTutkintoonjohtavuus(ammOa.koulutustyyppi.toString))
  }

  it should "fail if amm tutkinnon osa has ePerusteId or koulutusKoodi" in {
    failsValidation(ammTk.copy(ePerusteId = Some(123)), "ePerusteId", notMissingMsg(Some("123")))
    failsValidation(ammTk.copy(koulutuksetKoodiUri = Seq("koulutus_371101#1")), "koulutuksetKoodiUri", notEmptyMsg)
  }

  it should "pass amm osaamisala koulutus" in {
    passesValidation(ammOa)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(koulutuksetKoodiUri = Seq("ankka"), oid = Some(KoulutusOid("2017"))),
      ValidationError("koulutuksetKoodiUri[0]", validationMsg("ankka")),
      ValidationError("oid", validationMsg("2017")))
  }
}

class KoulutusMetadataValidationSpec extends SubEntityValidationSpec[KoulutusMetadata] {

  val amm: KoulutusMetadata = AmmKoulutus.metadata.get
  val yo: YliopistoKoulutusMetadata = YoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata]
  val min: AmmatillinenKoulutusMetadata = AmmatillinenKoulutusMetadata()
  val ammOa: AmmatillinenOsaamisalaKoulutusMetadata = AmmOsaamisalaKoulutus.metadata.get.asInstanceOf[AmmatillinenOsaamisalaKoulutusMetadata]
  val ammTo: AmmatillinenTutkinnonOsaKoulutusMetadata = AmmTutkinnonOsaKoulutus.metadata.get.asInstanceOf[AmmatillinenTutkinnonOsaKoulutusMetadata]
  val lukio: LukioKoulutusMetadata = LukioKoulutus.metadata.get.asInstanceOf[LukioKoulutusMetadata]
  val tuva: TuvaKoulutusMetadata = TuvaKoulutus.metadata.get.asInstanceOf[TuvaKoulutusMetadata]

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

  "Ammatillinen osaamisala koulutus metadata validator" should "pass valid metadata" in {
    passesValidation(Julkaistu, ammOa)
  }

  it should "fail if osaamisala koodi URI is invalid or missing" in {
    failsValidation(Julkaistu, ammOa.copy(osaamisalaKoodiUri = None), "osaamisalaKoodiUri", missingMsg)
    failsValidation(Julkaistu, ammOa.copy(osaamisalaKoodiUri = Some("mummo")), "osaamisalaKoodiUri", validationMsg("mummo"))
  }

  "Ammatillinen tutkinnon osa koulutus metadata validator" should "pass valid metadata" in {
    passesValidation(Julkaistu, ammTo)
  }

  it should "fail if no tutkinnon osat defined or tutkinnon osa is invalid" in {
    failsValidation(Julkaistu, ammTo.copy(tutkinnonOsat = Seq()), "tutkinnonOsat", missingMsg)
    failsValidation(Julkaistu, ammTo.copy(tutkinnonOsat = Seq(TutkinnonOsa(Some(123L), Some("mummo"), Some(123L), Some(123L)))), "tutkinnonOsat[0].koulutusKoodiUri", validationMsg("mummo"))
    failsValidation(Julkaistu, ammTo.copy(tutkinnonOsat = Seq(TutkinnonOsa(None, Some("koulutus_371101#1"), Some(123L), Some(123L)))), "tutkinnonOsat[0].ePerusteId", missingMsg)
  }

  "Lukio metadata validation" should "pass valid metadata" in {
    passesValidation(Julkaistu, lukio)
  }

  it should "fail if any opintojenLaajuusKoodiUri is invalid" in {
    failsValidation(Tallennettu, lukio.copy(opintojenLaajuusKoodiUri = Some("mummo")), "opintojenLaajuusKoodiUri", validationMsg("mummo"))
  }

  it should "fail if any koulutusalaKoodiUrit are invalid" in {
    failsValidation(Tallennettu, lukio.copy(koulutusalaKoodiUrit = Seq("mummo")), "koulutusalaKoodiUrit[0]", validationMsg("mummo"))
  }

  "Tuva metadata validation" should "pass valid metadata" in {
    passesValidation(Tallennettu, tuva)
  }

  it should "fail if linkkiEPerusteisiin is invalid" in {
    failsValidation(Julkaistu, tuva.copy(linkkiEPerusteisiin = Map(Fi -> "linkki", Sv -> "http://example.com")), "linkkiEPerusteisiin.fi", invalidUrl("linkki"))
  }

  it should "fail if kuvaus has missing languages in a julkaistu tuva koulutus" in {
    passesValidation(Tallennettu, tuva.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, tuva.copy(kuvaus = Map(Fi -> "kuvaus")), "kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if kuvaus is missing from julkaistu tuva" in {
    failsValidation(Julkaistu, tuva.copy(kuvaus = Map()), "kuvaus", invalidKielistetty(Seq(Fi, Sv)))
  }
}
