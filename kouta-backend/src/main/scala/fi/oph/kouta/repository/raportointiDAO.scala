package fi.oph.kouta.repository

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.raportointi.{
  HakukohdeLiiteRaporttiItem,
  HakukohdeRaporttiItem,
  ValintakoeRaporttiItem,
  ValintaperusteRaporttiItem
}
import fi.oph.kouta.domain.{Ajanjakso, Haku, Koulutus, OppilaitoksenOsa, Oppilaitos, Sorakuvaus, Toteutus}
import fi.oph.kouta.repository.HakuDAO.getHakuResult
import fi.oph.kouta.repository.OppilaitoksenOsaDAO.getOppilaitoksenOsaResult
import fi.oph.kouta.repository.OppilaitosDAO.getOppilaitosResult
import fi.oph.kouta.repository.PistehistoriaDAO.getPistehistoriaResult
import fi.oph.kouta.repository.SorakuvausDAO.getSorakuvausResult
import fi.oph.kouta.service.Pistetieto
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

trait RaportointiDAO {
  def listKoulutukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Koulutus]
  def listToteutukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Toteutus]
  def listHakukohteet(startTime: Option[Instant], endTime: Option[Instant]): Seq[HakukohdeRaporttiItem]
  def listHaut(startTime: Option[Instant], endTime: Option[Instant]): Seq[Haku]
  def listValintaperusteet(startTime: Option[Instant], endTime: Option[Instant]): Seq[ValintaperusteRaporttiItem]
  def listSorakuvaukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Sorakuvaus]
  def listOppilaitokset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Oppilaitos]
  def listOppilaitoksenOsat(startTime: Option[Instant], endTime: Option[Instant]): Seq[OppilaitoksenOsa]
  def listPistehistoria(startTime: Option[Instant], endTime: Option[Instant]): Seq[Pistetieto]
  def listAmmattinimikkeet(): Seq[Keyword]
  def listAsiasanat(): Seq[Keyword]
}

object RaportointiDAO extends RaportointiDAO with EntitySQL {
  override def listKoulutukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Koulutus] =
    KoutaDatabase.runBlocking(selectKoulutukset(startTime, endTime))

  override def listToteutukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Toteutus] =
    KoutaDatabase.runBlocking(selectToteutukset(startTime, endTime))

  private def timeLimitsDefined(startTime: Option[Instant], endTime: Option[Instant]) =
    startTime.isDefined || endTime.isDefined

  override def listHakukohteet(startTime: Option[Instant], endTime: Option[Instant]): Seq[HakukohdeRaporttiItem] = {
    KoutaDatabase
      .runBlockingTransactionally(for {
        hakukohteet   <- selectHakukohteet(startTime, endTime)
        hakuajat      <- selectHakukohteidenHakuajat(if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None)
        liitteet      <- selectHakukohteidenLiitteet(if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None)
        valintakokeet <- selectHakukohteidenValintakokeet(if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None)
      } yield (hakukohteet, hakuajat, liitteet, valintakokeet))
      .map { case (hakukohteet, hakuajat, liitteet, valintakokeet) =>
        hakukohteet.map(hk =>
          hk.copy(
            hakuajat = hakuajat.filter(_.oid.toString() == hk.oid.toString()).map(x => Ajanjakso(x.alkaa, x.paattyy)),
            liitteet = liitteet
              .filter(_.hakukohdeOid.toString() == hk.oid.toString()),
            valintakokeet = valintakokeet
              .filter(_.parentOidOrUUID == hk.oid.toString())
          )
        )
      }
      .get
  }

  override def listHaut(startTime: Option[Instant], endTime: Option[Instant]): Seq[Haku] =
    KoutaDatabase
      .runBlockingTransactionally(for {
        haut     <- selectHaut(startTime, endTime)
        hakuajat <- selectHakujenHakuajat(if (timeLimitsDefined(startTime, endTime)) Some(haut) else None)
      } yield (haut, hakuajat))
      .map { case (haut, hakuajat) =>
        haut.map(h =>
          h.copy(hakuajat =
            hakuajat.filter(_.oid.toString() == h.oid.get.toString()).map(x => Ajanjakso(x.alkaa, x.paattyy)).toList
          )
        )
      }
      .get

  override def listValintaperusteet(
      startTime: Option[Instant],
      endTime: Option[Instant]
  ): Seq[ValintaperusteRaporttiItem] =
    KoutaDatabase
      .runBlockingTransactionally(for {
        vp   <- selectValintaperusteet(startTime, endTime)
        vpvk <- selectValintaperusteidenValintakokeet(if (startTime.isDefined) Some(vp) else None)
      } yield (vp, vpvk))
      .map { case (vp, vpvk) =>
        vp.map(v => v.copy(valintakokeet = vpvk.filter(_.parentOidOrUUID.toString() == v.id.toString())))
      }
      .get

  override def listSorakuvaukset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Sorakuvaus] =
    KoutaDatabase.runBlocking(selectSorakuvaukset(startTime, endTime))

  override def listOppilaitokset(startTime: Option[Instant], endTime: Option[Instant]): Seq[Oppilaitos] =
    KoutaDatabase.runBlocking(selectOppilaitokset(startTime, endTime))

  override def listOppilaitoksenOsat(startTime: Option[Instant], endTime: Option[Instant]): Seq[OppilaitoksenOsa] =
    KoutaDatabase.runBlocking(selectOppilaitoksenOsat(startTime, endTime))

  override def listPistehistoria(startTime: Option[Instant], endTime: Option[Instant]): Seq[Pistetieto] =
    KoutaDatabase.runBlocking(selectPistehistoria(startTime, endTime))

  override def listAmmattinimikkeet(): Seq[Keyword] =
    KoutaDatabase.runBlocking(selectAmmattinimikkeet())

  override def listAsiasanat(): Seq[Keyword] =
    KoutaDatabase.runBlocking(selectAsiasanat())
}

sealed trait EntitySQL extends RaportointiExtractors with SQLHelpers {
  def modificationTimeCondition(
      minModificationTime: Option[Instant],
      maxModificationTime: Option[Instant],
      fieldName: String = "last_modified"
  ): String = {
    (minModificationTime, maxModificationTime) match {
      case (Some(minModificationTimeVal), Some(maxModificationTimeVal)) =>
        s"where $fieldName between $minModificationTimeVal and $maxModificationTimeVal"
      case (Some(minModificationTimeVal), None) => s"where $minModificationTimeVal < $fieldName"
      case (None, Some(maxModificationTimeVal)) => s"where $maxModificationTimeVal > $fieldName"
      case _                                    => ""
    }
  }

  def hakukohdeOidInCondition(hakukohteet: Option[Seq[HakukohdeRaporttiItem]]) = {
    hakukohteet match {
      case Some(hakukohteet) => s"where hakukohde_oid in (${createOidInParams(hakukohteet.map(_.oid))})"
      case _                 => ""
    }
  }

  def selectKoulutukset(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[Koulutus]] = {
    val selectPart = s"""select k.oid, k.external_id, k.johtaa_tutkintoon, k.tyyppi, k.koulutukset_koodi_uri, k.tila,
      array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat,
      k.nimi, k.sorakuvaus_id, k.metadata, k.julkinen, k.muokkaaja, k.organisaatio_oid, k.esikatselu,
      k.kielivalinta, k.teemakuva, k.eperuste_id, k.last_modified
      from koulutukset k
      left join koulutusten_tarjoajat kt on k.oid = kt.koulutus_oid"""
    (startTime, endTime) match {
      case (Some(startTimeVal), Some(endTimeVal)) =>
        sql"""#$selectPart where k.last_modified between $startTimeVal and $endTimeVal group by k.oid""".as[Koulutus]
      case (Some(startTimeVal), None) =>
        sql"""#$selectPart where $startTimeVal < k.last_modified group by k.oid""".as[Koulutus]
      case (None, Some(endTimeVal)) =>
        sql"""#$selectPart where $endTimeVal > k.last_modified group by k.oid""".as[Koulutus]
    }
  }

  def selectToteutukset(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[Toteutus]] = {
    sql"""select t.oid, t.external_id, t.koulutus_oid, t.tila,
                array_agg(distinct tt.tarjoaja_oid) as toteutuksen_tarjoajat,
                t.nimi, t.metadata, t.muokkaaja, t.esikatselu, t.organisaatio_oid, t.kielivalinta, t.teemakuva,
                t.sorakuvaus_id, t.last_modified
                from toteutukset t
                left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
                ${modificationTimeCondition(startTime, endTime, "t.last_modified")}
                group by t.oid
         """.as[Toteutus]
  }

  def selectHakukohteet(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[HakukohdeRaporttiItem]] = {
    sql"""select oid, external_id, toteutus_oid, haku_oid, tila, nimi, hakukohde_koodi_uri, hakulomaketyyppi,
                hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, kaytetaan_haun_hakulomaketta,
                jarjestyspaikka_oid, pohjakoulutusvaatimus_koodi_urit, pohjakoulutusvaatimus_tarkenne,
                muu_pohjakoulutusvaatimus_kuvaus, toinen_aste_onko_kaksoistutkinto,
                kaytetaan_haun_aikataulua, valintaperuste_id, liitteet_onko_sama_toimitusaika,
                liitteet_onko_sama_toimitusosoite, liitteiden_toimitusaika,
                liitteiden_toimitustapa, liitteiden_toimitusosoite, esikatselu, metadata, muokkaaja, organisaatio_oid,
                kielivalinta, last_modified
                from hakukohteet
                ${modificationTimeCondition(startTime, endTime)}
         """.as[HakukohdeRaporttiItem]
  }

  def selectHakukohteidenHakuajat(hakukohteet: Option[Seq[HakukohdeRaporttiItem]]): DBIO[Seq[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika)
                from hakukohteiden_hakuajat
                ${hakukohdeOidInCondition(hakukohteet)}
               """.as[Hakuaika]
  }

  def selectHakukohteidenLiitteet(
      hakukohteet: Option[Seq[HakukohdeRaporttiItem]]
  ): DBIO[Seq[HakukohdeLiiteRaporttiItem]] = {
    sql"""select id, hakukohde_oid, tyyppi_koodi_uri, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
            from hakukohteiden_liitteet
            ${hakukohdeOidInCondition(hakukohteet)}
           """.as[HakukohdeLiiteRaporttiItem]
  }

  def selectHakukohteidenValintakokeet(
      hakukohteet: Option[Seq[HakukohdeRaporttiItem]]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    sql"""select id, hakukohde_oid as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
          from hakukohteiden_valintakokeet
            ${hakukohdeOidInCondition(hakukohteet)}
           """.as[ValintakoeRaporttiItem]
  }

  def selectHaut(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[Haku]] = {
    sql"""select oid, external_id, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja,
                 ajastettu_julkaisu, ajastettu_haun_ja_hakukohteiden_arkistointi, ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri,
                 hakulomaketyyppi, hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, metadata, organisaatio_oid,
                 muokkaaja, kielivalinta, last_modified from haut
                ${modificationTimeCondition(startTime, endTime)}
         """.as[Haku]
  }

  def selectHakujenHakuajat(haut: Option[Seq[Haku]]): DBIO[Seq[Hakuaika]] = {
    val hakuOidInCondition = haut match {
      case Some(haut) => s"where haku_oid in (${createOidInParams(haut.map(_.oid.get))})"
      case _          => ""
    }

    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat
                $hakuOidInCondition
          """.as[Hakuaika]
  }

  def selectValintaperusteet(
      startTime: Option[Instant],
      endTime: Option[Instant]
  ): DBIO[Seq[ValintaperusteRaporttiItem]] = {
    sql"""select id, external_id, tila, koulutustyyppi, hakutapa_koodi_uri, kohdejoukko_koodi_uri, nimi, julkinen,
                esikatselu, metadata, organisaatio_oid, muokkaaja, kielivalinta, last_modified
                from valintaperusteet
                ${modificationTimeCondition(startTime, endTime)}
         """.as[ValintaperusteRaporttiItem]
  }

  def selectValintaperusteidenValintakokeet(
      valintaperusteet: Option[Seq[ValintaperusteRaporttiItem]]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    val valintaperusteIdInCondition = valintaperusteet match {
      case Some(valintaperusteet) =>
        s"where valintaperuste_id in (${createUUIDInParams(valintaperusteet.map(_.id))})"
      case _ => ""
    }

    sql"""select id, valintaperuste_id as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
                from valintaperusteiden_valintakokeet
                $valintaperusteIdInCondition
           """.as[ValintakoeRaporttiItem]
  }

  def selectSorakuvaukset(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[Sorakuvaus]] = {
    sql"""select id, external_id, tila, nimi, koulutustyyppi, kielivalinta,
                metadata, organisaatio_oid, muokkaaja, lower(system_time)
                from sorakuvaukset
                ${modificationTimeCondition(startTime, endTime, "lower(system_time)")}
         """.as[Sorakuvaus]
  }

  def selectOppilaitokset(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Seq[Oppilaitos]] = {
    sql"""select oid, tila, kielivalinta, metadata, muokkaaja, esikatselu, organisaatio_oid, teemakuva, logo,
                lower(system_time)
                from oppilaitokset
                ${modificationTimeCondition(startTime, endTime, "lower(system_time)")}
         """.as[Oppilaitos]
  }

  def selectOppilaitoksenOsat(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Vector[OppilaitoksenOsa]] = {
    sql"""select oid, oppilaitos_oid, tila, kielivalinta, metadata, muokkaaja, esikatselu, organisaatio_oid,
                teemakuva, lower(system_time)
                from oppilaitosten_osat
                ${modificationTimeCondition(startTime, endTime, "lower(system_time)")}
         """.as[OppilaitoksenOsa]
  }

  def selectPistehistoria(startTime: Option[Instant], endTime: Option[Instant]): DBIO[Vector[Pistetieto]] = {
    sql"""select tarjoaja_oid, hakukohdekoodi, pisteet, vuosi, valintatapajono_oid, hakukohde_oid, haku_oid,
                valintatapajono_tyyppi
                from pistehistoria
                ${modificationTimeCondition(startTime, endTime, "updated")}
          """.as[Pistetieto]
  }

  def selectAmmattinimikkeet(): DBIO[Seq[Keyword]] = {
    sql"""select distinct ammattinimike, kieli from ammattinimikkeet""".as[Keyword]
  }

  def selectAsiasanat(): DBIO[Seq[Keyword]] = {
    sql"""select distinct asiasana, kieli from asiasanat""".as[Keyword]
  }
}
