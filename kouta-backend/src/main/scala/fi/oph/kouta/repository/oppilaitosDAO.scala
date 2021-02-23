package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Oppilaitos}
import fi.oph.kouta.util.MiscUtils.optionWhen
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait OppilaitosDAO extends EntityModificationDAO[OrganisaatioOid] {
  def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos]
  def getUpdateActions(oppilaitos: Oppilaitos): DBIO[Option[Oppilaitos]]

  def get(oid: OrganisaatioOid): Option[(Oppilaitos, Instant)]
}

object OppilaitosDAO extends OppilaitosDAO with OppilaitosSQL {

  override def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos] =
    for {
      _ <- insertOppilaitos(oppilaitos)
      m <- selectLastModified(oppilaitos.oid)
    } yield oppilaitos.withModified(m.get)

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

  override def getUpdateActions(oppilaitos: Oppilaitos): DBIO[Option[Oppilaitos]] =
    for {
      k <- updateOppilaitos(oppilaitos)
      m <- selectLastModified(oppilaitos.oid)
    } yield optionWhen(k > 0)(oppilaitos.withModified(m.get))

  def updateJustOppilaitos(oppilaitos: Oppilaitos): DBIO[Oppilaitos] =
    for {
      _ <- updateOppilaitos(oppilaitos)
      m <- selectLastModified(oppilaitos.oid)
    } yield oppilaitos.withModified(m.get)

  def getTila(oppilaitosOid: OrganisaatioOid): Option[Julkaisutila] =
    KoutaDatabase.runBlocking(selectTila(oppilaitosOid))
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
    sql"""select oid,
                 tila,
                 kielivalinta,
                 metadata,
                 muokkaaja,
                 esikatselu,
                 organisaatio_oid,
                 teemakuva,
                 logo,
                 lower(system_time)
          from oppilaitokset
          where oid = $oid""".as[Oppilaitos].headOption
  }

  def insertOppilaitos(oppilaitos: Oppilaitos): DBIO[Int] = {
    sqlu"""insert into oppilaitokset (
            oid,
            tila,
            kielivalinta,
            metadata,
            muokkaaja,
            esikatselu,
            organisaatio_oid,
            teemakuva,
            logo)
          values (
            ${oppilaitos.oid},
            ${oppilaitos.tila.toString}::julkaisutila,
            ${toJsonParam(oppilaitos.kielivalinta)}::jsonb,
            ${toJsonParam(oppilaitos.metadata)}::jsonb,
            ${oppilaitos.muokkaaja},
            ${oppilaitos.esikatselu},
            ${oppilaitos.organisaatioOid},
            ${oppilaitos.teemakuva},
            ${oppilaitos.logo})"""
  }

  def updateOppilaitos(oppilaitos: Oppilaitos): DBIO[Int] = {
    sqlu"""update oppilaitokset set
              tila = ${oppilaitos.tila.toString}::julkaisutila,
              kielivalinta = ${toJsonParam(oppilaitos.kielivalinta)}::jsonb,
              metadata = ${toJsonParam(oppilaitos.metadata)}::jsonb,
              muokkaaja = ${oppilaitos.muokkaaja},
              esikatselu = ${oppilaitos.esikatselu},
              organisaatio_oid = ${oppilaitos.organisaatioOid},
              teemakuva = ${oppilaitos.teemakuva},
              logo = ${oppilaitos.logo}
            where oid = ${oppilaitos.oid}
            and (
              tila is distinct from ${oppilaitos.tila.toString}::julkaisutila
              or metadata is distinct from ${toJsonParam(oppilaitos.metadata)}::jsonb
              or kielivalinta is distinct from ${toJsonParam(oppilaitos.kielivalinta)}::jsonb
              or organisaatio_oid is distinct from ${oppilaitos.organisaatioOid}
              or esikatselu is distinct from ${oppilaitos.esikatselu}
              or teemakuva is distinct from ${oppilaitos.teemakuva}
              or logo is distinct from ${oppilaitos.logo}
            )"""
  }

  def selectTila(oppilaitosOid: OrganisaatioOid): DBIO[Option[Julkaisutila]] =
    sql"""select tila from oppilaitokset
            where oid = $oppilaitosOid
    """.as[Julkaisutila].headOption
}
