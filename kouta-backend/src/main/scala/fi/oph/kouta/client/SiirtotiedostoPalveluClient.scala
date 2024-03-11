package fi.oph.kouta.client

import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.oph.kouta.domain.raportointi.RaportointiDateTimeFormat
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.writePretty
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner

import java.io.ByteArrayInputStream
import java.net.URI
import java.time.{Duration, Instant, LocalDateTime, ZonedDateTime}
import java.util.{Date, Optional}

object SiirtotiedostoPalveluClient extends SiirtotiedostoPalveluClient

class SiirtotiedostoPalveluClient extends KoutaJsonFormats {
  val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration;
  val siirtotiedostoPalvelu   = new SiirtotiedostoPalvelu(config.region.getOrElse("eu-west-1"), config.transferFileBucket)
  val saveRetryCount          = config.transferFileSaveRetryCount

  def saveSiirtotiedosto[T](
      contentCreationTime: ZonedDateTime,
      contentType: String,
      content: Seq[T]
  ): String =
    siirtotiedostoPalvelu
      .saveSiirtotiedosto(
        RaportointiDateTimeFormat.format(contentCreationTime),
        "kouta",
        contentType,
        "",
        new ByteArrayInputStream(writePretty(content).getBytes()),
        saveRetryCount
      )
      .key
}
