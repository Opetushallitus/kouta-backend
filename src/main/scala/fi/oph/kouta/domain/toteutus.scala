package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait ToteutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteystieto: Option[Yhteystieto]
}

trait KorkeakoulutusToteutusMetadata extends ToteutusMetadata {
  val alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
  val ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
}

trait Osaamisala {
  val linkki: Kielistetty
  val otsikko: Kielistetty
}

case class AmmatillinenOsaamisala(
                                   koodi: Option[String],
                                   linkki: Kielistetty = Map(),
                                   otsikko: Kielistetty = Map(),
                                 ) extends Osaamisala

case class KorkeakouluOsaamisala(
                                  nimi: Kielistetty = Map(),
                                  kuvaus: Kielistetty = Map(),
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map()
                                ) extends Osaamisala

case class YliopistoToteutusMetadata(
                                      tyyppi: Koulutustyyppi = Yo,
                                      kuvaus: Kielistetty = Map(),
                                      osaamisalat: List[Osaamisala] = List(),
                                      opetus: Option[Opetus] = None,
                                      asiasanat: List[Keyword] = List(),
                                      ammattinimikkeet: List[Keyword] = List(),
                                      yhteystieto: Option[Yhteystieto] = None,
                                      alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                      ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()
                                    ) extends KorkeakoulutusToteutusMetadata

case class AmmattikorkeakouluToteutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              osaamisalat: List[Osaamisala] = List(),
                                              opetus: Option[Opetus] = None,
                                              asiasanat: List[Keyword] = List(),
                                              ammattinimikkeet: List[Keyword] = List(),
                                              yhteystieto: Option[Yhteystieto] = None,
                                              alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                              ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()
                                             ) extends KorkeakoulutusToteutusMetadata

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteystieto: Option[Yhteystieto] = None,
                                       ) extends ToteutusMetadata

case class Opetus(opetuskieliKoodiUrit: Seq[String],
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUri: Option[String] = None,
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean] = Some(false),
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Kielistetty = Map(),
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

case class Toteutus(oid: Option[ToteutusOid] = None,
                    koulutusOid: KoulutusOid,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     assertValid(koulutusOid),
     validateIfDefined[ToteutusOid](oid, assertValid(_)),
     validateOidList(tarjoajat)
  )
}

case class ToteutusListItem(oid: ToteutusOid,
                            koulutusOid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem