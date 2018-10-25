package fi.oph.kouta.util

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain._
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats}

trait KoutaJsonFormats {

  val ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Helsinki"))

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
    new CustomSerializer[Opetusaika](formats => ( {
      case JString(s) => Opetusaika.withName(s)
    }, {
      case j: Opetusaika => JString(j.toString)
    })) +
    new CustomSerializer[Hakutapa](formats => ( {
      case JString(s) => Hakutapa.withName(s)
    }, {
      case j: Hakutapa => JString(j.toString)
    })) +
    new CustomSerializer[Alkamiskausi](formats => ( {
      case JString(s) => Alkamiskausi.withName(s)
    }, {
      case j: Alkamiskausi => JString(j.toString)
    })) +
    new CustomSerializer[Hakulomaketyyppi](formats => ({
      case JString(s) => Hakulomaketyyppi.withName(s)
    }, {
      case j: Hakulomaketyyppi => JString(j.toString)
    })) +
    new CustomSerializer[Instant](formats => ({
      case JString(i) => Instant.from(ISO_OFFSET_DATE_TIME_FORMATTER.parse(i))
    }, {
      case i: Instant => JString(ISO_OFFSET_DATE_TIME_FORMATTER.format(i))
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
    }))+
    new CustomSerializer[ValintaperusteenKohde](formats => ({
      case JString(s) => ValintaperusteenKohde.withName(s)
    }, {
      case j: ValintaperusteenKohde => JString(j.toString)
    }))

  def toJson(data:AnyRef) = write(data)
}