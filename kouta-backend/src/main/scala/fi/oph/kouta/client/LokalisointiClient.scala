package fi.oph.kouta.client

import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.properties.OphProperties
import fi.oph.kouta.client.LokalisointiClientUtil.{Kaannos, parseKaannokset}
import fi.oph.kouta.domain.Kieli

object LokalisointiClient extends LokalisointiClient(KoutaConfigurationFactory.configuration.urlProperties)

class LokalisointiClient(urlProperties: OphProperties)
    extends HttpClient
    with CallerId
    with Logging
    with KoutaJsonFormats {

  val lokalisointiUrl: String =
    urlProperties.url(
      "lokalisointi-service.localisation"
    )

  def getKaannoksetWithKey(key: String): Map[Kieli, String] = {
    get(lokalisointiUrl + s"&key=$key", followRedirects = true) { response =>
      parseKaannokset(parse(response).extract[List[Kaannos]])
    }
  }

}
