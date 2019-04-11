package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException

import fi.oph.kouta.domain.{Toteutus, ToteutusListItem}
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid._
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO extends EntityModificationDAO[ToteutusOid] {
  def getPutActions(toteutus: Toteutus): DBIO[ToteutusOid]
  def getUpdateActions(toteutus: Toteutus, notModifiedSince: Instant): DBIO[Boolean]

  def put(toteutus: Toteutus): ToteutusOid
  def get(oid: ToteutusOid): Option[(Toteutus, Instant)]
  def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean
  def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]

  def getJulkaistutByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]
  def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem]
  def listByKoulutusOid(koulutusOid: KoulutusOid): Seq[ToteutusListItem]
  def listByKoulutusOidAndOrganisaatioOids(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem]
}

object ToteutusDAO extends ToteutusDAO with ToteutusSQL {

  override def getPutActions(toteutus: Toteutus): DBIO[ToteutusOid] =
    for {
      oid <- insertToteutus(toteutus)
      _ <- insertToteutuksenTarjoajat(toteutus.copy(oid = Some(oid)))
      _ <- insertAmmattinimikkeet(toteutus)
      _ <- insertAsiasanat(toteutus)
    } yield oid

  override def put(toteutus: Toteutus): ToteutusOid =
    KoutaDatabase.runBlockingTransactionally(getPutActions(toteutus)).get

  override def getUpdateActions(toteutus: Toteutus, notModifiedSince: Instant): DBIO[Boolean] =
    checkNotModified(toteutus.oid.get, notModifiedSince).andThen(
      for {
        t  <- updateToteutus(toteutus)
        tt <- updateToteutuksenTarjoajat(toteutus)
        _  <- insertAsiasanat(toteutus)
        _  <- insertAmmattinimikkeet(toteutus)
      } yield 0 < (t + tt.sum))

  override def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(toteutus, notModifiedSince)).get

  override def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      k <- selectToteutus(oid).as[Toteutus].headOption
      t <- selectToteutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (k, t, l) match {
      case (Some(k), t, Some(l)) => Some((k.copy(tarjoajat = t.map(_.tarjoajaOid).toList), l))
      case _ => None
    }).get
  }

  private def updateToteutuksenTarjoajat(toteutus: Toteutus) = {
    val Toteutus(oid, _, _, tarjoajat, _, _, muokkaaja, _, _, _) = toteutus
    if(tarjoajat.nonEmpty) {
      DBIO.sequence( tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      DBIO.sequence(List(deleteTarjoajat(oid)))
    }
  }

  private def insertAmmattinimikkeet(toteutus: Toteutus) =
    KeywordDAO.insert(Ammattinimike, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(List()))

  private def insertAsiasanat(toteutus: Toteutus) =
    KeywordDAO.insert(Asiasana, toteutus.metadata.map(_.asiasanat).getOrElse(List()))

  override def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectToteutuksetByKoulutusOid(koulutusOid).as[Toteutus]
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList).as[Tarjoaja]
      } yield (toteutukset, tarjoajat)).map {
        case (toteutukset, tarjoajat) => {
          toteutukset.map(t =>
            t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList))
        }
      }.get
  }

  override def getJulkaistutByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectJulkaistutToteutuksetByKoulutusOid(koulutusOid).as[Toteutus]
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList).as[Tarjoaja]
      } yield (toteutukset, tarjoajat) ).map {
        case (toteutukset, tarjoajat) => {
          toteutukset.map(t =>
            t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList))
      }
    }.get
  }

  override def listModifiedSince(since: Instant): Seq[ToteutusOid] =
    KoutaDatabase.runBlocking(selectModifiedSince(since))

  override def listByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem] =
    KoutaDatabase.runBlocking(selectByOrganisaatioOids(organisaatioOids))

  override def listByKoulutusOid(koulutusOid: KoulutusOid): Seq[ToteutusListItem] =
    KoutaDatabase.runBlocking(selectByKoulutusOid(koulutusOid))

  override def listByKoulutusOidAndOrganisaatioOids(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem] =
    KoutaDatabase.runBlocking(selectByKoulutusOidAndOrganisaatioOids(koulutusOid, organisaatioOids))
}

trait ToteutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: ToteutusOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(t.system_time)),
            max(lower(ta.system_time)),
            max(upper(th.system_time)),
            max(upper(tah.system_time)))
          from toteutukset t
          left join toteutusten_tarjoajat ta on t.oid = ta.toteutus_oid
          left join toteutukset_history th on t.oid = th.oid
          left join toteutusten_tarjoajat_history tah on t.oid = tah.toteutus_oid
          where t.oid = $oid""".as[Option[Instant]].head
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[ToteutusOid]] = {
    sql"""select oid from toteutukset where $since < lower(system_time)
          union
          select oid from toteutukset_history where $since <@ system_time
          union
          select toteutus_oid from toteutusten_tarjoajat where $since < lower(system_time)
          union
          select toteutus_oid from toteutusten_tarjoajat_history where $since <@ system_time""".as[ToteutusOid]
  }

}

sealed trait ToteutusSQL extends ToteutusExtractors with ToteutusModificationSQL with SQLHelpers {

  def selectToteutus(oid: ToteutusOid) =
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja, organisaatio_oid, kielivalinta, lower(system_time)
          from toteutukset
          where oid = $oid"""

  def selectToteutuksetByKoulutusOid(oid: KoulutusOid) =
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja, organisaatio_oid, kielivalinta, lower(system_time)
          from toteutukset
          where koulutus_oid = $oid"""

  def selectJulkaistutToteutuksetByKoulutusOid(oid: KoulutusOid) =
    sql"""select oid, koulutus_oid, tila, nimi, metadata, muokkaaja, organisaatio_oid, kielivalinta, lower(system_time)
          from toteutukset
          where koulutus_oid = $oid
          and tila = 'julkaistu'::julkaisutila"""

  def selectToteutuksenTarjoajat(oid: ToteutusOid) =
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid = $oid"""

  def selectToteutustenTarjoajat(oids: List[ToteutusOid]) = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid in (#${createOidInParams(oids)})"""
  }

  def insertToteutus(toteutus: Toteutus) = {
    sql"""insert into toteutukset (
            koulutus_oid,
            tila,
            nimi,
            metadata,
            muokkaaja,
            organisaatio_oid,
            kielivalinta
          ) values (
            ${toteutus.koulutusOid},
            ${toteutus.tila.toString}::julkaisutila,
            ${toJsonParam(toteutus.nimi)}::jsonb,
            ${toJsonParam(toteutus.metadata)}::jsonb,
            ${toteutus.muokkaaja},
            ${toteutus.organisaatioOid},
            ${toJsonParam(toteutus.kielivalinta)}::jsonb
          ) returning oid""".as[ToteutusOid].head
  }

  def insertToteutuksenTarjoajat(toteutus: Toteutus) = {
    DBIO.sequence( toteutus.tarjoajat.map(t =>
      sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values (${toteutus.oid}, $t, ${toteutus.muokkaaja})"""))
  }

  def updateToteutus(toteutus: Toteutus) = {
    sqlu"""update toteutukset set
              koulutus_oid = ${toteutus.koulutusOid},
              tila = ${toteutus.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(toteutus.nimi)}::jsonb,
              metadata = ${toJsonParam(toteutus.metadata)}::jsonb,
              muokkaaja = ${toteutus.muokkaaja},
              organisaatio_oid = ${toteutus.organisaatioOid},
              kielivalinta = ${toJsonParam(toteutus.kielivalinta)}::jsonb
            where oid = ${toteutus.oid}
            and ( koulutus_oid is distinct from ${toteutus.koulutusOid}
            or tila is distinct from ${toteutus.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(toteutus.nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(toteutus.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(toteutus.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${toteutus.organisaatioOid} )"""
  }

  def insertTarjoaja(oid: Option[ToteutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid ) = {
    sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint toteutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid: Option[ToteutusOid], exclude: List[OrganisaatioOid]) = {
    sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid and tarjoaja_oid not in (#${createOidInParams(exclude)})"""
  }

  def deleteTarjoajat(oid: Option[ToteutusOid]) = sqlu"""delete from toteutusten_tarjoajat where toteutus_oid = $oid"""

  def selectByOrganisaatioOids(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select oid, koulutus_oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from toteutukset
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})""".as[ToteutusListItem]
  }

  def selectByKoulutusOid(koulutusOid: KoulutusOid) = {
    sql"""select oid, koulutus_oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from toteutukset
          where koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }

  def selectByKoulutusOidAndOrganisaatioOids(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select oid, koulutus_oid, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from toteutukset
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          and koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }
}