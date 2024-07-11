package fi.oph.kouta.auditlog

import com.google.gson.{JsonElement, JsonParser}
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.write

trait GsonSupport extends KoutaJsonFormats {

  def toGson[T <: AnyRef](entity: T): JsonElement = {
    val json = write[T](entity)

    JsonParser.parseString(json)
  }
}
