package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.Validations._
import org.scalatest.Assertion

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] {

  val amm = JulkaistuAmmToteutus
  val ammMetadata = AmmToteutuksenMetatieto
  val yo = JulkaistuYoToteutus
  val yoMetadata = YoToteutuksenMetaTieto
  val min = MinToteutus

  def failsValidation(opetus: Opetus, expected: List[String], tila: Julkaisutila): Assertion =
    opetus.validate(tila, Seq(Fi, Sv)) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs (expected)
    }

  def failsValidation(opetus: Opetus, expected: String, tila: Julkaisutila = Julkaistu): Assertion =
    opetus.validate(tila, Seq(Fi, Sv)) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors => errors should contain theSameElementsAs Seq(expected)
    }

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(ToteutusOid("1.2.3"))), validationMsg("1.2.3"))
    failsValidation(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass incomplete toteutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(koulutusOid = KoulutusOid("1.2.3")), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu toteutus is invalid" in {
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)), invalidOidsMsg(List("mummo", "varis").map(OrganisaatioOid)))
    failsValidation(amm.copy(metadata = None), missingMsg("metadata"))
    failsValidation(amm.copy(teemakuva = Some("url")), invalidUrl("url"))
  }

  it should "fail if toteutus metadata is invalid" in {
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(kuvaus = Map(Fi -> "kuvaus")))), invalidKielistetty("kuvaus", Seq(Sv)))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = None))), missingMsg("opetus"))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = Nil))), missingMsg("osaamisalat"))
  }

  it should "fail if ammatillinen osaamisala is invalid" in {
    val osaamisala = AmmToteutuksenMetatieto.osaamisalat.head

    val invalidUrlList = List(osaamisala.copy(linkki = Map(Fi -> "url", Sv -> "http://osaaminen.fi/")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = invalidUrlList))), invalidUrl("url"))

    val missingSwedishLink = List(osaamisala.copy(linkki = Map(Fi -> "http://osaaminen.fi/")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = missingSwedishLink))), invalidKielistetty("linkki", Seq(Sv)))

    val missingSwedishOtsikko = List(osaamisala.copy(otsikko = Map(Fi -> "otsikko")))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = missingSwedishOtsikko))), invalidKielistetty("otsikko", Seq(Sv)))

    val invalidKoodiUri = List(osaamisala.copy(koodiUri = "mummo"))
    failsValidation(amm.copy(metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = invalidKoodiUri))), validationMsg("mummo"))
  }

  it should "fail if korkeakoulutus osaamisala is invalid" in {
    val osaamisala = YoToteutuksenMetaTieto.alemmanKorkeakoulututkinnonOsaamisalat.head

    val missingName = osaamisala.copy(nimi = Map(), kuvaus = Map())
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingName)))), invalidKielistetty("nimi", Seq(Fi, Sv)))

    val missingFinnishNimi = osaamisala.copy(nimi = Map(Sv -> "nimi"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingFinnishNimi)))), invalidKielistetty("nimi", Seq(Fi)))

    val missingSwedishKuvaus = osaamisala.copy(kuvaus = Map(Fi -> "kuvaus"))
    failsValidation(yo.copy(metadata = Some(YoToteutuksenMetaTieto.copy(alemmanKorkeakoulututkinnonOsaamisalat = Seq(missingSwedishKuvaus)))), invalidKielistetty("kuvaus", Seq(Sv)))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = AmmToteutuksenMetatieto.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(amm.copy(metadata = Some(metadata)), invalidKielistetty("nimi", Seq(Fi, Sv)))
  }

  it should "pass valid ammatillinen toteutus" in {
    passesValidation(amm)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(oid = Some(ToteutusOid("kurppa")), muokkaaja = UserOid("Hannu Hanhi")),
      validationMsg("kurppa"), validationMsg("Hannu Hanhi"))
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
    failsValidation(Julkaistu, opetus.copy(opetuskieliKoodiUrit = Seq()), missingMsg("opetuskieliKoodiUrit"))

    passesValidation(Tallennettu, opetus.copy(opetusaikaKoodiUrit = Seq()))
    failsValidation(Julkaistu, opetus.copy(opetusaikaKoodiUrit = Seq()), missingMsg("opetusaikaKoodiUrit"))

    passesValidation(Tallennettu, opetus.copy(opetustapaKoodiUrit = Seq()))
    failsValidation(Julkaistu, opetus.copy(opetustapaKoodiUrit = Seq()), missingMsg("opetustapaKoodiUrit"))
  }

  it should "fail if koodiUrit are invalid" in {
    failsValidation(Tallennettu, opetus.copy(opetuskieliKoodiUrit = Seq("mummo")), validationMsg("mummo"))
    failsValidation(Tallennettu, opetus.copy(opetusaikaKoodiUrit = Seq("mummo")), validationMsg("mummo"))
    failsValidation(Tallennettu, opetus.copy(opetustapaKoodiUrit = Seq("mummo")), validationMsg("mummo"))
  }

  it should "fail if kuvaukset are missing languages in julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(opetuskieletKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetuskieletKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetuskieletKuvaus", Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(opetusaikaKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetusaikaKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetusaikaKuvaus", Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(opetustapaKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(opetustapaKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetustapaKuvaus", Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(maksullisuusKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(maksullisuusKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("maksullisuusKuvaus", Seq(Sv)))

    passesValidation(Tallennettu, opetus.copy(stipendinKuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, opetus.copy(stipendinKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("stipendinKuvaus", Seq(Sv)))
  }

  it should "fail if alkamispaivamaarat are invalid" in {
    failsValidation(Tallennettu, opetus.copy(koulutuksenAlkamispaivamaara = Some(TestData.inFuture(9000)), koulutuksenPaattymispaivamaara = Some(TestData.inFuture(300))), InvalidKoulutuspaivamaarat)
  }

  it should "fail if stipendinMaara is negative" in {
    failsValidation(Tallennettu, opetus.copy(stipendinMaara = Some(-2)), notNegativeMsg("stipendinMaara"))
  }

  it should "fail if onkoMaksullinen is missing in a julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(onkoMaksullinen = None))
    failsValidation(Julkaistu, opetus.copy(onkoMaksullinen = None), missingMsg("onkoMaksullinen"))
  }

  it should "fail if maksunMaara is negative" in {
    failsValidation(Tallennettu, opetus.copy(maksunMaara = Some(-1000)), notNegativeMsg("maksunMaara"))
  }

  it should "fail a julkaistu, maksullinen opetus if maksunMaara is not defined" in {
    passesValidation(Tallennettu, opetus.copy(onkoMaksullinen = Some(true), maksunMaara = None))
    failsValidation(Julkaistu, opetus.copy(onkoMaksullinen = Some(true), maksunMaara = None), missingMsg("maksunMaara"))
  }

  it should "fail if julkaistu opetus is missing koulutuksenAlkamispaivamaara" in {
    passesValidation(Tallennettu, opetus.copy(koulutuksenAlkamispaivamaara = None))
    failsValidation(Julkaistu, opetus.copy(koulutuksenAlkamispaivamaara = None), missingMsg("koulutuksenAlkamispaivamaara"))
  }

  it should "fail a julkaistu opetus if koulutuksen koulutuksenAlkamispaivamaara is in the past" in {
    passesValidation(Arkistoitu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast)))
    failsValidation(Julkaistu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast)), pastDateMsg(moreInPast, "koulutuksenAlkamispaivamaara"))
  }

  it should "fail a julkaistu opetus if koulutuksen koulutuksenPaattymispaivamaara is in the past" in {
    passesValidation(Arkistoitu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast), koulutuksenPaattymispaivamaara = Some(past)))

    failsValidation(Julkaistu, opetus.copy(koulutuksenAlkamispaivamaara = Some(moreInPast), koulutuksenPaattymispaivamaara = Some(past)),
      pastDateMsg(moreInPast, "koulutuksenAlkamispaivamaara"), pastDateMsg(past, "koulutuksenPaattymispaivamaara"))
  }

  it should "fail if onkoStipendia is missing in a julkaistu opetus" in {
    passesValidation(Tallennettu, opetus.copy(onkoStipendia = None))
    failsValidation(Julkaistu, opetus.copy(onkoStipendia = None), missingMsg("onkoStipendia"))
  }

  it should "fail if a julkaistu, stipendillinen opetus is missing stipendinMaara" in {
    passesValidation(Tallennettu, opetus.copy(onkoStipendia = Some(true), stipendinMaara = None))
    failsValidation(Julkaistu, opetus.copy(onkoStipendia = Some(true), stipendinMaara = None), missingMsg("stipendinMaara"))
  }

  it should "fail if lisatiedot are invalid" in {
    val lisatieto = Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
      teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv"))

    failsValidation(Julkaistu, opetus.copy(lisatiedot = Seq(lisatieto.copy(otsikkoKoodiUri = "mummo"))), validationMsg("mummo"))
    failsValidation(Julkaistu, opetus.copy(lisatiedot = Seq(lisatieto.copy(teksti = Map(Fi -> "lisatieto")))), invalidKielistetty("lisatieto", Seq(Sv)))
  }
}
