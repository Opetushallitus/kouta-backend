package fi.oph.kouta.util

import fi.oph.kouta.domain.Koulutustyyppi

object MiscUtils {
  def optionWhen[T](cond: Boolean)(result: => T): Option[T] = if(cond) Some(result) else None
  def isYhteishakuHakutapa(hakutapa: Option[String]): Boolean = hakutapa.exists(hakutapaKoodi => hakutapaKoodi.startsWith("hakutapa_01"))
  def withoutKoodiVersion(koodiUri: String) = koodiUri.split("#").head
  def isToisenAsteenYhteishaku(koulutustyyppi: Option[Koulutustyyppi], hakutapaKoodiUri: Option[String]) =
    koulutustyyppi.isDefined && hakutapaKoodiUri.isDefined && Koulutustyyppi.isToisenAsteenYhteishakuKoulutustyyppi(
      koulutustyyppi.get
    ) && isYhteishakuHakutapa(
      hakutapaKoodiUri
    )

  def EBkoodiuri = "koulutus_301104"
  def isEBlukiokoulutus(koulutuksetKoodiUri: Seq[String]) = koulutuksetKoodiUri.map(withoutKoodiVersion(_)).contains(EBkoodiuri)

  def DIAkoodiuri = "koulutus_301103"
  def isDIAlukiokoulutus(koulutuksetKoodiUri: Seq[String]) = koulutuksetKoodiUri.map(withoutKoodiVersion(_)).contains(DIAkoodiuri)

  def retryStatusCodes = Set(500, 502, 504)
}
