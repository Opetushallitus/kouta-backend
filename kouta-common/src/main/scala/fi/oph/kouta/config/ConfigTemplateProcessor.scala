package fi.oph.kouta.config

import com.typesafe.config.ConfigFactory
import fi.oph.kouta.template.JinjaTemplateProcessor

import java.io.{File, StringReader}
import java.net.URL
import java.util.Properties

object ConfigTemplateProcessor {
  def createSettings[T <: ApplicationSettings](projectName: String, attributesFile: String)(implicit applicationSettingsParser: ApplicationSettingsParser[T]): T = {
    val templateURL: URL = new File("src/main/resources/oph-configuration/" + projectName + ".properties.template").toURI.toURL
    val attributesURL = new File(attributesFile).toURI.toURL

    val templatedData = JinjaTemplateProcessor.processJinjaWithYamlAttributes(templateURL, attributesURL)
    parseTemplatedData(templatedData)
  }

  def createSettings[T <: ApplicationSettings](template: URL, attributes: URL)(implicit applicationSettingsParser: ApplicationSettingsParser[T]): T = {
    val templatedData: String = JinjaTemplateProcessor.processJinjaWithYamlAttributes(template, attributes)
    parseTemplatedData(templatedData)
  }

  def parseTemplatedData[T <: ApplicationSettings](templatedData: String)(implicit applicationSettingsParser: ApplicationSettingsParser[T]): T = {
    val properties = new Properties()
    properties.load(new StringReader(templatedData))
    applicationSettingsParser.parse(ConfigFactory.load(ConfigFactory.parseProperties(properties)))
  }
}
