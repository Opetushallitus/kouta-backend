package fi.oph.kouta.validation

import fi.oph.kouta.domain.{OppilaitoksenOsa, Oppilaitos, OppilaitosMetadata, TietoaOpiskelusta, Yhteystieto}

case class OppilaitosOrOsaDiffResolver[E <: Validatable](oppilaitosOrOsa: E, oldOppilaitosOrOsa: Option[E]) {
  private def oppilaitosMetadata(): Option[OppilaitosMetadata] = oppilaitosOrOsa match {
    case oppilaitos: Oppilaitos => oppilaitos.metadata
    case _ => None
  }

  private def oldOppilaitosMetadata(): Option[OppilaitosMetadata] = oldOppilaitosOrOsa match {
    case Some(oppilaitos: Oppilaitos) => oppilaitos.metadata
    case _ => None
  }

  def newTietoaOpiskelusta(): Seq[TietoaOpiskelusta] = {
    val tietoaOpiskelusta = oppilaitosMetadata().map(_.tietoaOpiskelusta).getOrElse(Seq())
    if (oldOppilaitosMetadata().map(_.tietoaOpiskelusta).getOrElse(Seq()).toSet != tietoaOpiskelusta.toSet) tietoaOpiskelusta
    else Seq()
  }

  def newHakijapalveluidenYhteystiedot(): Option[Yhteystieto] = {
    val yhteystieto = oppilaitosOrOsa match {
      case oppilaitos: Oppilaitos => oppilaitos.metadata.flatMap(_.hakijapalveluidenYhteystiedot)
      case osa: OppilaitoksenOsa => osa.metadata.flatMap(_.hakijapalveluidenYhteystiedot)
      case _ => None
    }

    val oldYhteysTieto = oldOppilaitosOrOsa match {
      case Some(oppilaitos: Oppilaitos) => oppilaitos.metadata.flatMap(_.hakijapalveluidenYhteystiedot)
      case Some(osa: OppilaitoksenOsa) => osa.metadata.flatMap(_.hakijapalveluidenYhteystiedot)
      case _ => None
    }

    if (yhteystieto != oldYhteysTieto) yhteystieto else None
  }

  def jarjestaaUrheilijanAmmatillistakoulutustaChanged(): Boolean = {
    val oldValue: Boolean = (oldOppilaitosOrOsa match {
      case Some(oppilaitos: Oppilaitos) => oppilaitos.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
      case Some(osa: OppilaitoksenOsa) => osa.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
      case _ => None
    }).getOrElse(false)
    val newValue: Boolean = (oppilaitosOrOsa match {
      case oppilaitos: Oppilaitos => oppilaitos.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
      case osa: OppilaitoksenOsa => osa.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
      case _ => None
    }).getOrElse(false)
    !oldValue.equals(newValue)
  }
}
