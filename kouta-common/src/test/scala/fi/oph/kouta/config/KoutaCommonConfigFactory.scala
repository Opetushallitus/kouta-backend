package fi.oph.kouta.config

import com.typesafe.config.{Config => TypesafeConfig}
import fi.oph.kouta.util.{KoutaBaseConfig, KoutaConfigFactory}
import fi.vm.sade.properties.OphProperties

case class KoutaAuthorizationConfiguration(config: TypesafeConfig, urlProperties: OphProperties)
    extends KoutaBaseConfig(config, urlProperties)

object KoutaCommonConfigFactory extends KoutaConfigFactory[KoutaAuthorizationConfiguration]("kouta-common") {
  def createConfigCaseClass(
      config: TypesafeConfig,
      urlProperties: OphProperties
  ): KoutaAuthorizationConfiguration = KoutaAuthorizationConfiguration(config, urlProperties)
}
