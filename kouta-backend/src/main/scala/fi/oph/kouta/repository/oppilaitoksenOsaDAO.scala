package fi.oph.kouta.repository

import java.time.Instant
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{OppilaitoksenOsa, OppilaitoksenOsaListItem, TilaFilter}
import fi.oph.kouta.servlet.EntityNotFoundException
import fi.oph.kouta.util.MiscUtils.optionWhen
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation.ReadCommitted

import scala.concurrent.ExecutionContext.Implicits.global

trait OppilaitoksenOsaDAO extends EntityModificationDAO[OrganisaatioOid] {
  def getPutActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[OppilaitoksenOsa]
  def getUpdateActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Option[OppilaitoksenOsa]]

  def get(oid: OrganisaatioOid): Option[(OppilaitoksenOsa, Instant)]
  def getByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsa]
  def listByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsaListItem]
  def listByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[OppilaitoksenOsaListItem]
}

object OppilaitoksenOsaDAO extends OppilaitoksenOsaDAO with OppilaitoksenOsaSQL {
  def oppilaitosExists(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[_] =
    checkOppilaitosExists(oppilaitoksenOsa).flatMap {
      case false => DBIO.failed(EntityNotFoundException(s"""Oppilaitos ${oppilaitoksenOsa.oppilaitosOid} ei löyty kannasta!"""))
      case true => DBIO.successful(true)
    }

  override def getPutActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[OppilaitoksenOsa] =
    for {
      _ <- insertOppilaitoksenOsa(oppilaitoksenOsa)
      m <- selectLastModified(oppilaitoksenOsa.oid)
    } yield oppilaitoksenOsa.withModified(m.get)

  override def get(oid: OrganisaatioOid): Option[(OppilaitoksenOsa, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(isolation = ReadCommitted)(
      for {
        k <- selectOppilaitoksenOsa(oid)
        l <- selectLastModified(oid)
      } yield (k, l)
    ).get match {
      case (Some(k), Some(l)) => Some(k, l)
      case _ => None
    }
  }

  override def getUpdateActions(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Option[OppilaitoksenOsa]] =
    for {
      k <- updateOppilaitoksenOsa(oppilaitoksenOsa)
      m <- selectLastModified(oppilaitoksenOsa.oid)
    } yield optionWhen(k > 0)(oppilaitoksenOsa.withModified(m.get))

  def updateJustOppilaitoksenOsa(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[OppilaitoksenOsa] =
    for {
      _ <- updateOppilaitoksenOsa(oppilaitoksenOsa)
      m <- selectLastModified(oppilaitoksenOsa.oid)
    } yield oppilaitoksenOsa.withModified(m.get)

  override def getByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsa] =
    KoutaDatabase.runBlocking(selectByOppilaitosOid(oppilaitosOid))

  def getJulkaistutByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsa] =
    KoutaDatabase.runBlocking(selectJulkaistutByOppilaitosOid(oppilaitosOid))

  override def listByOppilaitosOid(oppilaitosOid: OrganisaatioOid): Seq[OppilaitoksenOsaListItem] =
    KoutaDatabase.runBlocking(selectListByOppilaitosOid(oppilaitosOid))

  override def listByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[OppilaitoksenOsaListItem] =
    KoutaDatabase.runBlocking(selectListByOppilaitosOidAndOrganisaatioOids(oppilaitosOid, organisaatioOids))
}

sealed trait OppilaitoksenOsaModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: OrganisaatioOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(o.system_time)))
          from oppilaitosten_osat o
          where o.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[OrganisaatioOid]] = {
    sql"""select oid from oppilaitosten_osat where $since < lower(system_time)
          union
          select oid from oppilaitosten_osat_history where $since <@ system_time""".as[OrganisaatioOid]
  }
}

sealed trait OppilaitoksenOsaSQL extends OppilaitoksenOsaExtractors with OppilaitoksenOsaModificationSQL with SQLHelpers {
  val selectOppilaitoksenOsaSQL =
    """select oid,
      |       oppilaitos_oid,
      |       tila,
      |       kielivalinta,
      |       metadata,
      |       muokkaaja,
      |       esikatselu,
      |       organisaatio_oid,
      |       teemakuva,
      |       lower(system_time)
      |""".stripMargin

  def checkOppilaitosExists(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Boolean]  = {
    sql"""select 1 from oppilaitokset where oid = ${oppilaitoksenOsa.oppilaitosOid}""".as[Int].headOption.map(_.isDefined)
  }

  def selectOppilaitoksenOsa(oid: OrganisaatioOid): DBIO[Option[OppilaitoksenOsa]] = {
    sql"""select oid,
                 oppilaitos_oid,
                 tila,
                 kielivalinta,
                 metadata,
                 muokkaaja,
                 esikatselu,
                 organisaatio_oid,
                 teemakuva,
                 lower(system_time)
          from oppilaitosten_osat
          where oid = $oid""".as[OppilaitoksenOsa].headOption
  }

  def selectByOppilaitosOid(oppilaitosOid: OrganisaatioOid): DBIO[Vector[OppilaitoksenOsa]] = {
    sql"""#$selectOppilaitoksenOsaSQL
          from oppilaitosten_osat
          where oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsa]
  }

  def selectJulkaistutByOppilaitosOid(oppilaitosOid: OrganisaatioOid): DBIO[Vector[OppilaitoksenOsa]] = {
    sql"""#$selectOppilaitoksenOsaSQL
          from oppilaitosten_osat
          where oppilaitos_oid = $oppilaitosOid #${tilaConditions(TilaFilter.onlyJulkaistut)}""".as[OppilaitoksenOsa]
  }

  def insertOppilaitoksenOsa(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Int] = {
    sqlu"""insert into oppilaitosten_osat (
             oid,
             oppilaitos_oid,
             tila,
             kielivalinta,
             metadata,
             esikatselu,
             muokkaaja,
             organisaatio_oid,
             teemakuva)
           values (
             ${oppilaitoksenOsa.oid},
             ${oppilaitoksenOsa.oppilaitosOid},
             ${oppilaitoksenOsa.tila.toString}::julkaisutila,
             ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb,
             ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb,
             ${oppilaitoksenOsa.esikatselu},
             ${oppilaitoksenOsa.muokkaaja},
             ${oppilaitoksenOsa.organisaatioOid},
             ${oppilaitoksenOsa.teemakuva})"""
  }

  def updateOppilaitoksenOsa(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Int] = {
    sqlu"""update oppilaitosten_osat set
              tila = ${oppilaitoksenOsa.tila.toString}::julkaisutila,
              kielivalinta = ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb,
              metadata = ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb,
              esikatselu = ${oppilaitoksenOsa.esikatselu},
              muokkaaja = ${oppilaitoksenOsa.muokkaaja},
              organisaatio_oid = ${oppilaitoksenOsa.organisaatioOid},
              teemakuva = ${oppilaitoksenOsa.teemakuva}
            where oid = ${oppilaitoksenOsa.oid}
            and (
              tila is distinct from ${oppilaitoksenOsa.tila.toString}::julkaisutila
              or metadata is distinct from ${toJsonParam(oppilaitoksenOsa.metadata)}::jsonb
              or kielivalinta is distinct from ${toJsonParam(oppilaitoksenOsa.kielivalinta)}::jsonb
              or organisaatio_oid is distinct from ${oppilaitoksenOsa.organisaatioOid}
              or esikatselu is distinct from ${oppilaitoksenOsa.esikatselu}
              or teemakuva is distinct from ${oppilaitoksenOsa.teemakuva}
            )"""
  }

  def selectListByOppilaitosOid(oppilaitosOid: OrganisaatioOid): DBIO[Vector[OppilaitoksenOsaListItem]] = {
    sql"""select oid, oppilaitos_oid, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from oppilaitosten_osat
          where oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsaListItem]
  }

  def selectListByOppilaitosOidAndOrganisaatioOids(oppilaitosOid: OrganisaatioOid, organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[OppilaitoksenOsaListItem]] = {
    sql"""select oid, oppilaitos_oid, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from oppilaitosten_osat
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          and oppilaitos_oid = $oppilaitosOid""".as[OppilaitoksenOsaListItem]
  }
}
