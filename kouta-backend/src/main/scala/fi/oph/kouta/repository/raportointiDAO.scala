package fi.oph.kouta.repository

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.raportointi.{HakuRaporttiItem, HakukohdeLiiteRaporttiItem, HakukohdeRaporttiItem, KoulutusEnrichmentData, KoulutusRaporttiItem, OppilaitoksenOsa, Oppilaitos, OppilaitosOrOsaRaporttiItem, Organisaatiotyyppi, PistetietoRaporttiItem, SorakuvausRaporttiItem, ToteutusRaporttiItem, ValintakoeRaporttiItem, ValintaperusteRaporttiItem}
import fi.oph.kouta.domain.Ajanjakso
import fi.oph.kouta.domain.oid.KoulutusOid
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

trait RaportointiDAO {
  def listKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[KoulutusRaporttiItem]
  def listKoulutusEnrichmentDataItems(oids: Seq[KoulutusOid]): Seq[KoulutusEnrichmentData]
  def listToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[ToteutusRaporttiItem]
  def listHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[HakukohdeRaporttiItem]
  def listHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[HakuRaporttiItem]
  def listValintaperusteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[ValintaperusteRaporttiItem]
  def listSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[SorakuvausRaporttiItem]
  def listOppilaitoksetAndOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[OppilaitosOrOsaRaporttiItem]
  def listPistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[PistetietoRaporttiItem]
  def listAmmattinimikkeet(): Seq[Keyword]
  def listAsiasanat(): Seq[Keyword]
}

object RaportointiDAO extends RaportointiDAO with EntitySQL {
  override def listKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[KoulutusRaporttiItem] =
    KoutaDatabase.runBlocking(selectKoulutukset(startTime, endTime))

  override def listKoulutusEnrichmentDataItems(oids: Seq[KoulutusOid]): Seq[KoulutusEnrichmentData] = Seq()
  override def listToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[ToteutusRaporttiItem] =
    KoutaDatabase.runBlocking(selectToteutukset(startTime, endTime))

  private def timeLimitsDefined(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]) =
    startTime.isDefined || endTime.isDefined

  override def listHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[HakukohdeRaporttiItem] = {
    KoutaDatabase
      .runBlockingTransactionally(for {
        hakukohteet <- selectHakukohteet(startTime, endTime)
        hakuajat    <- selectHakukohteidenHakuajat(if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None)
        liitteet    <- selectHakukohteidenLiitteet(if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None)
        valintakokeet <- selectHakukohteidenValintakokeet(
          if (timeLimitsDefined(startTime, endTime)) Some(hakukohteet) else None
        )
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

  override def listHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[HakuRaporttiItem] =
    KoutaDatabase
      .runBlockingTransactionally(for {
        haut     <- selectHaut(startTime, endTime)
        hakuajat <- selectHakujenHakuajat(if (timeLimitsDefined(startTime, endTime)) Some(haut) else None)
      } yield (haut, hakuajat))
      .map { case (haut, hakuajat) =>
        haut.map(h =>
          h.copy(hakuajat =
            hakuajat.filter(_.oid.toString() == h.oid.toString()).map(x => Ajanjakso(x.alkaa, x.paattyy)).toList
          )
        )
      }
      .get

  override def listValintaperusteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): Seq[ValintaperusteRaporttiItem] =
    KoutaDatabase
      .runBlockingTransactionally(for {
        vp   <- selectValintaperusteet(startTime, endTime)
        vpvk <- selectValintaperusteidenValintakokeet(if (startTime.isDefined) Some(vp) else None)
      } yield (vp, vpvk))
      .map { case (vp, vpvk) =>
        vp.map(v => v.copy(valintakokeet = vpvk.filter(_.parentOidOrUUID == v.id.toString())))
      }
      .get

  override def listSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[SorakuvausRaporttiItem] =
    KoutaDatabase.runBlocking(selectSorakuvaukset(startTime, endTime))

  override def listOppilaitoksetAndOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[OppilaitosOrOsaRaporttiItem] = {
    val oppilaitokset = KoutaDatabase.runBlocking(selectOppilaitokset(startTime, endTime)).toList
    val osat = KoutaDatabase.runBlocking(selectOppilaitoksenOsat(startTime, endTime)).toList
    oppilaitokset ++ osat
  }

  override def listPistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): Seq[PistetietoRaporttiItem] =
    KoutaDatabase.runBlocking(selectPistehistoria(startTime, endTime))

  override def listAmmattinimikkeet(): Seq[Keyword] =
    KoutaDatabase.runBlocking(selectAmmattinimikkeet())

  override def listAsiasanat(): Seq[Keyword] =
    KoutaDatabase.runBlocking(selectAsiasanat())
}

sealed trait EntitySQL extends RaportointiExtractors with SQLHelpers {
  def modificationTimeCondition(
      minModificationTime: Option[LocalDateTime],
      maxModificationTime: Option[LocalDateTime],
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

  private def hakukohdeOidInCondition(hakukohteet: Option[Seq[HakukohdeRaporttiItem]]) = {
    hakukohteet match {
      case Some(hakukohteet) => s"where hakukohde_oid in (${createOidInParams(hakukohteet.map(_.oid))})"
      case _                 => ""
    }
  }

  private def selectByTimerange(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime], selectPart: String, timeField: String, groupBy: String = "") = {
    val groupByStr = if (!groupBy.isEmpty) s" group by $groupBy" else ""
    (startTime, endTime) match {
      case (Some(startTimeVal), Some(endTimeVal)) =>
        sql"""#$selectPart where #$timeField between $startTimeVal and $endTimeVal#$groupByStr"""
      case (Some(startTimeVal), None) =>
        sql"""#$selectPart where $startTimeVal < #$timeField#$groupByStr"""
      case (None, Some(endTimeVal)) =>
        sql"""#$selectPart where $endTimeVal > #$timeField#$groupByStr"""
      case (_, _ ) => sql"""#$selectPart#$groupByStr"""
    }
  }

  def selectKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[KoulutusRaporttiItem]] = {
    val selectPart = s"""select k.oid, k.external_id, k.johtaa_tutkintoon, k.tyyppi, k.koulutukset_koodi_uri, k.tila,
      array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat,
      k.nimi, k.sorakuvaus_id, k.metadata, k.julkinen, k.muokkaaja, k.organisaatio_oid, k.esikatselu,
      k.kielivalinta, k.teemakuva, k.eperuste_id, k.last_modified
      from koulutukset k
      left join koulutusten_tarjoajat kt on k.oid = kt.koulutus_oid"""
    selectByTimerange(startTime, endTime, selectPart, "k.last_modified", "k.oid").as[KoulutusRaporttiItem]
  }

  def selectKoulutusEnrichmentDataItems(oids: Seq[KoulutusOid]): DBIO[Seq[KoulutusEnrichmentData]] = {
    sql"""select oid, array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat, metadata
         from koulutukset where oid in (#${createOidInParams(oids)})
       """.as[KoulutusEnrichmentData]
  }

  def selectToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[ToteutusRaporttiItem]] = {
    val selectPart = s"""select t.oid, t.external_id, t.koulutus_oid, t.tila,
                        array_agg(distinct tt.tarjoaja_oid) as toteutuksen_tarjoajat,
                        t.nimi, t.metadata, t.muokkaaja, t.esikatselu, t.organisaatio_oid, t.kielivalinta, t.teemakuva,
                        t.sorakuvaus_id, t.last_modified
                        from toteutukset t
                        left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid"""
    selectByTimerange(startTime, endTime, selectPart, "t.last_modified", "t.oid").as[ToteutusRaporttiItem]
  }

  def selectHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[HakukohdeRaporttiItem]] = {
    val selectPart = s"""select oid, external_id, toteutus_oid, haku_oid, tila, nimi, hakukohde_koodi_uri, hakulomaketyyppi,
                hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, kaytetaan_haun_hakulomaketta,
                jarjestyspaikka_oid, pohjakoulutusvaatimus_koodi_urit, pohjakoulutusvaatimus_tarkenne,
                muu_pohjakoulutusvaatimus_kuvaus, toinen_aste_onko_kaksoistutkinto,
                kaytetaan_haun_aikataulua, valintaperuste_id, liitteet_onko_sama_toimitusaika,
                liitteet_onko_sama_toimitusosoite, liitteiden_toimitusaika,
                liitteiden_toimitustapa, liitteiden_toimitusosoite, esikatselu, metadata, muokkaaja, organisaatio_oid,
                kielivalinta, last_modified
                from hakukohteet"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified").as[HakukohdeRaporttiItem]
  }

  def selectHakukohteidenHakuajat(hakukohteet: Option[Seq[HakukohdeRaporttiItem]]): DBIO[Seq[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika)
                from hakukohteiden_hakuajat
                #${hakukohdeOidInCondition(hakukohteet)}
               """.as[Hakuaika]
  }

  def selectHakukohteidenLiitteet(
      hakukohteet: Option[Seq[HakukohdeRaporttiItem]]
  ): DBIO[Seq[HakukohdeLiiteRaporttiItem]] = {
    sql"""select id, hakukohde_oid, tyyppi_koodi_uri, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
            from hakukohteiden_liitteet
            #${hakukohdeOidInCondition(hakukohteet)}
           """.as[HakukohdeLiiteRaporttiItem]
  }

  def selectHakukohteidenValintakokeet(
      hakukohteet: Option[Seq[HakukohdeRaporttiItem]]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    sql"""select id, hakukohde_oid as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
          from hakukohteiden_valintakokeet
            #${hakukohdeOidInCondition(hakukohteet)}
           """.as[ValintakoeRaporttiItem]
  }

  def selectHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[HakuRaporttiItem]] = {
    val selectPart = s"""select oid, external_id, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja,
                    hakukohteen_muokkaamisen_takaraja, ajastettu_julkaisu, ajastettu_haun_ja_hakukohteiden_arkistointi,
                    ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu, kohdejoukko_koodi_uri,
                    kohdejoukon_tarkenne_koodi_uri, hakulomaketyyppi, hakulomake_ataru_id, hakulomake_kuvaus,
                    hakulomake_linkki, metadata, organisaatio_oid, muokkaaja, kielivalinta, last_modified from haut"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified").as[HakuRaporttiItem]
  }

  def selectHakujenHakuajat(haut: Option[Seq[HakuRaporttiItem]]): DBIO[Seq[Hakuaika]] = {
    val hakuOidInCondition = haut match {
      case Some(haut) => s"where haku_oid in (${createOidInParams(haut.map(_.oid))})"
      case _          => ""
    }

    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat
                #$hakuOidInCondition
          """.as[Hakuaika]
  }

  def selectValintaperusteet(
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): DBIO[Seq[ValintaperusteRaporttiItem]] = {
    val selectPart = """select id, external_id, tila, koulutustyyppi, hakutapa_koodi_uri, kohdejoukko_koodi_uri, nimi,
                    julkinen, esikatselu, metadata, organisaatio_oid, muokkaaja, kielivalinta, last_modified
                    from valintaperusteet"""
    selectByTimerange(startTime, endTime, selectPart, "last_modified").as[ValintaperusteRaporttiItem]
  }

  def selectValintaperusteidenValintakokeet(
      valintaperusteet: Option[Seq[ValintaperusteRaporttiItem]]
  ): DBIO[Seq[ValintakoeRaporttiItem]] = {
    val valintaperusteIdInCondition = valintaperusteet match {
      case Some(valintaperusteet) =>
        s"""where valintaperuste_id in (${createUUIDInParams(valintaperusteet.map(_.id))})"""
      case _ => ""
    }

    sql"""select id, valintaperuste_id as parentOidOrUUID, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja
                from valintaperusteiden_valintakokeet
                #$valintaperusteIdInCondition
           """.as[ValintakoeRaporttiItem]
  }

  def selectSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[SorakuvausRaporttiItem]] = {
    val selectPart = """select id, external_id, tila, nimi, koulutustyyppi, kielivalinta,
                       metadata, organisaatio_oid, muokkaaja, lower(system_time) from sorakuvaukset"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)").as[SorakuvausRaporttiItem]
  }

  def selectOppilaitokset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[OppilaitosOrOsaRaporttiItem]] = {
    val selectPart = s"""select oid, '${Oppilaitos.name}' as tyyppi, null, tila, esikatselu, metadata, kielivalinta, organisaatio_oid, muokkaaja, teemakuva,
                    logo, lower(system_time) from oppilaitokset"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)").as[OppilaitosOrOsaRaporttiItem]
  }

  def selectOppilaitoksenOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Seq[OppilaitosOrOsaRaporttiItem]] = {
    val selectPart = s"""select oid, '${OppilaitoksenOsa.name}' as tyyppi, oppilaitos_oid, tila, esikatselu, metadata, kielivalinta, organisaatio_oid, muokkaaja,
                    teemakuva, null, lower(system_time) from oppilaitosten_osat"""
    selectByTimerange(startTime, endTime, selectPart, "lower(system_time)").as[OppilaitosOrOsaRaporttiItem]
  }

  def selectPistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]): DBIO[Vector[PistetietoRaporttiItem]] = {
    val selectPart = """select tarjoaja_oid, hakukohdekoodi, pisteet, vuosi, valintatapajono_oid, hakukohde_oid, haku_oid,
                    valintatapajono_tyyppi, aloituspaikat, ensisijaisesti_hakeneet from pistehistoria"""
    selectByTimerange(startTime, endTime, selectPart, "updated").as[PistetietoRaporttiItem]
  }

  def selectAmmattinimikkeet(): DBIO[Seq[Keyword]] = {
    sql"""select distinct kieli, ammattinimike from ammattinimikkeet order by kieli""".as[Keyword]
  }

  def selectAsiasanat(): DBIO[Seq[Keyword]] = {
    sql"""select distinct kieli, asiasana from asiasanat order by kieli""".as[Keyword]
  }
}
