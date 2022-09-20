package fi.oph.kouta.service

import fi.oph.kouta.domain.Koulutustyyppi

case class KoulutustyyppiAuthorizationFailedException(requiredKoulutustyyppi: Koulutustyyppi, existingKoulutustyypit: Seq[Koulutustyyppi]) extends RuntimeException({
  val existingTyypit = existingKoulutustyypit.mkString(",")
  s"Authorization failed, missing koulutustyyppi: $requiredKoulutustyyppi, existing koulutustyypit: $existingTyypit"
})
