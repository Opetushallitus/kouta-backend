package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Koulutustyyppi, Valintakoe, Valintaperuste, ValintaperusteListItem}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ValintaperusteDAO extends EntityModificationDAO[UUID] {
  def getPutActions(valintaperuste: Valintaperuste): DBIO[UUID]
  def getUpdateActions(valintaperuste: Valintaperuste, notModifiedSince: Instant): DBIO[Boolean]

  def put(valintaperuste: Valintaperuste): UUID
  def get(id: UUID): Option[(Valintaperuste, Instant)]
  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean

  def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]): Seq[ValintaperusteListItem]
  def listAllowedByOrganisaatiotAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], hakuOid: HakuOid): Seq[ValintaperusteListItem]
  def listBySorakuvausId(sorakuvausId: UUID): Seq[ValintaperusteListItem]
}

object ValintaperusteDAO extends ValintaperusteDAO with ValintaperusteSQL {

  override def getPutActions(valintaperuste: Valintaperuste): DBIO[UUID] = {
    for {
      id <- DBIO.successful(UUID.randomUUID)
      _  <- insertValintaperuste(valintaperuste.copy(id = Some(id)))
      _  <- insertValintakokeet(valintaperuste.copy(id = Some(id)))
    } yield id
  }

  override def put(valintaperuste: Valintaperuste): UUID =
    KoutaDatabase.runBlockingTransactionally(getPutActions(valintaperuste)).get

  override def getUpdateActions(valintaperuste: Valintaperuste, notModifiedSince: Instant): DBIO[Boolean] =
    checkNotModified(valintaperuste.id.get, notModifiedSince).andThen(
      for {
        v <- updateValintaperuste(valintaperuste)
        k <- updateValintakokeet(valintaperuste)
      } yield 0 < v + k.sum
    )

  override def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(valintaperuste, notModifiedSince)).get

  override def get(id: UUID): Option[(Valintaperuste, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      v <- selectValintaperuste(id).as[Valintaperuste].headOption
      k <- selectValintakokeet(id)
      l <- selectLastModified(id)
    } yield (v, k, l)).map {
      case (Some(v), k, Some(l)) => Some((v.copy(valintakokeet = k.toList), l))
      case _ => None
    }.get
  }

  override def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectAllowedByOrganisaatiot(organisaatioOids, koulutustyypit))

  override def listAllowedByOrganisaatiotAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], hakuOid: HakuOid): Seq[ValintaperusteListItem] =
    KoutaDatabase.runBlocking(selectAllowedByOrganisaatiotAndHaunKohdejoukko(organisaatioOids, koulutustyypit, hakuOid))

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
            max(lower(vk.system_time)),
            max(upper(vph.system_time)),
            max(upper(vkh.system_time)))
          from valintaperusteet vp
          left join valintaperusteet_history vph on vp.id = vph.id
          left join valintaperusteiden_valintakokeet vk on vp.id = vk.valintaperuste_id
          left join valintaperusteiden_valintakokeet_history vkh on vp.id = vkh.valintaperuste_id
          where vp.id = ${id.toString}::uuid""".as[Option[Instant]].head
  }
}

sealed trait ValintaperusteSQL extends ValintaperusteExtractors with ValintaperusteModificationSQL with SQLHelpers {

  val ophOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

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

  def insertValintakokeet(valintaperuste: Valintaperuste) =
    DBIO.sequence(
      valintaperuste.valintakokeet.map(k =>
        insertValintakoe(valintaperuste.id, k.copy(id = Some(UUID.randomUUID())), valintaperuste.muokkaaja)))

  def insertValintakoe(valintaperusteId: Option[UUID], valintakoe: Valintakoe, muokkaaja: UserOid) =
    sqlu"""insert into valintaperusteiden_valintakokeet (id, valintaperuste_id, tyyppi_koodi_uri, tilaisuudet, muokkaaja)
           values (${valintakoe.id.map(_.toString)}::uuid,
                   ${valintaperusteId.map(_.toString)}::uuid,
                   ${valintakoe.tyyppiKoodiUri},
                   ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
                   $muokkaaja)"""

  def selectValintaperuste(id: UUID) =
    sql"""select id, tila, koulutustyyppi, hakutapa_koodi_uri, kohdejoukko_koodi_uri, kohdejoukon_tarkenne_koodi_uri, nimi,
                 julkinen, metadata, sorakuvaus_id, organisaatio_oid, muokkaaja, kielivalinta, lower(system_time)
          from valintaperusteet where id = ${id.toString}::uuid"""

  def selectValintakokeet(id: UUID) = {
    sql"""select id, tyyppi_koodi_uri, tilaisuudet
          from valintaperusteiden_valintakokeet
          where valintaperuste_id = ${id.toString}::uuid""".as[Valintakoe]
  }

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

  def updateValintakoe(valintaperusteId: Option[UUID], valintakoe: Valintakoe, muokkaaja: UserOid) = {
    sqlu"""update valintaperusteiden_valintakokeet set
              tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri},
              tilaisuudet = ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
              muokkaaja = $muokkaaja
           where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid and id = ${valintakoe.id.map(_.toString)}::uuid and (
              tilaisuudet is distinct from ${toJsonParam(valintakoe.tilaisuudet)}::jsonb or
              tyyppi_koodi_uri is distinct from ${valintakoe.tyyppiKoodiUri})"""
  }

  def updateValintakokeet(valintaperuste: Valintaperuste) = {
    val (valintaperusteId, valintakokeet, muokkaaja) = (valintaperuste.id, valintaperuste.valintakokeet, valintaperuste.muokkaaja)
    val (insert, update) = valintakokeet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) {
      deleteValintakokeet(valintaperusteId, update.map(_.id.get))
    } else {
      deleteValintakokeet(valintaperusteId)
    }
    val insertSQL = insert.map(v => insertValintakoe(valintaperusteId, v.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateValintakoe(valintaperusteId, v, muokkaaja))

    DBIO.sequence(List(deleteSQL) ++ insertSQL ++ updateSQL)
  }

  def deleteValintakokeet(valintaperusteId: Option[UUID], exclude: List[UUID]) = {
    sqlu"""delete from valintaperusteiden_valintakokeet where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid and id not in (#${createUUIDInParams(exclude)})"""
  }

  def deleteValintakokeet(valintaperusteId: Option[UUID]) = {
    sqlu"""delete from valintaperusteiden_valintakokeet where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid"""
  }

  def selectAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]) = {
    sql"""select v.id, v.nimi, v.tila, v.organisaatio_oid, v.muokkaaja, m.modified
          from valintaperusteet v
          inner join (
            select vp.id id, greatest(
              max(lower(vp.system_time)),
              max(lower(vk.system_time)),
              max(upper(vph.system_time)),
              max(upper(vkh.system_time))) modified
            from valintaperusteet vp
            left join valintaperusteet_history vph on vp.id = vph.id
            left join valintaperusteiden_valintakokeet vk on vp.id = vk.valintaperuste_id
            left join valintaperusteiden_valintakokeet_history vkh on vp.id = vkh.valintaperuste_id
            group by vp.id) m on v.id = m.id
          where ( v.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and (v.organisaatio_oid <> ${ophOid} or v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
          or (v.julkinen  = ${true} and v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)}))
      """.as[ValintaperusteListItem]
  }

  def selectAllowedByOrganisaatiotAndHaunKohdejoukko(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], hakuOid: HakuOid) = {
    sql"""select v.id, v.nimi, v.tila, v.organisaatio_oid, v.muokkaaja, m.modified
          from valintaperusteet v
          inner join haut h on v.kohdejoukko_koodi_uri is not distinct from h.kohdejoukko_koodi_uri and v.kohdejoukon_tarkenne_koodi_uri is not distinct from h.kohdejoukon_tarkenne_koodi_uri
          inner join (
            select vp.id id, greatest(
              max(lower(vp.system_time)),
              max(lower(vk.system_time)),
              max(upper(vph.system_time)),
              max(upper(vkh.system_time))) modified
            from valintaperusteet vp
            left join valintaperusteet_history vph on vp.id = vph.id
            left join valintaperusteiden_valintakokeet vk on vp.id = vk.valintaperuste_id
            left join valintaperusteiden_valintakokeet_history vkh on vp.id = vkh.valintaperuste_id
            group by vp.id) m on v.id = m.id
          where h.oid = $hakuOid
          and ((v.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and (v.organisaatio_oid <> ${ophOid} or v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
          or (v.julkinen  = ${true} and v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
      """.as[ValintaperusteListItem]
  }

  def selectBySorakuvausId(sorakuvausId: UUID) = {
    sql"""select v.id, v.nimi, v.tila, v.organisaatio_oid, v.muokkaaja, m.modified
          from valintaperusteet v
          inner join (
            select vp.id id, greatest(
              max(lower(vp.system_time)),
              max(lower(vk.system_time)),
              max(upper(vph.system_time)),
              max(upper(vkh.system_time))) modified
            from valintaperusteet vp
            left join valintaperusteet_history vph on vp.id = vph.id
            left join valintaperusteiden_valintakokeet vk on vp.id = vk.valintaperuste_id
            left join valintaperusteiden_valintakokeet_history vkh on vp.id = vkh.valintaperuste_id
            group by vp.id) m on v.id = m.id
          where v.sorakuvaus_id = ${sorakuvausId.toString}::uuid""".as[ValintaperusteListItem]
  }
}
