package fi.oph.kouta.domain

sealed trait KoodistoNimi extends EnumType

object KoodistoNimi extends Enum[KoodistoNimi] {
  override def name: String = "koodisto"
  val values = List(KoulutusKoodisto, OpintojenLaajuusyksikkoKoodisto, KoulutusalaKoodisto)
}

case object KoulutusKoodisto extends KoodistoNimi { val name = "koulutus" }
case object OpintojenLaajuusyksikkoKoodisto extends KoodistoNimi { val name = "opintojenlaajuusyksikko" }
case object KoulutusalaKoodisto extends KoodistoNimi { val name = "kansallinenkoulutusluokitus2016koulutusalataso2" }

