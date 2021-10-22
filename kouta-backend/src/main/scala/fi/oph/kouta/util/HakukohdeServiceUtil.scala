package fi.oph.kouta.util

import fi.oph.kouta.domain.{Toteutus, TuvaToteutusMetadata}

object HakukohdeServiceUtil {
  def getJarjestetaanErityisopetuksena(toteutus: Toteutus): Option[Boolean] = {
    toteutus.metadata match {
      case Some(toteutusMetadata) =>
        toteutusMetadata match {
          case tuva: TuvaToteutusMetadata =>
            Some(tuva.jarjestetaanErityisopetuksena)
          case _ => None
        }
    }
  }

}
