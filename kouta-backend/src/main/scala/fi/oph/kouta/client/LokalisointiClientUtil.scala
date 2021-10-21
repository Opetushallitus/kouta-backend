package fi.oph.kouta.client

import fi.oph.kouta.domain.Kieli

object LokalisointiClientUtil {
  case class Kaannos(category: String, key: String, locale: String, value: String) {}

  def parseKaannokset(kaannokset: List[Kaannos]) = {
    kaannokset
      .map(kaannos => {
        Kieli.withName(kaannos.locale) -> kaannos.value
      })
      .toMap
  }
}
