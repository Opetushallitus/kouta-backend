package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{En, Fi, Kielistetty, Sv}
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt

object KoodistoKaannosClient extends KoodistoKaannosClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoodistoKaannosClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val koodistoCache: Cache[String, Map[String, Kielistetty]] = Scaffeine()
    .expireAfterWrite(5.minutes)
    .build()

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

  private def getKoodistoKaannokset(koodisto: String): Map[String, Kielistetty] = {
    get(urlProperties.url("koodisto-service.koodisto-koodit", koodisto), followRedirects = true) { response =>
      parseKoodit(parse(response).extract[List[CodeElement]])
    }
  }

  def getKoodistoKaannoksetFromCache(koodisto: String): Map[String, Kielistetty] = {
    koodistoCache.get(koodisto, koodisto => getKoodistoKaannokset(koodisto))
  }
}
