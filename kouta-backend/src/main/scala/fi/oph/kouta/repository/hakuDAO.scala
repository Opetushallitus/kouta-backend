package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Ajanjakso, Haku, HakuListItem}
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.instantToModified
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO extends EntityModificationDAO[HakuOid] {
  def getPutActions(haku: Haku): DBIO[Haku]
  def getUpdateActions(haku: Haku): DBIO[Option[Haku]]

  def get(oid: HakuOid): Option[(Haku, Instant)]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakuListItem]
  def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakuListItem]
}

object HakuDAO extends HakuDAO with HakuSQL {

  override def getPutActions(haku: Haku): DBIO[Haku] =
    for {
      oid <- insertHaku(haku)
      _ <- insertHakuajat(haku.withOid(oid))
      m <- selectLastModified(oid)
    } yield haku.withOid(oid).withModified(m.get)

  override def get(oid: HakuOid): Option[(Haku, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHaku(oid)
      a <- selectHaunHakuajat(oid)
      l <- selectLastModified(oid)
    } yield (h, a, l) ).map {
      case (Some(h), a, Some(l)) => Some((
        h.copy(modified = Some(instantToModified(l)),
          hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList),
        l))
      case _ => None
    }.get
  }

  def getUpdateActions(haku: Haku): DBIO[Option[Haku]] =
    for {
      x <- updateHaku(haku)
      y <- updateHaunHakuajat(haku)
      m <- selectLastModified(haku.oid.get)
    } yield optionWhen(x + y > 0)(haku.withModified(m.get))

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakuListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByAllowedOrganisaatiot(organisaatioOids))
  }


  override def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakuListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid))
}

trait HakuModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakuOid]] = {
    sql"""select oid from haut where $since < lower(system_time)
          union
          select oid from haut_history where $since <@ system_time
          union
          select haku_oid from hakujen_hakuajat where $since < lower(system_time)
          union
          select haku_oid from hakujen_hakuajat_history where $since <@ system_time""".as[HakuOid]
  }

  def selectLastModified(oid: HakuOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(ha.system_time)),
            max(lower(hh.system_time)),
            max(upper(hah.system_time)),
            max(upper(hhh.system_time)))
          from haut ha
          left join haut_history hah on ha.oid = hah.oid
          left join hakujen_hakuajat hh on ha.oid = hh.haku_oid
          left join hakujen_hakuajat_history hhh on ha.oid = hhh.haku_oid
          where ha.oid = $oid""".as[Option[Instant]].head
  }
}

sealed trait HakuSQL extends HakuExtractors with HakuModificationSQL with SQLHelpers {

  def insertHaku(haku: Haku): DBIO[HakuOid] = {
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
                             hakulomake_ataru_id,
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

  def selectHaku(oid: HakuOid): DBIO[Option[Haku]] = {
    sql"""select oid, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja,
                 ajastettu_julkaisu, alkamiskausi_koodi_uri, alkamisvuosi, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri,
                 hakulomaketyyppi, hakulomake_ataru_id, hakulomake_kuvaus, hakulomake_linkki, metadata, organisaatio_oid,
                 muokkaaja, kielivalinta, lower(system_time) from haut where oid = $oid""".as[Haku].headOption
  }

  def selectHaunHakuajat(oid: HakuOid): DBIO[Vector[Hakuaika]] = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid""".as[Hakuaika]
  }

  def updateHaku(haku: Haku): DBIO[Int] = {
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
            and ( hakutapa_koodi_uri is distinct from ${haku.hakutapaKoodiUri}
            or alkamiskausi_koodi_uri is distinct from ${haku.alkamiskausiKoodiUri}
            or alkamisvuosi is distinct from ${haku.alkamisvuosi}
            or kohdejoukko_koodi_uri is distinct from ${haku.kohdejoukkoKoodiUri}
            or kohdejoukon_tarkenne_koodi_uri is distinct from ${haku.kohdejoukonTarkenneKoodiUri}
            or hakulomaketyyppi is distinct from ${haku.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake_ataru_id is distinct from ${haku.hakulomakeAtaruId.map(_.toString)}::uuid
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
    """select distinct ha.oid, ha.nimi, ha.tila, ha.organisaatio_oid, ha.muokkaaja, m.modified
         from haut ha
         inner join (
           select ha.oid oid, greatest(
             max(lower(ha.system_time)),
             max(lower(hh.system_time)),
             max(upper(hah.system_time)),
             max(upper(hhh.system_time))) modified
           from haut ha
           left join haut_history hah on ha.oid = hah.oid
           left join hakujen_hakuajat hh on ha.oid = hh.haku_oid
           left join hakujen_hakuajat_history hhh on ha.oid = hhh.haku_oid
           group by ha.oid
         ) m on m.oid = ha.oid"""

  def selectByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[HakuListItem]] = {
    sql"""#$selectHakuListSql
          where ha.organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[HakuListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid): DBIO[Vector[HakuListItem]] = {
    sql"""#$selectHakuListSql
          inner join hakukohteet on hakukohteet.haku_oid = ha.oid
          inner join toteutukset on toteutukset.oid = hakukohteet.toteutus_oid
          where toteutukset.oid = $toteutusOid""".as[HakuListItem]
  }
}
