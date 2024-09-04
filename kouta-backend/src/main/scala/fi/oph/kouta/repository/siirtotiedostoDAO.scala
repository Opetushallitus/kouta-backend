package fi.oph.kouta.repository

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.siirtotiedosto.{HakuRaporttiItem, HakukohdeLiiteRaporttiItem, HakukohdeRaporttiItem, KoulutusEnrichmentData, KoulutusRaporttiItem, OppilaitoksenOsa, Oppilaitos, OppilaitosOrOsaRaporttiItem, PistetietoRaporttiItem, Siirtotiedosto, SorakuvausRaporttiItem, ToteutusRaporttiItem, ValintakoeRaporttiItem, ValintaperusteRaporttiItem}
import fi.oph.kouta.domain.{Ajanjakso, TilaFilter, Toteutus}
import fi.oph.kouta.domain.oid.{KoulutusOid, ToteutusOid}
import fi.oph.kouta.repository.ToteutusDAO.getToteutusResult
import slick.dbio.{DBIO, DBIOAction}
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global

object SiirtotiedostoDAO
    extends SiirtotiedostoDAO(
      KoutaDatabase
    ) {
  def apply(databaseAccessor: KoutaDatabaseAccessor): SiirtotiedostoDAO =
    new SiirtotiedostoDAO(databaseAccessor)
}

object SiirtotiedostoRaportointiDAO extends SiirtotiedostoDAO(SimpleDatabaseAccessor)

class SiirtotiedostoDAO(
    databaseAccessor: KoutaDatabaseAccessor
) extends EntitySQL {
  def listKoulutukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[KoulutusRaporttiItem] =
    databaseAccessor.runBlocking(selectKoulutukset(startTime, endTime, limit, offset))

  def listKoulutusEnrichmentDataItems(oids: Seq[KoulutusOid]): Seq[KoulutusEnrichmentData] =
    databaseAccessor.runBlocking(selectKoulutusEnrichmentDataItems(oids))

  def listToteutukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[ToteutusRaporttiItem] =
    databaseAccessor.runBlocking(selectToteutukset(startTime, endTime, limit, offset))

  def getSingleTotetutus(oid: ToteutusOid, tilaFilter: TilaFilter): Option[Toteutus] =
    databaseAccessor.runBlocking(selectToteutus(oid, tilaFilter)).headOption

  def listHakukohteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[HakukohdeRaporttiItem] = {
    databaseAccessor
      .runBlockingTransactionally(for {
        hakukohteet   <- selectHakukohteet(startTime, endTime, limit, offset)
        hakuajat      <- selectHakukohteidenHakuajat(hakukohteet)
        liitteet      <- selectHakukohteidenLiitteet(hakukohteet)
        valintakokeet <- selectHakukohteidenValintakokeet(hakukohteet)
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

  def listHaut(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[HakuRaporttiItem] =
    databaseAccessor
      .runBlockingTransactionally(for {
        haut     <- selectHaut(startTime, endTime, limit, offset)
        hakuajat <- selectHakujenHakuajat(haut)
      } yield (haut, hakuajat))
      .map { case (haut, hakuajat) =>
        haut.map(h =>
          h.copy(hakuajat =
            hakuajat.filter(_.oid.toString() == h.oid.toString()).map(x => Ajanjakso(x.alkaa, x.paattyy)).toList
          )
        )
      }
      .get

  def listValintaperusteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[ValintaperusteRaporttiItem] =
    databaseAccessor
      .runBlockingTransactionally(for {
        vp   <- selectValintaperusteet(startTime, endTime, limit, offset)
        vpvk <- selectValintaperusteidenValintakokeet(vp)
      } yield (vp, vpvk))
      .map { case (vp, vpvk) =>
        vp.map(v => v.copy(valintakokeet = vpvk.filter(_.parentOidOrUUID == v.id.toString())))
      }
      .get

  def listSorakuvaukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[SorakuvausRaporttiItem] =
    databaseAccessor.runBlocking(selectSorakuvaukset(startTime, endTime, limit, offset))

  def listOppilaitokset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[OppilaitosOrOsaRaporttiItem] =
    databaseAccessor.runBlocking(selectOppilaitokset(startTime, endTime, limit, offset))

  def listOppilaitostenOsat(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[OppilaitosOrOsaRaporttiItem] =
    databaseAccessor.runBlocking(selectOppilaitoksenOsat(startTime, endTime, limit, offset))

  def listPistehistoria(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[PistetietoRaporttiItem] =
    databaseAccessor.runBlocking(selectPistehistoria(startTime, endTime, limit, offset))

  def listAmmattinimikkeet(
      _start: Option[LocalDateTime],
      _end: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[Keyword] =
    databaseAccessor.runBlocking(selectAmmattinimikkeet(limit, offset))

  def listAsiasanat(
      _start: Option[LocalDateTime],
      _end: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): Seq[Keyword] =
    databaseAccessor.runBlocking(selectAsiasanat(limit, offset))

  def findLatestSiirtotiedostoData(): Option[Siirtotiedosto] =
    databaseAccessor.runBlocking(selectLatestSiirtotiedostoData()).headOption

  def saveSiirtotiedostoData(siirtotiedosto: Siirtotiedosto) =
    databaseAccessor.runBlocking(persistSiirtotiedostoData(siirtotiedosto))
}

sealed trait EntitySQL extends SiirtotiedostoExtractors with SQLHelpers {
  private def selectByTimerange(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      selectPart: String,
      timeField: String,
      limit: Int,
      offset: Int,
      groupBy: String = ""
  ) = {
    val groupByStr = if (!groupBy.isEmpty) s" group by $groupBy" else ""
    (startTime, endTime) match {
      case (Some(startTimeVal), Some(endTimeVal)) =>
        sql"""#$selectPart where #$timeField >= $startTimeVal and #$timeField < $endTimeVal#$groupByStr
             limit #$limit offset #$offset"""
      case (Some(startTimeVal), None) =>
        sql"""#$selectPart where $startTimeVal < #$timeField#$groupByStr limit #$limit offset #$offset"""
      case (None, Some(endTimeVal)) =>
        sql"""#$selectPart where $endTimeVal > #$timeField#$groupByStr limit #$limit offset #$offset"""
      case (_, _) => sql"""#$selectPart#$groupByStr limit #$limit offset #$offset"""
    }
  }

  def selectKoulutukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[KoulutusRaporttiItem]] = {
    val selectPart = s"""select k.oid, k.external_id, k.johtaa_tutkintoon, k.tyyppi, k.koulutukset_koodi_uri, k.tila,
      array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat,
      k.nimi, k.sorakuvaus_id, k.metadata, k.julkinen, k.muokkaaja, k.organisaatio_oid, k.esikatselu,
      k.kielivalinta, k.teemakuva, k.eperuste_id, k.last_modified
      from koulutukset k
      left join koulutusten_tarjoajat kt on k.oid = kt.koulutus_oid"""
    selectByTimerange(startTime, endTime, selectPart, "k.last_modified", limit, offset, "k.oid")
      .as[KoulutusRaporttiItem]
  }

  def selectKoulutusEnrichmentDataItems(oids: Seq[KoulutusOid]): DBIO[Seq[KoulutusEnrichmentData]] = {
    sql"""select k.oid, array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat, k.metadata
         from koulutukset k
         left join koulutusten_tarjoajat kt on k.oid = kt.koulutus_oid
         where k.oid in (#${createOidInParams(oids)})
         group by k.oid
       """.as[KoulutusEnrichmentData]
  }

  def selectToteutukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[ToteutusRaporttiItem]] = {
    val selectPart = s"""select t.oid, t.external_id, t.koulutus_oid, t.tila,
                        array_agg(distinct tt.tarjoaja_oid) as toteutuksen_tarjoajat,
                        t.nimi, t.metadata, t.muokkaaja, t.esikatselu, t.organisaatio_oid, t.kielivalinta, t.teemakuva,
                        t.sorakuvaus_id, t.last_modified
                        from toteutukset t
                        left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid"""
    selectByTimerange(startTime, endTime, selectPart, "t.last_modified", limit, offset, "t.oid")
      .as[ToteutusRaporttiItem]
  }

  def selectToteutus(oid: ToteutusOid, tilaFilter: TilaFilter): DBIO[Seq[Toteutus]] =
    sql"""select oid, external_id, koulutus_oid, tila, nimi, metadata, muokkaaja, esikatselu, organisaatio_oid,
              kielivalinta, teemakuva, sorakuvaus_id, last_modified, null, null from toteutukset
          where oid = $oid #${tilaConditions(tilaFilter)}""".as[Toteutus]

  def selectHakukohteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[HakukohdeRaporttiItem]] = {
    val selectPart =
      s"""select oid, external_id, toteutus_oid, haku_oid, tila, nimi, hakukohde_koodi_uri, hakulomaketyyppi,
                hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, kaytetaan_haun_hakulomaketta,
                jarjestyspaikka_oid, pohjakoulutusvaatimus_koodi_urit, pohjakoulutusvaatimus_tarkenne,
                muu_pohjakoulutusvaatimus_kuvaus, toinen_aste_onko_kaksoistutkinto,
                kaytetaan_haun_aikataulua, valintaperuste_id, liitteet_onko_sama_toimitusaika,
                liitteet_onko_sama_toimitusosoite, liitteiden_toimitusaika,
                liitteiden_toimitustapa, liitteiden_toimitusosoite, esikatselu, metadata, muokkaaja, organisaatio_oid,
                kielivalinta, last_modified
                from hakukohteet"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified", limit, offset).as[HakukohdeRaporttiItem]
  }

  def selectHakukohteidenHakuajat(hakukohteet: Seq[HakukohdeRaporttiItem]): DBIO[Seq[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika)
                from hakukohteiden_hakuajat
                where hakukohde_oid in (#${createOidInParams(hakukohteet.map(_.oid))})
               """.as[Hakuaika]
  }

  def selectHakukohteidenLiitteet(
      hakukohteet: Seq[HakukohdeRaporttiItem]
  ): DBIO[Seq[HakukohdeLiiteRaporttiItem]] = {
    sql"""select id, hakukohde_oid, tyyppi_koodi_uri, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
            from hakukohteiden_liitteet
            where hakukohde_oid in (#${createOidInParams(hakukohteet.map(_.oid))})
           """.as[HakukohdeLiiteRaporttiItem]
  }

  def selectHakukohteidenValintakokeet(
      hakukohteet: Seq[HakukohdeRaporttiItem]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    sql"""select id, hakukohde_oid as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
          from hakukohteiden_valintakokeet
            where hakukohde_oid in (#${createOidInParams(hakukohteet.map(_.oid))})
           """.as[ValintakoeRaporttiItem]
  }

  def selectHaut(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[HakuRaporttiItem]] = {
    val selectPart = s"""select oid, external_id, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja,
                    hakukohteen_muokkaamisen_takaraja, hakukohteen_liittaja_organisaatiot, ajastettu_julkaisu,
                    ajastettu_haun_ja_hakukohteiden_arkistointi, ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
                    kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, hakulomaketyyppi, hakulomake_ataru_id,
                    hakulomake_kuvaus, hakulomake_linkki, metadata, organisaatio_oid, muokkaaja, kielivalinta,
                    last_modified from haut"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified", limit, offset).as[HakuRaporttiItem]
  }

  def selectHakujenHakuajat(haut: Seq[HakuRaporttiItem]): DBIO[Seq[Hakuaika]] = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat
          where haku_oid in (#${createOidInParams(haut.map(_.oid))})
          """.as[Hakuaika]
  }

  def selectValintaperusteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[ValintaperusteRaporttiItem]] = {
    val selectPart = """select id, external_id, tila, koulutustyyppi, hakutapa_koodi_uri, kohdejoukko_koodi_uri, nimi,
                    julkinen, esikatselu, metadata, organisaatio_oid, muokkaaja, kielivalinta, last_modified
                    from valintaperusteet"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified", limit, offset).as[ValintaperusteRaporttiItem]
  }

  def selectValintaperusteidenValintakokeet(
      valintaperusteet: Seq[ValintaperusteRaporttiItem]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    if (valintaperusteet.nonEmpty) {
      sql"""select id, valintaperuste_id as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
                from valintaperusteiden_valintakokeet
                where valintaperuste_id in (#${createUUIDInParams(valintaperusteet.map(_.id))})
           """.as[ValintakoeRaporttiItem]

    } else DBIOAction.successful[Seq[ValintakoeRaporttiItem]](Seq())
  }

  def selectSorakuvaukset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[SorakuvausRaporttiItem]] = {
    val selectPart = """select id, external_id, tila, nimi, koulutustyyppi, kielivalinta,
                       metadata, organisaatio_oid, muokkaaja, lower(system_time) from sorakuvaukset"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)", limit, offset).as[SorakuvausRaporttiItem]
  }

  def selectOppilaitokset(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[OppilaitosOrOsaRaporttiItem]] = {
    val selectPart =
      s"""select oid, '${Oppilaitos.name}' as tyyppi, null, tila, esikatselu, metadata, kielivalinta, organisaatio_oid, muokkaaja, teemakuva,
                    logo, lower(system_time) from oppilaitokset"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)", limit, offset)
      .as[OppilaitosOrOsaRaporttiItem]
  }

  def selectOppilaitoksenOsat(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Seq[OppilaitosOrOsaRaporttiItem]] = {
    val selectPart =
      s"""select oid, '${OppilaitoksenOsa.name}' as tyyppi, oppilaitos_oid, tila, esikatselu, metadata, kielivalinta, organisaatio_oid, muokkaaja,
                    teemakuva, null, lower(system_time) from oppilaitosten_osat"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)", limit, offset)
      .as[OppilaitosOrOsaRaporttiItem]
  }

  def selectPistehistoria(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      limit: Int,
      offset: Int
  ): DBIO[Vector[PistetietoRaporttiItem]] = {
    val selectPart =
      """select tarjoaja_oid, hakukohdekoodi, pisteet, vuosi, valintatapajono_oid, hakukohde_oid, haku_oid,
                    valintatapajono_tyyppi, aloituspaikat, ensisijaisesti_hakeneet, updated from pistehistoria"""
    selectByTimerange(startTime, endTime, selectPart, "updated", limit, offset).as[PistetietoRaporttiItem]
  }

  def selectAmmattinimikkeet(limit: Int, offset: Int): DBIO[Seq[Keyword]] = {
    sql"""select distinct kieli, ammattinimike from ammattinimikkeet order by kieli
         limit #$limit offset #$offset""".as[Keyword]
  }

  def selectAsiasanat(limit: Int, offset: Int): DBIO[Seq[Keyword]] = {
    sql"""select distinct kieli, asiasana from asiasanat order by kieli limit #$limit offset #$offset""".as[Keyword]
  }

  def selectLatestSiirtotiedostoData(): DBIO[Seq[Siirtotiedosto]] =
    sql"""select id,  window_start, window_end, run_start, run_end, info, success, error_message
          from siirtotiedostot where run_start = (select max(run_start) from siirtotiedostot)
       """.as[Siirtotiedosto]

  def persistSiirtotiedostoData(siirtotiedosto: Siirtotiedosto): DBIO[Int] =
    sqlu"""insert into siirtotiedostot
          (id,  window_start, window_end, run_start, run_end, info, success, error_message)
          values
            (${siirtotiedosto.id.toString}::uuid,
            ${siirtotiedosto.windowStart},
            ${siirtotiedosto.windowEnd},
            ${formatTimestampParam(Some(siirtotiedosto.runStart))}::timestamp,
            ${formatTimestampParam(siirtotiedosto.runEnd)}::timestamp,
            ${toJsonParam(siirtotiedosto.info)}::jsonb,
            ${siirtotiedosto.success},
            ${siirtotiedosto.errorMessage}
          ) on conflict on constraint siirtotiedostot_pkey do update set window_start = excluded.window_start,
                                                                         window_end = excluded.window_end,
                                                                         run_start = excluded.run_start,
                                                                         run_end = excluded.run_end,
                                                                         info = excluded.info,
                                                                         success = excluded.success,
                                                                         error_message = excluded.error_message"""

}
