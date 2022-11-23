package fi.oph.kouta.util

import fi.oph.kouta.domain.{Koulutustyyppi, Organisaatio, oppilaitostyypitForAvoinKorkeakoulutus}

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
  def isEBlukiokoulutus(koulutuksetKoodiUri: Seq[String]) = koulutuksetKoodiUri.map(uri => withoutKoodiVersion(uri)).contains(EBkoodiuri)

  def DIAkoodiuri = "koulutus_301103"
  def isDIAlukiokoulutus(koulutuksetKoodiUri: Seq[String]) = koulutuksetKoodiUri.map(uri => withoutKoodiVersion(uri)).contains(DIAkoodiuri)

  def retryStatusCodes = Set(500, 502, 504)

  def hasCorrectOrganisaatioAndOppilaitostyyppi(organisaatio: Organisaatio, organisaatiotyyppi: String): Boolean = {
    organisaatio.organisaatiotyypit.contains(organisaatiotyyppi) && organisaatio.oppilaitostyyppi.exists(tyyppi => oppilaitostyypitForAvoinKorkeakoulutus.contains(tyyppi))
  }

  def filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot: List[Organisaatio], organisaatiotyyppi: String): List[Organisaatio] = {
    organisaatiot.foldLeft(List[Organisaatio]()) { (accumulator, org) =>
      val filteredChildren = filterOrganisaatiotWithOrganisaatiotyyppi(org.children, organisaatiotyyppi)
      val filteredOrg = org.copy(children = filteredChildren)

      if (hasCorrectOrganisaatioAndOppilaitostyyppi(filteredOrg, organisaatiotyyppi) || filteredChildren.nonEmpty) {
        accumulator :+ filteredOrg
      } else {
        accumulator
      }
    }
  }
}
