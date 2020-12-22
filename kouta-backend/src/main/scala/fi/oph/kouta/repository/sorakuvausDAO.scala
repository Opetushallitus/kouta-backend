package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{Julkaisutila, Koulutustyyppi, Sorakuvaus, SorakuvausListItem}
import fi.oph.kouta.util.MiscUtils.optionWhen
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait SorakuvausDAO extends EntityModificationDAO[UUID] {
  def getPutActions(sorakuvaus: Sorakuvaus): DBIO[Sorakuvaus]
  def getUpdateActions(sorakuvaus: Sorakuvaus): DBIO[Option[Sorakuvaus]]

  def get(id: UUID): Option[(Sorakuvaus, Instant)]
  def listByKoulutustyypit(koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): Seq[SorakuvausListItem]
}

object SorakuvausDAO extends SorakuvausDAO with SorakuvausSQL {

  override def getPutActions(sorakuvaus: Sorakuvaus): DBIO[Sorakuvaus] = {
    for {
      id <- DBIO.successful(UUID.randomUUID)
      _ <- insertSorakuvaus(sorakuvaus.withId(id))
      m <- selectLastModified(id)
    } yield sorakuvaus.withId(id).withModified(m.get)
  }

  override def getUpdateActions(sorakuvaus: Sorakuvaus): DBIO[Option[Sorakuvaus]] =
    for {
      v <- updateSorakuvaus(sorakuvaus)
      m <- selectLastModified(sorakuvaus.id.get)
    } yield optionWhen(v > 0)(sorakuvaus.withModified(m.get))

  override def get(id: UUID): Option[(Sorakuvaus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(for {
      v <- selectSorakuvaus(id)
      l <- selectLastModified(id)
    } yield (v, l) match {
      case (Some(sorakuvaus), Some(lastModified)) => Some((sorakuvaus, lastModified))
      case _ => None
    }).get
  }

  override def listByKoulutustyypit(koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): Seq[SorakuvausListItem] =
    koulutustyypit match {
      case Nil => Seq()
      case _   => KoutaDatabase.runBlocking(selectByKoulutustyypit(koulutustyypit, myosArkistoidut))
    }

  def getTilaAndTyyppi(sorakuvausId: UUID): (Option[Julkaisutila], Option[Koulutustyyppi]) =
    KoutaDatabase.runBlocking(selectTilaAndTyyppi(sorakuvausId)) match {
      case None => (None, None)
      case Some((tila, tyyppi)) => (Some(tila), Some(tyyppi))
    }
}

sealed trait SorakuvausModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[UUID]] = {
    sql"""select id from sorakuvaukset where $since < lower(system_time)
          union
          select id from sorakuvaukset_history where $since <@ system_time""".as[UUID]
  }

  def selectLastModified(id: UUID): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(sk.system_time)),
            max(upper(skh.system_time)))
          from sorakuvaukset sk
          left join sorakuvaukset_history skh on sk.id = skh.id
          where sk.id = ${id.toString}::uuid""".as[Option[Instant]].head
  }
}

sealed trait SorakuvausSQL extends SorakuvausExtractors with SorakuvausModificationSQL with SQLHelpers {

  def insertSorakuvaus(sorakuvaus: Sorakuvaus): DBIO[Int] = {
    sqlu"""insert into sorakuvaukset (
                     id,
                     tila,
                     nimi,
                     koulutustyyppi,
                     kielivalinta,
                     metadata,
                     organisaatio_oid,
                     muokkaaja
         ) values (  ${sorakuvaus.id.map(_.toString)}::uuid,
                     ${sorakuvaus.tila.toString}::julkaisutila,
                     ${toJsonParam(sorakuvaus.nimi)}::jsonb,
                     ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi,
                     ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb,
                     ${toJsonParam(sorakuvaus.metadata)}::jsonb,
                     ${sorakuvaus.organisaatioOid},
                     ${sorakuvaus.muokkaaja} )"""
  }

  def selectSorakuvaus(id: UUID): DBIO[Option[Sorakuvaus]] =
    sql"""select id, tila, nimi, koulutustyyppi, kielivalinta,
                 metadata, organisaatio_oid, muokkaaja, lower(system_time)
          from sorakuvaukset where id = ${id.toString}::uuid""".as[Sorakuvaus].headOption

  def updateSorakuvaus(sorakuvaus: Sorakuvaus): DBIO[Int] = {
    sqlu"""update sorakuvaukset set
                     tila = ${sorakuvaus.tila.toString}::julkaisutila,
                     nimi = ${toJsonParam(sorakuvaus.nimi)}::jsonb,
                     koulutustyyppi = ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi,
                     kielivalinta = ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb,
                     metadata = ${toJsonParam(sorakuvaus.metadata)}::jsonb,
                     organisaatio_oid = ${sorakuvaus.organisaatioOid},
                     muokkaaja = ${sorakuvaus.muokkaaja}
           where id = ${sorakuvaus.id.map(_.toString)}::uuid
           and (koulutustyyppi is distinct from ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi
             or tila is distinct from ${sorakuvaus.tila.toString}::julkaisutila
             or nimi is distinct from ${toJsonParam(sorakuvaus.nimi)}::jsonb
             or metadata is distinct from ${toJsonParam(sorakuvaus.metadata)}::jsonb
             or organisaatio_oid is distinct from ${sorakuvaus.organisaatioOid}
             or kielivalinta is distinct from ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb)"""
  }

  def selectByKoulutustyypit(koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): DBIO[Vector[SorakuvausListItem]] = {
    sql"""select id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from sorakuvaukset
          where koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})
          #${andTilaMaybeNotArkistoitu(myosArkistoidut)}""".as[SorakuvausListItem]
  }

  def selectTilaAndTyyppi(sorakuvausId: UUID): DBIO[Option[(Julkaisutila, Koulutustyyppi)]] =
    sql"""select tila, koulutustyyppi from sorakuvaukset
            where id = ${sorakuvausId.toString}::uuid
    """.as[(Julkaisutila, Koulutustyyppi)].headOption
}
