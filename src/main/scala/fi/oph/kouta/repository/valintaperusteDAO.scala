package fi.oph.kouta.repository

import java.time.Instant
import java.util.{ConcurrentModificationException, UUID}

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import fi.oph.kouta.domain.{IdListItem, Valintaperuste}
import slick.dbio.DBIO

trait ValintaperusteDAO extends EntityModificationDAO[UUID] {
  def put(valintaperuste:Valintaperuste):Option[UUID]
  def get(id:UUID): Option[(Valintaperuste, Instant)]
  def update(valintaperuste:Valintaperuste, notModifiedSince:Instant): Boolean

  def listByOrganisaatioOids(organisaatioOids:Seq[String]):Seq[IdListItem]
}

object ValintaperusteDAO extends ValintaperusteDAO with ValintaperusteSQL {

  override def put(valintaperuste: Valintaperuste): Option[UUID] = {
    KoutaDatabase.runBlocking(insertValintaperuste(valintaperuste)) match {
      case x if x < 1 => None
      case _ => valintaperuste.id
    }
  }

  override def get(id: UUID): Option[(Valintaperuste, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      v <- selectValintaperuste(id).as[Valintaperuste].headOption
      l <- selectLastModified(id)
    } yield (v, l) ) match {
      case Left(t) => {t.printStackTrace(); throw t}
      case Right((None, _)) | Right((_, None)) => None
      case Right((Some(v), Some(l))) => Some((v, l))
    }
  }

  override def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(valintaperuste.id.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown valintaperuste id ${valintaperuste.id.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut valintaperustetta ${valintaperuste.id.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateValintaperuste(valintaperuste))) match {
      case Left(t) => throw t
      case Right(x) => 0 < x
    }
  }

  override def listByOrganisaatioOids(organisaatioOids:Seq[String]):Seq[IdListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))
}

sealed trait ValintaperusteModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since:Instant): DBIO[Seq[UUID]] = {
    sql"""select id from valintaperusteet where $since < lower(system_time)
          union
          select id from valintaperusteet_history where $since <@ system_time""".as[UUID]
  }

  def selectLastModified(id:UUID):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(vp.system_time)),
            max(upper(vph.system_time)))
          from valintaperusteet vp
          left join valintaperusteet_history vph on vp.id = vph.id
          where vp.id = ${id.toString}::uuid""".as[Option[Instant]].head
  }
}

sealed trait ValintaperusteSQL extends ValintaperusteExtractors with ValintaperusteModificationSQL with SQLHelpers {

  def insertValintaperuste(valintaperuste: Valintaperuste) = {
    val Valintaperuste(id, tila, hakutapaKoodiUri, kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, nimi,
    onkoJulkinen, metadata, organisaatioOid, muokkaaja, kielivalinta) = valintaperuste
    sqlu"""insert into valintaperusteet (
                     id,
                     tila,
                     hakutapa_koodi_uri,
                     kohdejoukko_koodi_uri,
                     kohdejoukon_tarkenne_koodi_uri,
                     nimi,
                     onkoJulkinen,
                     metadata,
                     organisaatio_oid,
                     muokkaaja,
                     kielivalinta
         ) values (
                     ${id.map(_.toString)}::uuid,
                     ${tila.toString}::julkaisutila,
                     $hakutapaKoodiUri,
                     $kohdejoukkoKoodiUri,
                     $kohdejoukonTarkenneKoodiUri,
                     ${toJsonParam(nimi)}::jsonb,
                     $onkoJulkinen,
                     ${toJsonParam(metadata)}::jsonb,
                     $organisaatioOid,
                     $muokkaaja,
                     ${toJsonParam(kielivalinta)}::jsonb
         )"""
  }

  def selectValintaperuste(id:UUID) =
    sql"""select id, tila, hakutapa_koodi_uri, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, nimi,
                 onkoJulkinen, metadata, organisaatio_oid, muokkaaja, kielivalinta
          from valintaperusteet where id = ${id.toString}::uuid"""

  def updateValintaperuste(valintaperuste: Valintaperuste) = {
    val Valintaperuste(id, tila, hakutapaKoodiUri, kohdejoukkoKoodiUri, kohdejoukonTarkenneKoodiUri, nimi,
    onkoJulkinen, metadata, organisaatioOid, muokkaaja, kielivalinta) = valintaperuste
    sqlu"""update valintaperusteet set
                     tila = ${tila.toString}::julkaisutila,
                     hakutapa_koodi_uri = $hakutapaKoodiUri,
                     kohdejoukko_koodi_uri = $kohdejoukkoKoodiUri,
                     kohdejoukon_tarkenne_koodi_uri = $kohdejoukonTarkenneKoodiUri,
                     nimi = ${toJsonParam(nimi)}::jsonb,
                     onkoJulkinen = $onkoJulkinen,
                     metadata = ${toJsonParam(metadata)}::jsonb,
                     organisaatio_oid = $organisaatioOid,
                     muokkaaja = $muokkaaja,
                     kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
           where id = ${id.map(_.toString)}::uuid
           and (tila is distinct from ${tila.toString}::julkaisutila
           or hakutapa_koodi_uri is distinct from $hakutapaKoodiUri
           or kohdejoukko_koodi_uri is distinct from $kohdejoukkoKoodiUri
           or kohdejoukon_tarkenne_koodi_uri is distinct from $kohdejoukonTarkenneKoodiUri
           or nimi is distinct from ${toJsonParam(nimi)}::jsonb
           or onkoJulkinen is distinct from $onkoJulkinen
           or metadata is distinct from ${toJsonParam(metadata)}::jsonb
           or organisaatio_oid is distinct from $organisaatioOid
           or muokkaaja is distinct from $muokkaaja
           or kielivalinta is distinct from ${toJsonParam(kielivalinta)}::jsonb
         )"""
  }

  def selectByOrganisaatioOids(organisaatioOids:Seq[String]) = {
    sql"""select id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from valintaperusteet
          where organisaatio_oid in (#${createInParams(organisaatioOids)})""".as[IdListItem]
  }
}