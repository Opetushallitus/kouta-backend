package fi.oph.kouta

import java.time.Instant

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

  case class Yhteystieto(nimi:String,
                         titteli:Kielistetty = Map(),
                         sahkoposti:Option[String] = None,
                         puhelinnumero:Option[String] = None)

  case class ListParams(tilat:List[Julkaisutila] = List(),
                        tarjoajat:List[String] = List())

  case class ListResponse(oid:String, nimi: Kielistetty)
}
