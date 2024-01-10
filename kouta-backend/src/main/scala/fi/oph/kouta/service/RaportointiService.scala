package fi.oph.kouta.service

import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.oph.kouta.domain.raportointi.KoulutusRaporttiItem
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import org.json4s.jackson.Serialization.writePretty

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant
import java.util.{Date, Optional}

object RaportointiService extends RaportointiService(KoulutusService) {
  def apply(koulutusService: KoulutusService): RaportointiService = {
    new RaportointiService(koulutusService)
  }
}
class RaportointiService(koulutusService: KoulutusService) extends KoutaJsonFormats {
  val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration;
  val siirtotiedostoPalvelu =
    new SiirtotiedostoPalvelu(config.region.getOrElse("eu-west-1"), config.transferFileBucket)

  private def saveEntitiesToS3(
      contentStartTime: Option[Instant],
      contentEndTime: Option[Instant],
      contentType: String,
      contentJson: String
  ) = {
    siirtotiedostoPalvelu.saveSiirtotiedosto(
      Optional.ofNullable(contentStartTime.map(Date.from(_)).orNull),
      Optional.ofNullable(contentEndTime.map(Date.from(_)).orNull),
      "kouta",
      Optional.of(contentType),
      new ByteArrayInputStream(contentJson.getBytes())
    )
  }
  def saveKoulutukset(startTime: Option[Instant], endTime: Option[Instant])(implicit
      authenticated: Authenticated
  ): Unit = {
    val koulutukset           = RaportointiDAO.listKoulutukset(startTime, endTime).map(k => koulutusService.enrichKoulutus(k))
    val koulutusRaporttiItems = koulutukset.map(k => new KoulutusRaporttiItem(k))

    saveEntitiesToS3(startTime, endTime, "koulutukset", writePretty(koulutusRaporttiItems))
  }
}
