package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Haku, HakuMetadata, KoulutuksenAlkamiskausi}

import java.util.UUID

case class HakuDiffResolver(haku: Haku, oldHaku: Option[Haku]) {
  private def oldMetadata(): Option[HakuMetadata] =
    oldHaku.flatMap(_.metadata)

  private def koulutuksenAlkamiskausiKoodiUri(metadata: Option[HakuMetadata]): Option[String] =
    metadata.flatMap(_.koulutuksenAlkamiskausi).flatMap(_.koulutuksenAlkamiskausiKoodiUri)

  def newHakutapaKoodiUri(): Option[String] =
    if (oldHaku.flatMap(_.hakutapaKoodiUri) != haku.hakutapaKoodiUri) haku.hakutapaKoodiUri else None

  def newKohdejoukkoKoodiUri(): Option[String] =
    if (oldHaku.flatMap(_.kohdejoukkoKoodiUri) != haku.kohdejoukkoKoodiUri) haku.kohdejoukkoKoodiUri
    else None

  def newKohdejoukonTarkenneKoodiUri(): Option[String] =
    if (oldHaku.flatMap(_.kohdejoukonTarkenneKoodiUri) != haku.kohdejoukonTarkenneKoodiUri)
      haku.kohdejoukonTarkenneKoodiUri
    else None

  def newAtaruId(): Option[UUID] =
    if (oldHaku.flatMap(_.hakulomakeAtaruId) != haku.hakulomakeAtaruId) haku.hakulomakeAtaruId else None

  def koulutuksenAlkamiskausiWithNewValues(): Option[KoulutuksenAlkamiskausi] = {
    val alkamiskausiKoodiUri = koulutuksenAlkamiskausiKoodiUri(haku.metadata)
    if (koulutuksenAlkamiskausiKoodiUri(oldMetadata()) != alkamiskausiKoodiUri)
      Some(KoulutuksenAlkamiskausi(koulutuksenAlkamiskausiKoodiUri = alkamiskausiKoodiUri))
    else None
  }
}
