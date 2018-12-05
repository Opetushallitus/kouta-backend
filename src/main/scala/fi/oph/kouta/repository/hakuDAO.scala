package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain
import fi.oph.kouta.domain.{Ajanjakso, Haku}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO extends EntityModificationDAO[String] {
  def put(haku:Haku):Option[String]
  def get(oid:String): Option[(Haku, Instant)]
  def update(haku:Haku, notModifiedSince:Instant): Boolean
}
  
object HakuDAO extends HakuDAO with HakuSQL {

  override def put(haku: Haku): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertHaku(haku)
      _ <- insertHakuajat(haku.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  override def get(oid: String): Option[(Haku, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHaku(oid).as[Haku].headOption
      a <- selectHaunHakuajat(oid).as[Hakuaika]
      l <- selectLastModified(oid)
    } yield (h, a, l) ) match {
      case Left(t) => {t.printStackTrace(); throw t}
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(h), a, Some(l))) => Some((h.copy(hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList), l))
    }
  }

  override def update(haku: Haku, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(haku.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown haku oid ${haku.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut hakua ${haku.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateHaku(haku))
      .zip(updateHaunHakuajat(haku))) match {
      case Left(t) => throw t
      case Right((x, y)) => 0 < (x + y.sum)
    }
  }
}

trait HakuModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since:Instant): DBIO[Seq[String]] = {
    sql"""select oid from haut where ${since} < lower(system_time)
          union
          select oid from haut_history where ${since} <@ system_time
          union
          select haku_oid from hakujen_hakuajat where ${since} < lower(system_time)
          union
          select haku_oid from hakujen_hakuajat_history where ${since} <@ system_time""".as[String]
  }

  def selectLastModified(oid:String):DBIO[Option[Instant]] = {
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

  def insertHaku(haku:Haku) = {
    val Haku(_, tila, nimi, hakutapaKoodiUri, hakukohteenLiittamisenTakaraja, hakukohteenMuokkaamisenTakaraja, alkamiskausiKoodiUri, alkamisvuosi,
    kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, hakulomaketyyppi, hakulomake, metadata, organisaatioOid, _, muokkaaja, kielivalinta) = haku
    sql"""insert into haut ( tila,
                             nimi,
                             hakutapa_koodi_uri,
                             hakukohteen_liittamisen_takaraja,
                             hakukohteen_muokkaamisen_takaraja,
                             alkamiskausi_koodi_uri,
                             alkamisvuosi,
                             kohdejoukko_koodi_uri,
                             kohdejoukon_tarkenne_koodi_uri,
                             hakulomaketyyppi,
                             hakulomake,
                             metadata,
                             organisaatio_oid,
                             muokkaaja,
                             kielivalinta
          ) values ( ${tila.toString}::julkaisutila,
                     ${toJsonParam(nimi)}::jsonb,
                     ${hakutapaKoodiUri},
                     ${formatTimestampParam(hakukohteenLiittamisenTakaraja)}::timestamp,
                     ${formatTimestampParam(hakukohteenMuokkaamisenTakaraja)}::timestamp,
                     ${alkamiskausiKoodiUri},
                     ${alkamisvuosi},
                     ${kohdejoukkoKoodiUri},
                     ${kohdejoukonTarkenneKoodiUri},
                     ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
                     ${hakulomake},
                     ${toJsonParam(metadata)}::jsonb,
                     $organisaatioOid,
                     $muokkaaja,
                     ${toJsonParam(kielivalinta)}::jsonb
          ) returning oid""".as[String].headOption
  }

  def insertHakuajat(haku:Haku) = {
    DBIO.sequence(
      haku.hakuajat.map(t =>
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values (
                ${haku.oid},
                tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                        ${formatTimestampParam(Some(t.paattyy))}::timestamp, '[)'),
                ${haku.muokkaaja})"""))
  }

  def selectHaku(oid:String) = {
    sql"""select oid, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja, alkamiskausi_koodi_uri, alkamisvuosi,
          kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, hakulomaketyyppi, hakulomake, metadata, organisaatio_oid, muokkaaja, kielivalinta from haut where oid = $oid"""
  }

  def selectHaunHakuajat(oid:String) = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid"""
  }

  def updateHaku(haku:Haku) = {
    val Haku(oid, tila, nimi, hakutapaKoodiUri, hakukohteenLiittamisenTakaraja, hakukohteenMuokkaamisenTakaraja, alkamiskausiKoodiUri, alkamisvuosi,
    kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, hakulomaketyyppi, hakulomake, metadata, organisaatioOid, _, muokkaaja, kielivalinta) = haku
    sqlu"""update haut set
              hakutapa_koodi_uri = $hakutapaKoodiUri,
              hakukohteen_liittamisen_takaraja = ${formatTimestampParam(hakukohteenLiittamisenTakaraja)}::timestamp,
              hakukohteen_muokkaamisen_takaraja = ${formatTimestampParam(hakukohteenMuokkaamisenTakaraja)}::timestamp,
              alkamiskausi_koodi_uri = $alkamiskausiKoodiUri,
              alkamisvuosi = ${alkamisvuosi},
              kohdejoukko_koodi_uri = $kohdejoukkoKoodiUri,
              kohdejoukon_tarkenne_koodi_uri = $kohdejoukonTarkenneKoodiUri,
              hakulomaketyyppi = ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake = $hakulomake,
              organisaatio_oid = $organisaatioOid,
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJsonParam(nimi)}::jsonb,
              metadata = ${toJsonParam(metadata)}::jsonb,
              muokkaaja = $muokkaaja,
              kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
            where oid = $oid
            and ( hakutapa_koodi_uri is distinct from $hakutapaKoodiUri
            or alkamiskausi_koodi_uri is distinct from $alkamiskausiKoodiUri
            or alkamisvuosi is distinct from ${alkamisvuosi}
            or kohdejoukko_koodi_uri is distinct from $kohdejoukkoKoodiUri
            or kohdejoukon_tarkenne_koodi_uri is distinct from $kohdejoukonTarkenneKoodiUri
            or hakulomaketyyppi is distinct from ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake is distinct from $hakulomake
            or hakukohteen_liittamisen_takaraja is distinct from ${formatTimestampParam(hakukohteenLiittamisenTakaraja)}::timestamp
            or hakukohteen_muokkaamisen_takaraja is distinct from ${formatTimestampParam(hakukohteenMuokkaamisenTakaraja)}::timestamp
            or organisaatio_oid is distinct from $organisaatioOid
            or tila is distinct from ${tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(kielivalinta)}::jsonb)"""
  }

  def insertHakuaika(oid:Option[String], hakuaika:Ajanjakso, muokkaaja:String) = {
    sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                     ${formatTimestampParam(Some(hakuaika.paattyy))}::timestamp, '[)'), $muokkaaja)
               on conflict on constraint hakujen_hakuajat_pkey do nothing"""}

  def deleteHakuajat(oid:Option[String], exclude:List[Ajanjakso]) = {
    sqlu"""delete from hakujen_hakuajat where haku_oid = $oid and hakuaika not in (#${createRangeInParams(exclude)})"""
  }

  def updateHaunHakuajat(haku:Haku) = {
    val (oid, hakuajat, muokkaaja) = (haku.oid, haku.hakuajat, haku.muokkaaja)
    if(hakuajat.size > 0) {
      val insertSQL = hakuajat.map(insertHakuaika(oid, _, muokkaaja))
      val deleteSQL = deleteHakuajat(oid, hakuajat)

      DBIO.sequence( insertSQL :+ deleteSQL)
    } else {
      DBIO.sequence(List(sqlu"""delete from hakujen_hakuajat where haku_oid = $oid"""))
    }
  }
}