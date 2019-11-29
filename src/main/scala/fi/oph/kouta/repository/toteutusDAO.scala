package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Toteutus, ToteutusListItem}
import fi.oph.kouta.util.TimeUtils.{instantToLocalDateTime, localDateTimeToInstant}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO extends EntityModificationDAO[ToteutusOid] {
  def getPutActions(toteutus: Toteutus): DBIO[Toteutus]

  def getUpdateActions(toteutus: Toteutus, notModifiedSince: Instant): DBIO[Option[Toteutus]]

  def put(toteutus: Toteutus): Toteutus
  def get(oid: ToteutusOid): Option[(Toteutus, Instant)]

  def update(toteutus: Toteutus, notModifiedSince: Instant): Option[Toteutus]
  def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]
  def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid]

  def getJulkaistutByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem]
  def listByKoulutusOid(koulutusOid: KoulutusOid): Seq[ToteutusListItem]
  def listByKoulutusOidAndAllowedOrganisaatiot(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[ToteutusListItem]
}

object ToteutusDAO extends ToteutusDAO with ToteutusSQL {

  override def getPutActions(toteutus: Toteutus): DBIO[Toteutus] =
    for {
      (oid, t) <- insertToteutus(toteutus)
      tt <- insertToteutuksenTarjoajat(toteutus.copy(oid = Some(oid)))
      _ <- insertAmmattinimikkeet(toteutus)
      _ <- insertAsiasanat(toteutus)
    } yield toteutus.copy(oid = Some(oid)).withModified((t +: tt).max)

  override def put(toteutus: Toteutus): Toteutus =
    KoutaDatabase.runBlockingTransactionally(getPutActions(toteutus)).get

  override def getUpdateActions(toteutus: Toteutus, notModifiedSince: Instant): DBIO[Option[Toteutus]] =
    checkNotModified(toteutus.oid.get, notModifiedSince).andThen(
      for {
        t  <- updateToteutus(toteutus)
        tt <- updateToteutuksenTarjoajat(toteutus)
        _  <- insertAsiasanat(toteutus)
        _  <- insertAmmattinimikkeet(toteutus)
      } yield {
        val modified = (t ++ tt).sorted.lastOption
        modified.map(toteutus.withModified)
      }
    )

  override def update(toteutus: Toteutus, notModifiedSince: Instant): Option[Toteutus] =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(toteutus, notModifiedSince)).get

  override def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      t <- selectToteutus(oid).as[Toteutus].headOption
      tt <- selectToteutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (t, tt, l)).get match {
      case (Some(t), tt, Some(l)) => Some((t.copy(modified = Some(instantToLocalDateTime(l)), tarjoajat = tt.map(_.tarjoajaOid).toList), l))
      case _ => None
    }
  }

  private def updateToteutuksenTarjoajat(toteutus: Toteutus): DBIOAction[Vector[Instant], NoStream, Effect] = {
    val Toteutus(oid, _, _, tarjoajat, _, _, muokkaaja, _, _, _) = toteutus
    if(tarjoajat.nonEmpty) {
      combineInstants(tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat))
    } else {
      deleteTarjoajat(oid)
    }
  }

  private def insertAmmattinimikkeet(toteutus: Toteutus) =
    KeywordDAO.insert(Ammattinimike, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(List()))

  private def insertAsiasanat(toteutus: Toteutus) =
    KeywordDAO.insert(Asiasana, toteutus.metadata.map(_.asiasanat).getOrElse(List()))

  override def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid] =
    KoutaDatabase.runBlocking(selectTarjoajatByHakukohdeOid(hakukohdeOid))

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

  private def listWithTarjoajat(selectListItems : () => DBIO[Seq[ToteutusListItem]]): Seq[ToteutusListItem] =
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectListItems()
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid).toList).as[Tarjoaja]
      } yield (toteutukset, tarjoajat) ).map {
        case (toteutukset, tarjoajat) => {
          toteutukset.map(t =>
            t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.toString).map(_.tarjoajaOid).toList))
        }
    }.get

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => listWithTarjoajat(() => selectByCreatorOrTarjoaja(organisaatioOids))
  }

  override def listByKoulutusOid(koulutusOid: KoulutusOid): Seq[ToteutusListItem] =
    listWithTarjoajat(() => selectByKoulutusOid(koulutusOid))

  override def listByKoulutusOidAndAllowedOrganisaatiot(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => listWithTarjoajat(() => selectByKoulutusOidAndCreatorOrTarjoaja(koulutusOid, organisaatioOids))
  }

  override def listByHakuOid(hakuOid: HakuOid): Seq[ToteutusListItem] =
    listWithTarjoajat(() => selectByHakuOid(hakuOid))
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

  val selectToteutusSql =
    """select t.oid, t.koulutus_oid, t.tila, t.nimi, t.metadata, t.muokkaaja, t.organisaatio_oid, t.kielivalinta, m.modified
         from toteutukset t
         inner join (
           select t.oid oid, greatest(
             max(lower(t.system_time)),
             max(lower(ta.system_time)),
             max(upper(th.system_time)),
             max(upper(tah.system_time))) modified
           from toteutukset t
           left join toteutusten_tarjoajat ta on t.oid = ta.toteutus_oid
           left join toteutukset_history th on t.oid = th.oid
           left join toteutusten_tarjoajat_history tah on t.oid = tah.toteutus_oid
           group by t.oid) m on t.oid = m.oid"""

  def selectToteutus(oid: ToteutusOid) =
    sql"""#$selectToteutusSql
          where t.oid = $oid"""

  def selectToteutuksetByKoulutusOid(oid: KoulutusOid) =
    sql"""#$selectToteutusSql
          where t.koulutus_oid = $oid"""

  def selectJulkaistutToteutuksetByKoulutusOid(oid: KoulutusOid) =
    sql"""#$selectToteutusSql
          where t.koulutus_oid = $oid
          and t.tila = 'julkaistu'::julkaisutila"""

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
          ) returning oid, lower(system_time)""".as[(ToteutusOid, Instant)].head
  }

  def insertToteutuksenTarjoajat(toteutus: Toteutus) = {
    combineInstants(toteutus.tarjoajat.map(t =>
      sql"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values (${toteutus.oid}, $t, ${toteutus.muokkaaja})
             returning lower(system_time)""".as[Instant]))
  }

  def updateToteutus(toteutus: Toteutus) = {
    sql"""update toteutukset set
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
            or organisaatio_oid is distinct from ${toteutus.organisaatioOid} )
            returning lower(system_time)""".as[Instant]
  }

  def insertTarjoaja(oid: Option[ToteutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid ) = {
    sql"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint toteutusten_tarjoajat_pkey do nothing
             returning lower(system_time)""".as[Instant]
  }

  def deleteTarjoajat(oid: Option[ToteutusOid], exclude: List[OrganisaatioOid]) = {
    sql"""delete from toteutusten_tarjoajat
          where toteutus_oid = $oid and tarjoaja_oid not in (#${createOidInParams(exclude)})
          returning now()""".as[Instant]
  }

  def deleteTarjoajat(oid: Option[ToteutusOid]) =
    sql"""delete from toteutusten_tarjoajat
          where toteutus_oid = $oid
          returning now()""".as[Instant]

  val selectToteutusListSql =
    """select distinct t.oid, t.koulutus_oid, t.nimi, t.tila, t.organisaatio_oid, t.muokkaaja, m.modified
         from toteutukset t
         inner join (
           select t.oid oid, greatest(
             max(lower(t.system_time)),
             max(lower(ta.system_time)),
             max(upper(th.system_time)),
             max(upper(tah.system_time))) modified
           from toteutukset t
           left join toteutusten_tarjoajat ta on t.oid = ta.toteutus_oid
           left join toteutukset_history th on t.oid = th.oid
           left join toteutusten_tarjoajat_history tah on t.oid = tah.toteutus_oid
           group by t.oid) m on t.oid = m.oid"""

  def selectByCreatorOrTarjoaja(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""#$selectToteutusListSql
          inner join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
             or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)})""".as[ToteutusListItem]
  }

  def selectByKoulutusOid(koulutusOid: KoulutusOid) = {
    sql"""#$selectToteutusListSql
          where t.koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""#$selectToteutusListSql
          inner join hakukohteet h on h.toteutus_oid = t.oid
          where h.haku_oid = $hakuOid""".as[ToteutusListItem]
  }

  def selectByKoulutusOidAndCreatorOrTarjoaja(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""#$selectToteutusListSql
          inner join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and t.koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }

  def selectTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid) = {
    sql"""select distinct tt.tarjoaja_oid
          from toteutusten_tarjoajat tt
          inner join hakukohteet h on tt.toteutus_oid = h.toteutus_oid
          where h.oid = $hakukohdeOid""".as[OrganisaatioOid]
  }
}
