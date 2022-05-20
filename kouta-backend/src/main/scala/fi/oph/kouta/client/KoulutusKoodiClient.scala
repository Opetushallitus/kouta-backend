package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeSync
import scalacache.modes.sync.mode

import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object KoulutusKoodiClient extends KoulutusKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoulutusKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  case class KoodiUri(koodiUri: String, latestVersio: Int)

  implicit val koodiUriSingleCache = CaffeineCache[String]
  implicit val koodiUriCache       = CaffeineCache[Seq[KoodiUri]]

  val ammatillisetKoulutustyypit =
    Seq(
      "koulutustyyppi_1",
      "koulutustyyppi_4",
      "koulutustyyppi_5",
      "koulutustyyppi_7",
      "koulutustyyppi_8",
      "koulutustyyppi_11",
      "koulutustyyppi_12",
      "koulutustyyppi_13",
      "koulutustyyppi_18",
      "koulutustyyppi_19",
      "koulutustyyppi_26"
    )

  case class CodeElementWithVersion(koodiUri: String, versio: Int)
  case class KoodistoElement(koodistoUri: String)
  case class Koulutustyyppi(
      koodiUri: String,
      versio: Int = 1,
      koodisto: Option[KoodistoElement],
      voimassaLoppuPvm: Option[String]
  )
  val emptyKoodistoElement = KoodistoElement("")

  def getKoodiUriWithLatestVersion(koodiUriWithoutVersion: String): String =
    memoizeSync[String](Some(5.minutes)) {
      get(urlProperties.url("koodisto-service.latest-koodiuri", koodiUriWithoutVersion), followRedirects = true) {
        response =>
          {
            val codeElement = parse(response).extract[CodeElementWithVersion]
            s"${codeElement.koodiUri}#${codeElement.versio}"
          }
      }
    }

  def ammatillinenKoulutusKoodiUriExists(koodiUri: String): Boolean = {
    val now = ZonedDateTime.now().toLocalDateTime
    ammatillisetKoulutustyypit.exists(tyyppi => {
      var koodiUritOfKoulutustyyppi = koodiUriCache.get(tyyppi)
      if (koodiUritOfKoulutustyyppi.isEmpty) {
        get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", tyyppi), followRedirects = true) { response =>
          {
            val koulutukset = parse(response)
              .extract[List[Koulutustyyppi]]
              .filter(koulutus =>
                koulutus.koodisto.getOrElse(emptyKoodistoElement).koodistoUri == "koulutus" &&
                  isKoodiVoimassa(tyyppi, koulutus.koodiUri, now, koulutus.voimassaLoppuPvm)
              )
            koodiUritOfKoulutustyyppi = Some(koulutukset.map(koulutus => KoodiUri(koulutus.koodiUri, koulutus.versio)))
            koodiUriCache.put(tyyppi)(koodiUritOfKoulutustyyppi.get, Some(15.minutes))
          }
        }
      }
      koodiUriExists(koodiUri, koodiUritOfKoulutustyyppi.get)
    })
  }

  private def isKoodiVoimassa(
      koulutustyyppi: String,
      koodiUri: String,
      currentDate: LocalDateTime,
      dateToCompare: Option[String]
  ): Boolean =
    if (dateToCompare.isDefined) {
      Try[LocalDate] {
        LocalDate.parse(dateToCompare.get, ISO_LOCAL_DATE_FORMATTER)
      } match {
        case Success(dateVal) => currentDate.isBefore(dateVal.atTime(LocalTime.MAX))
        case Failure(_) =>
          logger.warn(
            s"Failed to parse voimassaLoppuPvm '${dateToCompare.get}' of koodiUri '${koodiUri}' of tyyppi '${koulutustyyppi}'"
          )
          false
      }
    } else {
      true
    }

  private def koodiUriExists(koodiUri: String, availableUris: Seq[KoodiUri]): Boolean = {
    val koodiUriBase = koodiUri.split("#").head
    val koodiUriVersion =
      if (koodiUri.contains("#")) {
        Try[Int] {
          koodiUri.split("#").last.toInt
        } match {
          case Success(version) => version
          case Failure(_)       => 0
        }
      } else {
        1
      }

    if (koodiUriVersion > 0) {
      availableUris
        .exists(candidate => koodiUriBase == candidate.koodiUri && koodiUriVersion <= candidate.latestVersio)
    } else {
      false
    }
  }
}
