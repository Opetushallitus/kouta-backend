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
    failsValidation(amm.copy(oid = Some(KoulutusOid("1.2.3"))), validationMsg("1.2.3"))
    failsValidation(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(nimi = Map()), invalidKielistetty("nimi", Seq(Fi, Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete koulutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(oid = Some(KoulutusOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu koulutus is invalid" in {
    failsValidation(amm.copy(johtaaTutkintoon = false), invalidTutkintoonjohtavuus("amm"))
    failsValidation(amm.copy(koulutusKoodiUri = None), missingMsg("koulutusKoodiUri"))
    failsValidation(amm.copy(koulutusKoodiUri = Some("mummo")), validationMsg("mummo"))
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)), invalidOidsMsg(List("mummo", "varis").map(OrganisaatioOid)))
    failsValidation(amm.copy(metadata = None), missingMsg("metadata"))
    failsValidation(amm.copy(teemakuva = Some("mummo")), invalidUrl("mummo"))
  }

  it should "fail if metadata is invalid" in {
    val metadata = amm.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    failsValidation(amm.copy(metadata = Some(metadata.copy(tyyppi = Muu))), InvalidMetadataTyyppi)
    failsValidation(amm.copy(metadata = Some(metadata.copy(kuvaus = Map(Fi -> "kuvaus")))), invalidKielistetty("kuvaus", Seq(Sv)))
    failsValidation(amm.copy(metadata = Some(metadata.copy(koulutusalaKoodiUrit = Seq("mummo")))), validationMsg("mummo"))

    val missingKielivalintaLisatiedot = Seq(Lisatieto("koulutuksenlisatiedot_32#1", Map(Fi -> "lisatieto")))
    failsValidation(amm.copy(metadata = Some(metadata.copy(lisatiedot = missingKielivalintaLisatiedot))), invalidKielistetty("lisatieto", Seq(Sv)))
    passesValidation(amm.copy(tila = Tallennettu).copy(metadata = Some(metadata.copy(lisatiedot = missingKielivalintaLisatiedot))))

    val invalidKoodiLisatieto = Seq(Lisatieto("mummo", Map(Fi -> "lisatieto", Sv -> "lisatieto sv")))
    failsValidation(amm.copy(metadata = Some(metadata.copy(lisatiedot = invalidKoodiLisatieto))), validationMsg("mummo"))
  }

  it should "fail if korkeakoulutus metadata is invalid" in {
    val metadata = yo.metadata.get.asInstanceOf[YliopistoKoulutusMetadata]
    failsValidation(yo.copy(metadata = Some(metadata.copy(kuvauksenNimi = Map(Fi -> "lisatieto")))), invalidKielistetty("kuvauksenNimi", Seq(Sv)))
    passesValidation(yo.copy(tila = Tallennettu).copy(metadata = Some(metadata.copy(kuvauksenNimi = Map(Fi -> "lisatieto")))))

    failsValidation(yo.copy(metadata = Some(metadata.copy(tutkintonimikeKoodiUrit = Seq("mummo")))), validationMsg("mummo"))
    failsValidation(yo.copy(metadata = Some(metadata.copy(opintojenLaajuusKoodiUri = Some("mummo")))), validationMsg("mummo"))
  }

  it should "pass valid ammatillinen koulutus" in {
    passesValidation(amm)
  }

  it should "pass valid korkeakoulutus koulutus" in {
    passesValidation(yo)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(koulutusKoodiUri = Some("ankka"), oid = Some(KoulutusOid("2017"))),
      validationMsg("ankka"), validationMsg("2017"))
  }
}
