package fi.oph.kouta.repository

import fi.oph.kouta.domain.Komo

import slick.jdbc.PostgresProfile.api._

object KomoDAO {

  private implicit val getResult = Komo.extractor

  def put(komo:Komo) = {
    KoutaDatabase.runBlocking(
      sqlu"""insert into komo (oid, koulutus, nimi)
             values (${komo.oid}, ${komo.koulutus}, ${komo.nimi})""")
  }

  def get(oid:String): Option[Komo] = {
    KoutaDatabase.runBlocking(
      sql"""select oid, koulutus, nimi from komo where oid=$oid"""
        .as[Komo].headOption)
  }

}
