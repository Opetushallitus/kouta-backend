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
      Erikoistumiskoulutus,
      Lk,
      Muu,
      TaiteenPerusopetus,
      Telma,
      Tuva,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoOsaamismerkki,
      Yo
    )

  def ammatilliset = List(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu)
  def korkeakoulu = List(
    Amk,
    Yo,
    AmmOpeErityisopeJaOpo,
    KkOpintojakso,
    KkOpintokokonaisuus,
    Erikoislaakari,
    OpePedagOpinnot,
    Erikoistumiskoulutus
  )
  def tutkintoonJohtavat     = List(Amm, Lk, Yo, Amk)

  // amm-koulutuksia saa tallentaa muut kuin OPH vain silloin kun on valittu tiettyjä koulutuskoodeja!
  def onlyOphCanSaveKoulutus: List[Koulutustyyppi] = List(Lk, Telma, Tuva, VapaaSivistystyoOpistovuosi, AikuistenPerusopetus, VapaaSivistystyoOsaamismerkki)
  def toinenAsteYhteishaku   = List(Amm, Lk, Telma, Tuva, VapaaSivistystyoOpistovuosi)

  def fromOppilaitostyyppi(oppilaitostyyppi: String): Seq[Koulutustyyppi] =
    oppilaitostyyppi2koulutustyyppi(oppilaitostyyppi)

  def isAmmatillinen(koulutustyyppi: Koulutustyyppi): Boolean =
    ammatilliset.contains(koulutustyyppi)

  def isKorkeakoulu(koulutustyyppi: Koulutustyyppi): Boolean =
    korkeakoulu.contains(koulutustyyppi)

  def isTutkintoonJohtava(koulutustyyppi: Koulutustyyppi): Boolean =
    tutkintoonJohtavat.contains(koulutustyyppi)

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

  // TODO: Tarvitaanko oppilaitostyyppiureissa versionumeroa? Nyt joudutaan poistamaan ne kouta-uin puolella
  def oppilaitostyyppi2koulutustyyppi: Map[String, Seq[Koulutustyyppi]] = Map(
    "oppilaitostyyppi_01#1" -> Seq(TaiteenPerusopetus), //Taiteen perusopetuksen oppilaitokset (ei musiikki)
    "oppilaitostyyppi_11#1" -> Seq(Tuva, AikuistenPerusopetus, TaiteenPerusopetus), //Peruskoulut
    "oppilaitostyyppi_12#1" -> Seq(
      Tuva,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Peruskouluasteen erityiskoulut
    "oppilaitostyyppi_15#1" -> Seq(Lk, Tuva, AikuistenPerusopetus, TaiteenPerusopetus), //Lukiot
    "oppilaitostyyppi_19#1" -> Seq(
      Lk,
      Tuva,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Perus- ja lukioasteen koulut
    "oppilaitostyyppi_21#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      Tuva,
      Telma,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Ammatilliset oppilaitokset
    "oppilaitostyyppi_22#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Telma,
      Tuva,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23#1" -> Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu), //Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Tuva
    ), //Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_28#1" -> Seq(Amm, Amk, AmmMuu), //Palo-, poliisi- ja vartiointialojen oppilaitokset
    "oppilaitostyyppi_29#1" -> Seq(Amm), //Sotilasalan ammatilliset oppilaitokset
    "oppilaitostyyppi_41#1" -> Seq(
      Amk,
      AmmOpeErityisopeJaOpo,
      KkOpintojakso,
      KkOpintokokonaisuus,
      Erikoistumiskoulutus
    ), //Ammattikorkeakoulut
    "oppilaitostyyppi_42#1" -> Seq(
      Yo,
      KkOpintojakso,
      KkOpintokokonaisuus,
      Erikoislaakari,
      OpePedagOpinnot,
      Erikoistumiskoulutus
    ), //Yliopistot
    "oppilaitostyyppi_43#1" -> Seq(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoistumiskoulutus), //Sotilaskorkeakoulut
    "oppilaitostyyppi_45#1" -> Seq(Yo, KkOpintojakso, KkOpintokokonaisuus), //Lastentarhanopettajaopistot
    "oppilaitostyyppi_46#1" -> Seq(
      Amk,
      AmmOpeErityisopeJaOpo,
      KkOpintojakso,
      KkOpintokokonaisuus,
      Erikoistumiskoulutus
    ), //Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_61#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      TaiteenPerusopetus,
    ), //Musiikkioppilaitokset
    "oppilaitostyyppi_62#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Tuva,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki,
      AikuistenPerusopetus,
      KkOpintojakso,
      KkOpintokokonaisuus,
      TaiteenPerusopetus
    ), //Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      Tuva,
      Telma,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoOsaamismerkki,
      VapaaSivistystyoMuu,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Kansanopistot
    "oppilaitostyyppi_64#1" -> Seq(
      Amm,
      Lk,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki,
      AikuistenPerusopetus,
      TaiteenPerusopetus
    ), //Kansalaisopistot
    "oppilaitostyyppi_65#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki
    ), //Opintokeskukset
    "oppilaitostyyppi_66#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      Lk,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki
    ), //Kesäyliopistot
    "oppilaitostyyppi_91#1" -> Seq(), //Kirjeoppilaitokset
    "oppilaitostyyppi_92#1" -> Seq(), //Neuvontajärjestöt
    "oppilaitostyyppi_93#1" -> Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu), //Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99#1" -> Seq(
      Amm,
      AmmTutkinnonOsa,
      AmmOsaamisala,
      AmmMuu,
      VapaaSivistystyoOpistovuosi,
      VapaaSivistystyoMuu,
      VapaaSivistystyoOsaamismerkki,
      TaiteenPerusopetus,
    ), //Muut oppilaitokset
    "oppilaitostyyppi_xx#1" -> Seq(Muu) //Ei tiedossa (oppilaitostyyppi)
  )

  def valuesToSwaggerEnum(padStr: String = "      |        "): String =
    values.map(padStr + "- " + _.toString + "\n").mkString
}

case object AikuistenPerusopetus extends Koulutustyyppi {
  val name = "aikuisten-perusopetus"
}

case object Amk extends Koulutustyyppi {
  val name = "amk"
}

case object Amm extends Koulutustyyppi {
  val name = "amm"
}

case object AmmMuu extends Koulutustyyppi {
  val name = "amm-muu"
}

case object AmmOpeErityisopeJaOpo extends Koulutustyyppi {
  val name = "amm-ope-erityisope-ja-opo"
}

case object AmmOsaamisala extends Koulutustyyppi {
  val name = "amm-osaamisala"
}

case object AmmTutkinnonOsa extends Koulutustyyppi {
  val name = "amm-tutkinnon-osa"
}

case object OpePedagOpinnot extends Koulutustyyppi {
  val name = "ope-pedag-opinnot"
}

case object Erikoislaakari extends Koulutustyyppi {
  val name = "erikoislaakari"
}

case object KkOpintojakso extends Koulutustyyppi {
  val name = "kk-opintojakso"
}

case object KkOpintokokonaisuus extends Koulutustyyppi {
  val name = "kk-opintokokonaisuus"
}

case object Erikoistumiskoulutus extends Koulutustyyppi {
  val name = "erikoistumiskoulutus"
}

case object Lk extends Koulutustyyppi {
  val name = "lk"
}

case object Muu extends Koulutustyyppi {
  val name = "muu"
}

case object TaiteenPerusopetus extends Koulutustyyppi {
  val name = "taiteen-perusopetus"
}

case object Telma extends Koulutustyyppi {
  val name = "telma"
}

case object Tuva extends Koulutustyyppi {
  val name = "tuva"
}

case object VapaaSivistystyoMuu extends Koulutustyyppi {
  val name = "vapaa-sivistystyo-muu"
}

case object VapaaSivistystyoOpistovuosi extends Koulutustyyppi {
  val name = "vapaa-sivistystyo-opistovuosi"
}

case object VapaaSivistystyoOsaamismerkki extends Koulutustyyppi {
  val name = "vapaa-sivistystyo-osaamismerkki"
}

case object Yo extends Koulutustyyppi {
  val name = "yo"
}
