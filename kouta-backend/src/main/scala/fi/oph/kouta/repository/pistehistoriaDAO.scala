package fi.oph.kouta.repository

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.Pistetieto
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

trait PistehistoriaDAO {
  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): Seq[Pistetieto]
  def savePistehistorias(pisteet: Seq[Pistetieto]): Int
}

object PistehistoriaDAO extends PistetietoSQL {

  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): Seq[Pistetieto] = {
    println("Haetaan pistehistoria organisaatiolle " + tarjoaja)
    KoutaDatabase.runBlocking(selectPistehistoria(tarjoaja, hakukohdekoodi))
  }

  def savePistehistorias(pisteet: Seq[Pistetieto]) = {
    logger.info(s"Saving ${pisteet.size} pistetietos, first: ${pisteet.head}")
    KoutaDatabase.runBlocking(persistPistehistoria(pisteet)).toList.sum
  }

}

sealed trait PistetietoSQL extends PistehistoriaExtractors with SQLHelpers {

  def selectPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): DBIO[Vector[Pistetieto]] = {
    val hakukohdekoodiInParam = hakukohdekoodi match {
      case koodi if koodi.startsWith("hakukohteetperusopetuksenjalkeinenyhteishaku") =>
        "'"+hakukohdekoodi+"','"+ koodi.replace("hakukohteetperusopetuksenjalkeinenyhteishaku", "hakukohteet")+"'"
      case _ => "'"+hakukohdekoodi+"'"
    }

    sql"""select tarjoajaOid, hakukohdekoodi, pisteet, vuosi, valintatapajonooid, hakukohdeoid, hakuoid from pistehistoria
          where tarjoajaOid = #${"'"+tarjoaja.toString+"'"}
            and hakukohdekoodi in (#$hakukohdekoodiInParam)""".as[Pistetieto]
  }

  def persistPistehistoria(pisteet: Seq[Pistetieto]) = {
    DBIO.sequence(
      pisteet.map((pistetieto: Pistetieto) => {
        sqlu"""insert into pistehistoria (tarjoajaOid, hakukohdekoodi, vuosi, pisteet, valintatapajonooid, hakukohdeoid, hakuoid)
              values (
                      ${pistetieto.tarjoaja},
                      ${pistetieto.hakukohdekoodi},
                      ${pistetieto.vuosi},
                      ${pistetieto.pisteet},
                      ${pistetieto.valintatapajonoOid},
                      ${pistetieto.hakukohdeOid},
                      ${pistetieto.hakuOid}
                ) on conflict (tarjoajaOid, hakukohdekoodi, vuosi) do update set pisteet = excluded.pisteet,
                                                                                 valintatapajonooid = excluded.valintatapajonooid,
                                                                                 hakukohdeoid = excluded.hakukohdeoid,
                                                                                 hakuoid = excluded.hakuoid,
                                                                                 updated = now()"""
      }))
  }
}