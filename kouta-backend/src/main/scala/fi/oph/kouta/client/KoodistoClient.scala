package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{contains, getVersio, removeVersio}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Kieli, Kielistetty, KoodistoNimi, OppiaineKoodisto, oppiaineKielitasoKoodiUriEtuliitteet}
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, itemNotFound, queryFailed}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class KoodistoNotFoundException(message: String)                        extends RuntimeException(message)

case class KoodistoError(url: String, status: Option[Int], message: String) extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)

case class KoodistoMetadataElement(
    nimi: String = "",
    kieli: String = ""
)

case class KoodistoElement(
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement] = None,
    voimassaLoppuPvm: Option[String] = None,
    metadata: List[KoodistoMetadataElement] = List.empty
) {

  def belongsToKoodisto(koodistoUri: String): Boolean = {
    koodisto.exists(k => koodistoUri.equals(k.koodistoUri))
  }

  def asKielistetty(): Kielistetty = {
    metadata.map(element => Kieli.withName(element.kieli.toLowerCase) -> element.nimi).toMap
  }
}

object KoodistoUtils {

  def removeVersio(koodiUri: String): String = {
    koodiUri.split("#")(0);
  }

  def getVersio(koodiUri: String): Option[Int] = {
    koodiUri.split("#") match {
      case parts if parts.length == 2 => Some(parts(1).toInt)
      case _ => None
    }
  }

  def contains(koodiUri: String, koodistoElements: Seq[KoodistoElement]): Boolean = {
    val versio = getVersio(koodiUri).getOrElse(Int.MinValue);
    val base = removeVersio(koodiUri);
    koodistoElements.exists(element => element.koodiUri.equals(base) && element.versio>=versio);
  }
}

object CachedKoodistoClient extends CachedKoodistoClient(KoutaConfigurationFactory.configuration.urlProperties)

class CachedKoodistoClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def getKoodistoKaannoksetFromCache(koodisto: String): Map[String, Kielistetty] = {
    getKoodistoKoodit(koodisto).map(elements => elements
        .filter(isKoodiVoimassa)
        .map(element => (element.koodiUri, element.asKielistetty()))
        .toMap)
      .getOrElse(Map.empty);
  }

  def getRinnasteisetCached(koodiUri: String, koodisto: String): Seq[KoodistoElement] = {
    logger.info(s"Haetaan rinnasteiset koodit koodiUrille $koodiUri")
    getRinnasteisetKoodit(removeVersio(koodiUri)) match {
      case Right(result) => result
        .filter(element => element.belongsToKoodisto(koodisto))
        .filter(isKoodiVoimassa)
      case Left(exp) if exp.status.isDefined && exp.status == 404 => Seq.empty
      case Left(exp) => {
        logger.error("Rinnasteisten koodien haku epäonnistui: ", exp)
        throw exp
      }
    }
  }

  def getKoodistoElementVersionOrLatestFromCache(koodiUri: String): Either[Throwable, KoodistoElement] = {
    getVersio(koodiUri) match {
      case Some(_) => getKoodistoElementVersion(koodiUri)
      case None => getKoodistoElementLatestVersion(koodiUri)
    }
  }

  def getKoulutuksetByTutkintotyyppi(tutkintotyyppi: String): Either[Throwable, Seq[KoodistoElement]] = {
    getYlakoodit(tutkintotyyppi) match {
      case Right(result) => Right(result.filter(isKoodiVoimassa))
      case Left(_) => Left(new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyyppi $tutkintotyyppi from koodisto."))
    }
  }

  def koodiUriExistsInKoodisto(koodisto: KoodistoNimi, koodiUri: String): ExternalQueryResult = {
    getKoodistoKoodit(koodisto.toString) match {
      case Right(elements: Seq[KoodiUri]) => fromBoolean(contains(koodiUri, elements.filter(isKoodiVoimassa)))
      case Left(exp) if exp.status.isDefined && exp.status.get == 404 => itemNotFound
      case Left(exp) => queryFailed
    }
  }

  def koulutusKoodiUriOfKoulutustyypitExistFromCache(koulutustyypit: Seq[String], koodiUri: String): ExternalQueryResult = {
    for(koulutusTyyppi <- koulutustyypit) {
      getYlakoodit(koulutusTyyppi).map(result => result
        .filter(isKoodiVoimassa)
        .filter(element => element.koodisto.isDefined && element.koodisto.get.koodistoUri == "koulutus")) match {
          case Right(koulutusTyyppiKoodiUrit) => if(contains(koodiUri, koulutusTyyppiKoodiUrit)) return itemFound
          case Left(_) => return queryFailed
        }
    }
    itemNotFound
  }

  // Oletus: sallitutKoodiUrit eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata sallituissa koodiUreissa
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(sallitutKoodiUrit: Seq[String], koodiUri: String): ExternalQueryResult = {
    getKoodistoKoodit("koulutus") match {
      case Right(elements) =>
        val koulutusKoodiUrit = elements
          .filter(isKoodiVoimassa)
          .filter(koodistoElement => sallitutKoodiUrit.exists(sallittuKoodiUri => koodistoElement.koodiUri == sallittuKoodiUri)
        )
        fromBoolean(contains(koodiUri, koulutusKoodiUrit))
      case Left(_) => queryFailed
    }
  }

  def oppiaineArvoExists(oppiaineArvo: String): ExternalQueryResult = {
    if (oppiaineKielitasoKoodiUriEtuliitteet contains oppiaineArvo) {
      itemFound
    } else {
      koodiUriExistsInKoodisto(OppiaineKoodisto, oppiaineArvo)
    }
  }

  protected def isKoodiVoimassa(koodistoElement: KoodistoElement) = {
    val dateToCompare = koodistoElement.voimassaLoppuPvm
    val currentDate = ZonedDateTime.now().toLocalDateTime
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
}

class CachedMethod[A, B](cache: Cache[A, B], method: A => Either[KoodistoError, B]) extends Function[A, Either[KoodistoError, B]] {

  def apply(key: A): Either[KoodistoError, B] = {
    try {
      Right(cache.get(key, k => {
        method(k: A) match {
          case Right(result: B) => result
          case Left(err: KoodistoError) => throw err
        }
      }))
    } catch {
      case(err: KoodistoError) => Left(err)
    }
  }

  def clearCache() = {
    cache.invalidateAll()
  }
}

abstract class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  implicit val formats = DefaultFormats

  private def getWithRetry[A](url: String, followRedirects: Boolean = false) (parse: String => A): Either[KoodistoError, A] = {
    val maxRetries = 1;
    var retry = 0;
    while(true) {
      Try[A] {
        get(url, (url, status, response) => throw KoodistoError(url, Some(status), response), followRedirects)(parse)
      } match {
        case Success(result) => return Right(result)
        case Failure(exp: KoodistoError) if retry<maxRetries && exp.status.isDefined && retryStatusCodes.contains(exp.status.get) =>
          retry += 1
          logger.warn(s"Failed to get data from koodisto url ${url}, retrying ($retry) ...")
        case Failure(exp: KoodistoError) => return Left(exp)
        case Failure(exp: Throwable) => Left(KoodistoError(url, None, exp.getMessage)) // parse feilaa
      }
    }
    throw new RuntimeException() // tänne ei tulla koskaan mutta kääntäjä vaatii
  }

  protected val getKoodistoKoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodisto => getWithRetry(
      urlProperties.url("koodisto-service.koodisto-koodit",
      koodisto), followRedirects = true
    ) { response => parse(response).extract[List[KoodistoElement]]})

  protected val getYlakoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.sisaltyy-ylakoodit", koodiUri),
      followRedirects = true
    ) { response => parse(response).extract[List[KoodistoElement]] })

  protected val getRinnasteisetKoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.koodisto-koodit.rinnasteiset", koodiUri),
      followRedirects = true)
    { response => parse(response).extract[List[KoodistoElement]] })

  protected val getKoodistoElementVersion = new CachedMethod[String, KoodistoElement](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
        urlProperties.url("koodisto-service.koodiuri-version", removeVersio(koodiUri), getVersio(koodiUri).get.toString),
        followRedirects = true
      ) { response => parse(response).extract[KoodistoElement] })

  protected val getKoodistoElementLatestVersion = new CachedMethod[String, KoodistoElement](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
        urlProperties.url("koodisto-service.latest-koodiuri", removeVersio(koodiUri)),
        followRedirects = true
      ) { response => parse(response).extract[KoodistoElement] })

  def invalidateCaches() = {
    getKoodistoKoodit.clearCache()
    getYlakoodit.clearCache()
    getRinnasteisetKoodit.clearCache()
    getKoodistoElementVersion.clearCache()
    getKoodistoElementLatestVersion.clearCache()
  }
}
