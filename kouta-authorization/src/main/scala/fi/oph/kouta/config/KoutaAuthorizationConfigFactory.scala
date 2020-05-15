package fi.oph.kouta.config

import java.io.File

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

  val urlProperties: OphProperties = profile match {
    case CONFIG_PROFILE_DEFAULT  => loadOphConfiguration()
    case CONFIG_PROFILE_TEMPLATE => loadTemplatedConfiguration()
    case _ =>
      throw new IllegalArgumentException(
        s"Unknown profile '${profile}'! Cannot load oph-properties! Use either " +
          s"'${CONFIG_PROFILE_DEFAULT}' or '${CONFIG_PROFILE_TEMPLATE}' profiles."
      )
  }

  private def loadOphConfiguration(): OphProperties = {
    val configFilePath = System.getProperty("user.home") + "/oph-configuration/kouta-backend.properties"
    logger.info(s"Reading properties from '$configFilePath'")
    new OphProperties(configFilePath)
  }

  private case class KoutaAuthorizationConfiguration(config: TypesafeConfig, urlProperties: OphProperties)
      extends ApplicationSettings(config)

  private def loadTemplatedConfiguration(): OphProperties = {
    val templateFilePath = Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE)).getOrElse(
      throw new IllegalArgumentException(
        s"Using 'template' profile but '${SYSTEM_PROPERTY_NAME_TEMPLATE}' " +
          "system property is missing. Cannot create oph-properties!"
      )
    )

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
