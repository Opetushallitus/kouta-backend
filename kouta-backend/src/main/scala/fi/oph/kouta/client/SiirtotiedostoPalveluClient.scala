package fi.oph.kouta.client

import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.writePretty

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.{Date, Optional}

object SiirtotiedostoPalveluClient extends SiirtotiedostoPalveluClient

class SiirtotiedostoPalveluClient extends KoutaJsonFormats {
  val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration;
  val siirtotiedostoPalvelu   = new SiirtotiedostoPalvelu(config.region.getOrElse("eu-west-1"), config.transferFileBucket)

  def saveSiirtotiedosto[T](
      contentStartTime: Option[Instant],
      contentEndTime: Option[Instant],
      contentType: String,
      content: Seq[T]
  ): String = {
    if (content.isEmpty) return "Ei hakutuloksia annetuilla aikarajoilla"

    val objectMetadata = siirtotiedostoPalvelu.saveSiirtotiedosto(
      Optional.ofNullable(contentStartTime.map(Date.from(_)).orNull),
      Optional.ofNullable(contentEndTime.map(Date.from(_)).orNull),
      "kouta",
      Optional.of(contentType),
      new ByteArrayInputStream(writePretty(content).getBytes())
    )
    s"$contentType, yhteensä ${content.size} kpl tallennettu S3 buckettiin avaimella ${objectMetadata.key}"
  }
}
