package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.validation.Validations._

import java.time.LocalDateTime

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] {

  val amm: Toteutus = JulkaistuAmmToteutus
  val ammMetadata: AmmatillinenToteutusMetadata = AmmToteutuksenMetatieto
  val lukioMetadata: LukioToteutusMetadata = LukioToteutuksenMetatieto
  val yo: Toteutus = JulkaistuYoToteutus
  val yoMetadata: YliopistoToteutusMetadata = YoToteutuksenMetatieto
  val min: Toteutus = MinToteutus
  val ammOa: Toteutus = AmmOsaamisalaToteutus
  val ammTo: Toteutus = AmmTutkinnonOsaToteutus
  val lukioTo: Toteutus = LukioToteutus
  val tuvaTo: Toteutus = TuvaToteutus
  val tuvaMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto
  val telmaTo: Toteutus = TelmaToteutus
  val telmaMetadata: TelmaToteutusMetadata = TelmaToteutuksenMetatieto
  val vapaaSivistystyoOpistovuosiToteutusMetadata = VapaaSivistystyoOpistovuosiToteutusMetatieto
  val vapaaSivistystyoMuuMetadata = VapaaSivistystyoMuuToteutusMetatieto
  val vapaaSivistystyoOpistovuosiTo = VapaaSivistystyoOpistovuosiToteutus
  val vapaaSivistystyoMuuTo = VapaaSivistystyoMuuToteutus

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(ToteutusOid("1.2.3"))), "oid", validationMsg("1.2.3"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
  }

  it should "pass incomplete toteutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(koulutusOid = KoulutusOid("1.2.3")), "koulutusOid", validationMsg("1.2.3"))
  }

  it should "fail if julkaistu toteutus is invalid" in {
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)),
      ValidationError("tarjoajat[0]", validationMsg("mummo")),
      ValidationError("tarjoajat[1]", validationMsg("varis")),
      ValidationError("tarjoajat[2]", validationMsg("1.2.3")))

    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
    failsValidation(amm.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
  }

  it should "fail if toteutus metadata is invalid" in {
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(kuvaus = Map(Fi -> "kuvaus")))), "metadata.kuvaus", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = None))), "metadata.opetus", missingMsg)
  }

  it should "fail if ammatillinen osaamisala is invalid" in {
    val osaamisala = AmmToteutuksenMetatieto.osaamisalat.head

    val invalidUrlList = List(osaamisala.copy(linkki = Map(Fi -> "url", Sv -> "http://osaaminen.fi/")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = invalidUrlList))), "metadata.osaamisalat[0].linkki.fi", invalidUrl("url"))

    val missingSwedishLink = List(osaamisala.copy(linkki = Map(Fi -> "http://osaaminen.fi/")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = missingSwedishLink))), "metadata.osaamisalat[0].linkki", invalidKielistetty(Seq(Sv)))

    val missingSwedishOtsikko = List(osaamisala.copy(otsikko = Map(Fi -> "otsikko")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = missingSwedishOtsikko))), "metadata.osaamisalat[0].otsikko", invalidKielistetty(Seq(Sv)))

    val invalidKoodiUri = List(osaamisala.copy(koodiUri = "mummo"))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = invalidKoodiUri))), "metadata.osaamisalat[0].koodiUri", validationMsg("mummo"))
  }

  it should "fail if korkeakoulutus osaamisala is invalid" in {
    val osaamisala = YoToteutuksenMetatieto.alemmanKorkeakoulututkinnonOsaamisalat.head

    val missingName = osaamisala.copy(nimi = Map(), kuvaus = Map())
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetatieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingName)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].nimi", invalidKielistetty(Seq(Fi, Sv)))

    val missingFinnishNimi = osaamisala.copy(nimi = Map(Sv -> "nimi"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetatieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingFinnishNimi)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].nimi", invalidKielistetty(Seq(Fi)))

    val missingSwedishKuvaus = osaamisala.copy(kuvaus = Map(Fi -> "kuvaus"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetatieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingSwedishKuvaus)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = AmmToteutuksenMetatieto.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(amm.copy(metadata = Some(metadata)), "metadata.yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "pass valid ammatillinen toteutus" in {
    passesValidation(amm)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(oid = Some(ToteutusOid("kurppa")), koulutusOid = KoulutusOid("Hannu Hanhi")),
      ValidationError("oid", validationMsg("kurppa")),
      ValidationError("koulutusOid", validationMsg("Hannu Hanhi")))
  }

  "Ammatillinen osaamisala toteutus validation" should "pass valid toteutus" in {
    passesValidation(ammOa)
    passesValidation(ammOa.copy(metadata = Some(AmmOsaamisalaToteutusMetadataHakemuspalvelu)))
    passesValidation(ammOa.copy(metadata = Some(AmmOsaamisalaToteutusMetadataEiSahkoista)))
  }

  it should "fail if metadata is invalid with muu hakulomake" in {
    val ammOaMetadata = ammOa.metadata.get.asInstanceOf[AmmatillinenOsaamisalaToteutusMetadata]

    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(hakulomakeLinkki = Map(Fi -> "url", Sv -> "http://osaaminen.fi/")))), "metadata.hakulomakeLinkki.fi", invalidUrl("url"))
    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(hakulomakeLinkki =  Map(Fi -> "http://osaaminen.fi/")))), "metadata.hakulomakeLinkki", invalidKielistetty(Seq(Sv)))
    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(lisatietoaHakeutumisesta =  Map(Fi -> "Lisätieto")))), "metadata.lisatietoaHakeutumisesta", invalidKielistetty(Seq(Sv)))
    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(lisatietoaValintaperusteista =  Map(Fi -> "Lisätieto")))), "metadata.lisatietoaValintaperusteista", invalidKielistetty(Seq(Sv)))

    val invalidAjanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = Some(TestData.inFuture(9000)))
    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(hakuaika = Some(invalidAjanjakso)))), "metadata.hakuaika", invalidAjanjaksoMsg(invalidAjanjakso))
    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(hakuaika = None))), "metadata.hakuaika", missingMsg)

    failsValidation(ammOa.copy(metadata = Some(ammOaMetadata.copy(hakulomaketyyppi = None))), "metadata.hakulomaketyyppi", missingMsg)
  }

  "Ammatillinen tutkinnon osa toteutus validation" should "pass valid toteutus" in {
    passesValidation(ammTo)
    passesValidation(ammTo.copy(metadata = Some(AmmTutkinnonOsaToteutusMetadataHakemuspalvelu)))
    passesValidation(ammTo.copy(metadata = Some(AmmTutkinnonOsaToteutusMetadataEiSahkoista)))
  }

  "Lukio validation" should "pass valid lukio toteutus" in {
    passesValidation(lukioTo)
  }

  it should "pass with empty painotukset and erityiset koulutustehtävät when yleislinja=true" in {
    passesValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      painotukset = Seq(),
      erityisetKoulutustehtavat = Seq(),
      yleislinja = true
    ))))
  }

  it should "fail with empty painotukset and erityiset koulutustehtävät when yleislinja=false" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      painotukset = Seq(),
      erityisetKoulutustehtavat = Seq(),
      yleislinja = false
    ))), "metadata.yleislinja", withoutLukiolinja)
  }

  it should "fail when julkaistu if only one kuvaus given for any lukiolinja painotus" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      painotukset = Seq(LukiolinjaTieto(
        koodiUri = "lukiopainotukset_0000#1",
        kuvaus = Map(Fi -> "Painotus 0 fi")
        )
      )))), "metadata.painotukset[0].kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if using wrong koodisto for lukiolinja painotus" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      painotukset = Seq(
        LukiolinjaTieto(
          koodiUri = "asdf_1#1",
          kuvaus = Map(Fi -> "Kuvaus fi", Sv -> "Kuvaus sv")
        )
      )
    ))), "metadata.painotukset[0].koodiUri", validationMsg("asdf_1#1"))
  }

  it should "fail when julkaistu if only one kuvaus given for any lukiolinja erityinen koulutustehtävä" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      erityisetKoulutustehtavat = Seq(
        LukiolinjaTieto(
          koodiUri = "lukiolinjaterityinenkoulutustehtava_0000#1",
          kuvaus = Map(Fi -> "Koulutustehtava 0 fi")
        )
      )
    ))), "metadata.erityisetKoulutustehtavat[0].kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if using wrong koodisto for lukiolinja erityiset koulutustehtävät" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      erityisetKoulutustehtavat = Seq(
        LukiolinjaTieto(
          koodiUri = "asdf_1#1",
          kuvaus = Map(Fi -> "Kuvaus fi", Sv -> "Kuvaus sv")
        )
      )
    ))), "metadata.erityisetKoulutustehtavat[0].koodiUri", validationMsg("asdf_1#1"))
  }

  it should "fail if kielivalikoima is invalid" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(A1Kielet = Seq("mummo")))))), "metadata.kielivalikoima.A1Kielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(A2Kielet = Seq("mummo")))))), "metadata.kielivalikoima.A2Kielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(B1Kielet = Seq("mummo")))))), "metadata.kielivalikoima.B1Kielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(B2Kielet = Seq("mummo")))))), "metadata.kielivalikoima.B2Kielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(B3Kielet = Seq("mummo")))))), "metadata.kielivalikoima.B3Kielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(aidinkielet = Seq("mummo")))))), "metadata.kielivalikoima.aidinkielet[0]", validationMsg("mummo"))
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(kielivalikoima = Some(Kielivalikoima(muutKielet = Seq("mummo")))))), "metadata.kielivalikoima.muutKielet[0]", validationMsg("mummo"))
  }
  
  it should "pass with empty diplomit" in {
    passesValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      diplomit = Seq()
    ))))
  }

  it should "fail if using wrong koodisto for lukiodiplomi" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      diplomit = Seq(
        LukiodiplomiTieto(
          koodiUri = "asdf_1#1",
          linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"),
          linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
        )
      )
    ))), "metadata.diplomit[0].koodiUri", validationMsg("asdf_1#1"))
  }

  it should "fail if lukiodiplomi link is not a valid URL" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      diplomit = Seq(
        LukiodiplomiTieto(
          koodiUri = "moduulikoodistolops2021_kald3#1",
          linkki = Map(Fi -> "ei linkki", Sv -> "http://link.se"),
          linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
        )
      )
    ))), "metadata.diplomit[0].linkki.fi", invalidUrl("ei linkki"))
  }

  it should "fail if lukiodiplomi link is only given in some of the selected languages" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      diplomit = Seq(
        LukiodiplomiTieto(
          koodiUri = "moduulikoodistolops2021_kald3#1",
          linkki = Map(Fi -> "http://linkki.fi"),
          linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
        )
      )
    ))), "metadata.diplomit[0].linkki", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if lukiodiplomi link alt text is only given in some of the selected languages" in {
    failsValidation(lukioTo.copy(metadata = Some(lukioMetadata.copy(
      diplomit = Seq(
        LukiodiplomiTieto(
          koodiUri = "moduulikoodistolops2021_kald3#1",
          linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"),
          linkinAltTeksti = Map(Fi -> "Suomeksi")
        )
      )
    ))), "metadata.diplomit[0].linkinAltTeksti", invalidKielistetty(Seq(Sv)))
  }

  "Tuva validation" should "pass valid tuva toteutus" in {
    passesValidation(tuvaTo)
  }

  it should "fail if kuvaus is missing from tuva" in {
    failsValidation(tuvaTo.copy(metadata = Some(tuvaMetadata.copy(
      kuvaus = Map()
    ))), "metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv)))
  }

  "Telma validation" should "pass valid telma toteutus" in {
    passesValidation(telmaTo)
  }

  it should "fail if kuvaus is missing from telma" in {
    failsValidation(telmaTo.copy(metadata = Some(telmaMetadata.copy(
      kuvaus = Map()
    ))), "metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv)))
  }

  "Vapaa sivistystyö opistovuosi validation" should "pass valid vapaa sivistystyö opistovuosi toteutus" in {
    passesValidation(vapaaSivistystyoOpistovuosiTo)
  }

    "Vapaa sivistystyö muu validation" should "pass valid vapaa sivistystyö muu toteutus" in {
    passesValidation(vapaaSivistystyoOpistovuosiTo)
  }
}

class OpetusValidationSpec extends SubEntityValidationSpec[Opetus] {
  val opetus: Opetus = ToteutuksenOpetus

  val past: LocalDateTime = TestData.inPast(1000)
  val moreInPast: LocalDateTime = TestData.inPast(9000)

  val apuraha: Apuraha = Apuraha(min = Some(50), max = Some(70), yksikko = Some(Prosentti), kuvaus = Map(Fi -> "kuvaus"))

  "Opetus validation" should "pass a valid opetus" in {
    passesValidation(Julkaistu, opetus)
  }

  it should "fail if koodiUrit are missing in julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(opetuskieliKoodiUrit = Seq()))
    failsValidation(Julkaistu, opetus.copy(opetuskieliKoodiUrit = Seq()), "opetuskieliKoodiUrit", missingMsg)

    passesValidation(Tallennettu, opetus.copy(opetusaikaKoodiUrit = Seq()))
    failsValidation(Julkaistu, opetus.copy(opetusaikaKoodiUrit = Seq()), "opetusaikaKoodiUrit", missingMsg)

    passesValidation(Tallennettu, opetus.copy(opetustapaKoodiUrit = Seq()))
    failsValidation(Julkaistu, opetus.copy(opetustapaKoodiUrit = Seq()), "opetustapaKoodiUrit", missingMsg)
  }

  it should "fail if koodiUrit are invalid" in {
    failsValidation(Tallennettu, opetus.copy(opetuskieliKoodiUrit = Seq("mummo")), "opetuskieliKoodiUrit[0]", validationMsg("mummo"))
    failsValidation(Tallennettu, opetus.copy(opetusaikaKoodiUrit = Seq("mummo")), "opetusaikaKoodiUrit[0]", validationMsg("mummo"))
    failsValidation(Tallennettu, opetus.copy(opetustapaKoodiUrit = Seq("mummo")), "opetustapaKoodiUrit[0]", validationMsg("mummo"))
  }

  it should "fail if kuvaukset are missing languages in julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(opetuskieletKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetuskieletKuvaus = Map(Fi -> "kuvaus")), "opetuskieletKuvaus", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(opetusaikaKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetusaikaKuvaus = Map(Fi -> "kuvaus")), "opetusaikaKuvaus", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(opetustapaKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetustapaKuvaus = Map(Fi -> "kuvaus")), "opetustapaKuvaus", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(maksullisuusKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(maksullisuusKuvaus = Map(Fi -> "kuvaus")), "maksullisuusKuvaus", invalidKielistetty(Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(apuraha = Some(apuraha)))
    failsValidation(Julkaistu, opetus.copy(apuraha = Some(apuraha)), "apuraha.kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if apuraha is negative" in {
    failsValidation(Tallennettu, opetus.copy(apuraha = Some(apuraha.copy(min = Some(-10)))), "apuraha.min", notNegativeMsg)
  }

  it should "fail if yksikko is prosentti and max is more than 100" in {
    val prosentti = 120
    val maxProsentti = 100
    failsValidation(Tallennettu, opetus.copy(
      apuraha = Some(apuraha.copy(yksikko = Some(Prosentti), max = Some(prosentti)))), "apuraha.max", lessOrEqualMsg(prosentti, maxProsentti))
  }

  it should "fail if maksullisuustyyppi is missing in a julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(maksullisuustyyppi = None))
    failsValidation(Julkaistu, opetus.copy(maksullisuustyyppi = None), "maksullisuustyyppi", missingMsg)
  }

  it should "fail if maksunMaara is negative" in {
    failsValidation(Tallennettu, opetus.copy(maksunMaara = Some(-1000)), "maksunMaara", notNegativeMsg)
  }

  it should "fail a julkaistu, maksullinen opetus if maksunMaara is not defined" in {
    passesValidation(Tallennettu, opetus.copy(maksullisuustyyppi = Some(Maksuton), maksunMaara = None))
    failsValidation(Julkaistu, opetus.copy(maksullisuustyyppi = Some(Maksullinen), maksunMaara = None), "maksunMaara", missingMsg)
    failsValidation(Julkaistu, opetus.copy(maksullisuustyyppi = Some(Lukuvuosimaksu), maksunMaara = None), "maksunMaara", missingMsg)
  }

  it should "fail if a julkaistu apuraha is missing min or max" in {
    passesValidation(Tallennettu, opetus.copy(onkoApuraha = true, apuraha = Some(apuraha.copy(min = None))))
    passesValidation(Tallennettu, opetus.copy(onkoApuraha = true, apuraha = Some(apuraha.copy(max = None))))
    failsValidation(Julkaistu, opetus.copy(
      onkoApuraha = true,
      apuraha = Some(apuraha.copy(
        min = None,
        kuvaus = Map(Fi -> "", Sv -> "")))), "apuraha.min", missingMsg)
    failsValidation(Julkaistu, opetus.copy(
      onkoApuraha = true,
      apuraha = Some(
        apuraha.copy(max = None, kuvaus
          = Map(Fi -> "", Sv -> "")))), "apuraha.max", missingMsg)
  }

  it should "fail if lisatiedot are invalid" in {
    val lisatieto = Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
      teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv"))

    failsValidation(Julkaistu, opetus.copy(lisatiedot = Seq(lisatieto.copy(otsikkoKoodiUri = "mummo"))), "lisatiedot[0].otsikkoKoodiUri", validationMsg("mummo"))
    failsValidation(Julkaistu, opetus.copy(lisatiedot = Seq(lisatieto.copy(teksti = Map(Fi -> "lisatieto")))), "lisatiedot[0].teksti", invalidKielistetty(Seq(Sv)))
  }

  "Opetus on julkaisu validation" should "pass a valid opetus" in {
    passesOnJulkaisuValidation(opetus)
  }

  it should "fail if suunniteltu kesto is not valid" in {
    failsValidation(Tallennettu, opetus.copy(suunniteltuKestoVuodet = Some(-1)), "suunniteltuKestoVuodet", notNegativeMsg)
    failsValidation(Tallennettu, opetus.copy(suunniteltuKestoKuukaudet = Some(-1)), "suunniteltuKestoKuukaudet", notNegativeMsg)
    failsValidation(Julkaistu, opetus.copy(suunniteltuKestoKuvaus = Map(Sv -> "moi")), "suunniteltuKestoKuvaus", invalidKielistetty(Seq(Fi)))
  }
}
