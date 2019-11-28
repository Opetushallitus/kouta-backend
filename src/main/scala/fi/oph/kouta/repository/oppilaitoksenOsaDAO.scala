package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.{OppilaitoksenOsa, OppilaitoksenOsaListItem}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait OppilaitoksenOsaDAO extends EntityModificationDAO[OrganisaatioOid] {
  def getPutActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[OppilaitoksenOsa]
  def getUpdateActions(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant): DBIO[Boolean]

  def put(oppilaitoksenOsa: OppilaitoksenOsa): OppilaitoksenOsa
  def get(oid: OrganisaatioOid): Option[(OppilaitoksenOsa, Instant)]
  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant): Boolean

  def getByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsa]
  def listByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsaListItem]
  def listByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[OppilaitoksenOsaListItem]
}

object OppilaitoksenOsaDAO extends OppilaitoksenOsaDAO with OppilaitoksenOsaSQL {

  override def getPutActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[OppilaitoksenOsa] =
    checkOppilaitosExists(oppilaitoksenOsa).flatMap {
      case false => DBIO.failed(new NoSuchElementException(s"""Oppilaitos ${oppilaitoksenOsa.oppilaitosOid} ei löyty kannasta!"""))
      case true => insertOppilaitoksenOsa(oppilaitoksenOsa).andThen(DBIO.successful(oppilaitoksenOsa))
    }

  override def put(oppilaitoksenOsa: OppilaitoksenOsa): OppilaitoksenOsa =
    KoutaDatabase.runBlockingTransactionally(getPutActions(oppilaitoksenOsa)).get

  override def get(oid: OrganisaatioOid): Option[(OppilaitoksenOsa, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        k <- selectOppilaitoksenOsa(oid).as[OppilaitoksenOsa].headOption
        l <- selectLastModified(oid)
      } yield (k, l)
    ).get match {
      case (Some(k), Some(l)) => Some(k, l)
      case _ => None
    }
  }

  override def getUpdateActions(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant): DBIO[Boolean] = {
    checkNotModified(oppilaitoksenOsa.oid, notModifiedSince).andThen(
      for {
        k <- updateOppilaitoksenOsa(oppilaitoksenOsa)
      } yield 0 < k
    )
  }

  override def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(oppilaitoksenOsa, notModifiedSince)).get

  override def getByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsa] =
    KoutaDatabase.runBlocking(selectByOppilaitosOid(oppilaitosOid))

  override def listByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsaListItem] =
    KoutaDatabase.runBlocking(selectListByOppilaitosOid(oppilaitosOid))

  override def listByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[OppilaitoksenOsaListItem] =
    KoutaDatabase.runBlocking(selectListByOppilaitosOidAndOrganisaatioOids(oppilaitosOid, organisaatioOids))
}

sealed trait OppilaitoksenOsaModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: OrganisaatioOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(o.system_time)),
            max(upper(oh.system_time)))
          from oppilaitosten_osat o
          left join oppilaitosten_osat_history oh on o.oid = oh.oid
          where o.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[OrganisaatioOid]] = {
    sql"""select oid from oppilaitosten_osat where $since < lower(system_time)
          union
          select oid from oppilaitosten_osat_history where $since <@ system_time""".as[OrganisaatioOid]
  }
}

sealed trait OppilaitoksenOsaSQL extends OppilaitoksenOsaExtractors with OppilaitoksenOsaModificationSQL with SQLHelpers {

  def checkOppilaitosExists(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Boolean]  = {
    sql"""select 1 from oppilaitokset where oid = ${oppilaitoksenOsa.oppilaitosOid}""".as[Int].headOption.map(_.isDefined)
  }

  def selectOppilaitoksenOsa(oid: OrganisaatioOid) = {
    sql"""select oid, oppilaitos_oid, tila, kielivalinta, metadata, muokkaaja, organisaatio_oid, lower(system_time)
          from oppilaitosten_osat where oid = $oid"""
  }

  def selectByOppilaitosOid(oppilaitosOid: OrganisaatioOid) = {
    sql"""select oid, oppilaitos_oid, tila, kielivalinta, metadata, muokkaaja, organisaatio_oid, lower(system_time)
          from oppilaitosten_osat where oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsa]
  }

  def insertOppilaitoksenOsa(oppilaitoksenOsa: OppilaitoksenOsa) = {
    sqlu"""insert into oppilaitosten_osat (
            oid,
            oppilaitos_oid,
            tila,
            kielivalinta,
            metadata,
            muokkaaja,
            organisaatio_oid)
          values (
            ${oppilaitoksenOsa.oid},
            ${oppilaitoksenOsa.oppilaitosOid},
            ${oppilaitoksenOsa.tila.toString}::julkaisutila,
            ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb,
            ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb,
            ${oppilaitoksenOsa.muokkaaja},
            ${oppilaitoksenOsa.organisaatioOid})"""
  }

  def updateOppilaitoksenOsa(oppilaitoksenOsa: OppilaitoksenOsa) = {
    sqlu"""update oppilaitosten_osat set
              tila = ${oppilaitoksenOsa.tila.toString}::julkaisutila,
              kielivalinta = ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb,
              metadata = ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb,
              muokkaaja = ${oppilaitoksenOsa.muokkaaja},
              organisaatio_oid = ${oppilaitoksenOsa.organisaatioOid}
            where oid = ${oppilaitoksenOsa.oid}
            and (tila is distinct from ${oppilaitoksenOsa.tila.toString}::julkaisutila
            or metadata is distinct from ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${oppilaitoksenOsa.organisaatioOid})"""
  }

  def selectListByOppilaitosOid(oppilaitosOid: OrganisaatioOid) = {
    sql"""select oid, oppilaitos_oid, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from oppilaitosten_osat
          where oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsaListItem]
  }

  def selectListByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select oid, oppilaitos_oid, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from oppilaitosten_osat
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          and oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsaListItem]
  }
}
