package fi.oph.kouta.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakukohdeOid, _}
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Extraction, Formats}

import scala.util.Try

trait KoutaJsonFormats extends DefaultKoutaJsonFormats {

  implicit def jsonFormats: Formats = koutaJsonFormats

  def toJson(data: AnyRef): String = write(data)
}

sealed trait DefaultKoutaJsonFormats {

  val ISO_LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  def koutaJsonFormats: Formats = genericKoutaFormats ++ Seq(
    koulutusMetadataSerializer,
    toteutusMetadataSerializer,
    valintatapaSisaltoSerializer,
    valintaperusteMetadataSerializer)

  private def genericKoutaFormats: Formats = DefaultFormats.strict
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

  private def kieliKeySerializer = new CustomKeySerializer[Kieli](_ => ( {
    case s: String => Kieli.withName(s)
  }, {
    case k: Kieli => k.toString
  }))

  private def localDateTimeSerializer = new CustomSerializer[LocalDateTime](_ => ({
    case JString(i) => LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
  }, {
    case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
  }))

  private def stringSerializer[A: Manifest](construct: String => A) = new CustomSerializer[A](_ => ({
    case JString(s) => construct(s)
  }, {
    case a: A => JString(a.toString)
  }))

  private def koulutusMetadataSerializer = new CustomSerializer[KoulutusMetadata](_ => ({
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats

      Try(s \ "tyyppi").toOption.collect {
          case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoKoulutusMetadata]
        case Amm => s.extract[AmmatillinenKoulutusMetadata]
        case Amk => s.extract[AmmattikorkeakouluKoulutusMetadata]
        case kt => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
      }
    }, {
    case j: KoulutusMetadata =>
      implicit def formats: Formats = genericKoutaFormats

        Extraction.decompose(j)
  }))

  private def toteutusMetadataSerializer = new CustomSerializer[ToteutusMetadata](_ => ({
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats

      Try(s \ "tyyppi").toOption.collect {
        case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoToteutusMetadata]
        case Amm => s.extract[AmmatillinenToteutusMetadata]
        case Amk => s.extract[AmmattikorkeakouluToteutusMetadata]
        case kt => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
      }
  }, {
    case j: ToteutusMetadata =>
      implicit def formats: Formats = genericKoutaFormats

      Extraction.decompose(j)
  }))

  private def valintaperusteMetadataSerializer = new CustomSerializer[ValintaperusteMetadata](_ => ( {
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats + valintatapaSisaltoSerializer

      Try(s \ "tyyppi").toOption.collect {
        case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoValintaperusteMetadata]
        case Amm => s.extract[AmmatillinenValintaperusteMetadata]
        case Amk => s.extract[AmmattikorkeakouluValintaperusteMetadata]
        case kt => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
      }
  }, {
    case j: ValintaperusteMetadata =>
      implicit def formats: Formats = genericKoutaFormats + valintatapaSisaltoSerializer

      Extraction.decompose(j)
  }))

  private def valintatapaSisaltoSerializer = new CustomSerializer[ValintatapaSisalto](implicit formats => ({
    case s: JObject =>
      Try(s \ "tyyppi").collect {
        case JString(tyyppi) if tyyppi == "teksti" =>
          Try(s \ "data").collect {
            case teksti: JObject => ValintatapaSisaltoTeksti(teksti.extract[Kielistetty])
          }.get
        case JString(tyyppi) if tyyppi == "taulukko" =>
          Try(s \ "data").collect {
            case taulukko: JObject => taulukko.extract[Taulukko]
          }.get
      }.get
  }, {
    case j: ValintatapaSisaltoTeksti =>
      implicit def formats: Formats = genericKoutaFormats

      JObject(List("tyyppi" -> JString("teksti"), "data" -> Extraction.decompose(j.teksti)))
    case j: Taulukko =>
      implicit def formats: Formats = genericKoutaFormats

      JObject(List("tyyppi" -> JString("taulukko"), "data" -> Extraction.decompose(j)))
  }))
}
