package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Haku, HakuMetadata, KoulutuksenAlkamiskausi}

import java.util.UUID

case class HakuDiffResolver(haku: Haku, oldHaku: Option[Haku]) {
  private def oldMetadata(): Option[HakuMetadata] =
    oldHaku.map(_.metadata).getOrElse(None)

  private def koulutuksenAlkamiskausiKoodiUri(metadata: Option[HakuMetadata]): Option[String] =
    metadata.map(_.koulutuksenAlkamiskausi).getOrElse(None).map(_.koulutuksenAlkamiskausiKoodiUri).getOrElse(None)

  def newHakutapaKoodiUri(): Option[String] =
    if (oldHaku.map(_.hakutapaKoodiUri).getOrElse(None) != haku.hakutapaKoodiUri) haku.hakutapaKoodiUri else None

  def newKohdejoukkoKoodiUri(): Option[String] =
    if (oldHaku.map(_.kohdejoukkoKoodiUri).getOrElse(None) != haku.kohdejoukkoKoodiUri) haku.kohdejoukkoKoodiUri
    else None

  def newKohdejoukonTarkenneKoodiUri(): Option[String] =
    if (oldHaku.map(_.kohdejoukonTarkenneKoodiUri).getOrElse(None) != haku.kohdejoukonTarkenneKoodiUri)
      haku.kohdejoukonTarkenneKoodiUri
    else None

  def newAtaruId(): Option[UUID] =
    if (oldHaku.map(_.hakulomakeAtaruId).getOrElse(None) != haku.hakulomakeAtaruId) haku.hakulomakeAtaruId else None

  def koulutuksenAlkamiskausiWithNewValues(): Option[KoulutuksenAlkamiskausi] = {
    val alkamiskausiKoodiUri = koulutuksenAlkamiskausiKoodiUri(haku.metadata)
    if (koulutuksenAlkamiskausiKoodiUri(oldMetadata()) != alkamiskausiKoodiUri)
      Some(KoulutuksenAlkamiskausi(koulutuksenAlkamiskausiKoodiUri = alkamiskausiKoodiUri))
    else None
  }
}
