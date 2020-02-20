package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.Validations._

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] {

  val amm = JulkaistuAmmToteutus
  val ammMetadata = AmmToteutuksenMetatieto
  val yo = JulkaistuYoToteutus
  val yoMetadata = YoToteutuksenMetaTieto
  val min = MinToteutus

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(ToteutusOid("1.2.3"))), "oid", validationMsg("1.2.3"))
    failsValidation(amm.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), "muokkaaja", validationMsg("moikka"))
  }

  it should "pass incomplete toteutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(koulutusOid = KoulutusOid("1.2.3")), "koulutusOid", validationMsg("1.2.3"))
  }

  it should "fail if julkaistu toteutus is invalid" in {
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)),
      ("tarjoajat[0]", validationMsg("mummo")), ("tarjoajat[1]", validationMsg("varis")))

    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
    failsValidation(amm.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
  }

  it should "fail if toteutus metadata is invalid" in {
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(kuvaus = Map(Fi -> "kuvaus")))), "metadata.kuvaus", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = None))), "metadata.opetus", missingMsg)
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = Nil))), "metadata.osaamisalat", missingMsg)
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
    val osaamisala = YoToteutuksenMetaTieto.alemmanKorkeakoulututkinnonOsaamisalat.head

    val missingName = osaamisala.copy(nimi = Map(), kuvaus = Map())
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingName)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].nimi", invalidKielistetty(Seq(Fi, Sv)))

    val missingFinnishNimi = osaamisala.copy(nimi = Map(Sv -> "nimi"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingFinnishNimi)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].nimi", invalidKielistetty(Seq(Fi)))

    val missingSwedishKuvaus = osaamisala.copy(kuvaus = Map(Fi -> "kuvaus"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingSwedishKuvaus)))), "metadata.alemmanKorkeakoulututkinnonOsaamisalat[0].kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = AmmToteutuksenMetatieto.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(amm.copy(metadata = Some(metadata)), "metadata.yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "pass valid ammatillinen toteutus" in {
    passesValidation(amm)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(oid = Some(ToteutusOid("kurppa")), muokkaaja = UserOid("Hannu Hanhi")),
      ("oid", validationMsg("kurppa")), ("muokkaaja", validationMsg("Hannu Hanhi")))
  }
}

class OpetusValidationSpec extends SubEntityValidationSpec[Opetus] {
  val opetus = ToteutuksenOpetus

  val past = TestData.inPast(1000)
  val moreInPast = TestData.inPast(9000)

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

    passesValidation(Tallennettu, opetus.copy(stipendinKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(stipendinKuvaus = Map(Fi -> "kuvaus")), "stipendinKuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if alkamispaivamaarat are invalid" in {
    val o = opetus.copy(koulutuksenAlkamispaivamaara = Some(TestData.inFuture(9000)), koulutuksenPaattymispaivamaara = Some(TestData.inFuture(300)))
    failsValidation(Tallennettu, o, "koulutuksenAlkamispaivamaara", InvalidKoulutuspaivamaarat)
  }

  it should "fail if stipendinMaara is negative" in {
    failsValidation(Tallennettu, opetus.copy(stipendinMaara = Some(-2)), "stipendinMaara", notNegativeMsg)
  }

  it should "fail if onkoMaksullinen is missing in a julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(onkoMaksullinen = None))
    failsValidation(Julkaistu, opetus.copy(onkoMaksullinen = None), "onkoMaksullinen", missingMsg)
  }

  it should "fail if maksunMaara is negative" in {
    failsValidation(Tallennettu, opetus.copy(maksunMaara = Some(-1000)), "maksunMaara", notNegativeMsg)
  }

  it should "fail a julkaistu, maksullinen opetus if maksunMaara is not defined" in {
    passesValidation(Tallennettu, opetus.copy(onkoMaksullinen = Some(true), maksunMaara = None))
    failsValidation(Julkaistu, opetus.copy(onkoMaksullinen = Some(true), maksunMaara = None), "maksunMaara", missingMsg)
  }

  it should "fail if julkaistu opetus is missing koulutuksenAlkamispaivamaara" in {
    passesValidation(Tallennettu, opetus.copy(koulutuksenAlkamispaivamaara = None))
    failsValidation(Julkaistu, opetus.copy(koulutuksenAlkamispaivamaara = None), "koulutuksenAlkamispaivamaara", missingMsg)
  }

  it should "fail if onkoStipendia is missing in a julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(onkoStipendia = None))
    failsValidation(Julkaistu, opetus.copy(onkoStipendia = None), "onkoStipendia", missingMsg)
  }

  it should "fail if a julkaistu, stipendillinen opetus is missing stipendinMaara" in {
    passesValidation(Tallennettu, opetus.copy(onkoStipendia = Some(true), stipendinMaara = None))
    failsValidation(Julkaistu, opetus.copy(onkoStipendia = Some(true), stipendinMaara = None), "stipendinMaara", missingMsg)
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

  it should "fail a julkaistu opetus if koulutuksen koulutuksenAlkamispaivamaara is in the past" in {
    passesValidation(Julkaistu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast)))
    failsOnJulkaisuValidation(opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast)), "koulutuksenAlkamispaivamaara", pastDateMsg(moreInPast))
  }

  it should "fail a julkaistu opetus if koulutuksen koulutuksenPaattymispaivamaara is in the past" in {
    passesValidation(Arkistoitu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast), koulutuksenPaattymispaivamaara = Some(past)))

    failsOnJulkaisuValidation(opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast), koulutuksenPaattymispaivamaara = Some(past)),
      ("koulutuksenAlkamispaivamaara", pastDateMsg(moreInPast)), ("koulutuksenPaattymispaivamaara", pastDateMsg(past)))
  }
}
