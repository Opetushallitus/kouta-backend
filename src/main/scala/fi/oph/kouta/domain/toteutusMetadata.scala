package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword

sealed trait ToteutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilo: Option[Yhteyshenkilo]
}

trait KorkeakoulutusToteutusMetadata extends ToteutusMetadata {
  val alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
  val ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
}

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilo: Option[Yhteyshenkilo] = None) extends ToteutusMetadata

case class YliopistoToteutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     opetus: Option[Opetus] = None,
                                     asiasanat: List[Keyword] = List(),
                                     ammattinimikkeet: List[Keyword] = List(),
                                     yhteyshenkilo: Option[Yhteyshenkilo] = None,
                                     alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                     ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

case class AmmattikorkeakouluToteutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              opetus: Option[Opetus] = None,
                                              asiasanat: List[Keyword] = List(),
                                              ammattinimikkeet: List[Keyword] = List(),
                                              yhteyshenkilo: Option[Yhteyshenkilo] = None,
                                              alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                              ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

trait Osaamisala {
  val linkki: Kielistetty
  val otsikko: Kielistetty
}

case class AmmatillinenOsaamisala(koodi: String,
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map()) extends Osaamisala

case class KorkeakouluOsaamisala(nimi: Kielistetty = Map(),
                                 kuvaus: Kielistetty = Map(),
                                 linkki: Kielistetty = Map(),
                                 otsikko: Kielistetty = Map()) extends Osaamisala

case class Opetus(opetuskieliKoodiUrit: Seq[String],
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean] = Some(false),
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  alkamiskausiKoodiUri: Option[String] = None,
                  alkamisvuosi: Option[String] = None,
                  alkamisaikaKuvaus: Kielistetty = Map(),
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoLukuvuosimaksua: Option[Boolean] = Some(false),
                  lukuvuosimaksu: Kielistetty = Map(),
                  lukuvuosimaksuKuvaus: Kielistetty = Map(),
                  onkoStipendia: Option[Boolean] = Some(false),
                  stipendinMaara: Kielistetty = Map(),
                  stipendinKuvaus: Kielistetty = Map())
