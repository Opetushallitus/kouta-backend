package fi.oph.kouta.validation

import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, queryFailed}

class ValidationContext {
  private var koodistoServiceOk = true
  private var ataruServiceOk = true

  def isKoodistoServiceOk(): Boolean = koodistoServiceOk
  def setKoodistoServiceOk(newKoodistoServiceOk: Boolean): Unit = koodistoServiceOk = newKoodistoServiceOk
  def updateKoodistoServiceStatusByQueryStatus(externalQueryResult: ExternalQueryResult): Unit =
    koodistoServiceOk = externalQueryResult != queryFailed

  def isAtaruServiceOk(): Boolean = ataruServiceOk
  def setAtaruServiceOk(newAtaruServiceOk: Boolean): Unit = ataruServiceOk = newAtaruServiceOk
  def updateAtaruServiceStatusByQueryStatus(externalQueryResult: ExternalQueryResult): Unit =
    ataruServiceOk = externalQueryResult != queryFailed
}
