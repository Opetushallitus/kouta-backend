package fi.oph.kouta

import java.time.Instant

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä
package object domain {
  type Kielistetty = Map[Kieli,String]

  sealed trait Julkaisutila {
    def name:String
    override def toString() = name
  }
  object Julkaisutila {
    def withName(n:String): Julkaisutila = n match {
      case Tallennettu.name => Tallennettu
      case Julkaistu.name => Julkaistu
      case Arkistoitu.name => Arkistoitu
      case x => throw new IllegalArgumentException(s"Unknown julkaisutila ${x}")
    }
    def values() = List(Tallennettu, Julkaistu, Arkistoitu)
  }
  case object Tallennettu extends Julkaisutila { val name = "tallennettu" }
  case object Julkaistu extends Julkaisutila { val name = "julkaistu" }
  case object Arkistoitu extends Julkaisutila { val name = "arkistoitu" }

  sealed trait Kieli {
    def koodi:String
    override def toString() = koodi
  }
  object Kieli {
    def withName(n:String) = n match {
      case Fi.koodi => Fi
      case Sv.koodi => Sv
      case En.koodi => En
      case x => throw new IllegalArgumentException(s"Unknown kieli ${x}")
    }
    def values = List(Fi, Sv, En)
  }
  case object Fi extends Kieli { val koodi = "fi" }
  case object Sv extends Kieli { val koodi = "sv" }
  case object En extends Kieli { val koodi = "en" }

  sealed trait Koulutustyyppi {
    def name: String
    override def toString() = name
  }
  object Koulutustyyppi {
    def withName(n:String) = n match {
      case Amm.name => Amm
      case Kk.name => Kk
      case Lk.name => Lk
      case Muu.name => Muu
      case x => throw new IllegalArgumentException(s"Unknown koulutustyyppi ${x}")
    }
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
}
