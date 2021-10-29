package fi.oph.kouta.util

import fi.oph.kouta.domain.{Haku, Koulutustyyppi}

object MiscUtils {
  def optionWhen[T](cond: Boolean)(result: => T): Option[T] = if(cond) Some(result) else None
  def isYhteishakuHakutapa(hakutapa: String): Boolean = hakutapa.startsWith("hakutapa_01")
  def optionWhen[T](cond: Boolean)(result: => T): Option[T] = if (cond) Some(result) else None

  def withoutKoodiVersion(koodiUri: String) = koodiUri.split("#").head

  def isToisenAsteenYhteishaku(koulutustyyppi: Koulutustyyppi, haku: Haku) =
    Koulutustyyppi.isToisenAsteenYhteishakuKoulutustyyppi(koulutustyyppi) && isYhteishakuHakutapa(
      haku.hakutapaKoodiUri.get
    );
}
