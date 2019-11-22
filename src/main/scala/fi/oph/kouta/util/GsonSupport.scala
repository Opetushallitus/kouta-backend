package fi.oph.kouta.util

import com.google.gson.{JsonElement, JsonParser}
import org.json4s.jackson.Serialization.write

trait GsonSupport extends KoutaJsonFormats {

  private val jsonParser = new JsonParser

  def toGson[T <: AnyRef](entity: T): JsonElement = {
    val json = write[T](entity)

    jsonParser.parse(json)
  }
}
