package fi.oph.kouta.repository

import fi.oph.kouta.domain
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain._
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.modifiedToInstant
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO extends EntityModificationDAO[HakuOid] {
  def getPutActions(haku: Haku): DBIO[Haku]
  def getUpdateActions(haku: Haku): DBIO[Option[Haku]]

  def get(oid: HakuOid, tilaFilter: TilaFilter): Option[(Haku, Instant)]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter, yhteishakuFilter: YhteishakuFilter): Seq[HakuListItem]
  def listByToteutusOid(toteutusOid: ToteutusOid, tilaFilter: TilaFilter): Seq[HakuListItem]
  def listArchivableHakuOids(): Seq[HakuOid]
  def archiveHakusByHakuOids(hakuOids: Seq[HakuOid]): Int
}

object HakuDAO extends HakuDAO with HakuSQL {

  override def getPutActions(haku: Haku): DBIO[Haku] =
    for {
      oid <- insertHaku(haku)
      _ <- insertHakuajat(haku.withOid(oid))
      m <- selectLastModified(oid)
    } yield haku.withOid(oid).withModified(m.get)

  override def get(oid: HakuOid, tilaFilter: TilaFilter): Option[(Haku, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHaku(oid, tilaFilter)
      a <- selectHaunHakuajat(oid)
    } yield (h, a) ).map {
      case (Some(h), a) => Some((
        h.copy(
          hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList),
        modifiedToInstant(h.modified.get)))
      case _ => None
    }.get
  }

  def getUpdateActions(haku: Haku): DBIO[Option[Haku]] =
    for {
      x <- updateHaku(haku)
      y <- updateHaunHakuajat(haku)
      m <- selectLastModified(haku.oid.get)
    } yield optionWhen(x + y > 0)(haku.withModified(m.get))

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter, yhteishakuFilter: YhteishakuFilter): Seq[HakuListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByAllowedOrganisaatiot(organisaatioOids, tilaFilter, yhteishakuFilter))
  }

  override def listByToteutusOid(toteutusOid: ToteutusOid, tilaFilter: TilaFilter): Seq[HakuListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid, tilaFilter))

  override def listArchivableHakuOids(): Seq[HakuOid] = {
    KoutaDatabase.runBlocking(selectArchivableHakuOids())
  }

  override def archiveHakusByHakuOids(hakuOids: Seq[HakuOid]): Int = KoutaDatabase.runBlocking(updateHakusToArchivedByHakuOids(hakuOids))
}

trait HakuModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakuOid]] = {
    sql"""select oid from haut where $since < last_modified
          union
          select oid from haut_history where $since <@ system_time
          union
          select haku_oid from hakujen_hakuajat_history where $since <@ system_time""".as[HakuOid]
  }

  def selectLastModified(oid: HakuOid): DBIO[Option[Instant]] = {
    sql"""select last_modified from haut ha where ha.oid = $oid""".as[Option[Instant]].head
  }
}

sealed trait HakuSQL extends HakuExtractors with HakuModificationSQL with SQLHelpers {

  def insertHaku(haku: Haku): DBIO[HakuOid] = {
    sql"""insert into haut ( external_id,
                             tila,
                             nimi,
                             hakutapa_koodi_uri,
                             hakukohteen_liittamisen_takaraja,
                             hakukohteen_muokkaamisen_takaraja,
                             ajastettu_julkaisu,
                             ajastettu_haun_ja_hakukohteiden_arkistointi,
                             ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu,
                             kohdejoukko_koodi_uri,
                             kohdejoukon_tarkenne_koodi_uri,
                             hakulomaketyyppi,
                             hakulomake_ataru_id,
                             hakulomake_kuvaus,
                             hakulomake_linkki,
                             metadata,
                             organisaatio_oid,
                             muokkaaja,
                             kielivalinta
          ) values ( ${haku.externalId},
                     ${haku.tila.toString}::julkaisutila,
                     ${toJsonParam(haku.nimi)}::jsonb,
                     ${haku.hakutapaKoodiUri},
                     ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp,
                     ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp,
                     ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp,
                     ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointi)}::timestamp,
                     ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointiAjettu)}::timestamp,
                     ${haku.kohdejoukkoKoodiUri},
                     ${haku.kohdejoukonTarkenneKoodiUri},
                     ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
                     ${haku.hakulomakeAtaruId.map(_.toString)}::uuid,
                     ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb,
                     ${toJsonParam(haku.hakulomakeLinkki)}::jsonb,
                     ${toJsonParam(haku.metadata)}::jsonb,
                     ${haku.organisaatioOid},
                     ${haku.muokkaaja},
                     ${toJsonParam(haku.kielivalinta)}::jsonb
          ) returning oid""".as[HakuOid].head
  }

  def insertHakuajat(haku: Haku): DBIO[List[Int]] = {
    DBIO.sequence(
      haku.hakuajat.map(t =>
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
              values (
                ${haku.oid},
                tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                        ${formatTimestampParam(t.paattyy)}::timestamp, '[)'),
                ${haku.muokkaaja})"""))
  }

  def selectHaku(oid: HakuOid, tilaFilter: TilaFilter): DBIO[Option[Haku]] = {
    sql"""select oid, external_id, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja,
                 ajastettu_julkaisu, ajastettu_haun_ja_hakukohteiden_arkistointi, ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri,
                 hakulomaketyyppi, hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, metadata, organisaatio_oid,
                 muokkaaja, kielivalinta, last_modified from haut where oid = $oid
                 #${tilaConditions(tilaFilter)}""".as[Haku].headOption
  }

  def selectHaunHakuajat(oid: HakuOid): DBIO[Vector[Hakuaika]] = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid""".as[Hakuaika]
  }

  def updateHaku(haku: Haku): DBIO[Int] = {
    sqlu"""update haut set
              external_id = ${haku.externalId},
              hakutapa_koodi_uri = ${haku.hakutapaKoodiUri},
              hakukohteen_liittamisen_takaraja = ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp,
              hakukohteen_muokkaamisen_takaraja = ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp,
              ajastettu_julkaisu = ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp,
              ajastettu_haun_ja_hakukohteiden_arkistointi = ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointi)}::timestamp,
              ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu = ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointiAjettu)}::timestamp,
              kohdejoukko_koodi_uri = ${haku.kohdejoukkoKoodiUri},
              kohdejoukon_tarkenne_koodi_uri = ${haku.kohdejoukonTarkenneKoodiUri},
              hakulomaketyyppi = ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake_ataru_id = ${haku.hakulomakeAtaruId.map(_.toString)}::uuid,
              hakulomake_kuvaus = ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb,
              hakulomake_linkki = ${toJsonParam(haku.hakulomakeLinkki)}::jsonb,
              organisaatio_oid = ${haku.organisaatioOid},
              tila = ${haku.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(haku.nimi)}::jsonb,
              metadata = ${toJsonParam(haku.metadata)}::jsonb,
              muokkaaja = ${haku.muokkaaja},
              kielivalinta = ${toJsonParam(haku.kielivalinta)}::jsonb
            where oid = ${haku.oid}
            and ( external_id is distinct from ${haku.externalId}
            or hakutapa_koodi_uri is distinct from ${haku.hakutapaKoodiUri}
            or kohdejoukko_koodi_uri is distinct from ${haku.kohdejoukkoKoodiUri}
            or kohdejoukon_tarkenne_koodi_uri is distinct from ${haku.kohdejoukonTarkenneKoodiUri}
            or hakulomaketyyppi is distinct from ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake_ataru_id is distinct from ${haku.hakulomakeAtaruId.map(_.toString)}::uuid
            or hakulomake_kuvaus is distinct from ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb
            or hakulomake_linkki is distinct from ${toJsonParam(haku.hakulomakeLinkki)}::jsonb
            or hakukohteen_liittamisen_takaraja is distinct from ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp
            or hakukohteen_muokkaamisen_takaraja is distinct from ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp
            or ajastettu_julkaisu is distinct from ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp
            or ajastettu_haun_ja_hakukohteiden_arkistointi is distinct from ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointi)}::timestamp
            or ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu is distinct from ${formatTimestampParam(haku.ajastettuHaunJaHakukohteidenArkistointiAjettu)}::timestamp
            or organisaatio_oid is distinct from ${haku.organisaatioOid}
            or tila is distinct from ${haku.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(haku.nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(haku.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(haku.kielivalinta)}::jsonb)"""
  }

  def insertHakuaika(oid: Option[HakuOid], hakuaika: Ajanjakso, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                     ${formatTimestampParam(hakuaika.paattyy)}::timestamp, '[)'), $muokkaaja)
               on conflict on constraint hakujen_hakuajat_pkey do nothing"""
  }

  def deleteHakuajat(oid: Option[HakuOid], exclude: List[Ajanjakso]): DBIO[Int] = {
    sqlu"""delete from hakujen_hakuajat where haku_oid = $oid and hakuaika not in (#${createRangeInParams(exclude)})"""
  }

  def updateHaunHakuajat(haku: Haku): DBIO[Int] = {
    val (oid, hakuajat, muokkaaja) = (haku.oid, haku.hakuajat, haku.muokkaaja)
    if (hakuajat.nonEmpty) {
      val insertSQL = hakuajat.map(insertHakuaika(oid, _, muokkaaja))
      val deleteSQL = deleteHakuajat(oid, hakuajat)
      DBIOHelpers.sumIntDBIOs(insertSQL :+ deleteSQL)
    } else {
      sqlu"""delete from hakujen_hakuajat where haku_oid = $oid"""
    }
  }

  val selectHakuListSql =
    """select distinct ha.oid, ha.nimi, ha.tila, ha.organisaatio_oid, ha.muokkaaja, ha.last_modified from haut ha"""

  def selectByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter, yf: YhteishakuFilter): DBIO[Vector[HakuListItem]] = {
    def includeYhteishaut = (yf.removeKk, yf.removeToinenaste) match {
      case (true, true) => s"and hakutapa_koodi_uri NOT LIKE 'hakutapa_01%'"
      case (true, false) => s"and kohdejoukko_koodi_uri NOT LIKE 'haunkohdejoukko_12%'"
      case (false, true) => s"and kohdejoukko_koodi_uri NOT LIKE 'haunkohdejoukko_11%'"
      case _ => ""
    }

    sql"""#$selectHakuListSql
          where ha.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          #$includeYhteishaut
          #${tilaConditions(tilaFilter, "ha.tila")}""".as[HakuListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid, tilaFilter: TilaFilter): DBIO[Vector[HakuListItem]] = {
    sql"""#$selectHakuListSql
          inner join hakukohteet on hakukohteet.haku_oid = ha.oid
          inner join toteutukset on toteutukset.oid = hakukohteet.toteutus_oid
          where toteutukset.oid = $toteutusOid
          #${tilaConditions(tilaFilter, "ha.tila")}""".as[HakuListItem]
  }

  def selectArchivableHakuOids(): DBIO[Seq[HakuOid]] = {
    sql"""select oid from haut
          where (ajastettu_haun_ja_hakukohteiden_arkistointi <= now()::date
              and tila = 'julkaistu'
              and ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu is null)
             or (oid in (select haku_oid
                        from hakujen_hakuajat
                        where upper(hakuaika)::date <= now()::date - '10 month'::interval)
              and tila = 'julkaistu'
              and ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu is null)""".as[HakuOid]
  }

  def updateHakusToArchivedByHakuOids(hakuOids: Seq[HakuOid]): DBIO[Int] = {
    sqlu"""update haut set tila = 'arkistoitu', ajastettu_haun_ja_hakukohteiden_arkistointi_ajettu = now()
           where oid in (#${createOidInParams(hakuOids)}) and tila = 'julkaistu'"""
  }
}
