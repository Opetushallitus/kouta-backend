package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{getVersio, removeVersio}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Kieli, Kielistetty}
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

case class KoodistoError(url: String, status: Option[Int], message: String) extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)

case class KoodistoMetadataElement(
    nimi: String = "",
    kieli: String = ""
)

case class KoodistoElement(
    koodiUri: String,
    koodiArvo: String,
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

object KoodistoClient extends KoodistoClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  implicit val formats = DefaultFormats

  private def getWithRetry[A](url: String, followRedirects: Boolean = false)(parse: String => A): Either[KoodistoError, A] = {
    val maxRetries = 1;
    var retry = 0;
    while (true) {
      Try[A] {
        get(url, (url, status, response) => throw KoodistoError(url, Some(status), response), followRedirects)(parse)
      } match {
        case Success(result) => return Right(result)
        case Failure(exp: KoodistoError) if retry < maxRetries && exp.status.isDefined && retryStatusCodes.contains(exp.status.get) =>
          retry += 1
          logger.warn(s"Failed to get data from koodisto url ${url}, retrying ($retry) ...")
        case Failure(exp: KoodistoError) => return Left(exp)
        case Failure(exp: Throwable) => Left(KoodistoError(url, None, exp.getMessage)) // parse feilaa
      }
    }
    throw new RuntimeException() // tänne ei tulla koskaan mutta kääntäjä vaatii
  }

  val getKoodistoKoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodisto => getWithRetry(
      urlProperties.url("koodisto-service.koodisto-koodit",
        koodisto), followRedirects = true
    ) { response => parse(response).extract[List[KoodistoElement]] })

  val getYlakoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.sisaltyy-ylakoodit", koodiUri),
      followRedirects = true
    ) { response => parse(response).extract[List[KoodistoElement]] })

  val getRinnasteisetKoodit = new CachedMethod[String, Seq[KoodistoElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.koodisto-koodit.rinnasteiset", koodiUri),
      followRedirects = true) { response => parse(response).extract[List[KoodistoElement]] })

  val getKoodistoElementVersion = new CachedMethod[String, KoodistoElement](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.koodiuri-version", removeVersio(koodiUri), getVersio(koodiUri).get.toString),
      followRedirects = true
    ) { response => parse(response).extract[KoodistoElement] })

  val getKoodistoElementLatestVersion = new CachedMethod[String, KoodistoElement](
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