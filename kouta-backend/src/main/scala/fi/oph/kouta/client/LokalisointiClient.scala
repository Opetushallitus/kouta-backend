package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.LokalisointiClientUtil.{Kaannos, parseKaannokset}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Kieli
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class LokalisointiQueryException(url: String, status: Int, message: String) extends RuntimeException(message)

object LokalisointiClient extends LokalisointiClient(KoutaConfigurationFactory.configuration.urlProperties)

class LokalisointiClient(urlProperties: OphProperties)
    extends HttpClient
    with CallerId
    with Logging
    with KoutaJsonFormats {

  implicit val LokalisointiCache: Cache[String, Map[Kieli, String]] = Scaffeine()
    .expireAfterWrite(5.minutes)
    .build()

  val lokalisointiUrl: String =
    urlProperties.url(
      "lokalisointi-service.localisation"
    )

  private def getKaannoksetWithKeyFromLokalisointiService(key: String): Map[Kieli, String] =
    get(lokalisointiUrl + s"&key=$key", followRedirects = true) { response =>
      parseKaannokset(parse(response).extract[List[Kaannos]])
    }

  private def getKaannoksetWithKey(key: String): Map[Kieli, String] = {
    Try[Map[Kieli, String]] {
      getKaannoksetWithKeyFromLokalisointiService(key)
    } match {
      case Success(kaannokset) => kaannokset
      case Failure(exp: LokalisointiQueryException) if exp.status == 404 =>
        logger.warn(
          s"Unable to find kaannokset with key $key, got response ${exp.status}, ${exp.message}"
        )
        Map()
      case Failure(exp: LokalisointiQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get kaannokset with key $key, retrying once...")
        Try[Map[Kieli, String]] {
          getKaannoksetWithKeyFromLokalisointiService(key)
        } match {
          case Success(kaannokset)   => kaannokset
          case Failure(t: Throwable) => throw exception(t, key, true)
        }
      case Failure(t: Throwable) => throw exception(t, key, false)
    }
  }

  def getKaannoksetWithKeyFromCache(key: String): Map[Kieli, String] = {
    LokalisointiCache.get(key, key => getKaannoksetWithKey(key))
  }

  private def exception(throwable: Throwable, key: String, retryDone: Boolean): Throwable = {
    val retryDoneMsg  = if (retryDone) " after retry" else ""
    val messagePrefix = s"Failed to get kaannokset with key $key$retryDoneMsg, got response"
    throwable match {
      case exp: LokalisointiQueryException =>
        new RuntimeException(s"${messagePrefix} ${exp.status}, ${exp.message}")
      case _ =>
        new RuntimeException(s"${messagePrefix} ${throwable.getMessage()}")
    }
  }
}
