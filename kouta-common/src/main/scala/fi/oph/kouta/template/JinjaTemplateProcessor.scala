package fi.oph.kouta.template

import java.net.URL
import java.util.HashMap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.MapType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.io.Resources
import com.hubspot.jinjava.Jinjava

import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._

object JinjaTemplateProcessor {
  val jinjava = new Jinjava()

  def processJinjaWithYamlAttributes(templateUrl: URL, vars: URL): String = {
    val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
    val mapType: MapType = mapper.getTypeFactory.constructMapType(classOf[HashMap[String, String]], classOf[String], classOf[String])
    val rawValue = mapper.readValue(vars, mapType).asInstanceOf[HashMap[String, String]]
    val attributes: Map[String, Any] = rawValue.asScala.toMap.asInstanceOf[Map[String, Any]]
    val template = Resources.toString(templateUrl, StandardCharsets.UTF_8)
    jinjava.render(template, attributes.asJava)
  }
}
