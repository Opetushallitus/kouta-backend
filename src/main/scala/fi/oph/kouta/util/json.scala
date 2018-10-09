package fi.oph.kouta.util

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Koulutustyyppi}
import org.json4s.{CustomKeySerializer, DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer

trait KoutaJsonFormats {
  implicit def jsonFormats: Formats = DefaultFormats +
    new EnumNameSerializer(Julkaisutila) +
    new EnumNameSerializer(Koulutustyyppi) +
    new CustomKeySerializer[Kieli.Kieli](formats => ( {
      case s: String => Kieli.values.find(_.toString == s.toLowerCase).get
    }, {
      case k: Kieli.Kieli => k.toString
    }))
}
