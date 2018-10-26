package fi.oph.kouta.repository

import java.time.Instant
import java.util.{ConcurrentModificationException, UUID}

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import fi.oph.kouta.domain.Valintaperuste
import slick.dbio.DBIO

trait ValintaperusteDAO {
  def put(valintaperuste:Valintaperuste):Option[UUID]
  def get(id:UUID): Option[(Valintaperuste, Instant)]
  def getLastModified(id:UUID): Option[Instant]
  def update(valintaperuste:Valintaperuste, notModifiedSince:Instant): Boolean
}

object ValintaperusteDAO extends ValintaperusteDAO with ValintaperusteExtractors with SQLHelpers {
  private def insertValintaperuste(valintaperuste: Valintaperuste) = {
    val Valintaperuste(id, tila, hakutapa, kohdejoukko, kohdejoukonTarkenne, nimi,
                       onkoJulkinen, metadata, organisaatio, muokkaaja, kielivalinta) = valintaperuste
    sqlu"""insert into valintaperusteet (
                     id,
                     tila,
                     hakutapa,
                     kohdejoukko,
                     kohdejoukon_tarkenne,
                     nimi,
                     onkoJulkinen,
                     metadata,
                     organisaatio,
                     muokkaaja,
                     kielivalinta
         ) values (
                     ${id.map(_.toString)}::uuid,
                     ${tila.toString}::julkaisutila,
                     $hakutapa,
                     $kohdejoukko,
                     $kohdejoukonTarkenne,
                     ${toJsonParam(nimi)}::jsonb,
                     $onkoJulkinen,
                     ${toJsonParam(metadata)}::jsonb,
                     $organisaatio,
                     $muokkaaja,
                     ${toJsonParam(kielivalinta)}::jsonb
         )"""
  }

  override def put(valintaperuste: Valintaperuste): Option[UUID] = {
    KoutaDatabase.runBlocking(insertValintaperuste(valintaperuste)) match {
      case x if x < 1 => None
      case _ => valintaperuste.id
    }
  }

  private def selectValintaperuste(id:UUID) = {
    sql"""select id, tila, hakutapa, kohdejoukko, kohdejoukon_tarkenne, nimi,
                 onkoJulkinen, metadata, organisaatio, muokkaaja, kielivalinta
          from valintaperusteet where id = ${id.toString}::uuid"""
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

  private def selectLastModified(id:UUID):DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(vp.system_time)),
            max(upper(vph.system_time)))
          from valintaperusteet vp
          left join valintaperusteet_history vph on vp.id = vph.id
          where vp.id = ${id.toString}::uuid""".as[Option[Instant]].head
  }

  override def getLastModified(id: UUID): Option[Instant] = KoutaDatabase.runBlocking( selectLastModified(id) )
  
  private def updateValintaperuste(valintaperuste: Valintaperuste) = {
    val Valintaperuste(id, tila, hakutapa, kohdejoukko, kohdejoukonTarkenne, nimi,
                       onkoJulkinen, metadata, organisaatio, muokkaaja, kielivalinta) = valintaperuste
    sqlu"""update valintaperusteet set
                     tila = ${tila.toString}::julkaisutila,
                     hakutapa = $hakutapa,
                     kohdejoukko = $kohdejoukko,
                     kohdejoukon_tarkenne = $kohdejoukonTarkenne,
                     nimi = ${toJsonParam(nimi)}::jsonb,
                     onkoJulkinen = $onkoJulkinen,
                     metadata = ${toJsonParam(metadata)}::jsonb,
                     organisaatio = $organisaatio,
                     muokkaaja = $muokkaaja,
                     kielivalinta = ${toJsonParam(kielivalinta)}::jsonb
           where id = ${id.map(_.toString)}::uuid
           and (tila <> ${tila.toString}::julkaisutila
           or hakutapa <> $hakutapa
           or kohdejoukko <> $kohdejoukko
           or kohdejoukon_tarkenne <> $kohdejoukonTarkenne
           or nimi <> ${toJsonParam(nimi)}::jsonb
           or onkoJulkinen <> $onkoJulkinen
           or metadata <> ${toJsonParam(metadata)}::jsonb
           or organisaatio <> $organisaatio
           or muokkaaja <> $muokkaaja
           or kielivalinta <> ${toJsonParam(kielivalinta)}::jsonb
         )"""
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
}