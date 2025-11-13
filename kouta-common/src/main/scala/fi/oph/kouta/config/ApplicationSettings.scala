package fi.oph.kouta.config

import java.io.File
import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigValueFactory}
import fi.oph.kouta.logging.Logging

object ApplicationSettingsLoader extends Logging {
  def loadSettings[T <: ApplicationSettings](fileLocation: String)(implicit parser: ApplicationSettingsParser[T]): T = {
    val configFile = new File(fileLocation)
    if (configFile.exists()) {
      logger.info("Using configuration file " + configFile)
      parser.parse(ConfigFactory.load(ConfigFactory.parseFile(configFile)))
    } else {
      throw new RuntimeException("Configuration file not found: " + fileLocation)
    }
  }
}

abstract class ApplicationSettings(config: Config) {

  def toProperties = {
    import scala.jdk.CollectionConverters._

    val keys = config.entrySet().asScala.toList.map(_.getKey)
    keys.map { key =>
      (key, config.getString(key))
    }.toMap
  }

  def withOverride[T <: ApplicationSettings](keyValuePair : (String, String))(implicit parser: ApplicationSettingsParser[T]): T = {
    parser.parse(config.withValue(keyValuePair._1, ConfigValueFactory.fromAnyRef(keyValuePair._2)))
  }

  def withoutPath[T <: ApplicationSettings](path: String)(implicit  parser: ApplicationSettingsParser[T]): T = {
    parser.parse(config.withoutPath(path))
  }


  def getStringWithDefault(path: String, default: String) = {
    try {
      config.getString(path)
    } catch {
      case _ :ConfigException.Missing | _ :ConfigException.Null | _ : ConfigException.WrongType => default
    }
  }
}

trait ApplicationSettingsParser[T <: ApplicationSettings] {

  def parse(config: Config): T
}
