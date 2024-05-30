package fi.oph.kouta.client

import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.writePretty

import java.io.ByteArrayInputStream
import java.util.UUID

object SiirtotiedostoPalveluClient extends SiirtotiedostoPalveluClient

class SiirtotiedostoPalveluClient extends KoutaJsonFormats {
  val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration;
  val siirtotiedostoPalvelu   = new SiirtotiedostoPalvelu(config.region.getOrElse("eu-west-1"), config.transferFileBucket, config.transferFileTargetRoleArn)
  val saveRetryCount          = config.transferFileSaveRetryCount

  def saveSiirtotiedosto[T](
      contentType: String,
      content: Seq[T],
      operationId: UUID,
      operationSubId: Int
  ): String = {
    siirtotiedostoPalvelu
      .saveSiirtotiedosto(
        "kouta",
        contentType,
        "",
        operationId.toString,
        operationSubId,
        new ByteArrayInputStream(writePretty(content).getBytes()),
        saveRetryCount
      )
      .key
  }
}
