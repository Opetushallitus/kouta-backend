package fi.oph.kouta.repository

import java.time.Instant
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Oppilaitos, OppilaitosAndOsa}
import fi.oph.kouta.util.MiscUtils.optionWhen
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation.ReadCommitted

import scala.concurrent.ExecutionContext.Implicits.global

trait OppilaitosDAO extends EntityModificationDAO[OrganisaatioOid] {
  def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos]
  def getUpdateActions(oppilaitos: Oppilaitos): DBIO[Option[Oppilaitos]]

  def get(oid: OrganisaatioOid): Option[(Oppilaitos, Instant)]
  def getTila(oppilaitosOid: OrganisaatioOid): Option[Julkaisutila]

  def get(oids: List[OrganisaatioOid]): Vector[OppilaitosAndOsa]
}

object OppilaitosDAO extends OppilaitosDAO with OppilaitosSQL {

  override def getPutActions(oppilaitos: Oppilaitos): DBIO[Oppilaitos] =
    for {
      _ <- insertOppilaitos(oppilaitos)
      m <- selectLastModified(oppilaitos.oid)
    } yield oppilaitos.withModified(m.get)

  override def get(oid: OrganisaatioOid): Option[(Oppilaitos, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(isolation = ReadCommitted)(
      for {
        k <- selectOppilaitos(oid)
        l <- selectLastModified(oid)
      } yield (k, l)
    ).get match {
      case (Some(k), Some(l)) => Some(k, l)
      case _ => None
    }
  }

  override def get(oids: List[OrganisaatioOid]): Vector[OppilaitosAndOsa] = {
    KoutaDatabase.runBlocking(selectOppilaitokset(oids).as[OppilaitosAndOsa])
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
            max(lower(o.system_time)))
          from oppilaitokset o
          where o.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[OrganisaatioOid]] = {
    sql"""select oid from oppilaitokset where $since < lower(system_time)
          union
          select oid from oppilaitokset_history where $since <@ system_time""".as[OrganisaatioOid]
  }
}

sealed trait OppilaitosSQL extends OppilaitosExtractors with OppilaitosModificationSQL with SQLHelpers {
  val selectOppilaitosAndOsaSQL =
    """select distinct oppilaitokset.oid,
      |       oppilaitokset.tila,
      |       oppilaitokset.kielivalinta,
      |       oppilaitokset.metadata,
      |       oppilaitokset.muokkaaja,
      |       oppilaitokset.esikatselu,
      |       oppilaitokset.organisaatio_oid,
      |       oppilaitokset.teemakuva,
      |       oppilaitokset.logo,
      |       oppilaitosten_osat.oid,
      |       oppilaitosten_osat.oppilaitos_oid,
      |       oppilaitosten_osat.tila,
      |       oppilaitosten_osat.kielivalinta,
      |       oppilaitosten_osat.metadata,
      |       oppilaitosten_osat.muokkaaja,
      |       oppilaitosten_osat.esikatselu,
      |       oppilaitosten_osat.organisaatio_oid,
      |       oppilaitosten_osat.teemakuva
      |""".stripMargin


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

  def selectOppilaitokset(oids: List[OrganisaatioOid]) = {
    sql"""#$selectOppilaitosAndOsaSQL
            from oppilaitokset
            full outer join oppilaitosten_osat
            on oppilaitokset.oid = oppilaitosten_osat.oppilaitos_oid
            where oppilaitokset.oid in (#${createOidInParams(oids)})
            union
            #$selectOppilaitosAndOsaSQL
            from oppilaitosten_osat
            right outer join oppilaitokset
            on oppilaitosten_osat.oppilaitos_oid = oppilaitokset.oid
            where oppilaitosten_osat.oid in (#${createOidInParams(oids)})
            """
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
