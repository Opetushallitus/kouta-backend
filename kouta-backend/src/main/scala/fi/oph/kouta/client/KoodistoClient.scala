package fi.oph.kouta.client

import com.github.blemale.scaffeine.Cache
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}

case class KoodiUri(koodiUri: String, latestVersio: Int)

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class KoodistoNotFoundException(message: String) extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)
case class KoodistoElement(
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement],
    voimassaLoppuPvm: Option[String]
)

case class KoodistoQueryResponse(success: Boolean, koodiUritInKoodisto: Seq[KoodiUri])

object KoodistoUtils {
  def koodiUriFromString(koodiUriString: String): KoodiUri = {
    if (koodiUriString.contains("#")) {
      val baseVal    = koodiUriString.split("#").head
      val versioPart = koodiUriString.split("#").last
      if (versioPart.forall(Character.isDigit)) {
        KoodiUri(baseVal, versioPart.toInt)
      } else {
        // Tämä on käytännössä virhetilanne, KoodiUrin versio on aina numeerinen
        KoodiUri(koodiUriString, 1)
      }
    } else {
      KoodiUri(koodiUriString, 1)
    }
  }

  def koodiUriToString(koodiUri: KoodiUri): String =
    s"${koodiUri.koodiUri}#${koodiUri.latestVersio}"

  def koodiUriStringsMatch(a: String, b: String): Boolean =
    koodiUriFromString(a).koodiUri.equals(koodiUriFromString(b).koodiUri)

  def koodiUriWithEqualOrHigherVersioNbrInList(
      koodiUri: String,
      koodiUriList: Seq[KoodiUri],
      checkVersio: Boolean = true
  ): Boolean = {
    val koodiUriObjectToSearch =
      if (checkVersio) koodiUriFromString(koodiUri)
      else
        koodiUriFromString(koodiUri).copy(latestVersio = 1)
    koodiUriList.exists(uri =>
      uri.koodiUri == koodiUriObjectToSearch.koodiUri &&
        uri.latestVersio >= koodiUriObjectToSearch.latestVersio
    )
  }
}

abstract class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val formats = DefaultFormats

  val errorHandler = (url: String, status: Int, response: String) => throw KoodistoQueryException(url, status, response)

  val emptyKoodistoSubElement = KoodistoSubElement("")

  protected def getKoodiFromKoodistoService(koodisto: String): Seq[KoodiUri] = {
    get(
      urlProperties.url("koodisto-service.koodisto-koodit", koodisto),
      errorHandler,
      followRedirects = true
    ) { response => {
      parse(response)
        .extract[List[KoodistoElement]]
        .filter(koodiUri =>
          isKoodiVoimassa(koodisto, koodiUri.koodiUri, dateToCompare = koodiUri.voimassaLoppuPvm)
        )
        .map(koodiUri => KoodiUri(koodiUri.koodiUri, koodiUri.versio))
    }
    }
  }

  protected def getAndUpdateFromKoodiUri(koodisto: String): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getKoodiFromKoodistoService(koodisto)
    } match {
      case Success(koodiUrit) => koodiUrit
      case Failure(exp: KoodistoQueryException) if exp.status == 404 =>
        throw KoodistoNotFoundException(
          s"Failed to find koodiuris from koodisto $koodisto, got response ${exp.status} ${exp.message}"
        )
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koodiuris from koodisto $koodisto, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoodiFromKoodistoService(koodisto)
        } match {
          case Success(koodiUrit) => koodiUrit
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(
              s"Failed to get koodiuris from koodisto $koodisto, got response ${exp.status} ${exp.message}"
            )
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(
          s"Failed to get koodiuris from koodisto $koodisto, got response ${exp.status} ${exp.message}"
        )
    }
  }

  protected def getAndUpdateFromKoodiUriCache(
                                               koodisto: String,
                                               koodiUriCache: Cache[String, Seq[KoodiUri]]
                                             ): KoodistoQueryResponse = {
    try {
      val koodiUritFromCache = koodiUriCache.get(koodisto, koodisto => getAndUpdateFromKoodiUri(koodisto))
      KoodistoQueryResponse(success = true, koodiUritFromCache)
    } catch {
      case _: KoodistoNotFoundException => KoodistoQueryResponse(success = true, Seq())
      case _: Throwable => KoodistoQueryResponse(success = false, Seq())
    }
  }

  protected def isKoodiVoimassa(
      koodisto: String,
      koodiUri: String,
      currentDate: LocalDateTime = ZonedDateTime.now().toLocalDateTime,
      dateToCompare: Option[String]
  ): Boolean =
    if (dateToCompare.isDefined) {
      Try[LocalDate] {
        LocalDate.parse(dateToCompare.get, ISO_LOCAL_DATE_FORMATTER)
      } match {
        case Success(dateVal) => currentDate.isBefore(dateVal.atTime(LocalTime.MAX))
        case Failure(_) =>
          logger.warn(
            s"Failed to parse voimassaLoppuPvm '${dateToCompare.get}' of koodiUri '${koodiUri}' of koodisto '${koodisto}'"
          )
          false
      }
    } else {
      true
    }
}
