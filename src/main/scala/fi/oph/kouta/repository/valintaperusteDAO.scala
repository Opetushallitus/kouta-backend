package fi.oph.kouta.repository

import java.time.Instant
import java.util.{ConcurrentModificationException, UUID}
import fi.oph.kouta.domain.oid._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import fi.oph.kouta.domain.{Valintaperuste, ValintaperusteListItem}
import slick.dbio.DBIO

trait ValintaperusteDAO extends EntityModificationDAO[UUID] {
  def getPutActions(valintaperuste: Valintaperuste): DBIO[UUID]
  def getUpdateActions(valintaperuste: Valintaperuste, notModifiedSince: Instant): DBIO[Boolean]

  def put(valintaperuste: Valintaperuste): UUID
  def get(id: UUID): Option[(Valintaperuste, Instant)]
  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ValintaperusteListItem]
  def ListByOrganisaatioOidAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid): Seq[ValintaperusteListItem]
  def listBySorakuvausId(sorakuvausId: UUID): Seq[ValintaperusteListItem]
}

object ValintaperusteDAO extends ValintaperusteDAO with ValintaperusteSQL {

  override def getPutActions(valintaperuste: Valintaperuste): DBIO[UUID] = {
    for {
      id <- DBIO.successful(UUID.randomUUID)
      _  <- insertValintaperuste(valintaperuste.copy(id = Some(id)))
    } yield id
  }

  override def put(valintaperuste: Valintaperuste): UUID =
    KoutaDatabase.runBlockingTransactionally(getPutActions(valintaperuste)).get

  override def getUpdateActions(valintaperuste: Valintaperuste, notModifiedSince: Instant): DBIO[Boolean] =
    checkNotModified(valintaperuste.id.get, notModifiedSince).andThen(
      for {
        v <- updateValintaperuste(valintaperuste)
      } yield 0 < v
    )

  override def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(valintaperuste, notModifiedSince)).get

  override def get(id: UUID): Option[(Valintaperuste, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      v <- selectValintaperuste(id).as[Valintaperuste].headOption
      l <- selectLastModified(id)
    } yield (v, l) match {
      case (Some(v), Some(l)) => Some((v, l))
      case _ => None
    }).get
  }

  override def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))

  override def ListByOrganisaatioOidAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOidsAndHaunKohdejoukko(organisaatioOids, hakuOid))

  override def listBySorakuvausId(sorakuvausId: UUID): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectBySorakuvausId(sorakuvausId))
}

sealed trait ValintaperusteModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[UUID]] = {
    sql"""select id from valintaperusteet where $since < lower(system_time)
          union
          select id from valintaperusteet_history where $since <@ system_time""".as[UUID]
  }

  def selectLastModified(id: UUID): DBIO[Option[Instant]] = {
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
    sqlu"""insert into valintaperusteet (
                     id,
                     tila,
                     koulutustyyppi,
                     hakutapa_koodi_uri,
                     kohdejoukko_koodi_uri,
                     kohdejoukon_tarkenne_koodi_uri,
                     nimi,
                     julkinen,
                     metadata,
                     sorakuvaus_id,
                     organisaatio_oid,
                     muokkaaja,
                     kielivalinta
         ) values (  ${valintaperuste.id.map(_.toString)}::uuid,
                     ${valintaperuste.tila.toString}::julkaisutila,
                     ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi,
                     ${valintaperuste.hakutapaKoodiUri},
                     ${valintaperuste.kohdejoukkoKoodiUri},
                     ${valintaperuste.kohdejoukonTarkenneKoodiUri},
                     ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     ${valintaperuste.julkinen},
                     ${toJsonParam(valintaperuste.metadata)}::jsonb,
                     ${valintaperuste.sorakuvausId.map(_.toString)}::uuid,
                     ${valintaperuste.organisaatioOid},
                     ${valintaperuste.muokkaaja},
                     ${toJsonParam(valintaperuste.kielivalinta)}::jsonb
         )"""
  }

  def selectValintaperuste(id: UUID) =
    sql"""select id, tila, koulutustyyppi, hakutapa_koodi_uri, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, nimi,
                 julkinen, metadata, sorakuvaus_id, organisaatio_oid, muokkaaja, kielivalinta, lower(system_time)
          from valintaperusteet where id = ${id.toString}::uuid"""

  def updateValintaperuste(valintaperuste: Valintaperuste) = {
    sqlu"""update valintaperusteet set
                     tila = ${valintaperuste.tila.toString}::julkaisutila,
                     koulutustyyppi = ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi,
                     hakutapa_koodi_uri = ${valintaperuste.hakutapaKoodiUri},
                     kohdejoukko_koodi_uri = ${valintaperuste.kohdejoukkoKoodiUri},
                     kohdejoukon_tarkenne_koodi_uri = ${valintaperuste.kohdejoukonTarkenneKoodiUri},
                     nimi = ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     julkinen = ${valintaperuste.julkinen},
                     metadata = ${toJsonParam(valintaperuste.metadata)}::jsonb,
                     sorakuvaus_id = ${valintaperuste.sorakuvausId.map(_.toString)}::uuid,
                     organisaatio_oid = ${valintaperuste.organisaatioOid},
                     muokkaaja = ${valintaperuste.muokkaaja},
                     kielivalinta = ${toJsonParam(valintaperuste.kielivalinta)}::jsonb
           where id = ${valintaperuste.id.map(_.toString)}::uuid
           and (koulutustyyppi is distinct from ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi
           or tila is distinct from ${valintaperuste.tila.toString}::julkaisutila
           or hakutapa_koodi_uri is distinct from ${valintaperuste.hakutapaKoodiUri}
           or kohdejoukko_koodi_uri is distinct from ${valintaperuste.kohdejoukkoKoodiUri}
           or kohdejoukon_tarkenne_koodi_uri is distinct from ${valintaperuste.kohdejoukonTarkenneKoodiUri}
           or nimi is distinct from ${toJsonParam(valintaperuste.nimi)}::jsonb
           or julkinen is distinct from ${valintaperuste.julkinen}
           or metadata is distinct from ${toJsonParam(valintaperuste.metadata)}::jsonb
           or sorakuvaus_id is distinct from ${valintaperuste.sorakuvausId.map(_.toString)}::uuid
           or organisaatio_oid is distinct from ${valintaperuste.organisaatioOid}
           or muokkaaja is distinct from ${valintaperuste.muokkaaja}
           or kielivalinta is distinct from ${toJsonParam(valintaperuste.kielivalinta)}::jsonb
         )"""
  }

  def selectByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from valintaperusteet
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[ValintaperusteListItem]
  }

  def selectByOrganisaatioOidsAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid) = {
    sql"""select v.id, v.nimi, v.tila, v.organisaatio_oid, v.muokkaaja, lower(v.system_time)
          from valintaperusteet v
          inner join haut h on v.kohdejoukko_koodi_uri is not distinct from h.kohdejoukko_koodi_uri
          and v.kohdejoukon_tarkenne_koodi_uri is not distinct from h.kohdejoukon_tarkenne_koodi_uri
          where h.oid = $hakuOid
          and v.organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[ValintaperusteListItem]
  }

  def selectBySorakuvausId(sorakuvausId: UUID) = {
    sql"""select id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from valintaperusteet
          where sorakuvaus_id = ${sorakuvausId.toString}::uuid""".as[ValintaperusteListItem]
  }
}