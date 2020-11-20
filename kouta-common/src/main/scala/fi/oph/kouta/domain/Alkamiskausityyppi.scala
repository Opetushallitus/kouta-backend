package fi.oph.kouta.domain

sealed trait Alkamiskausityyppi extends EnumType

object Alkamiskausityyppi extends Enum[Alkamiskausityyppi] {
  override def name: String = "alkamiskausityyppi"
  val values = List(HenkilökohtainenSuunnitelma, TarkkaAlkamisajankohta, AlkamiskausiJaVuosi)
}

case object HenkilökohtainenSuunnitelma extends Alkamiskausityyppi { val name = "henkilokohtainen suunnitelma" }
case object TarkkaAlkamisajankohta extends Alkamiskausityyppi { val name = "tarkka alkamisajankohta" }
case object AlkamiskausiJaVuosi extends Alkamiskausityyppi { val name = "alkamiskausi ja -vuosi" }