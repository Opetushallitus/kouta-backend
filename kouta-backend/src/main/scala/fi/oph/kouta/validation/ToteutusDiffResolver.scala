package fi.oph.kouta.validation

import fi.oph.kouta.domain.{
  AmmatillinenOsaamisala,
  AmmatillinenToteutusMetadata,
  Kielivalikoima,
  KoulutuksenAlkamiskausi,
  Lisatieto,
  LukioToteutusMetadata,
  LukiodiplomiTieto,
  LukiolinjaTieto,
  Opetus,
  Toteutus,
  ToteutusMetadata
}

case class ToteutusDiffResolver(toteutus: Toteutus, oldToteutus: Option[Toteutus]) {
  private def oldMetadata(): Option[ToteutusMetadata] = oldToteutus.flatMap(_.metadata)
  private def opetus(): Option[Opetus]                = toteutus.metadata.flatMap(_.opetus)
  private def oldOpetus(): Option[Opetus]             = oldMetadata().flatMap(_.opetus)
  private def koulutuksenAlkamiskausi(): Option[KoulutuksenAlkamiskausi] =
    opetus().flatMap(_.koulutuksenAlkamiskausi)
  private def oldKoulutuksenAlkamiskausi(): Option[KoulutuksenAlkamiskausi] =
    oldOpetus().flatMap(_.koulutuksenAlkamiskausi)

  def koulutustyyppiChanged(): Boolean =
    oldMetadata().isDefined && toteutus.metadata.isDefined && oldMetadata().get.tyyppi != toteutus.metadata.get.tyyppi

  def newOpetuskieliKoodiUrit(): Seq[String] = {
    val koodiUrit = opetus().map(_.opetuskieliKoodiUrit).getOrElse(Seq())
    if (oldOpetus().map(_.opetuskieliKoodiUrit).getOrElse(Seq()).toSet != koodiUrit.toSet) koodiUrit else Seq()
  }

  def newOpetusaikaKoodiUrit(): Seq[String] = {
    val koodiUrit = opetus().map(_.opetusaikaKoodiUrit).getOrElse(Seq())
    if (oldOpetus().map(_.opetusaikaKoodiUrit).getOrElse(Seq()).toSet != koodiUrit.toSet) koodiUrit else Seq()
  }

  def newOpetustapaKoodiUrit(): Seq[String] = {
    val koodiUrit = opetus().map(_.opetustapaKoodiUrit).getOrElse(Seq())
    if (oldOpetus().map(_.opetustapaKoodiUrit).getOrElse(Seq()).toSet != koodiUrit.toSet) koodiUrit else Seq()
  }

  def koulutuksenAlkamiskausiWithNewValues(): Option[KoulutuksenAlkamiskausi] = {
    val alkamiskausiKoodiUri    = koulutuksenAlkamiskausi().flatMap(_.koulutuksenAlkamiskausiKoodiUri)
    val oldAlkamiskausiKoodiUri = oldKoulutuksenAlkamiskausi().flatMap(_.koulutuksenAlkamiskausiKoodiUri)
    if (alkamiskausiKoodiUri != oldAlkamiskausiKoodiUri)
      Some(KoulutuksenAlkamiskausi(koulutuksenAlkamiskausiKoodiUri = alkamiskausiKoodiUri))
    else None
  }

  def newLisatiedot(): Seq[Lisatieto] = {
    val lisatiedot = opetus().map(_.lisatiedot).getOrElse(Seq())
    if (oldOpetus().map(_.lisatiedot).getOrElse(Seq()).toSet != lisatiedot.toSet) lisatiedot else Seq()
  }

  def newAmmatillisetOsaamisalat(): Seq[AmmatillinenOsaamisala] = {
    val osaamisalat = ammatillisetOsaamisalat(toteutus.metadata)
    if (ammatillisetOsaamisalat(oldMetadata()).toSet != osaamisalat.toSet) osaamisalat else Seq()
  }

  def newLukioPainotukset(): Seq[LukiolinjaTieto] = {
    val painotukset = lukioPainotukset(toteutus.metadata)
    if (lukioPainotukset(oldMetadata()).toSet != painotukset.toSet) painotukset else Seq()
  }

  def newLukioErityisetKoulutustehtavat(): Seq[LukiolinjaTieto] = {
    val erityisetKoulutustehtavat = lukioErityisetKoulutustehtavat(toteutus.metadata)
    if (lukioErityisetKoulutustehtavat(oldMetadata()).toSet != erityisetKoulutustehtavat.toSet)
      erityisetKoulutustehtavat
    else Seq()
  }

  def newLukioDiplomit(): Seq[LukiodiplomiTieto] = {
    val diplomit = lukioDiplomit(toteutus.metadata)
    if (lukioDiplomit(oldMetadata()).toSet != diplomit.toSet) diplomit else Seq()
  }

  def newA1Kielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.A1Kielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.A1Kielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newA2Kielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.A2Kielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.A2Kielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newB1Kielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.B1Kielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.B1Kielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newB2Kielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.B2Kielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.B2Kielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newB3Kielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.B3Kielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.B3Kielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newAidinkielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.aidinkielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.aidinkielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  def newMuutkielet(): Seq[String] = {
    val kielet = kielivalikoima(toteutus.metadata).map(_.muutKielet).getOrElse(Seq())
    if (kielivalikoima(oldMetadata()).map(_.muutKielet).getOrElse(Seq()).toSet != kielet.toSet) kielet else Seq()
  }

  private def ammatillisetOsaamisalat(metadata: Option[ToteutusMetadata]): Seq[AmmatillinenOsaamisala] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: AmmatillinenToteutusMetadata => m.osaamisalat
        case _                               => Seq()
      }

  private def kielivalikoima(metadata: Option[ToteutusMetadata]): Option[Kielivalikoima] =
    if (!metadata.isDefined) None
    else
      metadata.get match {
        case m: LukioToteutusMetadata => m.kielivalikoima
        case _                        => None
      }

  private def lukioPainotukset(metadata: Option[ToteutusMetadata]): Seq[LukiolinjaTieto] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: LukioToteutusMetadata => m.painotukset
        case _                        => Seq()
      }

  private def lukioErityisetKoulutustehtavat(metadata: Option[ToteutusMetadata]): Seq[LukiolinjaTieto] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: LukioToteutusMetadata => m.erityisetKoulutustehtavat
        case _                        => Seq()
      }

  private def lukioDiplomit(metadata: Option[ToteutusMetadata]): Seq[LukiodiplomiTieto] =
    if (!metadata.isDefined) Seq()
    else
      metadata.get match {
        case m: LukioToteutusMetadata => m.diplomit
        case _                        => Seq()
      }
}