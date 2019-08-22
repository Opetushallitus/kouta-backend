package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausListItem}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.dbio.DBIO

trait SorakuvausDAO extends EntityModificationDAO[UUID] {
  def getPutActions(sorakuvaus: Sorakuvaus): DBIO[UUID]
  def getUpdateActions(sorakuvaus: Sorakuvaus, notModifiedSince: Instant): DBIO[Boolean]

  def put(sorakuvaus: Sorakuvaus): UUID
  def get(id: UUID): Option[(Sorakuvaus, Instant)]
  def update(valintaperuste: Sorakuvaus, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid]): Seq[SorakuvausListItem]
}

object SorakuvausDAO extends SorakuvausDAO with SorakuvausSQL {

  override def getPutActions(sorakuvaus: Sorakuvaus): DBIO[UUID] = {
    for {
      id <- DBIO.successful(UUID.randomUUID)
      _ <- insertSorakuvaus(sorakuvaus.copy(id = Some(id)))
    } yield id
  }

  override def put(sorakuvaus: Sorakuvaus): UUID =
    KoutaDatabase.runBlockingTransactionally(getPutActions(sorakuvaus)).get

  override def getUpdateActions(sorakuvaus: Sorakuvaus, notModifiedSince: Instant): DBIO[Boolean] =
    checkNotModified(sorakuvaus.id.get, notModifiedSince).andThen(
      for {
        v <- updateSorakuvaus(sorakuvaus)
      } yield 0 < v
    )

  override def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(sorakuvaus, notModifiedSince)).get

  override def get(id: UUID): Option[(Sorakuvaus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(for {
      v <- selectSorakuvaus(id).as[Sorakuvaus].headOption
      l <- selectLastModified(id)
    } yield (v, l) match {
      case (Some(v), Some(l)) => Some((v, l))
      case _ => None
    }).get
  }

  override def listByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid]): Seq[SorakuvausListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOidsOrJulkinen(organisaatioOids))
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

  def insertSorakuvaus(sorakuvaus: Sorakuvaus) = {
    sqlu"""insert into sorakuvaukset (
                     id,
                     tila,
                     nimi,
                     koulutustyyppi,
                     julkinen,
                     kielivalinta,
                     metadata,
                     organisaatio_oid,
                     muokkaaja
         ) values (  ${sorakuvaus.id.map(_.toString)}::uuid,
                     ${sorakuvaus.tila.toString}::julkaisutila,
                     ${toJsonParam(sorakuvaus.nimi)}::jsonb,
                     ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi,
                     ${sorakuvaus.julkinen},
                     ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb,
                     ${toJsonParam(sorakuvaus.metadata)}::jsonb,
                     ${sorakuvaus.organisaatioOid},
                     ${sorakuvaus.muokkaaja}
         )"""
  }

  def selectSorakuvaus(id: UUID) =
    sql"""select id, tila, nimi, koulutustyyppi, julkinen, kielivalinta,
                 metadata, organisaatio_oid, muokkaaja, lower(system_time)
          from sorakuvaukset where id = ${id.toString}::uuid"""

  def updateSorakuvaus(sorakuvaus: Sorakuvaus) = {
    sqlu"""update sorakuvaukset set
                     tila = ${sorakuvaus.tila.toString}::julkaisutila,
                     nimi = ${toJsonParam(sorakuvaus.nimi)}::jsonb,
                     koulutustyyppi = ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi,
                     julkinen = ${sorakuvaus.julkinen},
                     kielivalinta = ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb,
                     metadata = ${toJsonParam(sorakuvaus.metadata)}::jsonb,
                     organisaatio_oid = ${sorakuvaus.organisaatioOid},
                     muokkaaja = ${sorakuvaus.muokkaaja}
           where id = ${sorakuvaus.id.map(_.toString)}::uuid
           and (koulutustyyppi is distinct from ${sorakuvaus.koulutustyyppi.toString}::koulutustyyppi
           or tila is distinct from ${sorakuvaus.tila.toString}::julkaisutila
           or nimi is distinct from ${toJsonParam(sorakuvaus.nimi)}::jsonb
           or julkinen is distinct from ${sorakuvaus.julkinen}
           or metadata is distinct from ${toJsonParam(sorakuvaus.metadata)}::jsonb
           or organisaatio_oid is distinct from ${sorakuvaus.organisaatioOid}
           or muokkaaja is distinct from ${sorakuvaus.muokkaaja}
           or kielivalinta is distinct from ${toJsonParam(sorakuvaus.kielivalinta)}::jsonb
         )"""
  }

  def selectByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from sorakuvaukset
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[SorakuvausListItem]
  }
}