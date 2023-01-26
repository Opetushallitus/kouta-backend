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
    TaiteenalaKoodisto,
    HakukohdeAmmErityisopetusKoodisto,
    HakukohdePoJalkYhteishakuKoodisto,
    PohjakoulutusvaatimusKoodisto,
    LiitetyyppiKoodisto,
    ValintakoeTyyppiKoodisto,
    KausiKoodisto,
    OppiaineKoodisto,
    KieliKoodisto,
    PostiosoiteKoodisto,
    HakutapaKoodisto,
    HaunKohdejoukkoKoodisto,
    HaunKohdejoukonTarkenneKoodisto,
    ValintatapaKoodisto,
    TietoaOpiskelustaKoodisto
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
case object LukioDiplomiKoodisto         extends KoodistoNimi { val name = "moduulikoodistolops2021"      }
case object OpinnonTyyppiKoodisto        extends KoodistoNimi { val name = "opinnontyyppi"                }
case object ErikoistumiskoulutusKoodisto extends KoodistoNimi { val name = "erikoistumiskoulutukset"      }
case object TaiteenalaKoodisto           extends KoodistoNimi { val name = "taiteenperusopetustaiteenala" }
case object HakukohdeAmmErityisopetusKoodisto extends KoodistoNimi {
  val name = "hakukohteeterammatillinenerityisopetus"
}
case object HakukohdePoJalkYhteishakuKoodisto extends KoodistoNimi {
  val name = "hakukohteetperusopetuksenjalkeinenyhteishaku"
}

case object PohjakoulutusvaatimusKoodisto extends KoodistoNimi { val name = "pohjakoulutusvaatimuskouta" }
case object LiitetyyppiKoodisto           extends KoodistoNimi { val name = "liitetyypitamm"             }
case object ValintakoeTyyppiKoodisto extends KoodistoNimi { val name = "valintakokeentyyppi" }
case object KausiKoodisto extends KoodistoNimi { val name = "kausi" }
case object OppiaineKoodisto extends KoodistoNimi { val name = "painotettavatoppiaineetlukiossa" }
case object KieliKoodisto extends KoodistoNimi { val name = "kieli" }
case object PostiosoiteKoodisto extends KoodistoNimi { val name = "posti" }
case object HakutapaKoodisto extends KoodistoNimi { val name = "hakutapa" }
case object HaunKohdejoukkoKoodisto extends KoodistoNimi { val name = "haunkohdejoukko" }
case object HaunKohdejoukonTarkenneKoodisto extends KoodistoNimi { val name = "haunkohdejoukontarkenne" }
case object ValintatapaKoodisto extends KoodistoNimi { val name = "valintatapajono" }
case object TietoaOpiskelustaKoodisto extends KoodistoNimi { val name = "organisaationkuvaustiedot" }
