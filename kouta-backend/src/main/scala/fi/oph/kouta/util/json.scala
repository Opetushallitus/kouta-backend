package fi.oph.kouta.util

import fi.oph.kouta.domain._
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.{CustomSerializer, Extraction, Formats}

import scala.util.Try

trait KoutaJsonFormats extends GenericKoutaJsonFormats with DefaultKoutaJsonFormats {
  override implicit def jsonFormats: Formats = koutaJsonFormats
}

sealed trait DefaultKoutaJsonFormats extends GenericKoutaFormats {

  def koutaJsonFormats: Formats = genericKoutaFormats ++ Seq(
    koulutusMetadataSerializer,
    toteutusMetadataSerializer,
    valintatapaSisaltoSerializer,
    valintaperusteMetadataSerializer)

  private def koulutusMetadataSerializer = new CustomSerializer[KoulutusMetadata](_ => ({
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats

      Try(s \ "tyyppi").toOption.collect {
          case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoKoulutusMetadata]
        case Amm =>
          Try(s \ "tyypinTarkenne").toOption.collect {
            case JString(tyyppi) => KoulutustyypinTarkenne.withName(tyyppi)
          } match {
            case Some(TutkinnonOsa) => s.extract[TutkinnonOsaMetadata]
            case _ => s.extract[AmmatillinenKoulutusMetadata]
          }
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
