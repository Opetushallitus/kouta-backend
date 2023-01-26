package fi.oph.kouta.client

import fi.oph.kouta.client.HakukoodiConstants.{hakukohdeKoodistoAmmErityisopetus, hakukohdeKoodistoPoJalkYhteishaku}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.PainotetutArvoSanatLukioKaikki
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, itemFound}
import fi.vm.sade.properties.OphProperties

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

package object HakukoodiConstants {
  val hakukohdeKoodistoAmmErityisopetus = "hakukohteeterammatillinenerityisopetus"
  val hakukohdeKoodistoPoJalkYhteishaku = "hakukohteetperusopetuksenjalkeinenyhteishaku"
}

class HakuKoodiClient(urlProperties: OphProperties) extends CachedKoodistoClient(urlProperties) {

  def liiteTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("liitetyypitamm", koodiUri)

  def valintakoeTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("valintakokeentyyppi", koodiUri)

  def kausiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kausi", koodiUri)

  def oppiaineKoodiUriExists(koodiUri: String): ExternalQueryResult = {
    if (PainotetutArvoSanatLukioKaikki.koodiUrit contains koodiUri) {
      itemFound
    } else {
      koodiUriExistsInKoodisto("painotettavatoppiaineetlukiossa", koodiUri)
    }
  }
  def kieliKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kieli", koodiUri)

  def postiosoitekoodiExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("posti", koodiUri)

  def hakutapaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("hakutapa", koodiUri)

  def haunkohdejoukkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("haunkohdejoukko", koodiUri)

  def haunkohdejoukonTarkenneKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("haunkohdejoukontarkenne", koodiUri)

  def valintatapaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("valintatapajono", koodiUri)

  def tietoaOpiskelustaOtsikkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("organisaationkuvaustiedot", koodiUri)

}
