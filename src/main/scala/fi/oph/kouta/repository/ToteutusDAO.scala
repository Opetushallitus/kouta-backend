package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain.Toteutus
import fi.oph.kouta.repository.dto.ToteutusDTOs
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO {
  def put(toteutus:Toteutus):Option[String]
  def get(oid:String): Option[(Toteutus, Instant)]
  def getLastModified(oid:String): Option[Instant]
  def update(toteutus:Toteutus, notModifiedSince:Instant): Boolean
}

object ToteutusDAO extends ToteutusDAO with ToteutusDTOs {

  private def insertToteutus(toteutus:Toteutus) = {
    val Toteutus(_, koulutusOid, tila, _, nimi, metadata, muokkaaja) = toteutus
    sql"""insert into toteutukset (koulutus_oid, tila, nimi, metadata, muokkaaja)
             values ($koulutusOid, ${tila.toString}::julkaisutila,
             ${toJson(nimi)}::jsonb, ${toJson(metadata)}::jsonb, $muokkaaja) returning oid""".as[String].headOption
  }

  private def insertToteutuksenTarjoajat(toteutus:Toteutus) = {
    val Toteutus(oid, _, _, tarjoajat, _, _, muokkaaja) = toteutus
    DBIO.sequence( tarjoajat.map(t =>
      sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)"""))
  }

  override def put(toteutus: Toteutus): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertToteutus(toteutus)
      _ <- insertToteutuksenTarjoajat(toteutus.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  private def selectToteutus(oid:String) = {
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja from toteutukset where oid = $oid"""
  }

  private def selectToteutuksenTarjoajat(oid:String) = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid = $oid"""
  }

  override def get(oid: String): Option[(Toteutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      x <- selectToteutus(oid).as[ToteutusDTO].headOption
      z <- selectToteutuksenTarjoajat(oid).as[TarjoajaDTO]
      l <- selectLastModified(oid)
    } yield (x, z, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(k), z, Some(l))) => Some((toteutus(k, z), l))
    }
  }

  def getLastModified(oid:String): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(oid) )

  private def selectLastModified(oid:String):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(t.system_time)),
            max(lower(ta.system_time)),
            max(upper(th.system_time)),
            max(upper(tah.system_time)))
          from toteutukset t
          left join toteutusten_tarjoajat ta on t.oid = ta.toteutus_oid
          left join toteutukset_history th on t.oid = th.oid
          left join toteutusten_tarjoajat_history tah on t.oid = tah.toteutus_oid
          where t.oid = $oid""".as[Option[Instant]].head
  }

  private def updateToteutus(toteutus: Toteutus) = {
    val Toteutus(oid, koulutusOid, tila, _, nimi, metadata, muokkaaja) = toteutus
    sqlu"""update toteutukset set
              koulutus_oid = ${koulutusOid},
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJson(nimi)}::jsonb,
              metadata = ${toJson(metadata)}::jsonb,
              muokkaaja = $muokkaaja
            where oid = $oid
            and ( koulutus_oid <> $koulutusOid
            or tila <> ${tila.toString}::julkaisutila
            or nimi <> ${toJson(nimi)}::jsonb
            or metadata <> ${toJson(metadata)}::jsonb)"""
  }

  private def updateToteutuksenTarjoajat(toteutus: Toteutus) = {
    val Toteutus(oid, _, _, tarjoajat, _, _, muokkaaja) = toteutus
    if(tarjoajat.size > 0) {
      val tarjoajatString = tarjoajat.map(s => s"'$s'").mkString(",")
      DBIO.sequence( tarjoajat.map(t =>
        sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)
             on conflict on constraint toteutusten_tarjoajat_pkey do nothing""") :+
        sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid and tarjoaja_oid not in (#${tarjoajatString})""")
    } else {
      DBIO.sequence(List(sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid"""))
    }
  }

  override def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(toteutus.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown toteutus oid ${toteutus.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut toteutusta ${toteutus.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateToteutus(toteutus))
      .zip(updateToteutuksenTarjoajat(toteutus))) match {
      case Left(t) => throw t
      case Right((x, y)) => 0 < (x + y.sum)
    }
  }
}