package fi.oph.kouta.util

object ToteutusServiceUtil {
  def isValidOpintojenlaajuus(koulutusOpintojenlaajuusMin: Option[Double], koulutusOpintojenlaajuusMax: Option[Double], toteutuksenLaajuus: Option[Double]): Boolean = {
    toteutuksenLaajuus match {
      case None => true
      case Some(laajuus) => {
        koulutusOpintojenlaajuusMax match {
          case None => {
            koulutusOpintojenlaajuusMin match {
              case None => true
              case Some(laajuusMin) => laajuus == laajuusMin
            }
          }
          case Some(maxLaajuus) => {
            koulutusOpintojenlaajuusMin match {
              case None => laajuus == maxLaajuus
              case Some(minLaajuus) => laajuus >= minLaajuus && laajuus <= maxLaajuus
            }
          }
        }
      }
    }
  }
}
