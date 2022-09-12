package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausMetadata}

case class SorakuvausDiffResolver(sorakuvaus: Sorakuvaus, oldSorakuvaus: Option[Sorakuvaus]) {
  private def oldMetadata(): Option[SorakuvausMetadata] = oldSorakuvaus.flatMap(_.metadata)

  def newKoulutusKoodiUrit(): Seq[String] = {
    val koodiUrit = sorakuvaus.metadata.map(_.koulutusKoodiUrit).getOrElse(Seq())
    if (oldMetadata().map(_.koulutusKoodiUrit).getOrElse(Seq()).toSet != koodiUrit.toSet)
      koodiUrit
    else Seq()
  }

  def newKoulutusalaKoodiUri(): Option[String] = {
    val koodiUri = sorakuvaus.metadata.flatMap(_.koulutusalaKoodiUri)
    if (oldMetadata().flatMap(_.koulutusalaKoodiUri) != koodiUri) koodiUri else None
  }
}
