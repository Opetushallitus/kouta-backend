package fi.oph.kouta.client

import com.github.blemale.scaffeine.Cache
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.client.KoodistoUtils.splitToBaseAndVersion
import fi.oph.kouta.domain.{Kieli, Kielistetty}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps

case class KoodiUri(koodiUri: String, versio: Int, nimi: Kielistetty = Map()) {
  def this(koodiUri: String, versio: Int, metadata: List[KoodistoMetadataElement]) = {
    this(
      koodiUri,
      versio,
      metadata.map(mDataElem => Kieli.withName(mDataElem.kieli.toLowerCase) -> mDataElem.nimi).toMap
    )
  }
}

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class KoodistoNotFoundException(message: String) extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)

case class KoodistoElement (
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement],
    voimassaLoppuPvm: Option[String]
)

case class KoodistoMetadataElement(
    nimi: String = "",
    kieli: String = ""
)

case class KoodistoElementWithNimi(
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement],
    voimassaLoppuPvm: Option[String],
    metadata: List[KoodistoMetadataElement]
)

case class KoodistoQueryResponse(success: Boolean, koodiUritInKoodisto: Seq[KoodiUri])

object KoodistoUtils {
  def koodiUriFromString(koodiUriString: String): KoodiUri = {
    splitToBaseAndVersion(koodiUriString) match {
      case (baseVal: String, Some(versio: Int)) => KoodiUri(baseVal, versio)
      case _ => KoodiUri(koodiUriString, 1)
    }
  }

  def splitToBaseAndVersion(koodiUri: String): (String, Option[Int]) =
    if (koodiUri.contains("#")) {
      val baseVal    = koodiUri.split("#").head
      val versioPart = koodiUri.split("#").last
      if (versioPart.forall(Character.isDigit)) {
        (baseVal, Some(versioPart.toInt))
      } else {
        // Tämä on käytännössä virhetilanne, KoodiUrin versio on aina numeerinen
        (koodiUri, None)
      }
    } else {
      (koodiUri, None)
    }

  def koodiUriStringsMatch(a: String, b: String): Boolean =
    koodiUriFromString(a).koodiUri.equals(koodiUriFromString(b).koodiUri)

  def koodiUrisEqual(koodiUri: KoodiUri, other: KoodiUri): Boolean =
    koodiUri.koodiUri == other.koodiUri &&
      koodiUri.versio == other.versio

  def koodiUriEqualOrNewerAsOther(koodiUri: KoodiUri, other: KoodiUri): Boolean =
    koodiUri.koodiUri == other.koodiUri &&
      koodiUri.versio >= other.versio

  def koodiUriWithEqualOrHigherVersioNbrInList(
      koodiUri: String,
      koodiUriList: Seq[KoodiUri],
      checkVersio: Boolean = true
  ): Boolean = {
    val koodiUriObjectToSearch =
      if (checkVersio) koodiUriFromString(koodiUri)
      else
        koodiUriFromString(koodiUri).copy(versio = 1)
    koodiUriList.exists(uri => koodiUriEqualOrNewerAsOther(uri, koodiUriObjectToSearch))
  }

  def asStringOption(koodiUri: Option[KoodiUri]): Option[String] =
    koodiUri match {
      case Some(uri) => Some(s"${uri.koodiUri}#${uri.versio}")
      case _ => None
    }

  def asStringSeq(koodiUri: Option[KoodiUri]): Seq[String] =
    koodiUri match {
      case Some(uri) => Seq(s"${uri.koodiUri}#${uri.versio}")
      case _ => Seq()
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
              s"Failed to get koodiuris from koodisto $koodisto after retry, got response ${exp.status} ${exp.message}"
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

  /*
  def getAndUpdateKoodiUriVersionOrLatestFromCache(koodiUriAsString: String, koodiuriVersionCache: CaffeineCache[KoodiUri]): Either[Throwable, Option[KoodiUri]] = {
    var returnValue: Either[Throwable, Option[KoodiUri]] = Right(None)
    var koodiUriVersionOrLatest = koodiuriVersionCache.get(koodiUriAsString)
    if (koodiUriVersionOrLatest.isEmpty) {
      Try[KoodiUri] {
        val baseAndVersion: (String, Option[Int]) = splitToBaseAndVersion(koodiUriAsString)
        get(
          if (baseAndVersion._2.isDefined)
            urlProperties.url("koodisto-service.koodiuri-version", baseAndVersion._1, baseAndVersion._2.get.toString)
          else
            urlProperties.url("koodisto-service.latest-koodiuri", baseAndVersion._1),
          errorHandler,
          followRedirects = true
        ) { response =>
          val elem = parse(response).extract[KoodistoElementWithNimi]
          new KoodiUri(elem.koodiUri, elem.versio, elem.metadata)
        }
      } match {
        case Success(koodiUri) =>
          koodiUriVersionOrLatest = Some(koodiUri)
          koodiuriVersionCache.put(koodiUriAsString)(koodiUriVersionOrLatest.get, Some(15.minutes))
        case Failure(exp: KoodistoQueryException) =>
          val error =
            s"Failed to get koodiuri-version from koodisto for $koodiUriAsString, got response ${exp.status} ${exp.message}"
          logger.error(error)
          returnValue = Left(new RuntimeException(error))
        case Failure(exp: Throwable) =>
          val error =
            s"Failed to get koodiuri-version from koodisto for $koodiUriAsString, got response ${exp.getMessage()}"
          logger.error(error)
          returnValue = Left(new RuntimeException(error))
      }
    }
    if (returnValue.left.toOption.isDefined) returnValue else Right(koodiUriVersionOrLatest)
  }

   */
}
