package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain.{OidListItem, Toteutus}
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO extends EntityModificationDAO[String] {
  def put(toteutus:Toteutus):Option[String]
  def get(oid:String): Option[(Toteutus, Instant)]
  def update(toteutus:Toteutus, notModifiedSince:Instant): Boolean
  def getByKoulutusOid(oid:String): Seq[Toteutus]

  def listByOrganisaatioOids(organisaatioOids:Seq[String]):Seq[OidListItem]
}

object ToteutusDAO extends ToteutusDAO with ToteutusSQL {

  override def put(toteutus: Toteutus): Option[String] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertToteutus(toteutus)
      _ <- insertToteutuksenTarjoajat(toteutus.copy(oid = oid))
      _ <- insertAmmattinimikkeet(toteutus)
      _ <- insertAsiasanat(toteutus)
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  override def get(oid: String): Option[(Toteutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      k <- selectToteutus(oid).as[Toteutus].headOption
      t <- selectToteutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (k, t, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(k), t, Some(l))) => Some((k.copy(tarjoajat = t.map(_.tarjoajaOid).toList), l))
    }
  }

  override def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(toteutus.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown toteutus oid ${toteutus.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut toteutusta ${toteutus.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateToteutus(toteutus))
      .zip(updateToteutuksenTarjoajat(toteutus))
      .zip(insertAsiasanat(toteutus))
      .zip(insertAmmattinimikkeet(toteutus))) match {
      case Left(t) => throw t
      case Right((((x, y), _), _)) => 0 < (x + y.sum)
    }
  }

  private def updateToteutuksenTarjoajat(toteutus: Toteutus) = {
    val Toteutus(oid, _, _, tarjoajat, _, _, muokkaaja, _, _) = toteutus
    if(tarjoajat.size > 0) {
      DBIO.sequence( tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      DBIO.sequence(List(deleteTarjoajat(oid)))
    }
  }

  private def insertAmmattinimikkeet(toteutus:Toteutus) =
    KeywordDAO.insert(Ammattinimike, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(List()))

  private def insertAsiasanat(toteutus:Toteutus) =
    KeywordDAO.insert(Asiasana, toteutus.metadata.map(_.asiasanat).getOrElse(List()))

  override def getByKoulutusOid(oid: String): Seq[Toteutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectToteutuksetByKoulutusOid(oid).as[Toteutus]
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList).as[Tarjoaja]
      } yield (toteutukset, tarjoajat) ) match {
        case Left(t) => throw t
        case Right((toteutukset, tarjoajat)) => {
          toteutukset.map(t =>
            t.copy(tarjoajat = tarjoajat.filter(_.oid == t.oid.get).map(_.tarjoajaOid).toList))
        }
      }
  }

  override def listModifiedSince(since:Instant):Seq[String] =
    KoutaDatabase.runBlocking(selectModifiedSince(since))

  override def listByOrganisaatioOids(organisaatioOids:Seq[String]):Seq[OidListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))
}

trait ToteutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid:String):DBIO[Option[Instant]] = {
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

  def selectModifiedSince(since:Instant): DBIO[Seq[String]] = {
    sql"""select oid from toteutukset where $since < lower(system_time)
          union
          select oid from toteutukset_history where $since <@ system_time
          union
          select toteutus_oid from toteutusten_tarjoajat where $since < lower(system_time)
          union
          select toteutus_oid from toteutusten_tarjoajat_history where $since <@ system_time""".as[String]
  }

}

sealed trait ToteutusSQL extends ToteutusExtractors with ToteutusModificationSQL with SQLHelpers {

  def selectToteutus(oid:String) =
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja, organisaatio_oid, kielivalinta from toteutukset where oid = $oid"""

  def selectToteutuksetByKoulutusOid(oid:String) =
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja, organisaatio_oid, kielivalinta from toteutukset where koulutus_oid = $oid"""

  def selectToteutuksenTarjoajat(oid:String) =
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid = $oid"""

  def selectToteutustenTarjoajat(oids:List[String]) = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid in (#${createInParams(oids)})"""
  }

  def insertToteutus(toteutus:Toteutus) = {
    val Toteutus(_, koulutusOid, tila, _, nimi, metadata, muokkaaja, organisaatioOid, kielivalinta) = toteutus
    sql"""insert into toteutukset (
            koulutus_oid,
            tila,
            nimi,
            metadata,
            muokkaaja,
            organisaatio_oid,
            kielivalinta
          ) values (
            $koulutusOid,
            ${tila.toString}::julkaisutila,
            ${toJsonParam(nimi)}::jsonb,
            ${toJsonParam(metadata)}::jsonb,
            $muokkaaja,
            $organisaatioOid,
            ${toJsonParam(kielivalinta)}::jsonb
          ) returning oid""".as[String].headOption
  }

  def insertToteutuksenTarjoajat(toteutus:Toteutus) = {
    DBIO.sequence( toteutus.tarjoajat.map(t =>
      sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values (${toteutus.oid}, $t, ${toteutus.muokkaaja})"""))
  }

  def updateToteutus(toteutus: Toteutus) = {
    val Toteutus(oid, koulutusOid, tila, _, nimi, metadata, muokkaaja, organisaatioOid, kielivalinta) = toteutus
    sqlu"""update toteutukset set
              koulutus_oid = ${koulutusOid},
              tila = ${tila.toString}::julkaisutila,
              nimi = ${toJsonParam(nimi)}::jsonb,
              metadata = ${toJsonParam(metadata)}::jsonb,
              muokkaaja = $muokkaaja,
              organisaatio_oid = $organisaatioOid,
              kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
            where oid = $oid
            and ( koulutus_oid is distinct from $koulutusOid
            or tila is distinct from ${tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(kielivalinta)}::jsonb
            or organisaatio_oid is distinct from $organisaatioOid )"""
  }

  def insertTarjoaja(oid:Option[String], tarjoaja:String, muokkaaja:String ) = {
    sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint toteutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid:Option[String], exclude:List[String]) = {
    sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid and tarjoaja_oid not in (#${createInParams(exclude)})"""
  }

  def deleteTarjoajat(oid:Option[String]) = sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid"""

  def selectByOrganisaatioOids(organisaatioOids:Seq[String]) = {
    sql"""select oid, nimi, tila, organisaatio_oid
          from toteutukset
          where organisaatio_oid in (#${createInParams(organisaatioOids)})""".as[OidListItem]
  }
}