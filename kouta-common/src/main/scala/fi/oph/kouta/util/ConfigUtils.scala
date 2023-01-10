package fi.oph.kouta.util

import com.typesafe.config.{Config => TypesafeConfig}
import fi.oph.kouta.util.ConfigUtils.getModuleDir
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.config.{
  ApplicationSettings,
  ApplicationSettingsLoader,
  ApplicationSettingsParser,
  ConfigTemplateProcessor
}
import fi.vm.sade.utils.slf4j.Logging

import java.io.File
import java.net.URL
import java.nio.file.{Files, Paths}

object ConfigUtils {
  def getModuleDir(moduleName: String): String = {
    val userDir = System.getProperty("user.dir")
    if (Files.exists(Paths.get(userDir, "src"))) {
      userDir
    } else {
      Paths.get(userDir, moduleName).toString
    }
  }
}

class KoutaBaseConfig(config: TypesafeConfig, urlProperties: OphProperties) extends ApplicationSettings(config)

abstract class KoutaConfigFactory[TConfig <: KoutaBaseConfig](moduleName: String) extends Logging {
  def createConfigCaseClass(config: TypesafeConfig, urlProperties: OphProperties): TConfig

  val SYSTEM_PROPERTY_NAME_CONFIG_PROFILE = s"${moduleName}.config-profile"
  val SYSTEM_PROPERTY_NAME_TEMPLATE       = s"${moduleName}.template-file"

  val CONFIG_PROFILE_DEFAULT  = "default"
  val CONFIG_PROFILE_TEMPLATE = "template"

  val TEST_TEMPLATE_FILE_PATH: String = "src/test/resources/test-vars.yml"
  val DEV_TEMPLATE_FILE_PATH: String  = "src/test/resources/dev-vars.yml"

  def isTesting = profile == CONFIG_PROFILE_TEMPLATE && DEFAULT_TEMPLATE_FILE_PATH == TEST_TEMPLATE_FILE_PATH

  def DEFAULT_TEMPLATE_FILE_PATH: String = Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE)) match {
    case Some(templateFilePath) => templateFilePath
    case _                      => TEST_TEMPLATE_FILE_PATH
  }

  def setupWithDefaultTemplateFile(): TConfig = {
    logger.debug(s"Using default test template ${DEFAULT_TEMPLATE_FILE_PATH}")
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, DEFAULT_TEMPLATE_FILE_PATH)
    configuration
  }

  def setupWithDevTemplate(): TConfig = {
    logger.debug(s"Using dev template ${DEV_TEMPLATE_FILE_PATH}")
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, DEV_TEMPLATE_FILE_PATH)
    configuration
  }

  def profile: String = System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_DEFAULT)

  lazy val configuration = {
    profile match {
      case CONFIG_PROFILE_DEFAULT  => loadOphConfiguration()
      case CONFIG_PROFILE_TEMPLATE => loadTemplatedConfiguration()
      case _ =>
        throw new IllegalArgumentException(
          s"Unknown profile '$profile'! Cannot load oph-properties! Use either " +
            s"'$CONFIG_PROFILE_DEFAULT' or '$CONFIG_PROFILE_TEMPLATE' profiles."
        )
    }
  }

  def init(): Unit = {}

  private def loadOphConfiguration(): TConfig = {
    val configFilePath = System.getProperty("user.home") + s"/oph-configuration/${moduleName}.properties"

    implicit val applicationSettingsParser: ApplicationSettingsParser[TConfig] = (c: TypesafeConfig) =>
      createConfigCaseClass(c, new OphProperties(configFilePath))

    logger.info(s"Reading properties from '$configFilePath'")
    ApplicationSettingsLoader.loadSettings(configFilePath)
  }: TConfig

  private def loadTemplatedConfiguration(): TConfig = {
    val templateFilePath = Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE)).getOrElse(
      throw new IllegalArgumentException(
        s"Using 'template' profile but '$SYSTEM_PROPERTY_NAME_TEMPLATE' " +
          "system property is missing. Cannot create oph-properties!"
      )
    )
    val prefix = getModuleDir(moduleName)

    implicit val applicationSettingsParser: ApplicationSettingsParser[TConfig] = (c: TypesafeConfig) =>
      createConfigCaseClass(
        c,
        new OphProperties(s"${prefix}/src/test/resources/${moduleName}.properties") {
          addDefault("host.virkailija", c.getString("host.virkailija"))
        }
      )

    val absTemplateFilePath = s"$prefix/$templateFilePath"
    logger.info(s"Reading template variables from '$absTemplateFilePath'. ")
    val templateURL: URL = new File(
      s"${prefix}/src/main/resources/oph-configuration/${moduleName}.properties.template"
    ).toURI.toURL
    val attributesURL = new File(absTemplateFilePath).toURI.toURL
    ConfigTemplateProcessor.createSettings(templateURL, attributesURL)
  }
}
