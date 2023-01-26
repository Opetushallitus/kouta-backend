package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class HakuKoodiClient(urlProperties: OphProperties) extends CachedKoodistoClient(urlProperties)
