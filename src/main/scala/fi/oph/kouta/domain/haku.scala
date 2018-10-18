package fi.oph.kouta.domain

import java.time.Instant

sealed trait Hakutapa {
  def name: String
  override def toString() = name
}
object Hakutapa {
  def withName(n:String) = n match {
    case Jatkuvahaku.name => Jatkuvahaku
    case Yhteishaku.name => Yhteishaku
    case Erillishaku.name => Erillishaku
    case x => throw new IllegalArgumentException(s"Unknown hakutapa ${x}")
  }
  def values() = List(Jatkuvahaku, Yhteishaku, Erillishaku)
}
case object Jatkuvahaku extends Hakutapa { val name = "jatkuvahaku" }
case object Yhteishaku extends Hakutapa { val name = "yhteishaku" }
case object Erillishaku extends Hakutapa { val name = "erillishaku" }

sealed trait Alkamiskausi {
  def name: String
  override def toString() = name
}
object Alkamiskausi {
  def withName(n:String) = n match {
    case Kevät.name => Kevät
    case Kesä.name => Kesä
    case Syksy.name => Syksy
    case x => throw new IllegalArgumentException(s"Unknown alkamiskausi ${x}")
  }
  def values() = List(Kevät, Kesä, Syksy)
}
case object Kevät extends Alkamiskausi { val name = "kevät" }
case object Kesä extends Alkamiskausi { val name = "kesä" }
case object Syksy extends Alkamiskausi { val name = "syksy" }

case class HaunHakuaika(alkaa:Instant, paattyy:Instant)
case class HakuMetadata(yhteystieto: Option[Yhteystieto] = None)

case class Haku(oid:Option[String] = None,
                tila:Julkaisutila = Tallennettu,
                nimi: Kielistetty = Map(),
                hakutapa: Option[Hakutapa] = None,
                tietojen_muuttaminen_paattyy: Option[Instant] = None,
                alkamiskausi: Option[Alkamiskausi] = None,
                alkamisvuosi: Option[String] = None,
                kohdejoukko: Option[String] = None,
                kohdejoukon_tarkenne: Option[String] = None,
                metadata: Option[HakuMetadata] = None,
                organisaatio: String,
                hakuajat: List[HaunHakuaika] = List(),
                muokkaaja:String)

