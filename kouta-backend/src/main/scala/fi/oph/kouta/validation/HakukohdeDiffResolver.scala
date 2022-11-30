package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Hakukohde, HakukohdeMetadata, Kielistetty, KoulutuksenAlkamiskausi, Liite, Osoite, PainotettuOppiaine, Valintakoe, ValintakokeenLisatilaisuudet}

import java.util.UUID

case class HakukohdeDiffResolver(hakukohde: Hakukohde, oldHakukohde: Option[Hakukohde]) {
  private def oldMetadata(): Option[HakukohdeMetadata] =
    oldHakukohde.flatMap(_.metadata)

  private def koulutuksenAlkamiskausiKoodiUri(metadata: Option[HakukohdeMetadata]): Option[String] =
    metadata.flatMap(_.koulutuksenAlkamiskausi).flatMap(_.koulutuksenAlkamiskausiKoodiUri)
  private def painotetutArvosanat(metadata: Option[HakukohdeMetadata]): Seq[PainotettuOppiaine] =
    metadata.flatMap(_.hakukohteenLinja).map(_.painotetutArvosanat).getOrElse(Seq())

  def newNimi(): Option[Kielistetty] =
    if (oldHakukohde.map(_.nimi).getOrElse(Map()) != hakukohde.nimi) Some(hakukohde.nimi) else None

  def newHakukohdeKoodiUri(): Option[String] =
    if (oldHakukohde.flatMap(_.hakukohdeKoodiUri) != hakukohde.hakukohdeKoodiUri)
      hakukohde.hakukohdeKoodiUri
    else None

  def newPohjakoulutusvaatimusKoodiUrit(): Seq[String] =
    if (
      oldHakukohde
        .map(_.pohjakoulutusvaatimusKoodiUrit)
        .getOrElse(Seq())
        .toSet != hakukohde.pohjakoulutusvaatimusKoodiUrit.toSet
    ) hakukohde.pohjakoulutusvaatimusKoodiUrit
    else Seq()

  def liitteidenToimitusosoiteWithNewValues(): Option[Osoite] = {
    val postinumeroKoodiUri =
      hakukohde.liitteidenToimitusosoite.map(_.osoite).flatMap(_.postinumeroKoodiUri)
    val oldPostinumeroKoodiUri = oldHakukohde
      .flatMap(_.liitteidenToimitusosoite)
      .map(_.osoite)
      .flatMap(_.postinumeroKoodiUri)
    if (postinumeroKoodiUri != oldPostinumeroKoodiUri) Some(Osoite(postinumeroKoodiUri = postinumeroKoodiUri))
    else None
  }

  def newLiitteet(): Seq[Liite] =
    if (oldHakukohde.map(_.liitteet).getOrElse(Seq()).toSet != hakukohde.liitteet.toSet) hakukohde.liitteet else Seq()

  def newValintakokeet(): Seq[Valintakoe] =
    if (oldHakukohde.map(_.valintakokeet).getOrElse(Seq()).toSet != hakukohde.valintakokeet.toSet)
      hakukohde.valintakokeet
    else Seq()

  def newAtaruId(): Option[UUID] =
    if (oldHakukohde.flatMap(_.hakulomakeAtaruId) != hakukohde.hakulomakeAtaruId)
      hakukohde.hakulomakeAtaruId
    else None

  def newPainotetutArvosanat(): Seq[PainotettuOppiaine] = {
    val arvosanat = painotetutArvosanat(hakukohde.metadata)
    if (painotetutArvosanat(oldMetadata()).toSet != arvosanat.toSet) arvosanat else Seq()
  }

  def newValintaperusteenValintakokeidenLisatilaisuudet(): Seq[ValintakokeenLisatilaisuudet] = {
    val lisatilaisuudet = hakukohde.metadata.map(_.valintaperusteenValintakokeidenLisatilaisuudet).getOrElse(Seq())
    if (
      oldMetadata()
        .map(_.valintaperusteenValintakokeidenLisatilaisuudet)
        .getOrElse(Seq())
        .toSet != lisatilaisuudet.toSet
    )
      lisatilaisuudet
    else Seq()
  }

  def koulutuksenAlkamiskausiWithNewValues(): Option[KoulutuksenAlkamiskausi] = {
    val alkamiskausiKoodiUri = koulutuksenAlkamiskausiKoodiUri(hakukohde.metadata)
    if (alkamiskausiKoodiUri != koulutuksenAlkamiskausiKoodiUri(oldMetadata()))
      Some(KoulutuksenAlkamiskausi(koulutuksenAlkamiskausiKoodiUri = alkamiskausiKoodiUri))
    else None
  }

  def toinenAsteOnkoKaksoistutkintoNewlyActivated(): Boolean = {
    val oldValue = oldHakukohde.flatMap(_.toinenAsteOnkoKaksoistutkinto).getOrElse(false)
    val newValue = hakukohde.toinenAsteOnkoKaksoistutkinto.getOrElse(false)
    if (oldValue == false && newValue == true) true else false
  }

  def jarjestaaUrheilijanAmmatillistakoulutustaChanged(): Boolean = {
    val oldValue: Option[Boolean] = oldHakukohde.flatMap(hk => hk.metadata.flatMap(m => m.jarjestaaUrheilijanAmmKoulutusta))
    val newValue: Option[Boolean] = hakukohde.metadata.flatMap(m => m.jarjestaaUrheilijanAmmKoulutusta)
    (oldValue, newValue) match {
      case (Some(o), Some(n))   => !o.equals(n)
      case (None, None)         => false
      case (_, _)               => true
    }
  }

  def liitteenOsoiteWithNewValues(liite: Option[Liite]): Option[Osoite] = {
    val postinumeroKoodiUri =
      liite.flatMap(_.toimitusosoite).map(_.osoite).flatMap(_.postinumeroKoodiUri)
    if (postinumeroKoodiUri.isDefined) Some(Osoite(postinumeroKoodiUri = postinumeroKoodiUri)) else None
  }
}
