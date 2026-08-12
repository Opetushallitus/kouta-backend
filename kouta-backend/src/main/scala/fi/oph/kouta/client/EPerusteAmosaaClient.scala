package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.vm.sade.properties.OphProperties
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse

case class EPerusteAmosaaQueryException(url: String, status: Int, message: String) extends RuntimeException(message)

object EPerusteAmosaaClient extends EPerusteAmosaaClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteAmosaaClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  val errorHandler = (url: String, status: Int, response: String) => throw EPerusteAmosaaQueryException(url, status, response)

  def getOpetussuunnitelmat(organisaatio: String): JValue = {
    val url = urlProperties.url("eperusteet-amosaa-service.opetussuunnitelmat") + s"?organisaatio=$organisaatio"
    get(url, errorHandler, followRedirects = true) { response =>
      parse(response)
    }
  }
}
