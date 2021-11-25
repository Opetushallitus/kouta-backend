package fi.oph.kouta.client

import fi.oph.kouta.util.UnitSpec
import fi.oph.kouta.client.LokalisointiClientUtil.{Kaannos, parseKaannokset}
import fi.oph.kouta.domain.{Fi, Sv}

class LokalisointiClientSpec extends UnitSpec {
  "parseKaannoksetResponse" should "parse kaannokset in a Map with language as a key and translation as the value" in {
    assert(
      parseKaannokset(
        List(
          Kaannos("kouta", "yleiset.vaativanaErityisenaTukena", "fi", "vaativana erityisenä tukena"),
          Kaannos("kouta", "yleiset.vaativanaErityisenaTukena", "sv", "krävande särskilt stöd")
        )
      ) === Map(
        Fi -> "vaativana erityisenä tukena",
        Sv -> "krävande särskilt stöd"
      )
    )
  }
}
