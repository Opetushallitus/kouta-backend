package fi.oph.kouta.repository

import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.repository.dto._
import fi.vm.sade.utils.slf4j.Logging
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.ConcurrentModificationException

import slick.dbio.DBIO
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO {
  def put(koulutus:Koulutus):Option[String]
  def get(oid:String): Option[(Koulutus, Instant)]
  def getLastModified(oid:String): Option[Instant]
  def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean
}

object KoulutusDAO extends KoulutusDAO with KoulutusDTOs with Logging {

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  private def insertKoulutus(koulutus:Koulutus) = {
    val Koulutus(_, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, _, _, _, muokkaaja) = koulutus
    sql"""insert into koulutukset (johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, muokkaaja)
             values ($johtaaTutkintoon, ${koulutustyyppi.toString}::koulutustyyppi,
             $koulutusKoodiUri, ${tila.toString}::julkaisutila, $muokkaaja) returning oid""".as[String].headOption
  }

  private def insertKoulutuksenTekstit(koulutus:Koulutus) = {
    val Koulutus(oid, _, _, _, _, _, nimi, kuvaus, muokkaaja) = koulutus
    val kielet = nimi.keys ++ kuvaus.keys
    DBIO.sequence( kielet.map(k => sqlu"""insert into koulutusten_tekstit (koulutus_oid, kielikoodi, nimi, kuvaus, muokkaaja)
             values ($oid, ${k.toString}, ${nimi.get(k)}, ${kuvaus.get(k)}, $muokkaaja)"""))
  }

  private def insertKoulutuksenTarjoajat(koulutus:Koulutus) = {
    val Koulutus(oid, _, _, _, _, tarjoajat, _, _, muokkaaja) = koulutus
    DBIO.sequence( tarjoajat.map(t =>
      sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)"""))
  }

  def put(koulutus:Koulutus): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertKoulutus(koulutus)
      _ <- insertKoulutuksenTekstit(koulutus.copy(oid = oid))
      _ <- insertKoulutuksenTarjoajat(koulutus.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  private def selectKoulutus(oid:String) = {
    sql"""select oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, muokkaaja from koulutukset where oid = $oid"""
  }

  private def selectKoulutuksenTekstit(oid:String) = {
    sql"""select koulutus_oid, kielikoodi, nimi, kuvaus from koulutusten_tekstit where koulutus_oid = $oid"""
  }

  private def selectKoulutuksenTarjoajat(oid:String) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  def get(oid:String): Option[(Koulutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      x <- selectKoulutus(oid).as[KoulutusDTO].headOption
      y <- selectKoulutuksenTekstit(oid).as[KoulutuksenTekstitDTO]
      z <- selectKoulutuksenTarjoajat(oid).as[KoulutuksenTarjoajatDTO]
      l <- selectLastModified(oid)
    } yield (x, y, z, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _, _)) | Right((_, _, _, None)) => None
      case Right((Some(k), y, z, Some(l))) => Some((koulutus(k, y, z), l))
    }
  }

  def getLastModified(oid:String): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(oid) )

  private def selectLastModified(oid:String):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(k.system_time)),
            max(lower(te.system_time)),
            max(lower(ta.system_time)),
            max(upper(kh.system_time)),
            max(upper(teh.system_time)),
            max(upper(tah.system_time)))
          from koulutukset k
          left join koulutusten_tekstit te on k.oid = te.koulutus_oid
          left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
          left join koulutukset_history kh on k.oid = kh.oid
          left join koulutusten_tekstit_history teh on k.oid = teh.koulutus_oid
          left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
          where k.oid = $oid""".as[Option[Instant]].head
  }

  private def updateKoulutus(koulutus:Koulutus) = {
    val Koulutus(oid, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, _, _, _, muokkaaja) = koulutus
    sqlu"""update koulutukset set
              johtaa_tutkintoon = $johtaaTutkintoon,
              tyyppi = ${koulutustyyppi.toString}::koulutustyyppi,
              koulutus_koodi_uri = $koulutusKoodiUri,
              tila = ${tila.toString}::julkaisutila,
              muokkaaja = $muokkaaja
            where oid = $oid
            and ( johtaa_tutkintoon <> $johtaaTutkintoon
            or tyyppi <> ${koulutustyyppi.toString}::koulutustyyppi
            or koulutus_koodi_uri <> $koulutusKoodiUri
            or tila <> ${tila.toString}::julkaisutila )"""
  }

  private def updateKoulutuksenTekstit(koulutus: Koulutus) = {
    val Koulutus(oid, _, _, _, _, _, nimi, kuvaus, muokkaaja) = koulutus
    val kielet = nimi.keys ++ kuvaus.keys
    DBIO.sequence( kielet.map(k =>
      sqlu"""insert into koulutusten_tekstit (koulutus_oid, kielikoodi, nimi, kuvaus, muokkaaja)
             values ($oid, ${k.toString}, ${nimi.get(k)}, ${kuvaus.get(k)}, $muokkaaja)
             on conflict on constraint koulutusten_tekstit_pkey do update
             set nimi = excluded.nimi,
                 kuvaus = excluded.kuvaus,
                 muokkaaja = excluded.muokkaaja
             where koulutusten_tekstit.nimi <> excluded.nimi
                or koulutusten_tekstit.kuvaus <> excluded.kuvaus
                or koulutusten_tekstit.muokkaaja <> excluded.muokkaaja"""))
  }

  private def updatenKoulutuksenTarjoajat(koulutus: Koulutus) = {
    val Koulutus(oid, _, _, _, _, tarjoajat, _, _, muokkaaja) = koulutus
    if(tarjoajat.size > 0) {
      val tarjoajatString = tarjoajat.map(s => s"'$s'").mkString(",")
      DBIO.sequence( tarjoajat.map(t =>
        sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)
             on conflict on constraint koulutusten_tarjoajat_pkey do nothing""") :+
        sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid and tarjoaja_oid not in (#${tarjoajatString})""")
    } else {
      DBIO.sequence(List(sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid"""))
    }
  }

  def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(koulutus.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown koulutus oid ${koulutus.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut koulutusta ${koulutus.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateKoulutus(koulutus))
      .zip(updateKoulutuksenTekstit(koulutus))
      .zip(updatenKoulutuksenTarjoajat(koulutus))) match {
      case Left(t) => throw t
      case Right(((x, y), z)) => 0 < (x + y.seq.sum + z.sum)
    }
  }
}