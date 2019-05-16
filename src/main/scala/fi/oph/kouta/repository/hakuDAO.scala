package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Ajanjakso, Haku, HakuListItem}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO extends EntityModificationDAO[HakuOid] {
  def getPutActions(haku: Haku): DBIO[HakuOid]
  def getUpdateActions(haku: Haku, notModifiedSince: Instant): DBIO[Boolean]

  def put(haku: Haku): HakuOid
  def get(oid: HakuOid): Option[(Haku, Instant)]
  def update(haku: Haku, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakuListItem]
  def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakuListItem]
}
  
object HakuDAO extends HakuDAO with HakuSQL {

  override def getPutActions(haku: Haku): DBIO[HakuOid] =
    for {
      oid <- insertHaku(haku)
      _   <- insertHakuajat(haku.copy(oid = Some(oid)))
    } yield oid

  override def put(haku: Haku): HakuOid =
    KoutaDatabase.runBlockingTransactionally(getPutActions(haku)).get

  override def get(oid: HakuOid): Option[(Haku, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHaku(oid).as[Haku].headOption
      a <- selectHaunHakuajat(oid).as[Hakuaika]
      l <- selectLastModified(oid)
    } yield (h, a, l) ).map {
      case (Some(h), a, Some(l)) => Some((h.copy(hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList), l))
      case _ => None
    }.get
  }

  def getUpdateActions(haku: Haku, notModifiedSince: Instant): DBIO[Boolean] =
    checkNotModified(haku.oid.get, notModifiedSince).andThen(
      for {
        x <- updateHaku(haku)
        y <- updateHaunHakuajat(haku)
      } yield 0 < (x + y.sum)
    )

  override def update(haku: Haku, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(haku, notModifiedSince)).get

  override def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakuListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))

  override def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakuListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid))
}

trait HakuModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakuOid]] = {
    sql"""select oid from haut where ${since} < lower(system_time)
          union
          select oid from haut_history where ${since} <@ system_time
          union
          select haku_oid from hakujen_hakuajat where ${since} < lower(system_time)
          union
          select haku_oid from hakujen_hakuajat_history where ${since} <@ system_time""".as[HakuOid]
  }

  def selectLastModified(oid: HakuOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(ha.system_time)),
            max(lower(hah.system_time)),
            max(upper(hh.system_time)),
            max(upper(hhh.system_time)))
          from haut ha
          left join haut_history hah on ha.oid = hah.oid
          left join hakujen_hakuajat hh on ha.oid = hh.haku_oid
          left join hakujen_hakuajat_history hhh on ha.oid = hhh.haku_oid
          where ha.oid = $oid""".as[Option[Instant]].head
  }
}

sealed trait HakuSQL extends HakuExtractors with HakuModificationSQL with SQLHelpers {

  def insertHaku(haku: Haku) = {
    sql"""insert into haut ( tila,
                             nimi,
                             hakutapa_koodi_uri,
                             hakukohteen_liittamisen_takaraja,
                             hakukohteen_muokkaamisen_takaraja,
                             ajastettu_julkaisu,
                             alkamiskausi_koodi_uri,
                             alkamisvuosi,
                             kohdejoukko_koodi_uri,
                             kohdejoukon_tarkenne_koodi_uri,
                             hakulomaketyyppi,
                             hakulomake,
                             hakulomake_id,
                             hakulomake_kuvaus,
                             hakulomake_linkki,
                             metadata,
                             organisaatio_oid,
                             muokkaaja,
                             kielivalinta
          ) values ( ${haku.tila.toString}::julkaisutila,
                     ${toJsonParam(haku.nimi)}::jsonb,
                     ${haku.hakutapaKoodiUri},
                     ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp,
                     ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp,
                     ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp,
                     ${haku.alkamiskausiKoodiUri},
                     ${haku.alkamisvuosi},
                     ${haku.kohdejoukkoKoodiUri},
                     ${haku.kohdejoukonTarkenneKoodiUri},
                     ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
                     ${toJsonParam(haku.hakulomake)}::jsonb,
                     ${haku.hakulomakeId},
                     ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb,
                     ${toJsonParam(haku.hakulomakeLinkki)}::jsonb,
                     ${toJsonParam(haku.metadata)}::jsonb,
                     ${haku.organisaatioOid},
                     ${haku.muokkaaja},
                     ${toJsonParam(haku.kielivalinta)}::jsonb
          ) returning oid""".as[HakuOid].head
  }

  def insertHakuajat(haku: Haku) = {
    DBIO.sequence(
      haku.hakuajat.map(t =>
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values (
                ${haku.oid},
                tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                        ${formatTimestampParam(Some(t.paattyy))}::timestamp, '[)'),
                ${haku.muokkaaja})"""))
  }

  def selectHaku(oid: HakuOid) = {
    sql"""select oid, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja,
                 ajastettu_julkaisu, alkamiskausi_koodi_uri, alkamisvuosi, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri,
                 hakulomaketyyppi, hakulomake, hakulomake_id, hakulomake_kuvaus, hakulomake_linkki, metadata, organisaatio_oid,
                 muokkaaja, kielivalinta, lower(system_time) from haut where oid = $oid"""
  }

  def selectHaunHakuajat(oid: HakuOid) = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid"""
  }

  def updateHaku(haku: Haku) = {
    sqlu"""update haut set
              hakutapa_koodi_uri = ${haku.hakutapaKoodiUri},
              hakukohteen_liittamisen_takaraja = ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp,
              hakukohteen_muokkaamisen_takaraja = ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp,
              ajastettu_julkaisu = ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp,
              alkamiskausi_koodi_uri = ${haku.alkamiskausiKoodiUri},
              alkamisvuosi = ${haku.alkamisvuosi},
              kohdejoukko_koodi_uri = ${haku.kohdejoukkoKoodiUri},
              kohdejoukon_tarkenne_koodi_uri = ${haku.kohdejoukonTarkenneKoodiUri},
              hakulomaketyyppi = ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake = ${toJsonParam(haku.hakulomake)}::jsonb,
              hakulomake_id = ${haku.hakulomakeId},
              hakulomake_kuvaus = ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb,
              hakulomake_linkki = ${toJsonParam(haku.hakulomakeLinkki)}::jsonb,
              organisaatio_oid = ${haku.organisaatioOid},
              tila = ${haku.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(haku.nimi)}::jsonb,
              metadata = ${toJsonParam(haku.metadata)}::jsonb,
              muokkaaja = ${haku.muokkaaja},
              kielivalinta = ${toJsonParam(haku.kielivalinta)}::jsonb
            where oid = ${haku.oid}
            and ( hakutapa_koodi_uri is distinct from ${haku.hakutapaKoodiUri}
            or alkamiskausi_koodi_uri is distinct from ${haku.alkamiskausiKoodiUri}
            or alkamisvuosi is distinct from ${haku.alkamisvuosi}
            or kohdejoukko_koodi_uri is distinct from ${haku.kohdejoukkoKoodiUri}
            or kohdejoukon_tarkenne_koodi_uri is distinct from ${haku.kohdejoukonTarkenneKoodiUri}
            or hakulomaketyyppi is distinct from ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake is distinct from ${toJsonParam(haku.hakulomake)}::jsonb
            or hakulomake_id is distinct from ${haku.hakulomakeId}
            or hakulomake_kuvaus is distinct from ${toJsonParam(haku.hakulomakeKuvaus)}::jsonb
            or hakulomake_linkki is distinct from ${toJsonParam(haku.hakulomakeLinkki)}::jsonb
            or hakukohteen_liittamisen_takaraja is distinct from ${formatTimestampParam(haku.hakukohteenLiittamisenTakaraja)}::timestamp
            or hakukohteen_muokkaamisen_takaraja is distinct from ${formatTimestampParam(haku.hakukohteenMuokkaamisenTakaraja)}::timestamp
            or ajastettu_julkaisu is distinct from ${formatTimestampParam(haku.ajastettuJulkaisu)}::timestamp
            or organisaatio_oid is distinct from ${haku.organisaatioOid}
            or tila is distinct from ${haku.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(haku.nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(haku.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(haku.kielivalinta)}::jsonb)"""
  }

  def insertHakuaika(oid: Option[HakuOid], hakuaika: Ajanjakso, muokkaaja: UserOid) = {
    sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                     ${formatTimestampParam(Some(hakuaika.paattyy))}::timestamp, '[)'), $muokkaaja)
               on conflict on constraint hakujen_hakuajat_pkey do nothing"""}

  def deleteHakuajat(oid: Option[HakuOid], exclude: List[Ajanjakso]) = {
    sqlu"""delete from hakujen_hakuajat where haku_oid = $oid and hakuaika not in (#${createRangeInParams(exclude)})"""
  }

  def updateHaunHakuajat(haku: Haku) = {
    val (oid, hakuajat, muokkaaja) = (haku.oid, haku.hakuajat, haku.muokkaaja)
    if(hakuajat.nonEmpty) {
      val insertSQL = hakuajat.map(insertHakuaika(oid, _, muokkaaja))
      val deleteSQL = deleteHakuajat(oid, hakuajat)

      DBIO.sequence( insertSQL :+ deleteSQL)
    } else {
      DBIO.sequence(List(sqlu"""delete from hakujen_hakuajat where haku_oid = $oid"""))
    }
  }

  def selectByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from haut
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[HakuListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid) = {
    sql"""select haut.oid, haut.nimi, haut.tila, haut.organisaatio_oid, haut.muokkaaja, lower(haut.system_time)
          from haut
          inner join hakukohteet on hakukohteet.haku_oid = haut.oid
          inner join toteutukset on toteutukset.oid = hakukohteet.toteutus_oid
          where toteutukset.oid = $toteutusOid""".as[HakuListItem]
  }
}
