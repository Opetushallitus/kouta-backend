package fi.oph.kouta.domain

sealed trait KoulutustyypinTarkenne extends EnumType

object KoulutustyypinTarkenne extends Enum[KoulutustyypinTarkenne] {
  override def name: String = "koulutustyypin tarkenne"
  def values = List(Valma, Telma, TutkinnonOsa, Osaamisala, Muut)
}

case object Valma extends KoulutustyypinTarkenne { val name = "valma" }
case object Telma extends KoulutustyypinTarkenne { val name = "telma" }
case object TutkinnonOsa extends KoulutustyypinTarkenne { val name = "tutkinnonOsa" }
case object Osaamisala extends KoulutustyypinTarkenne { val name = "osaamisala" }
case object Muut extends KoulutustyypinTarkenne { val name = "muu" }
