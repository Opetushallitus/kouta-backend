package fi.oph.kouta.domain

sealed trait KoodistoNimi extends EnumType

object KoodistoNimi extends Enum[KoodistoNimi] {
  override def name: String = "koodisto"
  val values = List(
    KoulutusKoodisto,
    OpintojenLaajuusyksikkoKoodisto,
    KoulutusalaKoodisto,
    KoulutuksenLisatiedotKoodisto,
    TutkintonimikeKoodisto,
    OpetuskieliKoodisto,
    OpetusaikaKoodisto,
    OpetustapaKoodisto,
    OsaamisalaKoodisto,
    LukioPainotuksetKoodisto,
    LukioErityinenKoulutustehtavaKoodisto,
    LukioDiplomiKoodisto,
    OpinnonTyyppiKoodisto,
    ErikoistumiskoulutusKoodisto,
    TaiteenalaKoodisto
  )
}

case object KoulutusKoodisto                extends KoodistoNimi { val name = "koulutus"                                        }
case object OpintojenLaajuusyksikkoKoodisto extends KoodistoNimi { val name = "opintojenlaajuusyksikko"                         }
case object KoulutusalaKoodisto             extends KoodistoNimi { val name = "kansallinenkoulutusluokitus2016koulutusalataso2" }
case object KoulutuksenLisatiedotKoodisto   extends KoodistoNimi { val name = "koulutuksenlisatiedot"                           }
case object TutkintonimikeKoodisto          extends KoodistoNimi { val name = "tutkintonimikekk"                                }
case object OpetuskieliKoodisto             extends KoodistoNimi { val name = "oppilaitoksenopetuskieli"                        }
case object OpetusaikaKoodisto              extends KoodistoNimi { val name = "opetusaikakk"                                    }
case object OpetustapaKoodisto              extends KoodistoNimi { val name = "opetuspaikkakk"                                  }
case object OsaamisalaKoodisto              extends KoodistoNimi { val name = "osaamisala"                                      }
case object LukioPainotuksetKoodisto        extends KoodistoNimi { val name = "lukiopainotukset"                                }
case object LukioErityinenKoulutustehtavaKoodisto extends KoodistoNimi {
  val name = "lukiolinjaterityinenkoulutustehtava"
}
case object LukioDiplomiKoodisto            extends KoodistoNimi { val name = "moduulikoodistolops2021"      }
case object OpinnonTyyppiKoodisto           extends KoodistoNimi { val name = "opinnontyyppi"                }
case object ErikoistumiskoulutusKoodisto extends KoodistoNimi { val name = "erikoistumiskoulutukset"      }
case object TaiteenalaKoodisto              extends KoodistoNimi { val name = "taiteenperusopetustaiteenala" }
