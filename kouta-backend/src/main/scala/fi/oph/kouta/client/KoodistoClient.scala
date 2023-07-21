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

case class KoodiElementLite(
  codeElementUri: String,
  codeElementVersion: Int = 1,
  passive: Boolean
) {
  def koodisto(): KoodistoSubElement = {
    KoodistoSubElement(codeElementUri.split("_")(0))
  }

  // This might cause issues as koodiArvo might not be same as what is in uri
  def koodiArvo(): String = {
    codeElementUri.split("_")(1)
  }

  def toKoodiElement: KoodiElement = {
    KoodiElement(codeElementUri, koodiArvo(), codeElementVersion, Some(koodisto())
    )
  }
}

case class KoodiElement(
    koodiUri: String,
    koodiArvo: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement] = None,
    voimassaLoppuPvm: Option[String] = None,
    metadata: List[KoodistoMetadataElement] = List.empty,
    withinCodeElements: Seq[KoodiElementLite] = Seq.empty,
    includesCodeElements: Seq[KoodiElementLite] = Seq.empty,
    levelsWithCodeElements: Seq[KoodiElementLite] = Seq.empty
) {

  def belongsToKoodisto(koodistoUri: String): Boolean = {
    koodisto.exists(k => koodistoUri.equals(k.koodistoUri))
  }

  def asKielistetty(): Kielistetty = {
    metadata.map(element => Kieli.withName(element.kieli.toLowerCase) -> element.nimi).toMap
  }

  def containsYlaKoodiWithKoodisto(ylaKoodiUri: String, ylaKoodistoUri: String): Boolean = {
    withinCodeElements.exists(koodi => koodi.codeElementUri == removeVersio(ylaKoodiUri) &&
      koodi.koodisto().koodistoUri == ylaKoodistoUri)
  }

  def hasYlakoodiWithinKoodisto(ylaKoodistoUri: String): Boolean = {
    withinCodeElements.exists(koodi => koodi.koodisto().koodistoUri == ylaKoodistoUri)
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

  def contains(koodiUri: String, koodistoElements: Seq[KoodiElement]): Boolean = {
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

  private def getWithRetry[A](url: String, followRedirects: Boolean = false, retries: Int = 1)(parse: String => A): Either[KoodistoError, A] = {
    Try[A] {
      get(url, (url, status, response) => throw KoodistoError(url, Some(status), response), followRedirects)(parse)
    } match {
      case Success(result) => Right(result)
      case Failure(exp: KoodistoError) if retries > 0 && exp.status.exists(retryStatusCodes.contains(_)) =>
        logger.warn(s"Failed to get data from koodisto url $url, retrying...")
        getWithRetry(url, followRedirects, retries - 1)(parse)
      case Failure(exp: KoodistoError) =>
        logger.error(s"Error when getting data from koodisto url $url: $exp")
        Left(exp)
      case Failure(exp: Throwable) =>
        logger.error(s"Error when parsing data from koodisto url $url: $exp")
        Left(KoodistoError(url, None, exp.getMessage))
    }
  }

  val getKoodistoKoodit = new CachedMethod[String, Seq[KoodiElement]](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodisto => getWithRetry(
      urlProperties.url("koodisto-service.koodisto-koodit-relaatioineen",
        removeVersio(koodisto),
        getVersio(koodisto).map(Integer.valueOf).getOrElse(Integer.valueOf(1))),
      followRedirects = true
    ) { response => parse(response).extract[List[KoodiElement]] })

  val getKoodistoElementVersion = new CachedMethod[String, KoodiElement](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.koodiuri-version", removeVersio(koodiUri), getVersio(koodiUri).get.toString),
      followRedirects = true
    ) { response => parse(response).extract[KoodiElement] })

  val getKoodistoElementLatestVersion = new CachedMethod[String, KoodiElement](
    Scaffeine().expireAfterWrite(10.minutes).build(),
    koodiUri => getWithRetry(
      urlProperties.url("koodisto-service.latest-koodiuri", removeVersio(koodiUri)),
      followRedirects = true
    ) { response => parse(response).extract[KoodiElement] })

  def invalidateCaches() = {
    getKoodistoKoodit.clearCache()
    getKoodistoElementVersion.clearCache()
    getKoodistoElementLatestVersion.clearCache()
  }
}