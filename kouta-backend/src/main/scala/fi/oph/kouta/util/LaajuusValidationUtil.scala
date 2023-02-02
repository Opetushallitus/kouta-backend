package fi.oph.kouta.util

import fi.oph.kouta.client.KoodistoUtils

object LaajuusValidationUtil {
  def isValidOpintojenLaajuus(
      koulutusLaajuusMin: Option[Double],
      koulutusLaajuusMax: Option[Double],
      toteutuksenLaajuus: Option[Double]
  ): Boolean =
    isAtLeast(toteutuksenLaajuus, koulutusLaajuusMin) && isAtMost(toteutuksenLaajuus, koulutusLaajuusMax)

  def isAtLeast(value: Option[Double], limitMin: Option[Double]) = {
    val limitMinN: Double = limitMin.getOrElse(Double.NegativeInfinity)
    value match {
      case Some(valueN) => valueN >= limitMinN
      case _            => true
    }
  }

  def isAtMost(value: Option[Double], limitMax: Option[Double]) = {
    val limitMaxN: Double = limitMax.getOrElse(Double.PositiveInfinity)
    value match {
      case Some(valueN) => valueN <= limitMaxN
      case _            => true
    }
  }

  def isValidOpintojenLaajuusyksikko(
      koulutusOpintojenLaajuusYksikko: Option[String],
      toteutusOpintojenLaajuusYksikko: Option[String]
  ) = {
    (koulutusOpintojenLaajuusYksikko, toteutusOpintojenLaajuusYksikko) match {
      case (Some(koulutusLaajuusyksikko), Some(toteutusLaajuusyksikko)) =>
        KoodistoUtils.koodiUriStringsMatch(koulutusLaajuusyksikko, toteutusLaajuusyksikko)
      case _ => true
    }
  }
}
