package fi.oph.kouta.util

import fi.oph.kouta.domain.{ToteutusMetadata, TuvaToteutusMetadata}

object HakukohdeServiceUtil {
  def getJarjestetaanErityisopetuksena(toteutuksenMetadata: ToteutusMetadata): Boolean = {
    toteutuksenMetadata match {
      case tuva: TuvaToteutusMetadata =>
        tuva.jarjestetaanErityisopetuksena
      case _ => false
    }
  }
}
