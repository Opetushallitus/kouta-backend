package fi.oph.kouta.repository

import fi.oph.kouta.domain._
import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain.oid._
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO extends EntityModificationDAO[KoulutusOid] {
  def put(koulutus: Koulutus): Option[KoulutusOid]
  def get(oid: KoulutusOid): Option[(Koulutus, Instant)]
  def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean

  def listByOrganisaatioOids(organisaatioOids:Seq[OrganisaatioOid]) :Seq[KoulutusListItem]
  def listByHakuOid(hakuOid: HakuOid) :Seq[KoulutusListItem]
}

object KoulutusDAO extends KoulutusDAO with KoulutusSQL {

  override def put(koulutus: Koulutus): Option[KoulutusOid] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertKoulutus(koulutus)
      _ <- insertKoulutuksenTarjoajat(koulutus.copy(oid = oid))
    } yield (oid) ) match {
      case Left(t) => throw t
      case Right(oid) => oid
    }
  }

  override def get(oid: KoulutusOid): Option[(Koulutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      k <- selectKoulutus(oid).as[Koulutus].headOption
      t <- selectKoulutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (k, t, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) | Right((_, _, None)) => None
      case Right((Some(k), t, Some(l))) => Some((k.copy(tarjoajat = t.map(_.tarjoajaOid).toList), l))
    }
  }

  override def update(koulutus: Koulutus, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(koulutus.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown koulutus oid ${koulutus.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut koulutusta ${koulutus.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateKoulutus(koulutus))
      .zip(updateKoulutuksenTarjoajat(koulutus))) match {
      case Left(t) => throw t
      case Right((x, y)) => 0 < (x + y.sum)
    }
  }

  private def updateKoulutuksenTarjoajat(koulutus: Koulutus) = {
    val (oid, tarjoajat, muokkaaja) = (koulutus.oid, koulutus.tarjoajat, koulutus.muokkaaja)
    if(tarjoajat.size > 0) {
      DBIO.sequence( tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      DBIO.sequence(List(deleteTarjoajat(oid)))
    }
  }

  override def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[KoulutusListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))

  override def listByHakuOid(hakuOid: HakuOid) :Seq[KoulutusListItem] =
    KoutaDatabase.runBlocking(selectByHakuOid(hakuOid))
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
            ${toJsonParam(koulutus.kielivalinta)}::jsonb) returning oid""".as[KoulutusOid].headOption
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

  def selectKoulutuksenTarjoajat(oid: KoulutusOid) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
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
    sql"""select oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from koulutukset
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          or julkinen = ${true}""".as[KoulutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""select distinct k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, lower(k.system_time)
          from koulutukset k
          inner join toteutukset t on k.oid = t.koulutus_oid
          inner join hakukohteet h on t.oid = h.toteutus_oid
          where h.haku_oid = ${hakuOid.toString}""".as[KoulutusListItem]
  }
}