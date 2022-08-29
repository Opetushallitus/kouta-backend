package fi.oph.kouta.validation

import fi.oph.kouta.domain.{
  Hakukohde,
  HakukohdeMetadata,
  KoulutuksenAlkamiskausi,
  Liite,
  Osoite,
  PainotettuOppiaine,
  Valintakoe,
  ValintakokeenLisatilaisuudet
}

import java.util.UUID

case class HakukohdeDiffResolver(hakukohde: Hakukohde, oldHakukohde: Option[Hakukohde]) {
  private def oldMetadata(): Option[HakukohdeMetadata] =
    oldHakukohde.map(_.metadata).getOrElse(None)

  private def koulutuksenAlkamiskausiKoodiUri(metadata: Option[HakukohdeMetadata]): Option[String] =
    metadata.map(_.koulutuksenAlkamiskausi).getOrElse(None).map(_.koulutuksenAlkamiskausiKoodiUri).getOrElse(None)
  private def painotetutArvosanat(metadata: Option[HakukohdeMetadata]): Seq[PainotettuOppiaine] =
    metadata.map(_.hakukohteenLinja).getOrElse(None).map(_.painotetutArvosanat).getOrElse(Seq())

  def newHakukohdeKoodiUri(): Option[String] =
    if (oldHakukohde.map(_.hakukohdeKoodiUri).getOrElse(None) != hakukohde.hakukohdeKoodiUri)
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
      hakukohde.liitteidenToimitusosoite.map(_.osoite).map(_.postinumeroKoodiUri).getOrElse(None)
    val oldPostinumeroKoodiUri = oldHakukohde
      .map(_.liitteidenToimitusosoite)
      .getOrElse(None)
      .map(_.osoite)
      .map(_.postinumeroKoodiUri)
      .getOrElse(None)
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
    if (oldHakukohde.map(_.hakulomakeAtaruId).getOrElse(None) != hakukohde.hakulomakeAtaruId)
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
    val oldValue = oldHakukohde.map(_.toinenAsteOnkoKaksoistutkinto).getOrElse(None).getOrElse(false)
    val newValue = hakukohde.toinenAsteOnkoKaksoistutkinto.getOrElse(false)
    if (oldValue == false && newValue == true) true else false
  }

  def liitteenOsoiteWithNewValues(liite: Option[Liite]): Option[Osoite] = {
    val postinumeroKoodiUri =
      liite.map(_.toimitusosoite).getOrElse(None).map(_.osoite).map(_.postinumeroKoodiUri).getOrElse(None)
    if (postinumeroKoodiUri.isDefined) Some(Osoite(postinumeroKoodiUri = postinumeroKoodiUri)) else None
  }
}
