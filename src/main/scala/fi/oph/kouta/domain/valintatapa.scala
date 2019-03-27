package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Valintatapa extends Validatable {
  def valintatapaKoodiUri: Option[String]
  def kuvaus: Kielistetty
  def sisalto: Seq[ValintatapaSisalto]
  def kaytaMuuntotaulukkoa: Boolean
  def kynnysehto: Kielistetty
  def enimmaispisteet: Option[Double]
  def vahimmaispisteet: Option[Double]

  override def validate(): IsValid = and(
    validateIfDefined[String](valintatapaKoodiUri, assertMatch(_, ValintatapajonoKoodiPattern))
  )
}

case class AmmatillinenValintatapa(valintatapaKoodiUri: Option[String] = None,
                                   kuvaus: Kielistetty = Map(),
                                   sisalto: Seq[ValintatapaSisalto],
                                   kaytaMuuntotaulukkoa: Boolean = false,
                                   kynnysehto: Kielistetty = Map(),
                                   enimmaispisteet: Option[Double] = None,
                                   vahimmaispisteet: Option[Double] = None) extends Valintatapa

sealed trait KorkeakoulutusValintatapa extends Valintatapa {
  def nimi: Kielistetty
}

case class AmmattikorkeakouluValintatapa(nimi: Kielistetty = Map(),
                                         valintatapaKoodiUri: Option[String] = None,
                                         kuvaus: Kielistetty = Map(),
                                         sisalto: Seq[ValintatapaSisalto],
                                         kaytaMuuntotaulukkoa: Boolean = false,
                                         kynnysehto: Kielistetty = Map(),
                                         enimmaispisteet: Option[Double] = None,
                                         vahimmaispisteet: Option[Double] = None) extends KorkeakoulutusValintatapa

case class YliopistoValintatapa(nimi: Kielistetty = Map(),
                                valintatapaKoodiUri: Option[String] = None,
                                kuvaus: Kielistetty = Map(),
                                sisalto: Seq[ValintatapaSisalto],
                                kaytaMuuntotaulukkoa: Boolean = false,
                                kynnysehto: Kielistetty = Map(),
                                enimmaispisteet: Option[Double] = None,
                                vahimmaispisteet: Option[Double] = None) extends KorkeakoulutusValintatapa

sealed trait ValintatapaSisalto

case class Taulukko(id: Option[UUID],
                    nimi: Kielistetty = Map(),
                    rows: Seq[Row] = Seq()) extends ValintatapaSisalto

case class ValintatapaSisaltoTeksti(teksti: Kielistetty) extends ValintatapaSisalto

case class Row(index: Int,
               isHeader: Boolean = false,
               columns: Seq[Column] = Seq())

case class Column(index: Int,
                  text: Kielistetty = Map())
