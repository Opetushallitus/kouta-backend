package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

package object HakukoodiConstants {
  val hakukohdeKoodistoAmmErityisopetus = "hakukohteeterammatillinenerityisopetus"
  val hakukohdeKoodistoPoJalkYhteishaku = "hakukohteetperusopetuksenjalkeinenyhteishaku"
}

class HakuKoodiClient(urlProperties: OphProperties) extends CachedKoodistoClient(urlProperties)
