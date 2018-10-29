package fi.oph.kouta.repository

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain
import fi.oph.kouta.domain.{Haku, Hakuaika}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO {
  def put(haku:Haku):Option[String]
  def get(oid:String): Option[(Haku, Instant)]
  def getLastModified(oid:String): Option[Instant]
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
      case Right((Some(h), a, Some(l))) => Some((h.copy(hakuajat = a.map(x => domain.Hakuaika(x.alkaa, x.paattyy)).toList), l))
    }
  }

  def getLastModified(oid:String): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(oid) )

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

sealed trait HakuSQL extends HakuExtractors with SQLHelpers {

  def insertHaku(haku:Haku) = {
    val Haku(_, tila, nimi, hakutapaKoodiUri, hakukohteenLiittamisenTakaraja, hakukohteenMuokkaamisenTakaraja, alkamiskausiKoodiUri, alkamisvuosi,
    kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, hakulomaketyyppi, hakulomake, metadata, organisaatio, _, muokkaaja, kielivalinta) = haku
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
                             organisaatio,
                             muokkaaja,
                             kielivalinta
          ) values ( ${tila.toString}::julkaisutila,
                     ${toJsonParam(nimi)}::jsonb,
                     ${hakutapaKoodiUri},
                     ${toTimestampParam(hakukohteenLiittamisenTakaraja)},
                     ${toTimestampParam(hakukohteenMuokkaamisenTakaraja)},
                     ${alkamiskausiKoodiUri},
                     ${alkamisvuosi},
                     ${kohdejoukkoKoodiUri},
                     ${kohdejoukonTarkenneKoodiUri},
                     ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
                     ${hakulomake},
                     ${toJsonParam(metadata)}::jsonb,
                     $organisaatio,
                     $muokkaaja,
                     ${toJsonParam(kielivalinta)}::jsonb
          ) returning oid""".as[String].headOption
  }

  def insertHakuajat(haku:Haku) = {
    DBIO.sequence(
      haku.hakuajat.map(t =>
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
             values (${haku.oid}, tstzrange(${Timestamp.from(t.alkaa)}, ${Timestamp.from(t.paattyy)}, '[)'), ${haku.muokkaaja})"""))
  }

  def selectHaku(oid:String) = {
    sql"""select oid, tila, nimi, hakutapa_koodi_uri, hakukohteen_liittamisen_takaraja, hakukohteen_muokkaamisen_takaraja, alkamiskausi_koodi_uri, alkamisvuosi,
          kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, hakulomaketyyppi, hakulomake, metadata, organisaatio, muokkaaja, kielivalinta from haut where oid = $oid"""
  }

  def selectHaunHakuajat(oid:String) = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid"""
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

  def updateHaku(haku:Haku) = {
    val Haku(oid, tila, nimi, hakutapaKoodiUri, hakukohteenLiittamisenTakaraja, hakukohteenMuokkaamisenTakaraja, alkamiskausiKoodiUri, alkamisvuosi,
    kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, hakulomaketyyppi, hakulomake, metadata, organisaatio, _, muokkaaja, kielivalinta) = haku
    val liittamisenTakaraja = toTimestampParam(hakukohteenLiittamisenTakaraja)
    val muokkaamisenTakaraja = toTimestampParam(hakukohteenMuokkaamisenTakaraja)
    sqlu"""update haut set
              hakutapa_koodi_uri = $hakutapaKoodiUri,
              hakukohteen_liittamisen_takaraja = $liittamisenTakaraja,
              hakukohteen_muokkaamisen_takaraja = $muokkaamisenTakaraja,
              alkamiskausi_koodi_uri = $alkamiskausiKoodiUri,
              alkamisvuosi = ${alkamisvuosi},
              kohdejoukko_koodi_uri = $kohdejoukkoKoodiUri,
              kohdejoukon_tarkenne_koodi_uri = $kohdejoukonTarkenneKoodiUri,
              hakulomaketyyppi = ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake = $hakulomake,
              organisaatio = $organisaatio,
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJsonParam(nimi)}::jsonb,
              metadata = ${toJsonParam(metadata)}::jsonb,
              muokkaaja = $muokkaaja,
              kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
            where oid = $oid
            and ( hakutapa_koodi_uri <> $hakutapaKoodiUri
            or alkamiskausi_koodi_uri <> $alkamiskausiKoodiUri
            or alkamisvuosi <> ${alkamisvuosi}
            or kohdejoukko_koodi_uri <> $kohdejoukkoKoodiUri
            or kohdejoukon_tarkenne_koodi_uri <> $kohdejoukonTarkenneKoodiUri
            or hakulomaketyyppi <> ${hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake <> $hakulomake
            or hakukohteen_liittamisen_takaraja <> $liittamisenTakaraja
            or hakukohteen_muokkaamisen_takaraja <> $muokkaamisenTakaraja
            or organisaatio <> $organisaatio
            or tila <> ${tila.toString}::julkaisutila
            or nimi <> ${toJsonParam(nimi)}::jsonb
            or metadata <> ${toJsonParam(metadata)}::jsonb
            or kielivalinta <> ${toJsonParam(kielivalinta)}::jsonb)"""
  }

  def updateHaunHakuajat(haku:Haku) = {
    val (oid, hakuajat, muokkaaja) = (haku.oid, haku.hakuajat, haku.muokkaaja)
    if(hakuajat.size > 0) {
      val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Helsinki"))
      val hakuajatString = hakuajat.map(s => s"'[${formatter.format(s.alkaa)}, ${formatter.format(s.paattyy)})'").mkString(",")
      DBIO.sequence( hakuajat.map(t => sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values ($oid, tstzrange(${Timestamp.from(t.alkaa)}, ${Timestamp.from(t.paattyy)}, '[)'), $muokkaaja)
               on conflict on constraint hakujen_hakuajat_pkey do nothing"""
      ) :+ sqlu"""delete from hakujen_hakuajat where haku_oid = $oid and hakuaika not in (#${hakuajatString})""")
    } else {
      DBIO.sequence(List(sqlu"""delete from hakujen_hakuajat where haku_oid = $oid"""))
    }
  }
}