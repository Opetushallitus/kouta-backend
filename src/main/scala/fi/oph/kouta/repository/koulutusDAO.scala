package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.TimeUtils.instantToLocalDateTime
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO extends EntityModificationDAO[KoulutusOid] {
  def getPutActions(koulutus: Koulutus): DBIO[KoulutusOid]
  def getUpdateActions(koulutus: Koulutus, notModifiedSince: Instant): DBIO[Boolean]

  def put(koulutus: Koulutus): KoulutusOid
  def get(oid: KoulutusOid): Option[(Koulutus, Instant)]
  def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]): Seq[KoulutusListItem]
  def listByHakuOid(hakuOid: HakuOid) :Seq[KoulutusListItem]
  def listJulkaistut(): Seq[KoulutusListItem]

  def getJulkaistutByTarjoajaOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[Koulutus]
}

object KoulutusDAO extends KoulutusDAO with KoulutusSQL {

  override def getPutActions(koulutus: Koulutus): DBIO[KoulutusOid] =
    for {
      oid <- insertKoulutus(koulutus)
      _   <- insertKoulutuksenTarjoajat(koulutus.copy(oid = Some(oid)))
    } yield oid

  override def put(koulutus: Koulutus): KoulutusOid =
    KoutaDatabase.runBlockingTransactionally(getPutActions(koulutus)).get

  override def get(oid: KoulutusOid): Option[(Koulutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        k <- selectKoulutus(oid).as[Koulutus].headOption
        t <- selectKoulutuksenTarjoajat(oid).as[Tarjoaja]
        l <- selectLastModified(oid)
      } yield (k, t, l)
    ).get match {
      case (Some(k), t, Some(l)) =>
        Some((k.copy(modified = Some(instantToLocalDateTime(l)), tarjoajat = t.map(_.tarjoajaOid).toList), l))
      case _ => None
    }
  }

  override def getUpdateActions(koulutus: Koulutus, notModifiedSince: Instant): DBIO[Boolean] = {
    checkNotModified(koulutus.oid.get, notModifiedSince).andThen(
      for {
        k <- updateKoulutus(koulutus)
        t <- updateKoulutuksenTarjoajat(koulutus)
      } yield 0 < (k + t.sum)
    )
  }

  override def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(koulutus, notModifiedSince)).get

  private def updateKoulutuksenTarjoajat(koulutus: Koulutus) = {
    val (oid, tarjoajat, muokkaaja) = (koulutus.oid, koulutus.tarjoajat, koulutus.muokkaaja)
    if (tarjoajat.nonEmpty) {
      DBIO.sequence(tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      DBIO.sequence(List(deleteTarjoajat(oid)))
    }
  }

  private def listWithTarjoajat(selectListItems: => DBIO[Seq[KoulutusListItem]]): Seq[KoulutusListItem] =
    KoutaDatabase.runBlockingTransactionally(
      for {
        koulutukset <- selectListItems
        tarjoajat   <- selectKoulutustenTarjoajat(koulutukset.map(_.oid).toList).as[Tarjoaja]
      } yield (koulutukset, tarjoajat) ).map {
      case (toteutukset, tarjoajat) => {
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.toString).map(_.tarjoajaOid).toList))
      }
    }.get

  override def listByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]): Seq[KoulutusListItem] =
    listWithTarjoajat {
      if (koulutustyypit.isEmpty) {
        selectByOrganisaatioOids(organisaatioOids)
      } else {
        selectByOrganisaatioOidsOrJulkinen(organisaatioOids, koulutustyypit)
      }
    }

  override def listJulkaistut(): Seq[KoulutusListItem] =
    listWithTarjoajat(selectJulkaistut())

  override def listByHakuOid(hakuOid: HakuOid) :Seq[KoulutusListItem] =
    listWithTarjoajat(selectByHakuOid(hakuOid))

  override def getJulkaistutByTarjoajaOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[Koulutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        koulutukset <- findJulkaistutKoulutuksetByTarjoajat(organisaatioOids).as[Koulutus]
        tarjoajat   <- selectKoulutustenTarjoajat(koulutukset.map(_.oid.get).toList).as[Tarjoaja]
      } yield (koulutukset, tarjoajat)).map {
      case (koulutukset, tarjoajat) => {
        koulutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList))
      }
    }.get
  }
}

sealed trait KoulutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: KoulutusOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(k.system_time)),
            max(lower(ta.system_time)),
            max(upper(kh.system_time)),
            max(upper(tah.system_time)))
          from koulutukset k
          left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
          left join koulutukset_history kh on k.oid = kh.oid
          left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
          where k.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[KoulutusOid]] = {
    sql"""select oid from koulutukset where $since < lower(system_time)
          union
          select oid from koulutukset_history where $since <@ system_time
          union
          select koulutus_oid from koulutusten_tarjoajat where $since < lower(system_time)
          union
          select koulutus_oid from koulutusten_tarjoajat_history where $since <@ system_time""".as[KoulutusOid]
  }
}

sealed trait KoulutusSQL extends KoulutusExtractors with KoulutusModificationSQL with SQLHelpers {

  def insertKoulutus(koulutus: Koulutus) = {
    sql"""insert into koulutukset (
            johtaa_tutkintoon,
            tyyppi,
            koulutus_koodi_uri,
            tila,
            nimi,
            metadata,
            julkinen,
            muokkaaja,
            organisaatio_oid,
            kielivalinta)
          values (
            ${koulutus.johtaaTutkintoon},
            ${koulutus.koulutustyyppi.map(_.toString)}::koulutustyyppi,
            ${koulutus.koulutusKoodiUri},
            ${koulutus.tila.toString}::julkaisutila,
            ${toJsonParam(koulutus.nimi)}::jsonb,
            ${toJsonParam(koulutus.metadata)}::jsonb,
            ${koulutus.julkinen},
            ${koulutus.muokkaaja},
            ${koulutus.organisaatioOid},
            ${toJsonParam(koulutus.kielivalinta)}::jsonb) returning oid""".as[KoulutusOid].head
  }

  def insertKoulutuksenTarjoajat(koulutus: Koulutus) = {
    DBIO.sequence( koulutus.tarjoajat.map(t =>
      sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values (${koulutus.oid}, $t, ${koulutus.muokkaaja})"""))
  }

  def selectKoulutus(oid: KoulutusOid) = {
    sql"""select oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila,
                 nimi, metadata, julkinen, muokkaaja, organisaatio_oid, kielivalinta, lower(system_time)
          from koulutukset where oid = $oid"""
  }

  def findJulkaistutKoulutuksetByTarjoajat(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select k.oid, k.johtaa_tutkintoon, k.tyyppi, k.koulutus_koodi_uri, k.tila,
                 k.nimi, k.metadata, k.julkinen, k.muokkaaja, k.organisaatio_oid, k.kielivalinta, lower(k.system_time)
          from koulutukset k
          inner join koulutusten_tarjoajat kt on k.oid = kt.koulutus_oid
          where k.tila = 'julkaistu'::julkaisutila
          and kt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)})"""
  }

  def selectKoulutuksenTarjoajat(oid: KoulutusOid) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  def selectKoulutustenTarjoajat(oids: List[KoulutusOid]) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid in (#${createOidInParams(oids)})"""
  }

  def updateKoulutus(koulutus: Koulutus) = {
    sqlu"""update koulutukset set
              johtaa_tutkintoon = ${koulutus.johtaaTutkintoon},
              tyyppi = ${koulutus.koulutustyyppi.map(_.toString)}::koulutustyyppi,
              koulutus_koodi_uri = ${koulutus.koulutusKoodiUri},
              tila = ${koulutus.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(koulutus.nimi)}::jsonb,
              metadata = ${toJsonParam(koulutus.metadata)}::jsonb,
              julkinen = ${koulutus.julkinen},
              muokkaaja = ${koulutus.muokkaaja},
              organisaatio_oid = ${koulutus.organisaatioOid},
              kielivalinta = ${toJsonParam(koulutus.kielivalinta)}::jsonb
            where oid = ${koulutus.oid}
            and ( johtaa_tutkintoon is distinct from ${koulutus.johtaaTutkintoon}
            or tyyppi is distinct from ${koulutus.koulutustyyppi.map(_.toString)}::koulutustyyppi
            or koulutus_koodi_uri is distinct from ${koulutus.koulutusKoodiUri}
            or tila is distinct from ${koulutus.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(koulutus.nimi)}::jsonb
            or julkinen is distinct from ${koulutus.julkinen}
            or metadata is distinct from ${toJsonParam(koulutus.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(koulutus.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${koulutus.organisaatioOid})"""
  }

  def insertTarjoaja(oid: Option[KoulutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid ) = {
    sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint koulutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid], exclude: List[OrganisaatioOid]) = {
    sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid and tarjoaja_oid not in (#${createOidInParams(exclude)})"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid]) = sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid"""

  def selectByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, m.modified
          from koulutukset k
          inner join (
            select k.oid oid, greatest(
              max(lower(k.system_time)),
              max(lower(ta.system_time)),
              max(upper(kh.system_time)),
              max(upper(tah.system_time))) modified
            from koulutukset k
            left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
            left join koulutukset_history kh on k.oid = kh.oid
            left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
            group by k.oid) m on k.oid = m.oid
          where k.organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[KoulutusListItem]
  }

  def selectByOrganisaatioOidsOrJulkinen(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi]) = {
    sql"""select k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, m.modified
          from koulutukset k
          inner join (
            select k.oid oid, greatest(
              max(lower(k.system_time)),
              max(lower(ta.system_time)),
              max(upper(kh.system_time)),
              max(upper(tah.system_time))) modified
            from koulutukset k
            left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
            left join koulutukset_history kh on k.oid = kh.oid
            left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
            group by k.oid) m on k.oid = m.oid
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          or (julkinen = ${true} and tyyppi in (#${createKoulutustyypitInParams(koulutustyypit)}))""".as[KoulutusListItem]
  }

  def selectJulkaistut() = {
    sql"""select oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from koulutukset
          where tila = 'julkaistu'::julkaisutila""".as[KoulutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""select distinct k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, m.modified
          from koulutukset k
          inner join (select k.oid oid, greatest(
            max(lower(k.system_time)),
            max(lower(ta.system_time)),
            max(upper(kh.system_time)),
            max(upper(tah.system_time))) modified
            from koulutukset k
            left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
            left join koulutukset_history kh on k.oid = kh.oid
            left join koulutusten_tarjoajat_history tah on k.oid = tah.koulutus_oid
            group by k.oid) m on k.oid = m.oid
          inner join toteutukset t on k.oid = t.koulutus_oid
          inner join hakukohteet h on t.oid = h.toteutus_oid
          where h.haku_oid = ${hakuOid.toString}""".as[KoulutusListItem]
  }
}
