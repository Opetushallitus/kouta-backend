package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{JulkaistuHaku, JulkaistuHakukohde}
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{HakuOid, ToteutusOid}
import fi.oph.kouta.domain.{AlkamiskausiJaVuosi, Amm, AmmatillinenOsaamisala, AmmatillinenToteutusMetadata, Apuraha, Euro, Fi, Julkaistu, Julkaisutila, KoulutuksenAlkamiskausi, Koulutustyyppi, Lisatieto, Opetus, Sv, Tallennettu, ToteutusMetadata, Yhteyshenkilo}
import fi.oph.kouta.service.HakukohdeServiceValidation
import fi.oph.kouta.validation.{Validations => v}
import org.scalatest.flatspec.AnyFlatSpec

import java.util.UUID

class HakukohdeServiceValidationSpec extends AnyFlatSpec {

  val anyHakukohde = JulkaistuHakukohde
  val anyHaku = Some(JulkaistuHaku)
  val anyDep = (
    Julkaistu,
    None,
    None
  )

  it should "pass validation for Oph-virkailija even though hakukohteen muokkaamisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = anyHaku
    val isOphPaakayttaja = true
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps
    )
    assert(errors.isEmpty)
  }
}

//    val deps = Map(
////      "1.2.246.562.17.00000000000000000001" ->
////      (Julkaistu,
////        Some(Amm),
////        Some(AmmatillinenToteutusMetadata(
////          Amm,
////          Map(),
////          List(AmmatillinenOsaamisala("osaamisala_0001#1", Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"), Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"))),
////          Some(Opetus(
////            List("oppilaitoksenopetuskieli_1#1"),
////            Map(Fi -> "Kielikuvaus fi", Sv -> "Kielikuvaus sv"),
////            List("opetusaikakk_1#1"),
////            Map(Fi -> "Opetusaikakuvaus fi", Sv -> "Opetusaikakuvaus sv"),
////            List("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
////            Map(Fi -> "Opetustapakuvaus fi", Sv -> "Opetustapakuvaus sv"),
////            None,
////            Map(Fi -> "Maksullisuuskuvaus fi", Sv -> "Maksullisuuskuvaus sv"),
////            Some(200.5),
////            Some(KoulutuksenAlkamiskausi(
////              Some(AlkamiskausiJaVuosi),
////              Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
////              None,
////              None,
////              Some("kausi_k#1"),
////              Some("2021")
////            )),
////          List(
////            Lisatieto("koulutuksenlisatiedot_03#1", Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")),
////            Lisatieto("koulutuksenlisatiedot_03#1", Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv"))),true,Some(Apuraha(Some(100),Some(200),Some(Euro),Map(Fi -> "apurahakuvaus fi", Sv -> "apurahakuvaus sv"))),
////            Some(3),Some(10),Map(Fi -> "Keston kuvaus fi", Sv -> "Keston kuvaus sv")))
////          ),
////          List(Keyword(Fi,"robotiikka"), Keyword(Fi,"robottiautomatiikka")),List(Keyword(Fi,"insinööri"), Keyword(Fi,"koneinsinööri")),
////        List(Yhteyshenkilo(
////          Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
////          Map(Fi -> "titteli", Sv -> "titteli sv"),
////          Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
////          Map(Fi -> "123", Sv -> "123"),
////          Map(Fi -> "http://opintopolku.fi, sv -> http://studieinfo.fi"))),false),
////        null,
////        null),
////      "1.2.246.562.29.00000000000000000002" -> (Julkaistu,None,None,"2021-11-04 14:34:00,2021-11-04 14:25:00"),
////      "03849099-da26-46b4-95df-4dab31cd6ca9" -> (Julkaistu,Some(Amm),None,null,null))
