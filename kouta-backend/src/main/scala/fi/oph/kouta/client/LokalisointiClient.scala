package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.LokalisointiClientUtil.{Kaannos, parseKaannokset}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Kieli
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration._

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

  private def getKaannoksetWithKey(key: String): Map[Kieli, String] = {
    get (lokalisointiUrl + s"&key=$key", followRedirects = true) {response =>
    parseKaannokset (parse (response).extract[List[Kaannos]] )
  }
  }
  def getKaannoksetWithKeyFromCache(key: String): Map[Kieli, String] = {
   LokalisointiCache.get(key, key => getKaannoksetWithKey(key))
  }
}
