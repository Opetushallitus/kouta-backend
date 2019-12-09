package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.Oppilaitos
import fi.oph.kouta.domain.oid.OrganisaatioOid
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait OppilaitosDAO extends EntityModificationDAO[OrganisaatioOid] {
  def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos]
  def getUpdateActions(oppilaitos: Oppilaitos, notModifiedSince: Instant): DBIO[Option[Oppilaitos]]
  def getUpdateActionsWithoutModifiedCheck(oppilaitos: Oppilaitos): DBIO[Option[Oppilaitos]]

  def put(oppilaitos: Oppilaitos): Oppilaitos
  def get(oid: OrganisaatioOid): Option[(Oppilaitos, Instant)]
  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant): Option[Oppilaitos]
}

object OppilaitosDAO extends OppilaitosDAO with OppilaitosSQL {

  override def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos] =
    insertOppilaitos(oppilaitos).map(modified => oppilaitos.withModified(modified.max))

  override def put(oppilaitos: Oppilaitos): Oppilaitos =
    KoutaDatabase.runBlockingTransactionally(getPutActions(oppilaitos)).get

  override def get(oid: OrganisaatioOid): Option[(Oppilaitos, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        k <- selectOppilaitos(oid)
        l <- selectLastModified(oid)
      } yield (k, l)
    ).get match {
      case (Some(k), Some(l)) => Some(k, l)
      case _ => None
    }
  }

  override def getUpdateActionsWithoutModifiedCheck(oppilaitos: Oppilaitos): DBIO[Option[Oppilaitos]] =
    for {
      k <- updateOppilaitos(oppilaitos)
    } yield {
      val modified = k.sorted.lastOption
      modified.map(oppilaitos.withModified)
    }

  override def getUpdateActions(oppilaitos: Oppilaitos, notModifiedSince: Instant): DBIO[Option[Oppilaitos]] =
    checkNotModified(oppilaitos.oid, notModifiedSince)
      .andThen(getUpdateActionsWithoutModifiedCheck(oppilaitos))

  override def update(oppilaitos: Oppilaitos, notModifiedSince: Instant): Option[Oppilaitos] =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(oppilaitos, notModifiedSince)).get
}

sealed trait OppilaitosModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: OrganisaatioOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(o.system_time)),
            max(upper(oh.system_time)))
          from oppilaitokset o
          left join oppilaitokset_history oh on o.oid = oh.oid
          where o.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[OrganisaatioOid]] = {
    sql"""select oid from oppilaitokset where $since < lower(system_time)
          union
          select oid from oppilaitokset_history where $since <@ system_time""".as[OrganisaatioOid]
  }
}

sealed trait OppilaitosSQL extends OppilaitosExtractors with OppilaitosModificationSQL with SQLHelpers {

  def selectOppilaitos(oid: OrganisaatioOid): DBIO[Option[Oppilaitos]] = {
    sql"""select oid, tila, kielivalinta, metadata, muokkaaja, organisaatio_oid, lower(system_time)
          from oppilaitokset where oid = $oid""".as[Oppilaitos].headOption
  }

  def insertOppilaitos(oppilaitos: Oppilaitos): DBIO[Vector[Instant]] = {
    sql"""insert into oppilaitokset (
            oid,
            tila,
            kielivalinta,
            metadata,
            muokkaaja,
            organisaatio_oid)
          values (
            ${oppilaitos.oid},
            ${oppilaitos.tila.toString}::julkaisutila,
            ${toJsonParam(oppilaitos.kielivalinta)}::jsonb,
            ${toJsonParam(oppilaitos.metadata)}::jsonb,
            ${oppilaitos.muokkaaja},
            ${oppilaitos.organisaatioOid})
          returning lower(system_time)""".as[Instant]
  }

  def updateOppilaitos(oppilaitos: Oppilaitos): DBIO[Vector[Instant]] = {
    sql"""update oppilaitokset set
              tila = ${oppilaitos.tila.toString}::julkaisutila,
              kielivalinta = ${toJsonParam(oppilaitos.kielivalinta)}::jsonb,
              metadata = ${toJsonParam(oppilaitos.metadata)}::jsonb,
              muokkaaja = ${oppilaitos.muokkaaja},
              organisaatio_oid = ${oppilaitos.organisaatioOid}
            where oid = ${oppilaitos.oid}
            and ( tila is distinct from ${oppilaitos.tila.toString}::julkaisutila
            or metadata is distinct from ${toJsonParam(oppilaitos.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(oppilaitos.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${oppilaitos.organisaatioOid})
            returning lower(system_time)""".as[Instant]
  }
}
