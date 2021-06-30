package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{AmmOsaamisala, AmmTutkinnonOsa, Ataru, Hakulomaketyyppi, Toteutus, ToteutusListItem}
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.instantToModified
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO extends EntityModificationDAO[ToteutusOid] {
  def getPutActions(toteutus: Toteutus): DBIO[Toteutus]
  def getUpdateActions(toteutus: Toteutus): DBIO[Option[Toteutus]]

  def get(oid: ToteutusOid): Option[(Toteutus, Instant)]
  def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]
  def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid]

  def getJulkaistutByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], vainHakukohteeseenLiitettavat: Boolean = false, myosArkistoidut: Boolean): Seq[ToteutusListItem]
  def listByKoulutusOid(koulutusOid: KoulutusOid): Seq[ToteutusListItem]
  def listByKoulutusOidAndAllowedOrganisaatiot(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[ToteutusListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[ToteutusListItem]
}

object ToteutusDAO extends ToteutusDAO with ToteutusSQL {

  override def getPutActions(toteutus: Toteutus): DBIO[Toteutus] =
    for {
      oid <- insertToteutus(toteutus)
      _   <- insertToteutuksenTarjoajat(toteutus.withOid(oid))
      m   <- selectLastModified(oid)
    } yield toteutus.withOid(oid).withModified(m.get)

  override def getUpdateActions(toteutus: Toteutus): DBIO[Option[Toteutus]] =
    for {
      t  <- updateToteutus(toteutus)
      tt <- updateToteutuksenTarjoajat(toteutus)
      m  <- selectLastModified(toteutus.oid.get)
    } yield optionWhen(t + tt > 0)(toteutus.withModified(m.get))

  def updateJustToteutus(toteutus: Toteutus): DBIO[Toteutus] =
    for {
      _  <- updateToteutus(toteutus)
      m  <- selectLastModified(toteutus.oid.get)
    } yield toteutus.withModified(m.get)

  override def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      t <- selectToteutus(oid).as[Toteutus].headOption
      tt <- selectToteutuksenTarjoajat(oid).as[Tarjoaja]
      l <- selectLastModified(oid)
    } yield (t, tt, l)).get match {
      case (Some(t), tt, Some(l)) => Some((t.copy(modified = Some(instantToModified(l)), tarjoajat = tt.map(_.tarjoajaOid).toList), l))
      case _ => None
    }
  }

  private def updateToteutuksenTarjoajat(toteutus: Toteutus): DBIO[Int] = {
    val (oid, tarjoajat, muokkaaja) = (toteutus.oid, toteutus.tarjoajat, toteutus.muokkaaja)
    if (tarjoajat.nonEmpty) {
      val actions = tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat)
      DBIOHelpers.sumIntDBIOs(actions)
    } else {
      deleteTarjoajat(oid)
    }
  }

  override def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid] =
    KoutaDatabase.runBlocking(selectTarjoajatByHakukohdeOid(hakukohdeOid))

  override def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectToteutuksetByKoulutusOid(koulutusOid).as[Toteutus]
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList)
      } yield (toteutukset, tarjoajat)
    ).map {
      case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList))
    }.get
  }

  override def getJulkaistutByKoulutusOid(koulutusOid: KoulutusOid): Seq[Toteutus] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectJulkaistutToteutuksetByKoulutusOid(koulutusOid).as[Toteutus]
        tarjoajat <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList)
      } yield (toteutukset, tarjoajat)
    ).map {
      case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList))
    }.get
  }

  override def listModifiedSince(since: Instant): Seq[ToteutusOid] =
    KoutaDatabase.runBlocking(selectModifiedSince(since))

  private def listWithTarjoajat(selectListItems : () => DBIO[Seq[ToteutusListItem]]): Seq[ToteutusListItem] =
    KoutaDatabase.runBlockingTransactionally(
      for {
        toteutukset <- selectListItems()
        tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid).toList)
      } yield (toteutukset, tarjoajat)
    ).map {
      case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.toString).map(_.tarjoajaOid).toList))
    }.get

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], vainHakukohteeseenLiitettavat: Boolean = false, myosArkistoidut: Boolean): Seq[ToteutusListItem] = (organisaatioOids, vainHakukohteeseenLiitettavat) match {
    case (Nil, _)   => Seq()
    case (_, false) => listWithTarjoajat(() => selectByCreatorOrTarjoaja(organisaatioOids, myosArkistoidut))
    case (_, true)  => listWithTarjoajat(() => selectHakukohteeseenLiitettavatByCreatorOrTarjoaja(organisaatioOids, myosArkistoidut))
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
    """select t.oid,
              t.external_id,
              t.koulutus_oid,
              t.tila,
              t.nimi,
              t.metadata,
              t.muokkaaja,
              t.esikatselu,
              t.organisaatio_oid,
              t.kielivalinta,
              t.teemakuva,
              t.sorakuvaus_id,
              m.modified
       from toteutukset t
                inner join (
           select t.oid oid,
                  greatest(
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

  def selectToteutustenTarjoajat(oids: List[ToteutusOid]): DBIO[Vector[Tarjoaja]] = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid in (#${createOidInParams(oids)})""".as[Tarjoaja]
  }

  def insertToteutus(toteutus: Toteutus): DBIO[ToteutusOid] = {
    sql"""insert into toteutukset (
            external_id,
            koulutus_oid,
            tila,
            nimi,
            metadata,
            muokkaaja,
            organisaatio_oid,
            esikatselu,
            kielivalinta,
            teemakuva,
            sorakuvaus_id
          ) values (
            ${toteutus.externalId},
            ${toteutus.koulutusOid},
            ${toteutus.tila.toString}::julkaisutila,
            ${toJsonParam(toteutus.nimi)}::jsonb,
            ${toJsonParam(toteutus.metadata)}::jsonb,
            ${toteutus.muokkaaja},
            ${toteutus.organisaatioOid},
            ${toteutus.esikatselu},
            ${toJsonParam(toteutus.kielivalinta)}::jsonb,
            ${toteutus.teemakuva},
            ${toteutus.sorakuvausId.map(_.toString)}::uuid
          ) returning oid""".as[ToteutusOid].head
  }

  def insertToteutuksenTarjoajat(toteutus: Toteutus): DBIO[Int] = {
    val inserts = toteutus.tarjoajat.map(t =>
      sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values (${toteutus.oid}, $t, ${toteutus.muokkaaja})""")
    DBIOHelpers.sumIntDBIOs(inserts)
  }

  def updateToteutus(toteutus: Toteutus): DBIO[Int] = {
    sqlu"""update toteutukset set
              external_id = ${toteutus.externalId},
              koulutus_oid = ${toteutus.koulutusOid},
              tila = ${toteutus.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(toteutus.nimi)}::jsonb,
              metadata = ${toJsonParam(toteutus.metadata)}::jsonb,
              muokkaaja = ${toteutus.muokkaaja},
              esikatselu = ${toteutus.esikatselu},
              organisaatio_oid = ${toteutus.organisaatioOid},
              kielivalinta = ${toJsonParam(toteutus.kielivalinta)}::jsonb,
              teemakuva = ${toteutus.teemakuva},
              sorakuvaus_id = ${toteutus.sorakuvausId.map(_.toString)}::uuid
            where oid = ${toteutus.oid}
            and ( koulutus_oid is distinct from ${toteutus.koulutusOid}
            or external_id is distinct from ${toteutus.externalId}
            or tila is distinct from ${toteutus.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(toteutus.nimi)}::jsonb
            or metadata is distinct from ${toJsonParam(toteutus.metadata)}::jsonb
            or esikatselu is distinct from ${toteutus.esikatselu}
            or kielivalinta is distinct from ${toJsonParam(toteutus.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${toteutus.organisaatioOid}
            or teemakuva is distinct from ${toteutus.teemakuva}
            or sorakuvaus_id is distinct from ${toteutus.sorakuvausId.map(_.toString)}::uuid)"""
  }

  def insertTarjoaja(oid: Option[ToteutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint toteutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid: Option[ToteutusOid], exclude: List[OrganisaatioOid]): DBIO[Int] = {
    sqlu"""delete from toteutusten_tarjoajat
           where toteutus_oid = $oid and tarjoaja_oid not in (#${createOidInParams(exclude)})"""
  }

  def deleteTarjoajat(oid: Option[ToteutusOid]): DBIO[Int] =
    sqlu"""delete from toteutusten_tarjoajat
           where toteutus_oid = $oid"""

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

  def selectByCreatorOrTarjoaja(organisaatioOids: Seq[OrganisaatioOid], myosArkistoidut: Boolean): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
             or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
              #${andTilaMaybeNotArkistoitu(myosArkistoidut)}""".as[ToteutusListItem]
  }

  def selectHakukohteeseenLiitettavatByCreatorOrTarjoaja(organisaatioOids: Seq[OrganisaatioOid], myosArkistoidut: Boolean): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
                 or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
                and (((t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AmmTutkinnonOsa.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AmmOsaamisala.toString}::koulutustyyppi )
                     or (t.metadata->>'hakulomaketyyppi')::hakulomaketyyppi = ${Ataru.toString}::hakulomaketyyppi )
                    #${andTilaMaybeNotArkistoitu(myosArkistoidut)}""".as[ToteutusListItem]
  }

  def selectByKoulutusOid(koulutusOid: KoulutusOid): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          where t.koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          inner join hakukohteet h on h.toteutus_oid = t.oid
          where h.haku_oid = $hakuOid""".as[ToteutusListItem]
  }

  def selectByKoulutusOidAndCreatorOrTarjoaja(koulutusOid: KoulutusOid, organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and t.koulutus_oid = $koulutusOid""".as[ToteutusListItem]
  }

  def selectTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): DBIO[Vector[OrganisaatioOid]] = {
    sql"""select distinct tt.tarjoaja_oid
          from toteutusten_tarjoajat tt
          inner join hakukohteet h on tt.toteutus_oid = h.toteutus_oid
          where h.oid = $hakukohdeOid""".as[OrganisaatioOid]
  }
}
