package fi.oph.kouta.util

import fi.oph.kouta.domain.{Hakukohde, Kielistetty, Toteutus}

object NameHelper {
  def generateHakukohdeDisplayName(hakukohde: Hakukohde, toteutus: Toteutus, kaannokset: Kielistetty): Kielistetty = {
    val jarjestetaanErityisopetuksena = HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(toteutus)

    if (jarjestetaanErityisopetuksena.getOrElse(false)) {
      hakukohde.nimi.map { case (key, value) =>
        val kaannos = kaannokset.find(_._1 == key).get
        kaannos match {
          case (kieli, str) =>
            kieli -> (value + s" ($str)")
        }
      }
    } else {
      hakukohde.nimi
    }
  }
}
