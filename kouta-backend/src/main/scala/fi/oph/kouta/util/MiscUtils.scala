package fi.oph.kouta.util

object MiscUtils {
  def optionWhen[T](cond: Boolean)(result: => T): Option[T] = if(cond) Some(result) else None
  def isYhteishakuHakutapa(hakutapa: String): Boolean = hakutapa.startsWith("hakutapa_01")
}
