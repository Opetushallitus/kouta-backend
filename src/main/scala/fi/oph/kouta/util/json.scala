package fi.oph.kouta.util

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Koulutustyyppi, Opetusaika}
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
    }))

  def toJson(data:AnyRef) = write(data)
}