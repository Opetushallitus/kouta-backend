package fi.oph.kouta.util

import fi.oph.kouta.client.KoodistoUtils

object ToteutusServiceUtil {
  def isValidOpintojenlaajuus(
      koulutusOpintojenlaajuusMin: Option[Double],
      koulutusOpintojenlaajuusMax: Option[Double],
      toteutuksenLaajuus: Option[Double]
  ): Boolean = {
    toteutuksenLaajuus match {
      case None => true
      case Some(laajuus) => {
        (koulutusOpintojenlaajuusMin, koulutusOpintojenlaajuusMax) match {
          case (Some(minLaajuus), Some(maxLaajuus)) => laajuus >= minLaajuus && laajuus <= maxLaajuus
          case (Some(minLaajuus), None)             => laajuus >= minLaajuus
          case (None, Some(maxLaajuus))             => laajuus <= maxLaajuus
          case (None, None)                         => true
        }
      }
    }
  }
  def isValidOpintojenLaajuusyksikko(
      koulutusOpintojenLaajuusYksikko: Option[String],
      toteutusOpintojenLaajuusYksikko: Option[String]
  ) = {
    (koulutusOpintojenLaajuusYksikko, toteutusOpintojenLaajuusYksikko) match {
      case (Some(koulutusLaajuusyksikko), Some(toteutusLaajuusyksikko)) =>
        KoodistoUtils.koodiUriStringsMatch(koulutusLaajuusyksikko, toteutusLaajuusyksikko)
      case (_, _) => true
    }
  }
}
