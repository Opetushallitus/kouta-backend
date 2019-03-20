package fi.oph.kouta.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
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

  def genericKoutaFormats: Formats = DefaultFormats
    .addKeySerializers(Seq(kieliKeySerializer)) ++
    Seq(
      julkaisuTilaSerializer,
      koulutusTyyppiSerializer,
      hakulomaketyyppiSerializer,
      localDateTimeSerializer,
      kieliSerializer,
      uuidSerializer,
      liitteenToimitustapaSerializer,
      hakuOidSerializer,
      hakuKohdeOidSerializer,
      koulutusOidSerializer,
      toteutusOidSerializer,
      organisaatioOidSerializer,
      userOidSerializer,
      oidSerializer)

  def koutaJsonFormats: Formats = genericKoutaFormats ++ Seq(
    koulutusMetadataSerializer,
    toteutusMetadataSerializer,
    valintatapaSisaltoSerializer)

  private def kieliKeySerializer = new CustomKeySerializer[Kieli](_ => ({
    case s: String => Kieli.withName(s)
  }, {
    case k: Kieli => k.toString
  }))

  private def julkaisuTilaSerializer = new CustomSerializer[Julkaisutila](_ => ({
      case JString(s) => Julkaisutila.withName(s)
    }, {
      case j: Julkaisutila => JString(j.toString)
  }))

  private def koulutusTyyppiSerializer = new CustomSerializer[Koulutustyyppi](_ => ({
      case JString(s) => Koulutustyyppi.withName(s)
    }, {
      case j: Koulutustyyppi => JString(j.toString)
  }))

  private def hakulomaketyyppiSerializer = new CustomSerializer[Hakulomaketyyppi](_ => ({
      case JString(s) => Hakulomaketyyppi.withName(s)
    }, {
      case j: Hakulomaketyyppi => JString(j.toString)
  }))

  private def localDateTimeSerializer = new CustomSerializer[LocalDateTime](_ => ({
      case JString(i) => LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
    }, {
      case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
  }))

  private def kieliSerializer = new CustomSerializer[Kieli](_ => ({
      case JString(s) => Kieli.withName(s)
    }, {
      case k: Kieli => JString(k.toString)
  }))

  private def uuidSerializer = new CustomSerializer[UUID](_ => ({
      case JString(s) => UUID.fromString(s)
    }, {
      case uuid: UUID => JString(uuid.toString)
  }))

  private def liitteenToimitustapaSerializer = new CustomSerializer[LiitteenToimitustapa](_ => ({
      case JString(s) => LiitteenToimitustapa.withName(s)
    }, {
      case j: LiitteenToimitustapa => JString(j.toString)
  }))

  private def hakuOidSerializer = new CustomSerializer[HakuOid](_ => ({
      case JString(s) => HakuOid(s)
    }, {
      case j: HakuOid => JString(j.toString)
  }))

  private def hakuKohdeOidSerializer = new CustomSerializer[HakukohdeOid](_ => ({
      case JString(s) => HakukohdeOid(s)
    }, {
      case j: HakukohdeOid => JString(j.toString)
  }))

  private def koulutusOidSerializer = new CustomSerializer[KoulutusOid](_ => ({
      case JString(s) => KoulutusOid(s)
    }, {
      case j: KoulutusOid => JString(j.toString)
  }))

  private def toteutusOidSerializer = new CustomSerializer[ToteutusOid](_ => ({
      case JString(s) => ToteutusOid(s)
    }, {
      case j: ToteutusOid => JString(j.toString)
  }))

  private def organisaatioOidSerializer = new CustomSerializer[OrganisaatioOid](_ => ({
      case JString(s) => OrganisaatioOid(s)
    }, {
      case j: OrganisaatioOid => JString(j.toString)
  }))

  private def userOidSerializer = new CustomSerializer[UserOid](_ => ({
      case JString(s) => UserOid(s)
    }, {
      case j: UserOid => JString(j.toString)
  }))

  private def oidSerializer = new CustomSerializer[Oid](_ => ({
      case JString(s) => GenericOid(s)
    }, {
      case j: Oid => JString(j.toString)
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

  private def valintatapaSisaltoSerializer = new CustomSerializer[ValintatapaSisalto](implicit formats => ({
    case s: JObject =>
      Try(s \ "tyyppi").collect {
        case JString(tyyppi) if tyyppi == "teksti" =>
          Try(s \ "data").collect {
            case JString(teksti) => ValintatapaSisaltoTeksti(teksti)
          }.get
        case JString(tyyppi) if tyyppi == "taulukko" =>
          Try(s \ "data").collect {
            case taulukko: JObject => taulukko.extract[Taulukko]
          }.get
      }.get
  }, {
    case j: ValintatapaSisaltoTeksti =>
      JObject(List("tyyppi" -> JString("teksti"), "data" -> JString(j.teksti)))
    case j: Taulukko =>
      implicit def formats: Formats = genericKoutaFormats

      JObject(List("tyyppi" -> JString("taulukko"), "data" -> Extraction.decompose(j)))
  }))
}
