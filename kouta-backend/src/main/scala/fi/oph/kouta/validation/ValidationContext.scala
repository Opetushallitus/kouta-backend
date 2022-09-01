package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Kielivalikoima}
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, queryFailed}

case class ValidationContext(
    tila: Julkaisutila,
    kielivalinta: Seq[Kieli],
    crudOperation: CrudOperation,
    private var koodistoServiceOk: Boolean = true,
    private var ataruServiceOk: Boolean = true
) {
  def isKoodistoServiceOk(): Boolean                            = koodistoServiceOk
  def setKoodistoServiceOk(newKoodistoServiceOk: Boolean): Unit = koodistoServiceOk = newKoodistoServiceOk
  def updateKoodistoServiceStatusByQueryStatus(externalQueryResult: ExternalQueryResult): Unit =
    koodistoServiceOk = externalQueryResult != queryFailed

  def isAtaruServiceOk(): Boolean                         = ataruServiceOk
  def setAtaruServiceOk(newAtaruServiceOk: Boolean): Unit = ataruServiceOk = newAtaruServiceOk
  def updateAtaruServiceStatusByQueryStatus(externalQueryResult: ExternalQueryResult): Unit =
    ataruServiceOk = externalQueryResult != queryFailed
}
