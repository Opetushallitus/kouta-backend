package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Valintakoe, Valintaperuste, ValintaperusteMetadata, Valintatapa}

case class ValintaperusteDiffResolver(vp: Valintaperuste, oldVp: Option[Valintaperuste]) {
  private def oldMetadata(): Option[ValintaperusteMetadata] =
    oldVp.map(_.metadata).getOrElse(None)

  def newHakutapaKoodiUri(): Option[String] =
    if (oldVp.map(_.hakutapaKoodiUri).getOrElse(None) != vp.hakutapaKoodiUri) vp.hakutapaKoodiUri else None

  def newKohdejoukkoKoodiUri(): Option[String] =
    if (oldVp.map(_.kohdejoukkoKoodiUri).getOrElse(None) != vp.kohdejoukkoKoodiUri) vp.kohdejoukkoKoodiUri
    else None

  def newValintakokeet(): Seq[Valintakoe] =
    if (oldVp.map(_.valintakokeet).getOrElse(Seq()).toSet != vp.valintakokeet.toSet)
      vp.valintakokeet
    else Seq()

  def newValintatavat(): Seq[Valintatapa] = {
    val valintatavat = vp.metadata.map(_.valintatavat).getOrElse(Seq())
    if (oldMetadata().map(_.valintatavat).getOrElse(Seq()).toSet != valintatavat.toSet) valintatavat else Seq()
  }
}
