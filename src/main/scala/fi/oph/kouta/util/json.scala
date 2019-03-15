package fi.oph.kouta.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats, Extraction}

import scala.util.Try

trait KoutaJsonFormats {
  val ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  def genericFormats: Formats = DefaultFormats +
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
    })) +
    new CustomSerializer[HakuOid](formats => ({
      case JString(s) => HakuOid(s)
    }, {
      case j: HakuOid => JString(j.toString)
    })) +
    new CustomSerializer[HakukohdeOid](formats => ({
      case JString(s) => HakukohdeOid(s)
    }, {
      case j: HakukohdeOid => JString(j.toString)
    })) +
    new CustomSerializer[KoulutusOid](formats => ({
      case JString(s) => KoulutusOid(s)
    }, {
      case j: KoulutusOid => JString(j.toString)
    })) +
    new CustomSerializer[ToteutusOid](formats => ({
      case JString(s) => ToteutusOid(s)
    }, {
      case j: ToteutusOid => JString(j.toString)
    })) +
    new CustomSerializer[OrganisaatioOid](formats => ({
      case JString(s) => OrganisaatioOid(s)
    }, {
      case j: OrganisaatioOid => JString(j.toString)
    })) +
    new CustomSerializer[UserOid](formats => ({
      case JString(s) => UserOid(s)
    }, {
      case j: UserOid => JString(j.toString)
    })) +
    new CustomSerializer[Oid](formats => ({
      case JString(s) => GenericOid(s)
    }, {
      case j: Oid => JString(j.toString)
    }))

  implicit def jsonFormats: Formats = genericFormats +
    new CustomSerializer[KoulutusMetadata](formats => ({
      case s: JObject => {
        implicit def formats = DefaultFormats

        Try((s \ "tyyppi")).toOption.map {
          case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
          case _ => Some(Amm)
        } match {
          case Some(Yo) => s.extract[YliopistoKoulutusMetadata]
          case Some(Amm) => s.extract[AmmatillinenKoulutusMetadata]
          case Some(Amk) => s.extract[AmmattikorkeakouluKoulutusMetadata]
          case _ => s.extract[KoulutusMetadata]
        }
      }
    }, {
      case j: KoulutusMetadata => {
        implicit def formats = genericFormats

        Extraction.decompose(j)
      }
    }))

  def toJson(data:AnyRef) = write(data)
}

