package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.service.LegacyHakukohde
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

object LegacyTarjontaClient extends LegacyTarjontaClient

case class LegacyHaku(oid: String,
                      hakukausiVuosi: Option[String])

case class TarjontaHakukohdeResult(result: LegacyHakukohde)

case class TarjontaHakuResult(result: LegacyHaku)


class LegacyTarjontaClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def getHakukohde(oid: String) = {
    val url = urlProperties.url(
      s"legacy-tarjonta-service.hakukohde.oid", oid)
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[TarjontaHakukohdeResult].result
      }
    }
  }

  def getHaku(oid: String) = {
    val url = urlProperties.url(
      s"legacy-tarjonta-service.haku.oid", oid)
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[TarjontaHakuResult].result
      }
    }
  }

}

