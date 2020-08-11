package fi.oph.kouta.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain._
import org.json4s.JsonAST.JString
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats}

trait GenericKoutaJsonFormats extends GenericKoutaFormats {
  implicit def jsonFormats: Formats = genericKoutaFormats

  def toJson(data: AnyRef): String = write(data)
}

trait GenericKoutaFormats {

  val ISO_LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  def genericKoutaFormats: Formats = DefaultFormats.strict
    .addKeySerializers(Seq(kieliKeySerializer)) ++
    Seq(
      localDateTimeSerializer,
      stringSerializer(Julkaisutila.withName),
      stringSerializer(Koulutustyyppi.withName),
      stringSerializer(Hakulomaketyyppi.withName),
      stringSerializer(Kieli.withName),
      stringSerializer(UUID.fromString),
      stringSerializer(LiitteenToimitustapa.withName),
      stringSerializer(HakuOid),
      stringSerializer(HakukohdeOid),
      stringSerializer(KoulutusOid),
      stringSerializer(ToteutusOid),
      stringSerializer(OrganisaatioOid),
      stringSerializer(UserOid),
      stringSerializer(GenericOid),
    )

  private def localDateTimeSerializer = new CustomSerializer[LocalDateTime](_ => ( {
    case JString(i) => LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
  }, {
    case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
  }))

  private def kieliKeySerializer = new CustomKeySerializer[Kieli](_ => ( {
    case s: String => Kieli.withName(s)
  }, {
    case k: Kieli => k.toString
  }))

  private def stringSerializer[A: Manifest](construct: String => A) = new CustomSerializer[A](_ => ( {
    case JString(s) => construct(s)
  }, {
    case a: A => JString(a.toString)
  }))
}
