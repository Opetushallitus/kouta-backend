package fi.oph.kouta.config

import com.typesafe.config.{Config => TypesafeConfig}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.config.{ApplicationSettings, ApplicationSettingsParser, ConfigTemplateProcessor}
import fi.vm.sade.utils.slf4j.Logging

object KoutaAuthorizationConfigFactory extends Logging {

  object Constants {
    val SYSTEM_PROPERTY_NAME_CONFIG_PROFILE = "kouta-backend.config-profile"
    val SYSTEM_PROPERTY_NAME_TEMPLATE       = "kouta-backend.template-file"

    val CONFIG_PROFILE_DEFAULT  = "default"
    val CONFIG_PROFILE_TEMPLATE = "template"
  }

  import Constants._

  val profile: String = System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_DEFAULT)
  logger.info(s"Using profile '${profile}'")

  private case class KoutaAuthorizationConfiguration(config: TypesafeConfig, urlProperties: OphProperties)
      extends ApplicationSettings(config)

  def loadTemplatedConfiguration(): OphProperties = {
    val templateFilePath = "src/test/resources/dev-vars.yml"

    implicit val applicationSettingsParser = new ApplicationSettingsParser[KoutaAuthorizationConfiguration] {
      override def parse(c: TypesafeConfig): KoutaAuthorizationConfiguration =
        KoutaAuthorizationConfiguration(c, new OphProperties("src/test/resources/kouta-backend.properties") {
          addDefault("host.virkailija", c.getString("host.virkailija"))
        })
    }

    logger.info(s"Reading template variables from '${templateFilePath}'")
    ConfigTemplateProcessor.createSettings("kouta-backend", templateFilePath).urlProperties
  }

}
