package fi.oph.kouta.util

import fi.oph.kouta.domain.{ToteutusMetadata, TuvaToteutusMetadata}

object HakukohdeServiceUtil {
  def getJarjestetaanErityisopetuksena(toteutuksenMetadata: ToteutusMetadata): Option[Boolean] = {
    toteutuksenMetadata match {
      case tuva: TuvaToteutusMetadata =>
        Some(tuva.jarjestetaanErityisopetuksena)
      case _ => None
    }
  }
}
