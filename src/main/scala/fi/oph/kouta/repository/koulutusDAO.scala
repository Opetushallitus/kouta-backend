package fi.oph.kouta.repository

import fi.oph.kouta.domain._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.ConcurrentModificationException

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO extends EntityModificationDAO[String] {
  def put(koulutus:Koulutus):Option[String]
  def get(oid:String): Option[(Koulutus, Instant)]
  def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean
  def list(params:ListParams):List[OidListResponse]
}

object KoulutusDAO extends KoulutusDAO with KoulutusSQL {

  override def put(koulutus:Koulutus): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertKoulutus(koulutus)
      _ <- insertKoulutuksenTarjoajat(koulutus.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  override def get(oid:String): Option[(Koulutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      k <- selectKoulutus(oid).as[Koulutus].headOption
      t <- selectKoulutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (k, t, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(k), t, Some(l))) => Some((k.copy(tarjoajat = t.map(_.tarjoajaOid).toList), l))
    }
  }

  override def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(koulutus.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown koulutus oid ${koulutus.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut koulutusta ${koulutus.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateKoulutus(koulutus))
      .zip(updateKoulutuksenTarjoajat(koulutus))) match {
      case Left(t) => throw t
      case Right((x, y)) => 0 < (x + y.sum)
    }
  }

  private def updateKoulutuksenTarjoajat(koulutus: Koulutus) = {
    val Koulutus(oid, _, _, _, _, tarjoajat, _, _, muokkaaja, _) = koulutus
    if(tarjoajat.size > 0) {
      DBIO.sequence( tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      DBIO.sequence(List(deleteTarjoajat(oid)))
    }
  }

  override def list(params:ListParams):List[OidListResponse] = {
    val sql = selectFromKoulutukset(params)
    query[OidListResponse](sql, params.tarjoajat.union( params.tilat.map(_.toString)).toArray, (r:java.sql.ResultSet) => {
      new OidListResponse(r.getString(1), extractKielistetty(Option(r.getString(2))))
    })
  }
}

sealed trait KoulutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid:String):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(k.system_time)),
            max(lower(ta.system_time)),
            max(upper(kh.system_time)),
            max(upper(tah.system_time)))
          from koulutukset k
          left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
          left join koulutukset_history kh on k.oid = kh.oid
          left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
          where k.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since:Instant): DBIO[Seq[String]] = {
    sql"""select oid from koulutukset where $since < lower(system_time)
          union
          select oid from koulutukset_history where $since <@ system_time
          union
          select koulutus_oid from koulutusten_tarjoajat where $since < lower(system_time)
          union
          select koulutus_oid from koulutusten_tarjoajat_history where $since <@ system_time""".as[String]
  }
}

sealed trait KoulutusSQL extends KoulutusExtractors with KoulutusModificationSQL with SQLHelpers {

  def insertKoulutus(koulutus:Koulutus) = {
    val Koulutus(_, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, _, nimi, metadata, muokkaaja, kielivalinta) = koulutus
    sql"""insert into koulutukset (
            johtaa_tutkintoon,
            tyyppi,
            koulutus_koodi_uri,
            tila,
            nimi,
            metadata,
            muokkaaja,
            kielivalinta)
          values (
            $johtaaTutkintoon,
            ${koulutustyyppi.map(_.toString)}::koulutustyyppi,
            $koulutusKoodiUri,
            ${tila.toString}::julkaisutila,
            ${toJsonParam(nimi)}::jsonb,
            ${toJsonParam(metadata)}::jsonb,
            $muokkaaja,
            ${toJsonParam(kielivalinta)}::jsonb) returning oid""".as[String].headOption
  }

  def insertKoulutuksenTarjoajat(koulutus:Koulutus) = {
    val Koulutus(oid, _, _, _, _, tarjoajat, _, _, muokkaaja, _) = koulutus
    DBIO.sequence( tarjoajat.map(t =>
      sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)"""))
  }

  def selectKoulutus(oid:String) = {
    sql"""select oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, nimi, metadata, muokkaaja, kielivalinta from koulutukset where oid = $oid"""
  }

  def selectKoulutuksenTarjoajat(oid:String) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  def updateKoulutus(koulutus:Koulutus) = {
    val Koulutus(oid, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, _, nimi, metatieto, muokkaaja, kielivalinta) = koulutus
    sqlu"""update koulutukset set
              johtaa_tutkintoon = $johtaaTutkintoon,
              tyyppi = ${koulutustyyppi.map(_.toString)}::koulutustyyppi,
              koulutus_koodi_uri = $koulutusKoodiUri,
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJsonParam(nimi)}::jsonb,
              metadata = ${toJsonParam(metatieto)}::jsonb,
              muokkaaja = $muokkaaja,
              kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
            where oid = $oid
            and ( johtaa_tutkintoon is distinct from $johtaaTutkintoon
            or tyyppi is distinct from ${koulutustyyppi.map(_.toString)}::koulutustyyppi
            or koulutus_koodi_uri is distinct from $koulutusKoodiUri
            or tila is distinct from ${tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(metatieto)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(kielivalinta)}::jsonb)"""
  }

  def insertTarjoaja(oid:Option[String], tarjoaja:String, muokkaaja:String ) = {
    sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint koulutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid:Option[String], exclude:List[String]) = {
    sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid and tarjoaja_oid not in (#${createInParams(exclude)})"""
  }

  def deleteTarjoajat(oid:Option[String]) = sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid"""

  def selectFromKoulutukset(params:ListParams) = s"select k.oid, k.nimi from koulutukset k ${joinTarjoajat(params.tarjoajat)} ${whereTilat(params.tilat)}"

  private def joinTarjoajat(tarjoajat:List[String]) = Option(tarjoajat).filterNot(_.isEmpty).map(t =>
    s"inner join koulutusten_tarjoajat t on k.oid = t.koulutus_oid and t.tarjoaja_oid in (${t.map(x => "?").mkString(",")}) ").getOrElse("")

  private def whereTilat(tilat:List[Julkaisutila]) = Option(tilat).filterNot(_.isEmpty).map(t =>
    s"where k.tila in (${t.map(x => "?::julkaisutila").mkString(",")}) ").getOrElse("")
}