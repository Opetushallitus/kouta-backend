package fi.oph.kouta.domain

sealed trait Koulutustyyppi extends EnumType

object Koulutustyyppi extends Enum[Koulutustyyppi] {
  override def name: String = "koulutustyyppi"
  def values() = List(Amm, Lk, Muu, Yo, Amk)
}

case object Amm extends Koulutustyyppi { val name = "amm" }
case object Lk extends Koulutustyyppi { val name = "lk" }
case object Muu extends Koulutustyyppi { val name = "muu" }
case object Yo extends Koulutustyyppi { val name = "yo" }
case object Amk extends Koulutustyyppi { val name = "amk" }
