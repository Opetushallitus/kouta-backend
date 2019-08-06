package fi.oph.kouta.domain

sealed trait KoulutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Map[Kieli, String]
  val lisatiedot: Seq[Lisatieto]
  val koulutusalaKoodiUrit: Seq[String]
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val kuvauksenNimi: Map[Kieli, String]
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Map[Kieli, String] = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        koulutusalaKoodiUrit: Seq[String] = Seq()) extends KoulutusMetadata

case class YliopistoKoulutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Map[Kieli, String] = Map(),
                                     lisatiedot: Seq[Lisatieto] = Seq(),
                                     koulutusalaKoodiUrit: Seq[String] = Seq(),
                                     tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                     opintojenLaajuusKoodiUri: Option[String] = None,
                                     kuvauksenNimi: Map[Kieli, String] = Map()) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Map[Kieli, String] = Map(),
                                              lisatiedot: Seq[Lisatieto] = Seq(),
                                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                                              tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                              opintojenLaajuusKoodiUri: Option[String] = None,
                                              kuvauksenNimi: Map[Kieli, String] = Map()) extends KorkeakoulutusKoulutusMetadata