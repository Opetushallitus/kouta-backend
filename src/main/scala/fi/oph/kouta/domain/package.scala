package fi.oph.kouta

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.domain.oid._

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä
package object domain {
  type Kielistetty = Map[Kieli,String]

  trait EnumType {
    def name:String
    override def toString = name
  }

  trait Enum[T <: EnumType] {
    def name:String
    def values():List[T]
    def withName(n:String):T = values().find(_.name.equals(n))
      .getOrElse(throw new IllegalArgumentException(s"Unknown ${name} '${n}'"))
  }

  sealed trait Julkaisutila extends EnumType

  object Julkaisutila extends Enum[Julkaisutila] {
    override def name: String = "julkaisutila"
    def values() = List(Tallennettu, Julkaistu, Arkistoitu)
  }
  case object Tallennettu extends Julkaisutila { val name = "tallennettu" }
  case object Julkaistu extends Julkaisutila { val name = "julkaistu" }
  case object Arkistoitu extends Julkaisutila { val name = "arkistoitu" }

  sealed trait Kieli extends EnumType

  object Kieli extends Enum[Kieli] {
    override def name: String = "kieli"
    def values = List(Fi, Sv, En)
  }
  case object Fi extends Kieli { val name = "fi" }
  case object Sv extends Kieli { val name = "sv" }
  case object En extends Kieli { val name = "en" }

  sealed trait Koulutustyyppi extends EnumType

  object Koulutustyyppi extends Enum[Koulutustyyppi] {
    override def name: String = "koulutustyyppi"
    def values() = List(Amm, Kk, Lk, Muu)
  }
  case object Amm extends Koulutustyyppi { val name = "amm" }
  case object Kk extends Koulutustyyppi { val name = "kk" }
  case object Lk extends Koulutustyyppi { val name = "lk" }
  case object Muu extends Koulutustyyppi { val name = "muu" }

  sealed trait Hakulomaketyyppi extends EnumType
  object Hakulomaketyyppi extends Enum[Hakulomaketyyppi] {
    override def name: String = "hakulomaketyyppi"
    def values() = List(Ataru, HakuApp, MuuHakulomake, EiSähköistä)
  }
  case object Ataru extends Hakulomaketyyppi { val name = "ataru"}
  case object HakuApp extends Hakulomaketyyppi { val name = "haku-app"}
  case object MuuHakulomake extends Hakulomaketyyppi { val name = "muu"}
  case object EiSähköistä extends Hakulomaketyyppi { val name = "ei sähköistä"}

  sealed trait LiitteenToimitustapa extends EnumType
  object LiitteenToimitustapa extends Enum[LiitteenToimitustapa] {
    override def name: String = "liitteen toimitusosoite"
    def values() = List(Lomake, Hakijapalvelu, MuuOsoite)
  }
  case object Lomake extends LiitteenToimitustapa { val name = "lomake"}
  case object Hakijapalvelu extends LiitteenToimitustapa { val name = "hakijapalvelu"}
  case object MuuOsoite extends LiitteenToimitustapa { val name = "osoite"}

  case class Yhteystieto(nimi:Kielistetty = Map(),
                         titteli:Kielistetty = Map(),
                         sahkoposti:Kielistetty = Map(),
                         puhelinnumero:Kielistetty = Map())

  case class Ajanjakso(alkaa:LocalDateTime, paattyy:LocalDateTime)

  case class OidListItem(oid:Oid, nimi: Kielistetty, tila:Julkaisutila, organisaatioOid:OrganisaatioOid, muokkaaja:UserOid, modified:LocalDateTime)
  case class IdListItem(id:UUID, nimi: Kielistetty, tila:Julkaisutila, organisaatioOid:OrganisaatioOid, muokkaaja:UserOid, modified:LocalDateTime)

  case class Osoite(osoite:Kielistetty = Map(),
                    postinumero:Option[String],
                    postitoimipaikka:Kielistetty = Map())

  case class ListEverything(koulutukset:Seq[KoulutusOid] = Seq(),
                            toteutukset:Seq[ToteutusOid] = Seq(),
                            haut:Seq[HakuOid] = Seq(),
                            hakukohteet:Seq[HakukohdeOid] = Seq(),
                            valintaperusteet:Seq[UUID] = Seq())
}