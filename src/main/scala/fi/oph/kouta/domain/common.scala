package fi.oph.kouta.domain

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä

sealed trait Julkaisutila {
  def name:String
  override def toString() = name
}
object Julkaisutila {
  def withName(n:String): Julkaisutila = n match {
    case Tallennettu.name => Tallennettu
    case Julkaistu.name => Julkaistu
    case Arkistoitu.name => Arkistoitu
    case x => throw new RuntimeException(x)
  }
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
    case x => throw new RuntimeException(x)
  }
}
case object Fi extends Kieli { val koodi = "fi" }
case object Sv extends Kieli { val koodi = "sv" }
case object En extends Kieli { val koodi = "en" }
