package fi.oph.kouta.domain

sealed trait Koulutustyyppi extends EnumType

object Koulutustyyppi extends Enum[Koulutustyyppi] {
  override def name: String = "koulutustyyppi"

  def values = List(Amm, Lk, Muu, Yo, Amk, AmmTutkinnonOsa, AmmOsaamisala)

  def ammatilliset = List(Amm, AmmTutkinnonOsa, AmmOsaamisala)
  def korkeakoulu = List(Amk, Yo)
  def tutkintoonJohtavat = List(Amm, Lk, Yo, Amk)

  def fromOppilaitostyyppi(oppilaitostyyppi: String): Koulutustyyppi =
    oppilaitostyyppi2koulutustyyppi(oppilaitostyyppi)

  def isAmmatillinen(koulutustyyppi: Koulutustyyppi): Boolean =
    ammatilliset.contains(koulutustyyppi)

  def isKorkeakoulu(koulutustyyppi: Koulutustyyppi) : Boolean =
    korkeakoulu.contains(koulutustyyppi)

  def isTutkintoonJohtava(koulutustyyppi: Koulutustyyppi): Boolean =
    tutkintoonJohtavat.contains(koulutustyyppi)

  val oppilaitostyyppi2koulutustyyppi: Map[String, Koulutustyyppi] = Map(
    "oppilaitostyyppi_01#1" -> Muu, //Taiteen perusopetuksen oppilaitokset (ei musiikki)
    "oppilaitostyyppi_11#1" -> Muu, //Peruskoulut
    "oppilaitostyyppi_12#1" -> Muu, //Peruskouluasteen erityiskoulut
    "oppilaitostyyppi_15#1" -> Lk, //Lukiot
    "oppilaitostyyppi_19#1" -> Lk, //Perus- ja lukioasteen koulut
    "oppilaitostyyppi_21#1" -> Amm, //Ammatilliset oppilaitokset
    "oppilaitostyyppi_22#1" -> Amm, //Ammatilliset erityisoppilaitokset
    "oppilaitostyyppi_23#1" -> Amm, //Ammatilliset erikoisoppilaitokset
    "oppilaitostyyppi_24#1" -> Amm, //Ammatilliset aikuiskoulutuskeskukset
    "oppilaitostyyppi_28#1" -> Amm, //Palo-, poliisi- ja vartiointialojen oppilaitokset
    "oppilaitostyyppi_29#1" -> Amm, //Sotilasalan ammatilliset oppilaitokset
    "oppilaitostyyppi_41#1" -> Amk, //Ammattikorkeakoulut
    "oppilaitostyyppi_42#1" -> Yo, //Yliopistot
    "oppilaitostyyppi_43#1" -> Yo, //Sotilaskorkeakoulut
    "oppilaitostyyppi_45#1" -> Yo, //Lastentarhanopettajaopistot
    "oppilaitostyyppi_46#1" -> Amk, //Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_61#1" -> Muu, //Musiikkioppilaitokset
    "oppilaitostyyppi_62#1" -> Muu, //Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63#1" -> Muu, //Kansanopistot
    "oppilaitostyyppi_64#1" -> Muu, //Kansalaisopistot
    "oppilaitostyyppi_65#1" -> Muu, //Opintokeskukset
    "oppilaitostyyppi_66#1" -> Muu, //Kesäyliopistot
    "oppilaitostyyppi_91#1" -> Muu, //Kirjeoppilaitokset
    "oppilaitostyyppi_92#1" -> Muu, //Neuvontajärjestöt
    "oppilaitostyyppi_93#1" -> Muu, //Muut koulutuksen järjestäjät
    "oppilaitostyyppi_99#1" -> Muu, //Muut oppilaitokset
    "oppilaitostyyppi_XX#1" -> Muu, //Ei tiedossa (oppilaitostyyppi)
  )
}

case object Amm extends Koulutustyyppi { val name = "amm" }
case object Lk extends Koulutustyyppi { val name = "lk" }
case object Yo extends Koulutustyyppi { val name = "yo" }
case object Amk extends Koulutustyyppi { val name = "amk" }
case object Muu extends Koulutustyyppi {val name = "muu"}
case object AmmTutkinnonOsa extends Koulutustyyppi {val name = "amm-tutkinnon-osa"}
case object AmmOsaamisala extends Koulutustyyppi {val name = "amm-osaamisala"}
