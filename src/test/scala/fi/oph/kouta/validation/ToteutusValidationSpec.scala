package fi.oph.kouta.validation

import java.time.LocalDateTime

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.domain._
import org.scalatest.Assertion

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] with Validations {

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

  it should "fail if julkaistu opetus is invalid" in {
    val opetus = ToteutuksenOpetus
    failsValidation(opetus.copy(opetuskieliKoodiUrit = Seq("mummo")), validationMsg("mummo"))
    failsValidation(opetus.copy(opetusaikaKoodiUrit = Seq("mummo")), validationMsg("mummo"))
    failsValidation(opetus.copy(opetustapaKoodiUrit = Seq("mummo")), validationMsg("mummo"))
    failsValidation(opetus.copy(koulutuksenAlkamispaivamaara = Some(LocalDateTime.now().plusDays(10)), koulutuksenPaattymispaivamaara = Some(LocalDateTime.now().plusDays(1))), InvalidKoulutuspaivamaarat)
    failsValidation(opetus.copy(stipendinMaara = Some(-2)), notNegativeMsg("stipendinMaara"))

    failsValidation(opetus.copy(opetuskieliKoodiUrit = Seq()), missingMsg("opetuskieliKoodiUrit"))
    failsValidation(opetus.copy(opetusaikaKoodiUrit = Seq()), missingMsg("opetusaikaKoodiUrit"))
    failsValidation(opetus.copy(opetustapaKoodiUrit = Seq()), missingMsg("opetustapaKoodiUrit"))

    failsValidation(opetus.copy(opetuskieletKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetuskieletKuvaus", Seq(Sv)))
    failsValidation(opetus.copy(opetusaikaKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetusaikaKuvaus", Seq(Sv)))
    failsValidation(opetus.copy(opetustapaKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("opetustapaKuvaus", Seq(Sv)))

    failsValidation(opetus.copy(onkoMaksullinen = None), missingMsg("onkoMaksullinen"))
    failsValidation(opetus.copy(maksullisuusKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("maksullisuusKuvaus", Seq(Sv)))
    failsValidation(opetus.copy(maksunMaara = Some(-1000)), notNegativeMsg("maksunMaara"))
    failsValidation(opetus.copy(onkoMaksullinen = Some(true), maksunMaara = None), missingMsg("maksunMaara"))

    failsValidation(opetus.copy(koulutuksenTarkkaAlkamisaika = true, koulutuksenAlkamispaivamaara = None), missingMsg("koulutuksenAlkamispaivamaara"))
    failsValidation(opetus.copy(koulutuksenTarkkaAlkamisaika = false, koulutuksenAlkamiskausi = None, koulutuksenAlkamisvuosi = Some(2020)), missingMsg("koulutuksenAlkamiskausi"))
    failsValidation(opetus.copy(koulutuksenTarkkaAlkamisaika = false, koulutuksenAlkamiskausi = Some("kausi_k#1"), koulutuksenAlkamisvuosi = None), missingMsg("koulutuksenAlkamisvuosi"))
    failsValidation(opetus.copy(koulutuksenAlkamiskausi = Some("virhe")), validationMsg("virhe"))
    failsValidation(opetus.copy(koulutuksenAlkamisvuosi = Some(1)), validationMsg("1"))

    failsValidation(opetus.copy(onkoStipendia = None), missingMsg("onkoStipendia"))
    failsValidation(opetus.copy(onkoStipendia = Some(true), stipendinMaara = None), missingMsg("stipendinMaara"))
    failsValidation(opetus.copy(stipendinKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("stipendinKuvaus", Seq(Sv)))
  }

  it should "fail if lisatiedot are invalid" in {
    val opetus = ammMetadata.opetus.get
    val lisatieto = ammMetadata.opetus.get.lisatiedot.head
    failsValidation(opetus.copy(lisatiedot = Seq(lisatieto.copy(otsikkoKoodiUri = "mummo"))) , validationMsg("mummo"))
    failsValidation(opetus.copy(lisatiedot = Seq(lisatieto.copy(teksti = Map(Fi -> "lisatieto")))) , invalidKielistetty("lisatieto", Seq(Sv)))
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
      List(validationMsg("kurppa"), validationMsg("Hannu Hanhi")))
  }
}
