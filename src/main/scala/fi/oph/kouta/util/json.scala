package fi.oph.kouta.util

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain._
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats}

trait KoutaJsonFormats {

  val ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  implicit def jsonFormats: Formats = DefaultFormats +
    new CustomSerializer[Julkaisutila](formats => ( {
      case JString(s) => Julkaisutila.withName(s)
    }, {
      case j: Julkaisutila => JString(j.toString)
    })) +
    new CustomSerializer[Koulutustyyppi](formats => ( {
      case JString(s) => Koulutustyyppi.withName(s)
    }, {
      case j: Koulutustyyppi => JString(j.toString)
    })) +
    new CustomKeySerializer[Kieli](formats => ( {
      case s: String => Kieli.withName(s)
    }, {
      case k: Kieli => k.toString
    })) +
    new CustomSerializer[Hakulomaketyyppi](formats => ({
      case JString(s) => Hakulomaketyyppi.withName(s)
    }, {
      case j: Hakulomaketyyppi => JString(j.toString)
    })) +
    new CustomSerializer[LocalDateTime](formats => ({
      case JString(i) => LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
    }, {
      case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
    })) +
    new CustomSerializer[Kieli](formats => ({
      case JString(s) => Kieli.withName(s)
    }, {
      case k: Kieli => JString(k.toString)
    })) +
    new CustomSerializer[UUID](formats => ({
      case JString(s) => UUID.fromString(s)
    }, {
      case uuid: UUID => JString(uuid.toString)
    })) +
    new CustomSerializer[LiitteenToimitustapa](formats => ({
      case JString(s) => LiitteenToimitustapa.withName(s)
    }, {
      case j: LiitteenToimitustapa => JString(j.toString)
    }))

  def toJson(data:AnyRef) = write(data)
}