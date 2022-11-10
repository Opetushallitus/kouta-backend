package fi.oph.kouta.validation

import fi.oph.kouta.domain._

case class KoulutusDiffResolver(koulutus: Koulutus, oldKoulutus: Option[Koulutus]) {
  private def oldMetadata(): Option[KoulutusMetadata] = oldKoulutus.flatMap(_.metadata)

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

  private def tutkinnonosat(metadata: Option[KoulutusMetadata]): Seq[TutkinnonOsa] =
    if (!metadata.isDefined) Seq()
    else
        metadata.get match {
          case m: AmmatillinenTutkinnonOsaKoulutusMetadata =>
            m.tutkinnonOsat
          case _ => Seq()
        }

  private def koulutusalaKoodiUrit(metadata: Option[KoulutusMetadata]): Seq[String] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: KorkeakoulutusKoulutusMetadata  => m.koulutusalaKoodiUrit
        case m: AmmatillinenMuuKoulutusMetadata => m.koulutusalaKoodiUrit
        case m: VapaaSivistystyoKoulutusMetadata => m.koulutusalaKoodiUrit
        case m: KkOpintojaksoKoulutusMetadata => m.koulutusalaKoodiUrit
        case m: KkOpintokokonaisuusKoulutusMetadata => m.koulutusalaKoodiUrit
        case m: ErikoislaakariKoulutusMetadata => m.koulutusalaKoodiUrit
        case m: LukioKoulutusMetadata => m.koulutusalaKoodiUrit
        case _                                  => Seq()
      }

  private def opintojenLaajuusyksikkoKoodiUri(metadata: Option[KoulutusMetadata]): Option[String] =
    if (!metadata.isDefined) None
    else
      metadata.get match {
        case m: AmmatillinenMuuKoulutusMetadata => m.opintojenLaajuusyksikkoKoodiUri
        case m: VapaaSivistystyoMuuKoulutusMetadata => m.opintojenLaajuusyksikkoKoodiUri
        case m: AikuistenPerusopetusKoulutusMetadata => m.opintojenLaajuusyksikkoKoodiUri
        case m: KkOpintojaksoKoulutusMetadata => m.opintojenLaajuusyksikkoKoodiUri
        case m: KkOpintokokonaisuusKoulutusMetadata => m.opintojenLaajuusyksikkoKoodiUri
        case _ => None
      }

  private def tutkintonimikeKoodiUrit(metadata: Option[KoulutusMetadata]): Seq[String] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: KorkeakoulutusKoulutusMetadata  => m.tutkintonimikeKoodiUrit
        case m: ErikoislaakariKoulutusMetadata => m.tutkintonimikeKoodiUrit
        case _                                  => Seq()
      }

}
