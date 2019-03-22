package fi.oph.kouta.repository

import java.time.Instant
import java.util.{ConcurrentModificationException, UUID}

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.valintaperuste.{Valintaperuste, ValintaperusteListItem}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ValintaperusteDAO extends EntityModificationDAO[UUID] {
  def put(valintaperuste: Valintaperuste): Option[UUID]
  def get(id: UUID): Option[(Valintaperuste, Instant)]
  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ValintaperusteListItem]
  def ListByOrganisaatioOidAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid): Seq[ValintaperusteListItem]
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

  override def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))

  override def ListByOrganisaatioOidAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOidsAndHaunKohdejoukko(organisaatioOids, hakuOid))
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
                     koulutustyyppi,
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
                     ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi,
                     ${valintaperuste.id.map(_.toString)}::uuid,
                     ${valintaperuste.tila.toString}::julkaisutila,
                     ${valintaperuste.hakutapaKoodiUri},
                     ${valintaperuste.kohdejoukkoKoodiUri},
                     ${valintaperuste.kohdejoukonTarkenneKoodiUri},
                     ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     ${valintaperuste.onkoJulkinen},
                     ${toJsonParam(valintaperuste.metadata)}::jsonb,
                     ${valintaperuste.organisaatioOid},
                     ${valintaperuste.muokkaaja},
                     ${toJsonParam(valintaperuste.kielivalinta)}::jsonb
         )"""
  }

  def selectValintaperuste(id: UUID) =
    sql"""select koulutustyyppi, id, tila, hakutapa_koodi_uri, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, nimi,
                 onkoJulkinen, metadata, organisaatio_oid, muokkaaja, kielivalinta, lower(system_time)
          from valintaperusteet where id = ${id.toString}::uuid"""

  def updateValintaperuste(valintaperuste: Valintaperuste) = {
    sqlu"""update valintaperusteet set
                     tila = ${valintaperuste.tila.toString}::julkaisutila,
                     hakutapa_koodi_uri = ${valintaperuste.hakutapaKoodiUri},
                     kohdejoukko_koodi_uri = ${valintaperuste.kohdejoukkoKoodiUri},
                     kohdejoukon_tarkenne_koodi_uri = ${valintaperuste.kohdejoukonTarkenneKoodiUri},
                     nimi = ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     onkoJulkinen = ${valintaperuste.onkoJulkinen},
                     metadata = ${toJsonParam(valintaperuste.metadata)}::jsonb,
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
           or onkoJulkinen is distinct from ${valintaperuste.onkoJulkinen}
           or metadata is distinct from ${toJsonParam(valintaperuste.metadata)}::jsonb
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
}
