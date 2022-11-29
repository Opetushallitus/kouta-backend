package fi.oph.kouta.validation

import fi.oph.kouta.domain._

case class KoulutusDiffResolver(koulutus: Koulutus, oldKoulutus: Option[Koulutus]) {
  private def oldMetadata(): Option[KoulutusMetadata] = oldKoulutus.flatMap(_.metadata)

  def newNimi(): Option[Kielistetty] =
    if (oldKoulutus.map(_.nimi).getOrElse(Map()) != koulutus.nimi) Some(koulutus.nimi) else None

  def newKoulutusKoodiUrit(): Seq[String] =
    if (oldKoulutus.map(_.koulutuksetKoodiUri).getOrElse(Seq()).toSet != koulutus.koulutuksetKoodiUri.toSet)
      koulutus.koulutuksetKoodiUri
    else Seq()

  def newEPerusteId(): Option[Long] =
    if (oldKoulutus.flatMap(_.ePerusteId) != koulutus.ePerusteId) koulutus.ePerusteId else None

  def newLisatiedot(): Seq[Lisatieto] = {
    val lisatiedot = koulutus.metadata.map(_.lisatiedot).getOrElse(Seq())
    if (oldMetadata().map(_.lisatiedot).getOrElse(Seq()).toSet != lisatiedot.toSet) lisatiedot else Seq()
  }

  def newTutkinnonosat(): Seq[TutkinnonOsa] = {
    val osat = tutkinnonosat(koulutus.metadata)
    if (osat.toSet != tutkinnonosat(oldMetadata()).toSet) osat else Seq()
  }

  def newOsaamisalaKoodiUri(): Option[String] = {
    val koodiUri = osaamisalaKoodiUri(koulutus.metadata)
    if (osaamisalaKoodiUri(oldMetadata()) != koodiUri) koodiUri else None
  }

  def newKoulutusalaKoodiUrit(): Seq[String] = {
    val koodiUrit = koulutusalaKoodiUrit(koulutus.metadata)
    if (koodiUrit.toSet != koulutusalaKoodiUrit(oldMetadata()).toSet) koodiUrit else Seq()
  }

  def newOpintojenLaajuusyksikkoKoodiUri(): Option[String] = {
    val koodiUri = opintojenLaajuusyksikkoKoodiUri(koulutus.metadata)
    if (koodiUri != opintojenLaajuusyksikkoKoodiUri(oldMetadata())) koodiUri else None
  }

  def newTutkintonimikeKoodiUrit(): Seq[String] = {
    val koodiUrit = tutkintonimikeKoodiUrit(koulutus.metadata)
    if (koodiUrit.toSet != tutkintonimikeKoodiUrit(oldMetadata()).toSet) koodiUrit else Seq()
  }

  def newErikoistumiskoulutusKoodiUri(): Option[String] = {
    val koodiUri = erikoitumiskoulutusKoodiUri(koulutus.metadata)
    if (koodiUri != erikoitumiskoulutusKoodiUri(oldMetadata())) koodiUri else None
  }

  private def tutkinnonosat(metadata: Option[KoulutusMetadata]): Seq[TutkinnonOsa] =
    metadata match {
      case Some(m: AmmatillinenTutkinnonOsaKoulutusMetadata) =>
        m.tutkinnonOsat
      case _ => Seq()
    }

  private def osaamisalaKoodiUri(metadata: Option[KoulutusMetadata]): Option[String] =
    metadata match {
      case Some(m: AmmatillinenOsaamisalaKoulutusMetadata) => m.osaamisalaKoodiUri
      case _                                               => None
    }

  private def koulutusalaKoodiUrit(metadata: Option[KoulutusMetadata]): Seq[String] =
    metadata match {
      case Some(m: KorkeakoulutusKoulutusMetadata)      => m.koulutusalaKoodiUrit
      case Some(m: AmmatillinenMuuKoulutusMetadata)     => m.koulutusalaKoodiUrit
      case Some(m: VapaaSivistystyoKoulutusMetadata)    => m.koulutusalaKoodiUrit
      case Some(m: KkOpintojaksoKoulutusMetadata)       => m.koulutusalaKoodiUrit
      case Some(m: KkOpintokokonaisuusKoulutusMetadata) => m.koulutusalaKoodiUrit
      case Some(m: ErikoislaakariKoulutusMetadata)      => m.koulutusalaKoodiUrit
      case Some(m: ErikoistumiskoulutusMetadata)        => m.koulutusalaKoodiUrit
      case Some(m: LukioKoulutusMetadata)               => m.koulutusalaKoodiUrit
      case _                                            => Seq()
    }

  private def opintojenLaajuusyksikkoKoodiUri(metadata: Option[KoulutusMetadata]): Option[String] =
    metadata match {
      case Some(m: AmmatillinenMuuKoulutusMetadata)      => m.opintojenLaajuusyksikkoKoodiUri
      case Some(m: VapaaSivistystyoMuuKoulutusMetadata)  => m.opintojenLaajuusyksikkoKoodiUri
      case Some(m: AikuistenPerusopetusKoulutusMetadata) => m.opintojenLaajuusyksikkoKoodiUri
      case Some(m: KkOpintojaksoKoulutusMetadata)        => m.opintojenLaajuusyksikkoKoodiUri
      case Some(m: KkOpintokokonaisuusKoulutusMetadata)  => m.opintojenLaajuusyksikkoKoodiUri
      case Some(m: ErikoistumiskoulutusMetadata)         => m.opintojenLaajuusyksikkoKoodiUri
      case _                                             => None
    }

  private def tutkintonimikeKoodiUrit(metadata: Option[KoulutusMetadata]): Seq[String] =
    metadata match {
      case Some(m: KorkeakoulutusKoulutusMetadata) => m.tutkintonimikeKoodiUrit
      case Some(m: ErikoislaakariKoulutusMetadata) => m.tutkintonimikeKoodiUrit
      case _                                       => Seq()
    }
  private def erikoitumiskoulutusKoodiUri(metadata: Option[KoulutusMetadata]): Option[String] =
    metadata match {
      case Some(m: ErikoistumiskoulutusMetadata) => m.erikoistumiskoulutusKoodiUri
      case _                                     => None
    }
}
