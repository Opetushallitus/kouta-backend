package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Arkistoitu, Koulutustyyppi, Valintakoe, Valintaperuste, ValintaperusteListItem}
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.instantToModified
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ValintaperusteDAO extends EntityModificationDAO[UUID] {
  def getPutActions(valintaperuste: Valintaperuste): DBIO[Valintaperuste]
  def getUpdateActions(valintaperuste: Valintaperuste): DBIO[Option[Valintaperuste]]

  def get(id: UUID): Option[(Valintaperuste, Instant)]
  def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): Seq[ValintaperusteListItem]
  def listAllowedByOrganisaatiotAndHakuAndKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid, koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean): Seq[ValintaperusteListItem]
}

object ValintaperusteDAO extends ValintaperusteDAO with ValintaperusteSQL {

  override def getPutActions(valintaperuste: Valintaperuste): DBIO[Valintaperuste] = {
    for {
      id <- DBIO.successful(UUID.randomUUID)
      _  <- insertValintaperuste(valintaperuste.copy(id = Some(id)))
      _  <- insertValintakokeet(valintaperuste.copy(id = Some(id)))
      m  <- selectLastModified(id)
    } yield valintaperuste.copy(id = Some(id)).withModified(m.get)
  }

  override def getUpdateActions(valintaperuste: Valintaperuste): DBIO[Option[Valintaperuste]] =
    for {
      v <- updateValintaperuste(valintaperuste)
      k <- updateValintakokeet(valintaperuste)
      m <- selectLastModified(valintaperuste.id.get)
    } yield optionWhen(v + k > 0)(valintaperuste.withModified(m.get))

  override def get(id: UUID): Option[(Valintaperuste, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        v <- selectValintaperuste(id).as[Valintaperuste].headOption
        k <- selectValintakokeet(id)
        l <- selectLastModified(id)
      } yield (v, k, l)
    ).map {
      case (Some(v), k, Some(l)) => Some((v.copy(modified = Some(instantToModified(l)), valintakokeet = k.toList), l))
      case _ => None
    }.get
  }

  override def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): Seq[ValintaperusteListItem] =
    (organisaatioOids, koulutustyypit) match {
      case (Nil, _) => Seq()
      case (_, Nil) => KoutaDatabase.runBlocking(selectByCreatorAndNotOph(organisaatioOids, myosArkistoidut)) //OPH:lla pitäisi olla aina kaikki koulutustyypit
      case (_, _)   => KoutaDatabase.runBlocking(selectByCreatorOrJulkinenForKoulutustyyppi(organisaatioOids, koulutustyypit, myosArkistoidut))
    }

  override def listAllowedByOrganisaatiotAndHakuAndKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid, koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean): Seq[ValintaperusteListItem] =
    organisaatioOids match {
      case Nil => Seq()
      case _   => KoutaDatabase.runBlocking(selectByCreatorOrJulkinenForHakuAndKoulutustyyppi(organisaatioOids, hakuOid, koulutustyyppi, myosArkistoidut))
    }
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

  def insertValintaperuste(valintaperuste: Valintaperuste): DBIO[Int] = {
    sqlu"""insert into valintaperusteet (
                     id,
                     external_id,
                     tila,
                     koulutustyyppi,
                     hakutapa_koodi_uri,
                     kohdejoukko_koodi_uri,
                     nimi,
                     julkinen,
                     esikatselu,
                     metadata,
                     organisaatio_oid,
                     muokkaaja,
                     kielivalinta
         ) values (  ${valintaperuste.id.map(_.toString)}::uuid,
                     ${valintaperuste.externalId},
                     ${valintaperuste.tila.toString}::julkaisutila,
                     ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi,
                     ${valintaperuste.hakutapaKoodiUri},
                     ${valintaperuste.kohdejoukkoKoodiUri},
                     ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     ${valintaperuste.julkinen},
                     ${valintaperuste.esikatselu},
                     ${toJsonParam(valintaperuste.metadata)}::jsonb,
                     ${valintaperuste.organisaatioOid},
                     ${valintaperuste.muokkaaja},
                     ${toJsonParam(valintaperuste.kielivalinta)}::jsonb )"""
  }

  def insertValintakokeet(valintaperuste: Valintaperuste): DBIO[Int] = {
    val inserts = valintaperuste.valintakokeet.map(k =>
      insertValintakoe(valintaperuste.id, k.copy(id = Some(UUID.randomUUID())), valintaperuste.muokkaaja))
    DBIOHelpers.sumIntDBIOs(inserts)
  }


  def insertValintakoe(valintaperusteId: Option[UUID], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Int] =
    sqlu"""insert into valintaperusteiden_valintakokeet (id, valintaperuste_id, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja)
           values (${valintakoe.id.map(_.toString)}::uuid,
                   ${valintaperusteId.map(_.toString)}::uuid,
                   ${valintakoe.tyyppiKoodiUri},
                   ${toJsonParam(valintakoe.nimi)}::jsonb,
                   ${toJsonParam(valintakoe.metadata)}::jsonb,
                   ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
                   $muokkaaja)"""

  def selectValintaperuste(id: UUID) =
    sql"""select id,
                 external_id,
                 tila,
                 koulutustyyppi,
                 hakutapa_koodi_uri,
                 kohdejoukko_koodi_uri,
                 nimi,
                 julkinen,
                 esikatselu,
                 metadata,
                 organisaatio_oid,
                 muokkaaja,
                 kielivalinta,
                 lower(system_time)
          from valintaperusteet
          where id = ${id.toString}::uuid"""

  def selectValintakokeet(id: UUID): DBIO[Vector[Valintakoe]] = {
    sql"""select id, tyyppi_koodi_uri, nimi, metadata, tilaisuudet
          from valintaperusteiden_valintakokeet
          where valintaperuste_id = ${id.toString}::uuid""".as[Valintakoe]
  }

  def updateValintaperuste(valintaperuste: Valintaperuste): DBIO[Int] = {
    sqlu"""update valintaperusteet set
                     external_id = ${valintaperuste.externalId},
                     tila = ${valintaperuste.tila.toString}::julkaisutila,
                     koulutustyyppi = ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi,
                     hakutapa_koodi_uri = ${valintaperuste.hakutapaKoodiUri},
                     kohdejoukko_koodi_uri = ${valintaperuste.kohdejoukkoKoodiUri},
                     nimi = ${toJsonParam(valintaperuste.nimi)}::jsonb,
                     julkinen = ${valintaperuste.julkinen},
                     esikatselu = ${valintaperuste.esikatselu},
                     metadata = ${toJsonParam(valintaperuste.metadata)}::jsonb,
                     organisaatio_oid = ${valintaperuste.organisaatioOid},
                     muokkaaja = ${valintaperuste.muokkaaja},
                     kielivalinta = ${toJsonParam(valintaperuste.kielivalinta)}::jsonb
           where id = ${valintaperuste.id.map(_.toString)}::uuid
           and (external_id is distinct from ${valintaperuste.externalId}
             or koulutustyyppi is distinct from ${valintaperuste.koulutustyyppi.toString}::koulutustyyppi
             or tila is distinct from ${valintaperuste.tila.toString}::julkaisutila
             or hakutapa_koodi_uri is distinct from ${valintaperuste.hakutapaKoodiUri}
             or kohdejoukko_koodi_uri is distinct from ${valintaperuste.kohdejoukkoKoodiUri}
             or nimi is distinct from ${toJsonParam(valintaperuste.nimi)}::jsonb
             or julkinen is distinct from ${valintaperuste.julkinen}
             or esikatselu is distinct from ${valintaperuste.esikatselu}
             or metadata is distinct from ${toJsonParam(valintaperuste.metadata)}::jsonb
             or organisaatio_oid is distinct from ${valintaperuste.organisaatioOid}
             or kielivalinta is distinct from ${toJsonParam(valintaperuste.kielivalinta)}::jsonb )"""
  }

  def updateValintakoe(valintaperusteId: Option[UUID], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""update valintaperusteiden_valintakokeet set
             tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri},
             nimi = ${toJsonParam(valintakoe.nimi)}::jsonb,
             metadata = ${toJsonParam(valintakoe.metadata)}::jsonb,
             tilaisuudet = ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
             muokkaaja = $muokkaaja
          where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid
            and id = ${valintakoe.id.map(_.toString)}::uuid
            and ( tilaisuudet is distinct from ${toJsonParam(valintakoe.tilaisuudet)}::jsonb
              or tyyppi_koodi_uri is distinct from ${valintakoe.tyyppiKoodiUri}
              or nimi is distinct from ${toJsonParam(valintakoe.nimi)}::jsonb
              or metadata is distinct from ${toJsonParam(valintakoe.metadata)}::jsonb)"""
  }

  def updateValintakokeet(valintaperuste: Valintaperuste): DBIO[Int] = {
    val (valintaperusteId, valintakokeet, muokkaaja) = (valintaperuste.id, valintaperuste.valintakokeet, valintaperuste.muokkaaja)
    val (insert, update) = valintakokeet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) {
      deleteValintakokeet(valintaperusteId, update.map(_.id.get))
    } else {
      deleteValintakokeet(valintaperusteId)
    }
    val insertSQL = insert.map(v => insertValintakoe(valintaperusteId, v.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateValintakoe(valintaperusteId, v, muokkaaja))

    deleteSQL.zipWith(DBIOHelpers.sumIntDBIOs(insertSQL ++ updateSQL))(_ + _)
  }

  def deleteValintakokeet(valintaperusteId: Option[UUID], exclude: Seq[UUID]): DBIO[Int] = {
    sqlu"""delete from valintaperusteiden_valintakokeet
           where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid and id not in (#${createUUIDInParams(exclude)})"""
}

  def deleteValintakokeet(valintaperusteId: Option[UUID]): DBIO[Int] = {
    sqlu"""delete from valintaperusteiden_valintakokeet
           where valintaperuste_id = ${valintaperusteId.map(_.toString)}::uuid"""
  }

  val selectValintaperusteListSql =
    """select distinct v.id, v.nimi, v.tila, v.organisaatio_oid, v.muokkaaja, m.modified
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
           group by vp.id) m on v.id = m.id"""

  def selectByCreatorAndNotOph(organisaatioOids: Seq[OrganisaatioOid], myosArkistoidut: Boolean): DBIO[Vector[ValintaperusteListItem]] = {
    sql"""#$selectValintaperusteListSql
          where (v.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and v.organisaatio_oid <> ${RootOrganisaatioOid}) #${andTilaMaybeNotArkistoitu(myosArkistoidut)}
      """.as[ValintaperusteListItem]
  }

  def selectByCreatorOrJulkinenForKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean): DBIO[Vector[ValintaperusteListItem]] = {
    sql"""#$selectValintaperusteListSql
          where ((v.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and
                  (v.organisaatio_oid <> ${RootOrganisaatioOid} or
                   v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
              or (v.julkinen  = ${true} and
                  v.koulutustyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
              #${andTilaMaybeNotArkistoitu(myosArkistoidut)}
      """.as[ValintaperusteListItem]
  }

  def andTilaMaybeNotArkistoituForValintaperuste(myosArkistoidut: Boolean): String = {
    if (myosArkistoidut) "" else s"and v.tila <> '$Arkistoitu'"
  }

  def selectByCreatorOrJulkinenForHakuAndKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], hakuOid: HakuOid, koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean): DBIO[Vector[ValintaperusteListItem]] = {
    sql"""#$selectValintaperusteListSql
          inner join haut h on v.kohdejoukko_koodi_uri is not distinct from h.kohdejoukko_koodi_uri
          where h.oid = $hakuOid
          and v.koulutustyyppi = ${koulutustyyppi.toString}::koulutustyyppi
          and (v.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or v.julkinen = ${true})
          #${andTilaMaybeNotArkistoituForValintaperuste(myosArkistoidut)}
      """.as[ValintaperusteListItem]
  }
}
