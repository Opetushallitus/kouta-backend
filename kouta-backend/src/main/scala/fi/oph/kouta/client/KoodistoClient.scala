package fi.oph.kouta.client

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.properties.OphProperties
import org.json4s.DefaultFormats

import java.time.format.DateTimeFormatter

case class KoodiUri(koodiUri: String, latestVersio: Int)

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)

object KoodistoUtils {
  def koodiUriFromString(koodiUriString: String): KoodiUri = {
    if (koodiUriString.contains("#")) {
      val baseVal = koodiUriString.split("#").head
      val versioPart = koodiUriString.split("#").last
      if (versioPart.forall(Character.isDigit)) {
        KoodiUri(baseVal, versioPart.toInt)
      } else {
        // Tämä on käytännössä virhetilanne, KoodiUrin versio on aina numeerinen
        KoodiUri(koodiUriString, 1)
      }
    } else {
      KoodiUri(koodiUriString, 1)
    }
  }

  def koodiUriExistsInList(koodiUri: String, koodiUriList: Seq[KoodiUri]): Boolean = {
    val theKoodiUri = koodiUriFromString(koodiUri)
    koodiUriList.exists(uri =>
      uri.koodiUri == theKoodiUri.koodiUri &&
        uri.latestVersio >= theKoodiUri.latestVersio)
  }
}

abstract class KoodistoClient(urlProperties: OphProperties)
    extends HttpClient
    with CallerId
    with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val formats = DefaultFormats

  val errorHandler = (url: String, status: Int, response: String) =>
    throw KoodistoQueryException(url, status, response)
}

