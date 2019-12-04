package fi.oph.kouta.util

import fi.oph.kouta.domain
import fi.oph.kouta.domain.keyword

abstract class Resource(val name: String, val idField: String) extends ResourceOperations {
  override def toString: String = name
}

object Resource {
  case object Koulutus         extends Resource("koulutus", "oid")
  case object Toteutus         extends Resource("toteutus", "oid")
  case object Haku             extends Resource("haku", "oid")
  case object Hakukohde        extends Resource("hakukohde", "oid")
  case object Valintaperuste   extends Resource("valintaperuste", "id")
  case object Sorakuvaus       extends Resource("sorakuvaus", "id")
  case object Oppilaitos       extends Resource("oppilaitos", "oid")
  case object OppilaitoksenOsa extends Resource("oppilaitoksen osa", "oid")
  case object Asiasana         extends Resource("asiasana", "asiasana")
  case object Ammattinimike    extends Resource("ammattinimike", "ammattinimike")

  def apply(entity: AnyRef): Resource = {
    entity match {
      case _: domain.Koulutus         => Koulutus
      case _: domain.Toteutus         => Toteutus
      case _: domain.Haku             => Haku
      case _: domain.Hakukohde        => Hakukohde
      case _: domain.Valintaperuste   => Valintaperuste
      case _: domain.Sorakuvaus       => Sorakuvaus
      case _: domain.Oppilaitos       => Oppilaitos
      case _: domain.OppilaitoksenOsa => OppilaitoksenOsa
      case keyword.Ammattinimike      => Ammattinimike
      case keyword.Asiasana           => Asiasana
      case r: Resource                => r
      case _                          => throw new IllegalArgumentException("Resource not found")
    }
  }
}
