package fi.oph.kouta.util

import fi.oph.kouta.domain.{En, Fi, Kieli, Koulutustyyppi, Sv}

import java.util.Optional

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

  def toKieli(kieli: String, useDefault: Boolean = false): Option[Kieli] = {
    if (useDefault) Some(Fi)
    else withoutKoodiVersion(kieli) match {
      case "kieli_fi" => Some(Fi)
      case "kieli_sv" => Some(Sv)
      case "kieli_en" => Some(En)
      case _ =>
        None
    }
  }

  def toKieliKoodiUri(kieli: Kieli): String = {
    kieli match {
      case Fi => "kieli_fi"
      case Sv => "kieli_sv"
      case En => "kieli_en"
      case _ => ""
    }
  }
  def toScalaOption[A](maybeA: Optional[A]): Option[A] =
    if (maybeA.isEmpty) None else Some(maybeA.get)
}
