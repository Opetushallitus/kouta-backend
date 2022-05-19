package fi.oph.kouta.client

import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import fi.oph.kouta.domain.{Fi, Sv, En, Kielistetty}
import org.json4s.{DefaultFormats}
import scalacache.caffeine._
import scalacache.memoization.memoizeSync
import scalacache.modes.sync._
import scala.concurrent.duration._

object KoodistoClient extends KoodistoClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {
  implicit val formats       = DefaultFormats
  implicit val KoodistoCache = CaffeineCache[Map[String, Kielistetty]]
  implicit val koodiUriCache = CaffeineCache[String]

  case class CodeElementMetadataItem(nimi: String, kieli: String)
  case class CodeElement(koodiUri: String, metadata: List[CodeElementMetadataItem] = List())
  case class CodeElementWithVersion(koodiUri: String, versio: Int)

  def parseKoodi(codeElement: CodeElement): Kielistetty = {
    codeElement.metadata
      .withFilter(metaItem => List("FI", "SV", "EN").contains(metaItem.kieli))
      .map((metaItem: CodeElementMetadataItem) =>
        (
          metaItem.kieli match {
            case "FI" => Fi
            case "SV" => Sv
            case "EN" => En
          },
          metaItem.nimi
        )
      )
      .toMap
  }
  def parseKoodit(codeElements: List[CodeElement]): Map[String, Kielistetty] = {
    codeElements.map(codeElement => (codeElement.koodiUri, parseKoodi(codeElement))).toMap
  }
  def getKoodistoKaannokset(koodisto: String): Map[String, Kielistetty] =
    memoizeSync[Map[String, Kielistetty]](Some(5.minutes)) {
      get(urlProperties.url("koodisto-service.koodisto-koodit", koodisto), followRedirects = true) { response =>
        parseKoodit(parse(response).extract[List[CodeElement]])
      }
    }

  def getKoodiUriWithLatestVersion(koodiUriWithoutVersion: String): String = {
    memoizeSync[String](Some(5.minutes)) {
      get(urlProperties.url("koodisto-service.latest-koodiuri", koodiUriWithoutVersion), followRedirects = true) { response =>
        {
          val codeElement = parse(response).extract[CodeElementWithVersion]
          s"${codeElement.koodiUri}#${codeElement.versio}"
        }
      }
    }
  }
}
