package fi.oph.kouta.ovara

import com.zaxxer.hikari.HikariConfig
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.siirtotiedosto.{SiirtotiedostoDateTimeFormat, Siirtotiedosto, SiirtotiedostoCounts, SiirtotiedostoInfo}
import fi.oph.kouta.repository.{KoutaDatabaseAccessor, SimpleDatabaseAccessor}
import fi.oph.kouta.service.SiirtotiedostoRaportointiService
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.logging.Logging
import org.json4s.jackson.Serialization.writePretty

import java.time.LocalDateTime
import java.util.UUID
import scala.sys.exit
import scala.util.{Failure, Success, Try}

object SiirtotiedostoApp extends Logging with KoutaJsonFormats {
  def main(args: Array[String]): Unit = {
    KoutaConfigurationFactory.init()
    val opId = UUID.randomUUID();
    val latestSiirtotiedostoData = SiirtotiedostoRaportointiService.findLatestSiirtotiedostoData()
    val currentDatetime          = LocalDateTime.now()
    logger.info("Launching siirtotiedosto operation {}", opId)

    val newSiirtotiedostoData = latestSiirtotiedostoData match {
      case Some(existingData) if existingData.success.getOrElse(false) =>
        siirtotiedostoData(opId, Some(existingData.windowEnd), currentDatetime)
      case Some(existingData) => siirtotiedostoData(opId, existingData.windowStart, currentDatetime) // retry previous
      case _                  => siirtotiedostoData(opId, None, currentDatetime)
    }
    SiirtotiedostoRaportointiService.saveSiirtotiedostoData(newSiirtotiedostoData)
    val siirtotiedostoOpResults = createSiirtotiedostot(
      newSiirtotiedostoData.id,
      newSiirtotiedostoData.windowStartAsLocalDate(),
      Some(currentDatetime)
    )
    val updatedSiirtotiedostoData = siirtotiedostoOpResults match {
      case Right(siirtotiedostoInfo: SiirtotiedostoInfo) =>
        val data = newSiirtotiedostoData.copy(
          runEnd = Some(LocalDateTime.now()),
          info = Some(siirtotiedostoInfo),
          success = Some(true)
        )
        logger.info("Generated siirtotiedostot {}", writePretty(data))
        data

      case Left(errorMsg: String) =>
        val data = newSiirtotiedostoData.copy(
          runEnd = Some(LocalDateTime.now()),
          success = Some(false),
          errorMessage = Some(errorMsg)
        )
        logger.error("Siirtotiedosto generation failed; {}", writePretty(data))
        data
    }
    SiirtotiedostoRaportointiService.saveSiirtotiedostoData(updatedSiirtotiedostoData)
    SimpleDatabaseAccessor.destroy()
    exit
  }

  private def siirtotiedostoData(opId: UUID, windowStart: Option[String], currentDatetime: LocalDateTime) =
    Siirtotiedosto(
      opId,
      windowStart,
      SiirtotiedostoDateTimeFormat.format(currentDatetime),
      currentDatetime,
      None,
      None,
      None,
      None
    )

  private def createSiirtotiedostot(
      operationId: UUID,
      windowStart: Option[LocalDateTime],
      windowEnd: Option[LocalDateTime]
  ): Either[String, SiirtotiedostoInfo] = {
    var koulutukset, toteutukset, hakukohteet, haut, valintaperusteet, sorakuvaukset, oppilaitoksetJaOsat,
        pistetiedot: Option[Int] = None
    Try[SiirtotiedostoCounts] {
      val koulutusResult = SiirtotiedostoRaportointiService.saveKoulutukset(operationId, windowStart, windowEnd)
      koulutukset = Some(koulutusResult.count)
      val toteutusResult = SiirtotiedostoRaportointiService.saveToteutukset(operationId, windowStart, windowEnd)
      toteutukset = Some(toteutusResult.count)
      val hakukohdeResult = SiirtotiedostoRaportointiService.saveHakukohteet(operationId, windowStart, windowEnd)
      hakukohteet = Some(hakukohdeResult.count)
      val hakuResult = SiirtotiedostoRaportointiService.saveHaut(operationId, windowStart, windowEnd)
      haut = Some(hakuResult.count)
      val valintaperusteResult = SiirtotiedostoRaportointiService.saveValintaperusteet(operationId, windowStart, windowEnd)
      valintaperusteet = Some(valintaperusteResult.count)
      val sorakuvausResult = SiirtotiedostoRaportointiService.saveSorakuvaukset(operationId, windowStart, windowEnd)
      sorakuvaukset = Some(sorakuvausResult.count)
      val oppilaitosJaOsaResult = SiirtotiedostoRaportointiService.saveOppilaitoksetJaOsat(operationId, windowStart, windowEnd)
      oppilaitoksetJaOsat = Some(oppilaitosJaOsaResult.count)
      val pistehistoriaResult = SiirtotiedostoRaportointiService.savePistehistoria(operationId, windowStart, windowEnd)
      pistetiedot = Some(pistehistoriaResult.count)
      SiirtotiedostoCounts(
        koulutukset,
        toteutukset,
        hakukohteet,
        haut,
        valintaperusteet,
        sorakuvaukset,
        oppilaitoksetJaOsat,
        pistetiedot
      )

    } match {
      case Success(counts: SiirtotiedostoCounts) => Right(SiirtotiedostoInfo(Some(counts)))
      case Failure(exception: Throwable)         =>
        logger.error(s"Jotain meni vikaan muodostettaessa siirtotiedostoa:", exception)
        Left(exception.getMessage)
    }
  }
}
