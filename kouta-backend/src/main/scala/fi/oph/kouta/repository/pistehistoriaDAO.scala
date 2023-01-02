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
    KoutaDatabase.runBlocking(selectPistehistoria(tarjoaja, hakukohdekoodi))
  }

  def savePistehistorias(pisteet: Seq[Pistetieto]) = {
    if (pisteet.isEmpty) {
      0
    } else {
      logger.info(s"Saving ${pisteet.size} pistetietos, first: ${pisteet.head}")
      KoutaDatabase.runBlocking(persistPistehistoria(pisteet)).toList.sum
    }
  }
}

sealed trait PistetietoSQL extends PistehistoriaExtractors with SQLHelpers {

  def selectPistehistoria(tarjoaja: OrganisaatioOid, hakukohdekoodi: String): DBIO[Vector[Pistetieto]] = {
    val koodiWithoutVersion = hakukohdekoodi.split("#").head
    val hakukohdekoodiInParam = koodiWithoutVersion match {
      case koodi if koodi.startsWith("hakukohteetperusopetuksenjalkeinenyhteishaku") =>
        "'"+koodi+"','"+ koodi.replace("hakukohteetperusopetuksenjalkeinenyhteishaku", "hakukohteet")+"'"
      case koodi => "'"+koodi+"'"
    }

    sql"""select tarjoaja_oid, hakukohdekoodi, pisteet, vuosi, valintatapajono_oid, hakukohde_oid, haku_oid from pistehistoria
          where tarjoaja_oid = #${"'"+tarjoaja.toString+"'"}
            and hakukohdekoodi in (#$hakukohdekoodiInParam)""".as[Pistetieto]
  }

  def persistPistehistoria(pisteet: Seq[Pistetieto]) = {
    DBIO.sequence(
      pisteet.map((pistetieto: Pistetieto) => {
        sqlu"""insert into pistehistoria (tarjoaja_oid, hakukohdekoodi, vuosi, pisteet, valintatapajono_oid, hakukohde_oid, haku_oid)
              values (
                      ${pistetieto.tarjoaja},
                      ${pistetieto.hakukohdekoodi.split('#').head},
                      ${pistetieto.vuosi},
                      ${pistetieto.pisteet},
                      ${pistetieto.valintatapajonoOid},
                      ${pistetieto.hakukohdeOid},
                      ${pistetieto.hakuOid}
                ) on conflict (tarjoaja_oid, hakukohdekoodi, vuosi) do update set pisteet = excluded.pisteet,
                                                                                 valintatapajono_oid = excluded.valintatapajono_oid,
                                                                                 hakukohde_oid = excluded.hakukohde_oid,
                                                                                 haku_oid = excluded.haku_oid,
                                                                                 updated = now()"""
      }))
  }
}