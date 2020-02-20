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
    failsValidation(amm.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), "muokkaaja", validationMsg("moikka"))
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
      ("tarjoajat[0]", validationMsg("mummo")), ("tarjoajat[1]", validationMsg("varis")))
    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
    failsValidation(amm.copy(teemakuva = Some("mummo")), "teemakuva", invalidUrl("mummo"))
  }

  it should "fail if metadata is invalid" in {
    val metadata = amm.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    failsValidation(amm.copy(metadata = Some(metadata.copy(tyyppi = Muu))), "metadata.tyyppi", InvalidMetadataTyyppi)
    failsValidation(amm.copy(metadata = Some(metadata.copy(kuvaus = Map(Fi -> "kuvaus")))), "metadata.kuvaus", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(metadata = Some(metadata.copy(koulutusalaKoodiUrit = Seq("mummo")))), "metadata.koulutusalaKoodiUrit[0]", validationMsg("mummo"))

    val missingKielivalintaLisatiedot = Seq(Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_32#1", teksti = Map(Fi -> "lisatieto")))
    failsValidation(amm.copy(metadata = Some(metadata.copy(lisatiedot = missingKielivalintaLisatiedot))), "metadata.lisatiedot[0].teksti", invalidKielistetty(Seq(Sv)))
    passesValidation(amm.copy(tila = Tallennettu, metadata = Some(metadata.copy(lisatiedot = missingKielivalintaLisatiedot))))

    val invalidKoodiLisatieto = Seq(Lisatieto("mummo", Map(Fi -> "lisatieto", Sv -> "lisatieto sv")))
    failsValidation(amm.copy(metadata = Some(metadata.copy(lisatiedot = invalidKoodiLisatieto))), "metadata.lisatiedot[0].otsikkoKoodiUri", validationMsg("mummo"))
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
