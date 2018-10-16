package fi.oph.kouta.util

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

import fi.oph.kouta.domain._
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats}

trait KoutaJsonFormats {
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
    new CustomSerializer[Instant](formats => ({
      case JString(i) => Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(i))
    }, {
      case i: Instant => JString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Helsinki")).format(i))
    }))

  def toJson(data:AnyRef) = write(data)
}