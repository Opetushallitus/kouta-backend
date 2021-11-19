package fi.oph.kouta.util

import fi.oph.kouta.domain.{Hakukohde, Kielistetty, Toteutus, ToteutusMetadata}

object NameHelper {
  def generateHakukohdeDisplayNameForTuva(
    hakukohdeNimi: Kielistetty,
    toteutusMetadata: ToteutusMetadata,
    kaannokset: Kielistetty
  ): Kielistetty = {
    val jarjestetaanErityisopetuksena = HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(toteutusMetadata)

    if (jarjestetaanErityisopetuksena) {
      hakukohdeNimi.map { case (key, value) =>
        val kaannos = kaannokset.find(_._1 == key).get
        kaannos match {
          case (kieli, str) =>
            kieli -> (value + s" ($str)")
        }
      }
    } else {
      hakukohdeNimi
    }
  }
}
