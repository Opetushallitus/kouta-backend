package fi.oph.kouta.util

abstract class Resource(val name: String) extends ResourceOperations {
  override def toString: String = name
}

object Resource {
  case object Koulutus         extends Resource("koulutus")
  case object Toteutus         extends Resource("toteutus")
  case object Haku             extends Resource("haku")
  case object Hakukohde        extends Resource("hakukohde")
  case object Valintaperuste   extends Resource("valintaperuste")
  case object Oppilaitos       extends Resource("oppilaitos")
  case object OppilaitoksenOsa extends Resource("oppilaitoksen osa")
  case object Asiasana         extends Resource("asiasana")
  case object Ammattinimike    extends Resource("ammattinimike")
}
