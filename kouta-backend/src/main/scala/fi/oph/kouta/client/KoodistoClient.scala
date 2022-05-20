package fi.oph.kouta.client

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.properties.OphProperties
import org.json4s.DefaultFormats

import java.time.format.DateTimeFormatter

abstract class KoodistoClient(urlProperties: OphProperties)
    extends HttpClient
    with CallerId
    with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val formats = DefaultFormats
}
