package fi.oph.kouta.util

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
  //case object Asiasana         extends Resource("asiasana")
  //case object Ammattinimike    extends Resource("ammattinimike")
}
