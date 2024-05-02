package fi.oph.kouta.util

import fi.oph.kouta.domain._
import fi.oph.kouta.security.{ExternalSession, Session}
import fi.oph.kouta.util.MiscUtils.{toKieli, toKieliKoodiUri, withoutKoodiVersion}
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
    sessionSerializer,
    organisaationYhteystietoSerializer
  )

  private def koulutusMetadataSerializer = new CustomSerializer[KoulutusMetadata](_ =>
    (
      { case s: JObject =>
        implicit def formats: Formats = genericKoutaFormats

        Try(s \ "tyyppi").toOption.collect { case JString(tyyppi) =>
          Koulutustyyppi.withName(tyyppi)
        }.getOrElse(Amm) match {
          case Yo                            => s.extract[YliopistoKoulutusMetadata]
          case Amm                           => s.extract[AmmatillinenKoulutusMetadata]
          case AmmTutkinnonOsa               => s.extract[AmmatillinenTutkinnonOsaKoulutusMetadata]
          case AmmOsaamisala                 => s.extract[AmmatillinenOsaamisalaKoulutusMetadata]
          case AmmMuu                        => s.extract[AmmatillinenMuuKoulutusMetadata]
          case Amk                           => s.extract[AmmattikorkeakouluKoulutusMetadata]
          case KkOpintojakso                 => s.extract[KkOpintojaksoKoulutusMetadata]
          case KkOpintokokonaisuus           => s.extract[KkOpintokokonaisuusKoulutusMetadata]
          case Lk                            => s.extract[LukioKoulutusMetadata]
          case Tuva                          => s.extract[TuvaKoulutusMetadata]
          case Telma                         => s.extract[TelmaKoulutusMetadata]
          case VapaaSivistystyoOpistovuosi   => s.extract[VapaaSivistystyoOpistovuosiKoulutusMetadata]
          case VapaaSivistystyoMuu           => s.extract[VapaaSivistystyoMuuKoulutusMetadata]
          case VapaaSivistystyoOsaamismerkki => s.extract[VapaaSivistystyoOsaamismerkkiKoulutusMetadata]
          case AmmOpeErityisopeJaOpo         => s.extract[AmmOpeErityisopeJaOpoKoulutusMetadata]
          case OpePedagOpinnot               => s.extract[OpePedagOpinnotKoulutusMetadata]
          case AikuistenPerusopetus          => s.extract[AikuistenPerusopetusKoulutusMetadata]
          case Erikoislaakari                => s.extract[ErikoislaakariKoulutusMetadata]
          case Erikoistumiskoulutus          => s.extract[ErikoistumiskoulutusMetadata]
          case TaiteenPerusopetus            => s.extract[TaiteenPerusopetusKoulutusMetadata]
          case Muu                           => s.extract[MuuKoulutusMetadata]
          case kt                            => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
        }
      },
      { case j: KoulutusMetadata =>
        implicit def formats: Formats = genericKoutaFormats

        Extraction.decompose(j)
      }
    )
  )

  private def toteutusMetadataSerializer = new CustomSerializer[ToteutusMetadata](_ =>
    (
      { case s: JObject =>
        implicit def formats: Formats = genericKoutaFormats

        Try(s \ "tyyppi").toOption.collect { case JString(tyyppi) =>
          Koulutustyyppi.withName(tyyppi)
        }.getOrElse(Amm) match {
          case Yo                          => s.extract[YliopistoToteutusMetadata]
          case Amm                         => s.extract[AmmatillinenToteutusMetadata]
          case AmmTutkinnonOsa             => s.extract[AmmatillinenTutkinnonOsaToteutusMetadata]
          case AmmOsaamisala               => s.extract[AmmatillinenOsaamisalaToteutusMetadata]
          case AmmMuu                      => s.extract[AmmatillinenMuuToteutusMetadata]
          case Amk                         => s.extract[AmmattikorkeakouluToteutusMetadata]
          case KkOpintojakso               => s.extract[KkOpintojaksoToteutusMetadata]
          case KkOpintokokonaisuus         => s.extract[KkOpintokokonaisuusToteutusMetadata]
          case Lk                          => s.extract[LukioToteutusMetadata]
          case Tuva                        => s.extract[TuvaToteutusMetadata]
          case Telma                       => s.extract[TelmaToteutusMetadata]
          case VapaaSivistystyoOpistovuosi => s.extract[VapaaSivistystyoOpistovuosiToteutusMetadata]
          case VapaaSivistystyoMuu         => s.extract[VapaaSivistystyoMuuToteutusMetadata]
          case VapaaSivistystyoOsaamismerkki => s.extract[VapaaSivistystyoOsaamismerkkiToteutusMetadata]
          case AmmOpeErityisopeJaOpo       => s.extract[AmmOpeErityisopeJaOpoToteutusMetadata]
          case OpePedagOpinnot             => s.extract[OpePedagOpinnotToteutusMetadata]
          case AikuistenPerusopetus        => s.extract[AikuistenPerusopetusToteutusMetadata]
          case Erikoislaakari              => s.extract[ErikoislaakariToteutusMetadata]
          case Erikoistumiskoulutus        => s.extract[ErikoistumiskoulutusToteutusMetadata]
          case TaiteenPerusopetus          => s.extract[TaiteenPerusopetusToteutusMetadata]
          case Muu                         => s.extract[MuuToteutusMetadata]
          case kt                          => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
        }
      },
      { case j: ToteutusMetadata =>
        implicit def formats: Formats = genericKoutaFormats

        Extraction.decompose(j)
      }
    )
  )

  private def valintaperusteMetadataSerializer = new CustomSerializer[ValintaperusteMetadata](_ =>
    (
      { case s: JObject =>
        implicit def formats: Formats = genericKoutaFormats + sisaltoSerializer

        Try(s \ "tyyppi").toOption.collect { case JString(tyyppi) =>
          Koulutustyyppi.withName(tyyppi)
        }.getOrElse(Amm) match {
          case kt if Koulutustyyppi.values contains kt => s.extract[GenericValintaperusteMetadata]
          case kt                                      => throw new UnsupportedOperationException(s"Unsupported koulutustyyppi $kt")
        }
      },
      { case j: ValintaperusteMetadata =>
        implicit def formats: Formats = genericKoutaFormats + sisaltoSerializer

        Extraction.decompose(j)
      }
    )
  )

  private def sisaltoSerializer = new CustomSerializer[Sisalto](implicit formats =>
    (
      { case s: JObject =>
        Try(s \ "tyyppi").collect {
          case JString(tyyppi) if tyyppi == "teksti" =>
            Try(s \ "data").collect { case teksti: JObject =>
              SisaltoTeksti(teksti.extract[Kielistetty])
            }.get
          case JString(tyyppi) if tyyppi == "taulukko" =>
            Try(s \ "data").collect { case taulukko: JObject =>
              taulukko.extract[Taulukko]
            }.get
        }.get
      },
      {
        case j: SisaltoTeksti =>
          implicit def formats: Formats = genericKoutaFormats

          JObject(List("tyyppi" -> JString("teksti"), "data" -> Extraction.decompose(j.teksti)))
        case j: Taulukko =>
          implicit def formats: Formats = genericKoutaFormats

          JObject(List("tyyppi" -> JString("taulukko"), "data" -> Extraction.decompose(j)))
      }
    )
  )

  private def sessionSerializer = new CustomSerializer[Session](_ =>
    (
      { case s: JObject =>
        implicit def formats: Formats = genericKoutaFormats

        s.extract[ExternalSession]
      },
      { case j: ExternalSession =>
        implicit def formats: Formats = genericKoutaFormats

        Extraction.decompose(j)
      }
    )
  )

  def toPostinumeroKoodiuri(jsonObject: JObject): Option[String] = {
    Try(jsonObject \ "postinumeroUri").collect { case JString(postinumerokoodiuri) =>
      postinumerokoodiuri
    }.toOption
  }

  private def organisaationYhteystietoSerializer = new CustomSerializer[OrganisaationYhteystieto](_ =>
    (
      { case s: JObject =>
        implicit def formats: Formats = genericKoutaFormats

        s \ "kieli" match {
          case JString(kieliKoodiUri) =>
            val kieli = toKieli(withoutKoodiVersion(kieliKoodiUri)).get

            s \ "osoiteTyyppi" match {
              case JString("kaynti") =>
                val postinumerokoodiuri = toPostinumeroKoodiuri(s)

                OrgOsoite(
                  osoiteTyyppi = "kaynti",
                  kieli = kieli,
                  osoite = (s \ "osoite").extract[String],
                  postinumeroUri = postinumerokoodiuri
                )

              case JString("posti") =>
                val postinumerokoodiuri = toPostinumeroKoodiuri(s)

                OrgOsoite(
                  osoiteTyyppi = "posti",
                  kieli = kieli,
                  osoite = (s \ "osoite").extract[String],
                  postinumeroUri = postinumerokoodiuri
                )

              case _ =>
                s \ "email" match {
                  case JString(email) =>
                    Email(
                      kieli = kieli,
                      email = email
                    )
                  case _ =>
                    s \ "tyyppi" match {
                      case JString("puhelin") =>
                        Puhelin(
                          kieli = kieli,
                          numero = (s \ "numero").extract[String]
                        )
                      case _ =>
                        s \ "www" match {
                          case JString(www) =>
                            Www(
                              kieli = kieli,
                              www = www
                            )
                          case _ => null
                        }
                    }
                }
            }
          case _ => null
        }
      },
      {
        case o: OrgOsoite =>
          implicit def formats: Formats = genericKoutaFormats

          val kieli = toKieliKoodiUri(o.kieli)
          JObject(List(
            "osoiteTyyppi" -> Extraction.decompose(o.osoiteTyyppi),
            "kieli" -> JString(kieli),
            "osoite" -> Extraction.decompose(o.osoite),
            "postinumeroUri" -> Extraction.decompose(o.postinumeroUri))
          )

        case p: Puhelin =>
          implicit def formats: Formats = genericKoutaFormats

          val kieli = toKieliKoodiUri(p.kieli)
          JObject(List(
            "tyyppi" -> JString("puhelin"),
            "numero" -> Extraction.decompose(p.numero),
            "kieli" -> JString(kieli))
          )

        case e: Email =>
          implicit def formats: Formats = genericKoutaFormats

          val kieli = toKieliKoodiUri(e.kieli)
          JObject(List(
            "kieli" -> JString(kieli),
            "email" -> Extraction.decompose(e.email))
          )
      }
    )
  )
}
