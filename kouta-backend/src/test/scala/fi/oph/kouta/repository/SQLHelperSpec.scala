package fi.oph.kouta.repository

import fi.oph.kouta.domain.{Julkaistu, Tallennettu, TilaFilter}
import fi.oph.kouta.util.UnitSpec

class SQLHelperSpec extends UnitSpec with SQLHelpers {
  "When all julkaisutilat selected" should "return empty SQL condition" in {
    assert(tilaConditions(TilaFilter.all()) == "")
  }

  "When several julkaisutilat selected" should "return in-clause" in {
    assert(tilaConditions(new TilaFilter(Set(Tallennettu, Julkaistu))) == "and tila in ('tallennettu','julkaistu')")
  }

  "When only one julkaisutila selected" should "return equals-clause" in {
    assert(tilaConditions(TilaFilter.onlyJulkaistut(), "some.tila") == "and some.tila = 'julkaistu'")
  }

  "When only one julkaisutila excluded" should "return not-equals-clause" in {
    assert(tilaConditions(TilaFilter.onlyOlemassaolevat(), "some.tila") == "and some.tila != 'poistettu'")
  }
}
