package fi.oph.kouta.template

import java.io.File
import java.net.URL
import java.util.HashMap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.MapType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.fusesource.scalate.{TemplateEngine, TemplateSource}

import scala.jdk.CollectionConverters._

object TemplateProcessor {
  val engine = new TemplateEngine
  // scalate 1.7.0 has bug in cache handling. FileTemplateSource uri is used by cache stale checker to check for file changes, should use original file argument
  engine.allowReload = false

  def processMustacheWithYamlAttributes(templatePath: String, yamlFile: String): String = {
    processMustacheWithYamlAttributes(
      new File(templatePath).toURI.toURL,
      new File(yamlFile).toURI.toURL
    )
  }

  def processMustacheWithYamlAttributes(template: URL, vars: URL): String = {
    val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
    val mapType: MapType = mapper.getTypeFactory.constructMapType(classOf[HashMap[String, String]], classOf[String], classOf[String])
    val rawValue = mapper.readValue(vars, mapType).asInstanceOf[HashMap[String, String]]
    val attributes: Map[String, Any] = rawValue.asScala.toMap.asInstanceOf[Map[String, Any]]
    engine.layout(TemplateSource.fromURL(template).templateType("mustache"), attributes)
  }


  def processTemplate(templatePath: String, attributes: Map[String, Any]): String = {
    engine.layout(engine.source(templatePath), attributes)
  }
}
