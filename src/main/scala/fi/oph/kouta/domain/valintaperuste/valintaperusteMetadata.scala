package fi.oph.kouta.domain.valintaperuste

import fi.oph.kouta.domain._

sealed trait ValintaperusteMetadata {
  def koulutustyyppi: Koulutustyyppi
  def valintatavat: Seq[Valintatapa]
  def kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus]
}

sealed trait KorkeakoulutusValintaperusteMetadata extends ValintaperusteMetadata {
  def valintatavat: Seq[KorkeakoulutusValintatapa]
  def kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus]
  def osaamistaustaKoodiUrit: Seq[String]
  def kuvaus: Kielistetty
}

case class AmmatillinenValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Amm,
                                              valintatavat: Seq[AmmatillinenValintatapa],
                                              kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus])
    extends ValintaperusteMetadata

case class YliopistoValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Yo,
                                           valintatavat: Seq[YliopistoValintatapa],
                                           kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                           osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                           kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class AmmattikorkeakouluValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Amk,
                                                    valintatavat: Seq[AmmattikorkeakouluValintatapa],
                                                    kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                                    osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                                    kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class ValintaperusteKielitaitovaatimus(kieliKoodiUri: Option[String] = None,
                                            kielitaidonVoiOsoittaa: Seq[Kielitaito] = Seq(),
                                            vaatimukset: Seq[Kielitaitovaatimus] = Seq())

case class Kielitaito(kielitaitoKoodiUri: Option[String] = None, lisatieto: Kielistetty = Map())

case class Kielitaitovaatimus(kielitaitovaatimusKoodiUri: Option[String] = None, kielitaitovaatimusKuvaukset: Seq[KielitaitovaatimusKuvaus] = Seq())

case class KielitaitovaatimusKuvaus(kielitaitovaatimusKuvausKoodiUri: Option[String] = None, kielitaitovaatimusTaso: Option[String] = None)
