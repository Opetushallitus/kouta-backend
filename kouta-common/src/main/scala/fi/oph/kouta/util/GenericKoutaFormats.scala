package fi.oph.kouta.util

import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import org.json4s.JsonAST.JString
import org.json4s.ext.JavaTypesSerializers
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats, JNull, MappingException, Serialization}

import scala.util.control.NonFatal

trait GenericKoutaJsonFormats extends GenericKoutaFormats {
  implicit def jsonFormats: Formats = genericKoutaFormats

  def toJson(data: AnyRef): String = write(data)
}

trait GenericKoutaFormats {

  val ISO_LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
  val ISO_MODIFIED_FORMATTER: DateTimeFormatter        = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  def genericKoutaFormats: Formats = DefaultFormats.strict
    .addKeySerializers(Seq(kieliKeySerializer)) ++ JavaTypesSerializers.all ++
    Seq(
      LocalDateTimeSerializer,
      ModifiedSerializer,
      stringSerializer(Julkaisutila.withName),
      stringSerializer(Koulutustyyppi.withName),
      stringSerializer(Hakulomaketyyppi.withName),
      stringSerializer(Hakutermi.withName),
      stringSerializer(Apurahayksikko.withName),
      stringSerializer(Maksullisuustyyppi.withName),
      stringSerializer(Alkamiskausityyppi.withName),
      stringSerializer(Kieli.withName),
      stringSerializer(LiitteenToimitustapa.withName),
      stringSerializer(HakuOid),
      stringSerializer(HakukohdeOid),
      stringSerializer(HakukohderyhmaOid),
      stringSerializer(KoulutusOid),
      stringSerializer(ToteutusOid),
      stringSerializer(OrganisaatioOid),
      stringSerializer(UserOid),
      stringSerializer(GenericOid),
      stringSerializer(InetAddress.getByName, (ip: InetAddress) => ip.getHostAddress),
    )

  case object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](_ => ( {
    case JString(i) =>
      try {
        LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
      } catch {
        case NonFatal(e) =>
          throw MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }
    case JNull => null
  }, {
    case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
  }))

  case object ModifiedSerializer extends CustomSerializer[Modified](_ => ( {
    case JString(i) =>
      try {
        Modified(LocalDateTime.from(ISO_MODIFIED_FORMATTER.parse(i)))
      } catch {
        case NonFatal(e) =>
          throw MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }
    case JNull => null
  }, {
    case i: Modified => JString(ISO_MODIFIED_FORMATTER.format(i.value))
  }))

  private def kieliKeySerializer = new CustomKeySerializer[Kieli](_ => ( {
    case s: String => Kieli.withName(s)
  }, {
    case k: Kieli => k.toString
  }))

  private def stringSerializer[A>:Null: Manifest](construct: String => A): CustomSerializer[A] =
    stringSerializer(construct, (a: A) => a.toString)

  private def stringSerializer[A>:Null: Manifest](construct: String => A, deconstruct: A => String) =
    new CustomSerializer[A](_ => ( {
      case JString(s) => 
        try {
          construct(s)
        } catch {
          case NonFatal(e) =>
            throw MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
        }
      case JNull => null
    }, {
      case a: A => JString(deconstruct(a))
    }))
}
