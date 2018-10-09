package fi.oph.kouta.domain

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
    case x => throw new RuntimeException(x)
  }
}
case object Amm extends Koulutustyyppi { val name = "amm" }
case object Kk extends Koulutustyyppi { val name = "kk" }
case object Lk extends Koulutustyyppi { val name = "lk" }
case object Muu extends Koulutustyyppi { val name = "muu" }

case class KoulutusMetadata(kuvaus:Map[Kieli, String] = Map())

case class Koulutus(oid:Option[String] = None,
                    johtaaTutkintoon:Boolean,
                    koulutustyyppi:Koulutustyyppi,
                    koulutusKoodiUri:String,
                    tila:Julkaisutila,
                    tarjoajat:List[String],
                    nimi: Map[Kieli, String],
                    metadata: KoulutusMetadata,
                    muokkaaja:String)