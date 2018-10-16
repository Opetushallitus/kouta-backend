package fi.oph.kouta.repository

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain.{Haku, HaunHakuaika}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakuDAO {
  def put(haku:Haku):Option[String]
  def get(oid:String): Option[(Haku, Instant)]
  def getLastModified(oid:String): Option[Instant]
  def update(haku:Haku, notModifiedSince:Instant): Boolean
}
  
object HakuDAO extends HakuDAO with HakuExtractors{

  private def insertHaku(haku:Haku) = {
    val Haku(_, tila, nimi, hakutapa, tietojen_muuttaminen_paattyy, alkamiskausi, alkamisvuosi,
             kohdejoukko, kohdejoukon_tarkenne, metadata, organisaatio, _, muokkaaja) = haku
    val tietojenMuuttaminen = tietojen_muuttaminen_paattyy.map(Timestamp.from).getOrElse(null)
    sql"""insert into haut ( tila,
                             nimi,
                             hakutapa,
                             alkamiskausi,
                             alkamisvuosi,
                             kohdejoukko,
                             kohdejoukon_tarkenne,
                             tietojen_muuttaminen_paattyy,
                             metadata,
                             organisaatio,
                             muokkaaja
          ) values ( ${tila.toString}::julkaisutila,
                     ${toJson(nimi)}::jsonb,
                     ${hakutapa.map(_.toString).getOrElse(null)}::hakutapa,
                     ${alkamiskausi.map(_.toString).getOrElse(null)}::alkamiskausi,
                     ${alkamisvuosi.getOrElse(null)},
                     ${kohdejoukko.getOrElse(null)},
                     ${kohdejoukon_tarkenne.getOrElse(null)},
                     $tietojenMuuttaminen,
                     ${toJson(metadata)}::jsonb,
                     $organisaatio,
                     $muokkaaja
          ) returning oid""".as[String].headOption
  }

  private def insertHakuajat(haku:Haku) = {
    val Haku(oid, _, _, _, _, _, _, _, _, _, _, hakuajat, muokkaaja) = haku
    DBIO.sequence(
      hakuajat.map(t => {
        val alkuaika = Timestamp.from(t.alkaa)
        val loppuaika = Timestamp.from(t.paattyy)
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
             values ($oid, tstzrange(${alkuaika}, ${loppuaika}, '[)'), $muokkaaja)"""}))
  }

  override def put(haku: Haku): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertHaku(haku)
      _ <- insertHakuajat(haku.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  private def selectHaku(oid:String) = {
    sql"""select oid, tila, nimi, hakutapa, tietojen_muuttaminen_paattyy, alkamiskausi, alkamisvuosi,
          kohdejoukko, kohdejoukon_tarkenne, metadata, organisaatio, muokkaaja from haut where oid = $oid"""
  }

  private def selectHaunHakuajat(oid:String) = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika) from hakujen_hakuajat where haku_oid = $oid"""
  }

  override def get(oid: String): Option[(Haku, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHaku(oid).as[Haku].headOption
      a <- selectHaunHakuajat(oid).as[Hakuaika]
      l <- selectLastModified(oid)
    } yield (h, a, l) ) match {
      case Left(t) => {t.printStackTrace(); throw t}
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(h), a, Some(l))) => Some((h.copy(hakuajat = a.map(x => HaunHakuaika(x.alkaa, x.paattyy)).toList), l))
    }
  }

  def getLastModified(oid:String): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(oid) )

  private def selectLastModified(oid:String):DBIO[Option[Instant]] = {
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

  private def updateHaku(haku:Haku) = {
    val Haku(oid, tila, nimi, hakutapa, tietojen_muuttaminen_paattyy, alkamiskausi, alkamisvuosi,
    kohdejoukko, kohdejoukon_tarkenne, metadata, organisaatio, _, muokkaaja) = haku
    val tietojenMuuttaminen = tietojen_muuttaminen_paattyy.map(Timestamp.from).getOrElse(null)
    sqlu"""update haut set
              hakutapa = ${hakutapa.map(_.toString).getOrElse(null)}::hakutapa,
              alkamiskausi = ${alkamiskausi.map(_.toString).getOrElse(null)}::alkamiskausi,
              alkamisvuosi = ${alkamisvuosi.getOrElse(null)},
              kohdejoukko = ${kohdejoukko.getOrElse(null)},
              kohdejoukon_tarkenne = ${kohdejoukon_tarkenne.getOrElse(null)},
              tietojen_muuttaminen_paattyy = $tietojenMuuttaminen,
              organisaatio = $organisaatio,
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJson(nimi)}::jsonb,
              metadata = ${toJson(metadata)}::jsonb,
              muokkaaja = $muokkaaja
            where oid = $oid
            and ( hakutapa <> ${hakutapa.map(_.toString).getOrElse(null)}::hakutapa
            or alkamiskausi <> ${alkamiskausi.map(_.toString).getOrElse(null)}::alkamiskausi
            or alkamisvuosi <> ${alkamisvuosi.getOrElse(null)}
            or kohdejoukko <> ${kohdejoukko.getOrElse(null)}
            or kohdejoukon_tarkenne <> ${kohdejoukon_tarkenne.getOrElse(null)}
            or tietojen_muuttaminen_paattyy <> $tietojenMuuttaminen
            or organisaatio <> $organisaatio
            or tila <> ${tila.toString}::julkaisutila
            or nimi <> ${toJson(nimi)}::jsonb
            or metadata <> ${toJson(metadata)}::jsonb)"""
  }

  private def updateHaunHakuajat(haku:Haku) = {
    val Haku(oid, _, _, _, _, _, _, _, _, _, _, hakuajat, muokkaaja) = haku
    if(hakuajat.size > 0) {
      val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Helsinki"))
      val hakuajatString = hakuajat.map(s => s"'[${formatter.format(s.alkaa)}, ${formatter.format(s.paattyy)})'").mkString(",")
      DBIO.sequence( hakuajat.map(t => {
        val alkuaika = Timestamp.from(t.alkaa)
        val loppuaika = Timestamp.from(t.paattyy)
        sqlu"""insert into hakujen_hakuajat (haku_oid, hakuaika, muokkaaja)
               values ($oid, tstzrange(${alkuaika}, ${loppuaika}, '[)'), $muokkaaja)
               on conflict on constraint hakujen_hakuajat_pkey do nothing"""
      }) :+ sqlu"""delete from hakujen_hakuajat where haku_oid = $oid and hakuaika not in (#${hakuajatString})""")
    } else {
      DBIO.sequence(List(sqlu"""delete from hakujen_hakuajat where haku_oid = $oid"""))
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
      case Right((x, y)) => {println(x); println(y); 0 < (x + y.sum)}
    }
  }
}