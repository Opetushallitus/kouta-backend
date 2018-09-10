package fi.oph.kouta.domain

import slick.jdbc.GetResult

case class Komo(oid:String, koulutus:String, nimi:Option[String])

object Komo {
  val extractor =
    GetResult(r => Komo(
      oid = r.nextString,
      koulutus = r.nextString,
      nimi = r.nextStringOption))
}


