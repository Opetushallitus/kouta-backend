package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{En, Fi, Kielistetty, Sv}
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeSync
import scalacache.modes.sync.mode

import scala.concurrent.duration.DurationInt

object KoodistoKaannosClient extends KoodistoKaannosClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoodistoKaannosClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val KoodistoCache = CaffeineCache[Map[String, Kielistetty]]

  case class CodeElementMetadataItem(nimi: String, kieli: String)
  case class CodeElement(koodiUri: String, metadata: List[CodeElementMetadataItem] = List())

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
}
