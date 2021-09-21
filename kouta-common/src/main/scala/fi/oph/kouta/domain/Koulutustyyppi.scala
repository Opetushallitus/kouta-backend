package fi.oph.kouta.domain

sealed trait Koulutustyyppi extends EnumType

object Koulutustyyppi extends Enum[Koulutustyyppi] {
  override def name: String = "koulutustyyppi"

  def values = List(Amm, Lk, Muu, Yo, Amk, Tuva, Telma, AmmTutkinnonOsa, AmmOsaamisala, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu)

  def ammatilliset = List(Amm, AmmTutkinnonOsa, AmmOsaamisala)
  def korkeakoulu = List(Amk, Yo)
  def tutkintoonJohtavat = List(Amm, Lk, Yo, Amk)

  def fromOppilaitostyyppi(oppilaitostyyppi: String): Seq[Koulutustyyppi] =
    oppilaitostyyppi2koulutustyyppi(oppilaitostyyppi)

  def isAmmatillinen(koulutustyyppi: Koulutustyyppi): Boolean =
    ammatilliset.contains(koulutustyyppi)

  def isKorkeakoulu(koulutustyyppi: Koulutustyyppi) : Boolean =
    korkeakoulu.contains(koulutustyyppi)

  def isTutkintoonJohtava(koulutustyyppi: Koulutustyyppi): Boolean =
    tutkintoonJohtavat.contains(koulutustyyppi)

  val koulutusaste2koulutustyyppi: Map[String, Koulutustyyppi] = Map(
    "koulutusasteoph2002_62" -> Amk,
    "koulutusasteoph2002_71" -> Amk,
    //
    "koulutusasteoph2002_63" -> Yo,
    "koulutusasteoph2002_72" -> Yo,
    "koulutusasteoph2002_73" -> Yo,
    "koulutusasteoph2002_81" -> Yo,
    "koulutusasteoph2002_82" -> Yo,
    //
  )

  val oppilaitostyyppi2koulutustyyppi: Map[String, Seq[Koulutustyyppi]] = Map(
    "oppilaitostyyppi_01#1" -> Seq(Muu), //Taiteen perusopetuksen oppilaitokset (ei musiikki)
    "oppilaitostyyppi_11#1" -> Seq(Muu, Tuva, Telma), //Peruskoulut
    "oppilaitostyyppi_12#1" -> Seq(Muu, Tuva, Telma), //Peruskouluasteen erityiskoulut
    "oppilaitostyyppi_15#1" -> Seq(Lk, Tuva, Telma), //Lukiot
    "oppilaitostyyppi_19#1" -> Seq(Lk, Tuva, Telma), //Perus- ja lukioasteen koulut
    "oppilaitostyyppi_21#1" -> Seq(Amm, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Ammatilliset oppilaitokset
    "oppilaitostyyppi_22#1" -> Seq(Amm, Tuva, Telma), //Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23#1" -> Seq(Amm, Tuva, Telma), //Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24#1" -> Seq(Amm, Tuva, Telma), //Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_28#1" -> Seq(Amm), //Palo-, poliisi- ja vartiointialojen oppilaitokset
    "oppilaitostyyppi_29#1" -> Seq(Amm), //Sotilasalan ammatilliset oppilaitokset
    "oppilaitostyyppi_41#1" -> Seq(Amk), //Ammattikorkeakoulut
    "oppilaitostyyppi_42#1" -> Seq(Yo), //Yliopistot
    "oppilaitostyyppi_43#1" -> Seq(Yo), //Sotilaskorkeakoulut
    "oppilaitostyyppi_45#1" -> Seq(Yo), //Lastentarhanopettajaopistot
    "oppilaitostyyppi_46#1" -> Seq(Amk), //Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_61#1" -> Seq(Muu), //Musiikkioppilaitokset
    "oppilaitostyyppi_62#1" -> Seq(Muu, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63#1" -> Seq(Amm, Muu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Kansanopistot
    "oppilaitostyyppi_64#1" -> Seq(Muu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Kansalaisopistot
    "oppilaitostyyppi_65#1" -> Seq(Muu), //Opintokeskukset
    "oppilaitostyyppi_66#1" -> Seq(Muu, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Kesäyliopistot
    "oppilaitostyyppi_91#1" -> Seq(Muu), //Kirjeoppilaitokset
    "oppilaitostyyppi_92#1" -> Seq(Muu), //Neuvontajärjestöt
    "oppilaitostyyppi_93#1" -> Seq(Muu), //Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99#1" -> Seq(Muu, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu), //Muut oppilaitokset
    "oppilaitostyyppi_XX#1" -> Seq(Muu), //Ei tiedossa (oppilaitostyyppi)
  )
}

case object Amm extends Koulutustyyppi { val name = "amm" }
case object Lk extends Koulutustyyppi { val name = "lk" }
case object Yo extends Koulutustyyppi { val name = "yo" }
case object Amk extends Koulutustyyppi { val name = "amk" }
case object Tuva extends Koulutustyyppi { val name = "tuva" }
case object Telma extends Koulutustyyppi { val name = "telma" }
case object Muu extends Koulutustyyppi {val name = "muu"}
case object AmmTutkinnonOsa extends Koulutustyyppi {val name = "amm-tutkinnon-osa"}
case object AmmOsaamisala extends Koulutustyyppi {val name = "amm-osaamisala"}
case object VapaaSivistystyoOpistovuosi extends Koulutustyyppi {val name = "vapaa-sivistystyo-opistovuosi"}
case object VapaaSivistystyoMuu extends Koulutustyyppi {val name = "vapaa-sivistystyo-muu"}

