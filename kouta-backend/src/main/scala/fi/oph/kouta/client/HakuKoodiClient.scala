package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import scalacache.caffeine.CaffeineCache

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class HakuKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val koodiUriCache   = CaffeineCache[Seq[KoodiUri]]

  def hakukohdeKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto(koodiUri.split("_").head, koodiUri)

  def pohjakoulutusVaatimusKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("pohjakoulutusvaatimuskouta", koodiUri)

  def liiteTyyppiKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("liitetyypitamm", koodiUri)

  def valintakoeTyyppiKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("valintakokeentyyppi", koodiUri)

  def kausiKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("kausi", koodiUri)

  def oppiaineKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("painotettavatoppiaineetlukiossa", koodiUri)

  def kieliKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("kieli", koodiUri)

  def postiosoitekoodiExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("posti", koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): Boolean = {
    val koodiUritInKoodisto = getAndUpdateFromKoodiUriCache(koodisto, koodiUriCache)
    koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritInKoodisto)
  }
}
