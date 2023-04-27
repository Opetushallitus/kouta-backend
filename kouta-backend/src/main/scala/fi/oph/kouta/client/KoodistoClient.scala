package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{
  koodiUriFromString,
  koodiUriWithEqualOrHigherVersioNbrInList,
  splitToBaseAndVersion
}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Kieli, Kielistetty, KoodistoNimi, OppiaineKoodisto, oppiaineKielitasoKoodiUriEtuliitteet}
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.validation.ExternalQueryResults.{
  ExternalQueryResult,
  fromBoolean,
  itemFound,
  itemNotFound,
  queryFailed
}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

case class KoodiUri(koodiUri: String, versio: Int, nimi: Kielistetty = Map())
object KoodiUri {
  def apply(koodiUri: String, versio: Int, metadata: List[KoodistoMetadataElement]): KoodiUri = KoodiUri(
    koodiUri,
    versio,
    metadata.map(mDataElem => Kieli.withName(mDataElem.kieli.toLowerCase) -> mDataElem.nimi).toMap
  )
}

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class KoodistoNotFoundException(message: String)                        extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)

case class KoodistoMetadataElement(
    nimi: String = "",
    kieli: String = ""
)

case class KoodistoElement(
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement],
    voimassaLoppuPvm: Option[String],
    metadata: List[KoodistoMetadataElement]
) {
  def belongsToKoodisto(koodistoUri: String): Boolean = {
    koodisto.exists(k => koodistoUri.equals(k.koodistoUri))
  }
}

case class KoodistoQueryResponse(success: Boolean, koodiUritInKoodisto: Seq[KoodiUri])

object KoodistoUtils {
  def koodiUriFromString(koodiUriString: String): KoodiUri = {
    splitToBaseAndVersion(koodiUriString) match {
      case (baseVal: String, Some(versio: Int)) => KoodiUri(baseVal, versio)
      case _                                    => KoodiUri(koodiUriString, 1)
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

  def asStringOption(koodiUri: Option[KoodiUri]): Option[String] = {
    koodiUri.map(uri => s"${uri.koodiUri}#${uri.versio}")
  }

  def asStringSeq(koodiUri: Option[KoodiUri]): Seq[String] =
    koodiUri match {
      case Some(uri) => Seq(s"${uri.koodiUri}#${uri.versio}")
      case _         => Seq()
    }
}

object CachedKoodistoClient extends CachedKoodistoClient(KoutaConfigurationFactory.configuration.urlProperties)

class CachedKoodistoClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {

  implicit val koodiUriCache: Cache[String, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val koodiuriVersionCache: Cache[String, KoodiUri] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val rinnasteinenKoodiUriCache: Cache[(String, String), Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val koulutuksetByTutkintotyyppiCache: Cache[String, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  def getKoodistoKaannoksetFromCache(koodisto: String): Map[String, Kielistetty] = {
    val res: KoodistoQueryResponse = getAndUpdateFromKoodiUriCache(koodisto, koodiUriCache)

    if (res.success) {
      res.koodiUritInKoodisto.map(koodi => (koodi.koodiUri, koodi.nimi)).toMap
    } else {
      Map.empty
    }
  }

  def getRinnasteisetCached(koodiUri: String, koodisto: String) = {
    rinnasteinenKoodiUriCache.get((koodiUri, koodisto), params => getRinnasteisetKooditInKoodisto(params._1, params._2))
  }

  def getKoodiUriVersionOrLatestFromCache(koodiUriAsString: String): Either[Throwable, KoodiUri] = {
    Try[KoodiUri] {
      koodiuriVersionCache.get(koodiUriAsString, koodiUriAsString => getKoodiUriVersionOrLatest(koodiUriAsString))
    } match {
      case Success(koodiUri) => Right(koodiUri)
      case Failure(exp)      => Left(exp)
    }
  }

  def getKoulutuksetByTutkintotyyppiCached(tutkintotyyppi: String) = {
    val res = koulutuksetByTutkintotyyppiCache.get(tutkintotyyppi, tutkintotyyppi => getYlakoodit(tutkintotyyppi))
    println(res)
    res
  }

  def koodiUriExistsInKoodisto(koodisto: KoodistoNimi, koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto(koodisto.toString, koodiUri)

  protected def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): ExternalQueryResult =
    getAndUpdateFromKoodiUriCache(koodisto, koodiUriCache) match {
      case resp if resp.success =>
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, resp.koodiUritInKoodisto))
      case _ => queryFailed
    }

  private def getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi: String): Seq[KoodiUri] = {
    getYlakoodit(
      tyyppi,
      koodi =>
        koodi.koodisto.getOrElse(emptyKoodistoSubElement).koodistoUri == "koulutus" &&
          isKoodiVoimassa(koodi)
    )
  }

  private def koulutuskoodiUriOfKoulutusTyypit(tyyppi: String): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
    } match {
      case Success(koulutukset) => koulutukset
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
        } match {
          case Success(koulutukset) => koulutukset
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(
              s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto after retry, got response ${exp.status} ${exp.message}"
            )
          case Failure(exp: Throwable) =>
            throw new RuntimeException(
              s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto after retry, got response ${exp.getMessage}"
            )
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(
          s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, got response ${exp.status} ${exp.message}"
        )
      case Failure(exp: Throwable) =>
        throw new RuntimeException(
          s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, got response ${exp.getMessage}"
        )
    }
  }

  def koulutusKoodiUriOfKoulutustyypitExistFromCache(
      koulutustyypit: Seq[String],
      koodiUri: String
  ): ExternalQueryResult = {
    try {
      val exits = koulutustyypit.exists(tyyppi => {
        val koodiUritOfKoulutustyyppi =
          koodiUriCache.get(tyyppi, tyyppi => koulutuskoodiUriOfKoulutusTyypit(tyyppi))
        koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritOfKoulutustyyppi)
      })
      if (exits) itemFound
      else itemNotFound
    } catch {
      case _: Throwable => queryFailed
    }
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(koodiUriFilter: Seq[String], koodiUri: String): ExternalQueryResult = {
    val filterSeq = koodiUriFilter.map(koodiUriFromString)

    val queryResponse = getAndUpdateFromKoodiUriCache("koulutus", koodiUriCache)
    if (queryResponse.success) {
      val koulutusKoodiUrit = queryResponse.koodiUritInKoodisto.filter(fromCache =>
        filterSeq.exists(filterItem => fromCache.koodiUri == filterItem.koodiUri)
      )
      fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koulutusKoodiUrit))
    } else {
      queryFailed
    }
  }

  def oppiaineArvoExists(oppiaineArvo: String): ExternalQueryResult = {
    if (oppiaineKielitasoKoodiUriEtuliitteet contains oppiaineArvo) {
      itemFound
    } else {
      koodiUriExistsInKoodisto(OppiaineKoodisto, oppiaineArvo)
    }
  }
}

abstract class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val formats = DefaultFormats

  val errorHandler = (url: String, status: Int, response: String) => throw KoodistoQueryException(url, status, response)

  val emptyKoodistoSubElement = KoodistoSubElement("")

  protected def getKoodistoKoodit(koodisto: String): Seq[KoodiUri] =
    get(
      urlProperties.url("koodisto-service.koodisto-koodit", koodisto),
      errorHandler,
      followRedirects = true
    ) { response =>
      {
        parse(response)
          .extract[List[KoodistoElement]]
          .filter(isKoodiVoimassa)
          .map(koodi => KoodiUri(koodi.koodiUri, koodi.versio, koodi.metadata))
      }
    }

  protected def getYlakoodit(
      koodi: String,
      filter: KoodistoElement => Boolean = (_: KoodistoElement) => true
  ): List[KoodiUri] = {
    get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", koodi), errorHandler, followRedirects = true) {
      response =>
        {
          parse(response)
            .extract[List[KoodistoElement]]
            .filter(filter)
            .map(element => KoodiUri(element.koodiUri, element.versio))
        }
    }
  }

  def getRinnasteisetKooditInKoodisto(koodiUri: String, koodisto: String): Seq[KoodiUri] = {
    logger.info(s"Haetaan rinnasteiset koodit koodiUrille $koodiUri")
    try {
      get(
        urlProperties.url("koodisto-service.koodisto-koodit.rinnasteiset", splitToBaseAndVersion(koodiUri)._1),
        errorHandler,
        followRedirects = true
      ) { response =>
        {
          parse(response)
            .extract[List[KoodistoElement]]
            .filter((koodi: KoodistoElement) =>
              koodi.belongsToKoodisto(koodisto) &&
                isKoodiVoimassa(koodi)
            )
            .map(koodiUri => KoodiUri(koodiUri.koodiUri, koodiUri.versio))
        }
      }
    } catch {
      case e: KoodistoQueryException if e.status == 404 => List.empty
      case e: Throwable =>
        logger.error("Rinnasteisten koodien haku epäonnistui: ", e)
        throw e
    }
  }

  private def getAndUpdateFromKoodiUri(koodisto: String): Seq[KoodiUri] = {
    val contentDesc = s"koodiuris from koodisto $koodisto"
    Try[Seq[KoodiUri]] {
      getKoodistoKoodit(koodisto)
    } match {
      case Success(koodiUrit) => koodiUrit
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koodiuris from koodisto $koodisto, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoodistoKoodit(koodisto)
        } match {
          case Success(koodiUrit)    => koodiUrit
          case Failure(t: Throwable) => throw exception(t, contentDesc, true)
        }
      case Failure(t: Throwable) => throw exception(t, contentDesc, false)
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
      case _: Throwable                 => KoodistoQueryResponse(success = false, Seq())
    }
  }

  protected def isKoodiVoimassa(koodistoElement: KoodistoElement) = {
    val dateToCompare = koodistoElement.voimassaLoppuPvm
    val currentDate   = ZonedDateTime.now().toLocalDateTime
    if (koodistoElement.voimassaLoppuPvm.isDefined) {
      Try[LocalDate] {
        LocalDate.parse(dateToCompare.get, ISO_LOCAL_DATE_FORMATTER)
      } match {
        case Success(dateVal) => currentDate.isBefore(dateVal.atTime(LocalTime.MAX))
        case Failure(_) =>
          logger.warn(
            s"Failed to parse voimassaLoppuPvm '${dateToCompare.get}' of koodiUri '${koodistoElement.koodiUri}' of koodisto '${koodistoElement.koodisto}'"
          )
          false
      }
    } else {
      true
    }
  }

  private def getKoodiUriVersionOrLatestFromKoodistoService(koodiUriAsString: String): KoodiUri = {
    val baseAndVersion: (String, Option[Int]) = splitToBaseAndVersion(koodiUriAsString)
    get(
      if (baseAndVersion._2.isDefined)
        urlProperties.url("koodisto-service.koodiuri-version", baseAndVersion._1, baseAndVersion._2.get.toString)
      else
        urlProperties.url("koodisto-service.latest-koodiuri", baseAndVersion._1),
      errorHandler,
      followRedirects = true
    ) { response =>
      val elem = parse(response).extract[KoodistoElement]
      KoodiUri(elem.koodiUri, elem.versio, elem.metadata)

    }
  }

  protected def getKoodiUriVersionOrLatest(koodiUriAsString: String): KoodiUri = {
    val contentDesc = s"koodiuri-version from koodisto for $koodiUriAsString"
    Try[KoodiUri] {
      getKoodiUriVersionOrLatestFromKoodistoService(koodiUriAsString)
    } match {
      case Success(koodiUri) => koodiUri
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get $contentDesc, retrying once...")
        Try[KoodiUri] {
          getKoodiUriVersionOrLatestFromKoodistoService(koodiUriAsString)
        } match {
          case Success(koodiUri)     => koodiUri
          case Failure(t: Throwable) => throw exception(t, contentDesc, true)
        }
      case Failure(t: Throwable) => throw exception(t, contentDesc, false)
    }
  }

  private def exception(throwable: Throwable, contentDesc: String, retryDone: Boolean): Throwable = {
    val retryDoneMsg = if (retryDone) " after retry" else ""
    throwable match {
      case exp: KoodistoQueryException if exp.status == 404 =>
        KoodistoNotFoundException(s"Unable to find $contentDesc, got response ${exp.status}, ${exp.message}")
      case exp: KoodistoQueryException =>
        new RuntimeException(s"Failed to get $contentDesc$retryDoneMsg, got response ${exp.status}, ${exp.message}")
      case _ =>
        new RuntimeException(s"Failed to get $contentDesc$retryDoneMsg, got response ${throwable.getMessage()}")
    }
  }

}
