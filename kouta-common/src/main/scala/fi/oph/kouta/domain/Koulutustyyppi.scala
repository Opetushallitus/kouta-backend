package fi.oph.kouta.domain

sealed trait Koulutustyyppi extends EnumType

object Koulutustyyppi extends Enum[Koulutustyyppi] {
  override def name: String = "koulutustyyppi"

  def values =
    List(
      AikuistenPerusopetus,
      Amk,
      Amm,
      AmmMuu,
      AmmOpeErityisopeJaOpo,
      AmmOsaamisala,
      AmmTutkinnonOsa,
      OpePedagOpinnot,
      Erikoislaakari,
      KkOpintojakso,
      KkOpintokokonaisuus,
      Lk,
      Muu,
      Telma,
      Tuva,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOpistovuosi,
      Yo
    )

  def ammatilliset = List(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu)
  def korkeakoulu =
    List(Amk, Yo, AmmOpeErityisopeJaOpo, KkOpintojakso, KkOpintokokonaisuus, Erikoislaakari, OpePedagOpinnot)
  def tutkintoonJohtavat     = List(Amm, Lk, Yo, Amk)
  def onlyOphCanSaveKoulutus = List(Amm, Lk, Telma, Tuva, VapaaSivistystyoOpistovuosi)
  def toinenAsteYhteishaku   = List(Amm, Lk, Telma, Tuva, VapaaSivistystyoOpistovuosi)

  def fromOppilaitostyyppi(oppilaitostyyppi: String): Seq[Koulutustyyppi] =
    oppilaitostyyppi2koulutustyyppi(oppilaitostyyppi)

  def isAmmatillinen(koulutustyyppi: Koulutustyyppi): Boolean =
    ammatilliset.contains(koulutustyyppi)

  def isKorkeakoulu(koulutustyyppi: Koulutustyyppi): Boolean =
    korkeakoulu.contains(koulutustyyppi)

  def isTutkintoonJohtava(koulutustyyppi: Koulutustyyppi): Boolean =
    tutkintoonJohtavat.contains(koulutustyyppi)

  def isKoulutusSaveAllowedOnlyForOph(koulutustyyppi: Koulutustyyppi): Boolean =
    onlyOphCanSaveKoulutus.contains(koulutustyyppi)

  def isToisenAsteenYhteishakuKoulutustyyppi(koulutustyyppi: Koulutustyyppi) =
    toinenAsteYhteishaku.contains(koulutustyyppi)

  val koulutusaste2koulutustyyppi: Map[String, Koulutustyyppi] = Map(
    "koulutusasteoph2002_62" -> Amk,
    "koulutusasteoph2002_71" -> Amk,
    //
    "koulutusasteoph2002_63" -> Yo,
    "koulutusasteoph2002_72" -> Yo,
    "koulutusasteoph2002_73" -> Yo,
    "koulutusasteoph2002_81" -> Yo,
    "koulutusasteoph2002_82" -> Yo
    //
  )

  def oppilaitostyyppi2koulutustyyppi: Map[String, Seq[Koulutustyyppi]] = Map(
    "oppilaitostyyppi_01#1" -> Seq(Muu), //Taiteen perusopetuksen oppilaitokset (ei musiikki)
    "oppilaitostyyppi_11#1" -> Seq(Muu, Tuva, AikuistenPerusopetus), //Peruskoulut
    "oppilaitostyyppi_12#1" -> Seq(Muu, Tuva, AikuistenPerusopetus), //Peruskouluasteen erityiskoulut
    "oppilaitostyyppi_15#1" -> Seq(Lk, Muu, Tuva, AikuistenPerusopetus), //Lukiot
    "oppilaitostyyppi_19#1" -> Seq(Lk, Muu, Tuva, AikuistenPerusopetus), //Perus- ja lukioasteen koulut
    "oppilaitostyyppi_21#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      Muu,
      Tuva,
      Telma,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      AikuistenPerusopetus
    ), //Ammatilliset oppilaitokset
    "oppilaitostyyppi_22#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Telma,
      Tuva,
      AikuistenPerusopetus
    ), //Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23#1" -> Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu), //Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Tuva
    ), //Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_28#1" -> Seq(Amm), //Palo-, poliisi- ja vartiointialojen oppilaitokset
    "oppilaitostyyppi_29#1" -> Seq(Amm), //Sotilasalan ammatilliset oppilaitokset
    "oppilaitostyyppi_41#1" -> Seq(
      Amk,
      AmmOpeErityisopeJaOpo,
      KkOpintojakso,
      KkOpintokokonaisuus
    ), //Ammattikorkeakoulut
    "oppilaitostyyppi_42#1" -> Seq(
      Yo,
      KkOpintojakso,
      KkOpintokokonaisuus,
      Erikoislaakari,
      OpePedagOpinnot
    ), //Yliopistot
    "oppilaitostyyppi_43#1" -> Seq(Yo, KkOpintojakso, KkOpintokokonaisuus), //Sotilaskorkeakoulut
    "oppilaitostyyppi_45#1" -> Seq(Yo, KkOpintojakso, KkOpintokokonaisuus), //Lastentarhanopettajaopistot
    "oppilaitostyyppi_46#1" -> Seq(
      Amk,
      AmmOpeErityisopeJaOpo,
      KkOpintojakso,
      KkOpintokokonaisuus
    ), //Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_61#1" -> Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu, Muu), //Musiikkioppilaitokset
    "oppilaitostyyppi_62#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Muu,
      Tuva,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      AikuistenPerusopetus,
      KkOpintojakso,
      KkOpintokokonaisuus
    ), //Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      Muu,
      Tuva,
      Telma,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      AikuistenPerusopetus
    ), //Kansanopistot
    "oppilaitostyyppi_64#1" -> Seq(
      Amm,
      Lk,
      Muu,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      AikuistenPerusopetus
    ), //Kansalaisopistot
    "oppilaitostyyppi_65#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      Muu
    ), //Opintokeskukset
    "oppilaitostyyppi_66#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      Muu
    ), //Kesäyliopistot
    "oppilaitostyyppi_91#1" -> Seq(Muu), //Kirjeoppilaitokset
    "oppilaitostyyppi_92#1" -> Seq(Muu), //Neuvontajärjestöt
    "oppilaitostyyppi_93#1" -> Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu, Muu), //Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      Muu
    ), //Muut oppilaitokset
    "oppilaitostyyppi_XX#1" -> Seq(Muu) //Ei tiedossa (oppilaitostyyppi)
  )

  def valuesToSwaggerEnum(padStr: String = "      |        "): String =
    values.map(padStr + "- " + _.toString + "\n").mkString
}

case object AikuistenPerusopetus        extends Koulutustyyppi { val name = "aikuisten-perusopetus"         }
case object Amk                         extends Koulutustyyppi { val name = "amk"                           }
case object Amm                         extends Koulutustyyppi { val name = "amm"                           }
case object AmmMuu                      extends Koulutustyyppi { val name = "amm-muu"                       }
case object AmmOpeErityisopeJaOpo       extends Koulutustyyppi { val name = "amm-ope-erityisope-ja-opo"     }
case object AmmOsaamisala               extends Koulutustyyppi { val name = "amm-osaamisala"                }
case object AmmTutkinnonOsa             extends Koulutustyyppi { val name = "amm-tutkinnon-osa"             }
case object OpePedagOpinnot             extends Koulutustyyppi { val name = "ope-pedag-opinnot"             }
case object Erikoislaakari              extends Koulutustyyppi { val name = "erikoislaakari"                }
case object KkOpintojakso               extends Koulutustyyppi { val name = "kk-opintojakso"                }
case object KkOpintokokonaisuus         extends Koulutustyyppi { val name = "kk-opintokokonaisuus"          }
case object Lk                          extends Koulutustyyppi { val name = "lk"                            }
case object Muu                         extends Koulutustyyppi { val name = "muu"                           }
case object Telma                       extends Koulutustyyppi { val name = "telma"                         }
case object Tuva                        extends Koulutustyyppi { val name = "tuva"                          }
case object VapaaSivistystyoMuu         extends Koulutustyyppi { val name = "vapaa-sivistystyo-muu"         }
case object VapaaSivistystyoOpistovuosi extends Koulutustyyppi { val name = "vapaa-sivistystyo-opistovuosi" }
case object Yo                          extends Koulutustyyppi { val name = "yo"                            }
