package fi.oph.kouta.util

import fi.oph.kouta.domain._
import fi.oph.kouta.security.{ExternalSession, Session}
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
    sisaltoSerializer,
    valintaperusteMetadataSerializer,
    sessionSerializer)

  private def koulutusMetadataSerializer = new CustomSerializer[KoulutusMetadata](_ => ({
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats

      Try(s \ "tyyppi").toOption.collect {
        case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoKoulutusMetadata]
        case Amm => s.extract[AmmatillinenKoulutusMetadata]
        case AmmTutkinnonOsa => s.extract[AmmatillinenTutkinnonOsaKoulutusMetadata]
        case AmmOsaamisala => s.extract[AmmatillinenOsaamisalaKoulutusMetadata]
        case Amk => s.extract[AmmattikorkeakouluKoulutusMetadata]
        case Lk => s.extract[LukioKoulutusMetadata]
        case Tuva => s.extract[TuvaKoulutusMetadata]
        case Telma => s.extract[TelmaKoulutusMetadata]
        case VapaaSivistystyoOpistovuosi => s.extract[VapaaSivistystyoOpistovuosiKoulutusMetadata]
        case VapaaSivistystyoMuu => s.extract[VapaaSivistystyoMuuKoulutusMetadata]
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
        case AmmTutkinnonOsa => s.extract[AmmatillinenTutkinnonOsaToteutusMetadata]
        case AmmOsaamisala => s.extract[AmmatillinenOsaamisalaToteutusMetadata]
        case Amk => s.extract[AmmattikorkeakouluToteutusMetadata]
        case Lk => s.extract[LukioToteutusMetadata]
        case Tuva => s.extract[TuvaToteutusMetadata]
        case Telma => s.extract[TelmaToteutusMetadata]
        case VapaaSivistystyoOpistovuosi => s.extract[VapaaSivistystyoOpistovuosiToteutusMetadata]
        case VapaaSivistystyoMuu => s.extract[VapaaSivistystyoMuuToteutusMetadata]
        case kt => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
      }
  }, {
    case j: ToteutusMetadata =>
      implicit def formats: Formats = genericKoutaFormats

      Extraction.decompose(j)
  }))

  private def valintaperusteMetadataSerializer = new CustomSerializer[ValintaperusteMetadata](_ => ( {
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats + sisaltoSerializer

      Try(s \ "tyyppi").toOption.collect {
        case JString(tyyppi) => Koulutustyyppi.withName(tyyppi)
      }.getOrElse(Amm) match {
        case Yo => s.extract[YliopistoValintaperusteMetadata]
        case Lk => s.extract[LukioValintaperusteMetadata]
        case Amm => s.extract[AmmatillinenValintaperusteMetadata]
        case Amk => s.extract[AmmattikorkeakouluValintaperusteMetadata]
        case AmmTutkinnonOsa => s.extract[AmmatillinenTutkinnonOsaValintaperusteMetadata]
        case AmmOsaamisala => s.extract[AmmatillinenOsaamisalaValintaperusteMetadata]
        case Tuva => s.extract[TutkintokoulutukseenValmentavaValintaperusteMetadata]
        case Telma => s.extract[TelmaValintaperusteMetadata]
        case Muu => s.extract[MuuValintaperusteMetadata]
        case kt => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
      }
  }, {
    case j: ValintaperusteMetadata =>
      implicit def formats: Formats = genericKoutaFormats + sisaltoSerializer

      Extraction.decompose(j)
  }))

  private def sisaltoSerializer = new CustomSerializer[Sisalto](implicit formats => ({
    case s: JObject =>
      Try(s \ "tyyppi").collect {
        case JString(tyyppi) if tyyppi == "teksti" =>
          Try(s \ "data").collect {
            case teksti: JObject => SisaltoTeksti(teksti.extract[Kielistetty])
          }.get
        case JString(tyyppi) if tyyppi == "taulukko" =>
          Try(s \ "data").collect {
            case taulukko: JObject => taulukko.extract[Taulukko]
          }.get
      }.get
  }, {
    case j: SisaltoTeksti =>
      implicit def formats: Formats = genericKoutaFormats

      JObject(List("tyyppi" -> JString("teksti"), "data" -> Extraction.decompose(j.teksti)))
    case j: Taulukko =>
      implicit def formats: Formats = genericKoutaFormats

      JObject(List("tyyppi" -> JString("taulukko"), "data" -> Extraction.decompose(j)))
  }))

  private def sessionSerializer = new CustomSerializer[Session](_ => ( {
    case s: JObject =>
      implicit def formats: Formats = genericKoutaFormats

      s.extract[ExternalSession]
  }, {
    case j: ExternalSession =>
      implicit def formats: Formats = genericKoutaFormats

      Extraction.decompose(j)
  }))
}
