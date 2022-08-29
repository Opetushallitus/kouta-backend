package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, queryFailed}
import fi.vm.sade.properties.OphProperties
import scalacache.caffeine.CaffeineCache

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class HakuKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val koodiUriCache   = CaffeineCache[Seq[KoodiUri]]

  def hakukohdeKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto(koodiUri.split("_").head, koodiUri)

  def pohjakoulutusVaatimusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("pohjakoulutusvaatimuskouta", koodiUri)

  def liiteTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("liitetyypitamm", koodiUri)

  def valintakoeTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("valintakokeentyyppi", koodiUri)

  def kausiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kausi", koodiUri)

  def oppiaineKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("painotettavatoppiaineetlukiossa", koodiUri)

  def kieliKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kieli", koodiUri)

  def postiosoitekoodiExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("posti", koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): ExternalQueryResult = {
    val queryRespone = getAndUpdateFromKoodiUriCache(koodisto, koodiUriCache)
    queryRespone.success match {
      case true =>
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, queryRespone.koodiUritInKoodisto))
      case _ => queryFailed
    }
  }
}
