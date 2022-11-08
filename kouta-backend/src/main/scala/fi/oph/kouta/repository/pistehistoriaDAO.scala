package fi.oph.kouta.repository

import fi.oph.kouta.domain.Pistetieto
import fi.oph.kouta.domain.oid.OrganisaatioOid
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

trait PistehistoriaDAO {
  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): Seq[Pistetieto]
}

object PistehistoriaDAO extends PistetietoSQL {

  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): Seq[Pistetieto] = {
    println("Haetaan pistehistoria organisaatiolle " + tarjoaja)
    KoutaDatabase.runBlocking(selectPistehistoria(tarjoaja, hakukohdekoodi))
  }

}

sealed trait PistetietoSQL extends PistehistoriaExtractors with SQLHelpers {

  def selectPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): DBIO[Vector[Pistetieto]] = {
    val hakukohdekoodiInParam = hakukohdekoodi match {
      case koodi if koodi.startsWith("hakukohteetperusopetuksenjalkeinenyhteishaku") =>
        "'"+hakukohdekoodi+"','"+ koodi.replace("hakukohteetperusopetuksenjalkeinenyhteishaku", "hakukohteet")+"'"
      case _ => "'"+hakukohdekoodi+"'"
    }

    sql"""select tarjoajaOid, hakukohdekoodi, pisteet, vuosi from pistehistoria
          where tarjoajaOid = #${"'"+tarjoaja.toString+"'"}
            and hakukohdekoodi in (#$hakukohdekoodiInParam)""".as[Pistetieto]
  }


}