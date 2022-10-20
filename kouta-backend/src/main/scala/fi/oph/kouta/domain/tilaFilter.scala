package fi.oph.kouta.domain

case class TilaFilter (tilat: Set[Julkaisutila] = Set()) {
  def isDefined(): Boolean = {
    tilat.nonEmpty && tilat.size < 4
  }

  def included() = tilat
  def excluded(): Set[Julkaisutila] = Set[Julkaisutila](Poistettu, Tallennettu, Julkaistu, Arkistoitu).diff(tilat)

  def contains(tila: Julkaisutila): Boolean = {
    tilat.contains(tila)
  }
}

object TilaFilter {
  def all(): TilaFilter = {
    TilaFilter(Set(Poistettu, Tallennettu, Julkaistu, Arkistoitu))
  }

  def onlyJulkaistut(): TilaFilter = {
    TilaFilter(Set(Julkaistu))
  }

  def onlyOlemassaolevat(): TilaFilter = {
    TilaFilter(Set(Tallennettu, Julkaistu, Arkistoitu))
  }

  def onlyOlemassaolevatAndArkistoimattomat(): TilaFilter = {
    TilaFilter(Set(Tallennettu, Julkaistu))
  }

  def vainJulkaistutOrVainOlemassaolevat(vainJulkaistut: Boolean, vainOlemassaolevat: Boolean): TilaFilter = {
    (vainJulkaistut, vainOlemassaolevat) match {
      case (true, _) => onlyJulkaistut
      case (false, true) => onlyOlemassaolevat
      case (_, _) => all
    }
  }

  def alsoPoistetutAddedToOthers(alsoPoistetut: Boolean): TilaFilter = {
    if (alsoPoistetut) all else onlyOlemassaolevat
  }

  def alsoArkistoidutAddedToOlemassaolevat(alsoArkistoidut: Boolean): TilaFilter = {
    if (alsoArkistoidut) onlyOlemassaolevat else onlyOlemassaolevatAndArkistoimattomat
  }
}
