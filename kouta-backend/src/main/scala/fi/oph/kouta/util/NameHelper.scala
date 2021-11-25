package fi.oph.kouta.util

import fi.oph.kouta.domain.{Fi, Kielistetty, ToteutusMetadata}

object NameHelper {
  def generateHakukohdeDisplayNameForTuva(
    hakukohdeNimi: Kielistetty,
    toteutusMetadata: ToteutusMetadata,
    kaannokset: Kielistetty
  ): Kielistetty = {
    val jarjestetaanErityisopetuksena = HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(toteutusMetadata)

    if (jarjestetaanErityisopetuksena) {
      val defaultKaannos = kaannokset.get(Fi).getOrElse("")
      hakukohdeNimi.map { case (key, value) =>
        val kaannos = kaannokset.get(key)
        kaannos match {
          case Some(str) => key -> (value + s" ($str)")
          case None => if (defaultKaannos.isEmpty) {
            key -> value
          } else {
            key -> (value + s" ($defaultKaannos)")
          }
        }
      }
    } else {
      hakukohdeNimi
    }
  }
}
