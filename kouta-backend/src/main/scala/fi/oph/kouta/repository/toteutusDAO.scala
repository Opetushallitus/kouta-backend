package fi.oph.kouta.repository

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain._
import fi.oph.kouta.service.ToteutusService
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.modifiedToInstant
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

trait ToteutusDAO extends EntityModificationDAO[ToteutusOid] {
  def getPutActions(toteutus: Toteutus): DBIO[Toteutus]
  def getUpdateActions(toteutus: Toteutus): DBIO[Option[Toteutus]]

  def get(oid: ToteutusOid, tilaFilter: TilaFilter): Option[(Toteutus, Instant)]
  def get(oids: List[ToteutusOid]): Seq[ToteutusLiitettyListItem]
  def getByKoulutusOid(koulutusOid: KoulutusOid, tilaFilter: TilaFilter): Seq[Toteutus]
  def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid]

  def listByAllowedOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      vainHakukohteeseenLiitettavat: Boolean = false,
      tilaFilter: TilaFilter
  ): Seq[ToteutusListItem]
  def listByKoulutusOid(koulutusOid: KoulutusOid, tilaFilter: TilaFilter): Seq[ToteutusListItem]
  def listByKoulutusOidAndAllowedOrganisaatiot(
      koulutusOid: KoulutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[ToteutusListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[ToteutusListItem]
  def listOpintojaksotByAllowedOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): Seq[ToteutusListItem]
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
      _ <- updateToteutus(toteutus)
      m <- selectLastModified(toteutus.oid.get)
    } yield toteutus.withModified(m.get)

  override def get(oid: ToteutusOid, tilaFilter: TilaFilter): Option[(Toteutus, Instant)] = {
    KoutaDatabase
      .runBlockingTransactionally(for {
        t  <- selectToteutus(oid, tilaFilter).as[Toteutus].headOption
        tt <- selectToteutuksenTarjoajat(oid).as[Tarjoaja]
      } yield (t, tt))
      .get match {
      case (Some(t), tt) => Some((t.copy(tarjoajat = tt.map(_.tarjoajaOid).toList), modifiedToInstant(t.modified.get)))
      case _             => None
    }
  }

  override def get(oids: List[ToteutusOid]) = {
    KoutaDatabase.runBlocking(selectLiitetytToteutuksetOrderedByOids(oids).as[ToteutusLiitettyListItem])
  }

  def updateToteutuksenTarjoajat(toteutus: Toteutus): DBIO[Int] = {
    val (oid, tarjoajat, muokkaaja) = (toteutus.oid, toteutus.tarjoajat, toteutus.muokkaaja)
    val oldTarjoajat = KoutaDatabase.runBlockingTransactionally(for {
      t <- selectToteutuksenTarjoajat(oid.get).as[Tarjoaja]
    } yield t.toList)
    val inserted = tarjoajat.filterNot(oid => oldTarjoajat.get.map(_.tarjoajaOid).contains(oid))
    val deleted  = oldTarjoajat.get.filterNot(t => tarjoajat.contains(t.tarjoajaOid))

    (inserted.nonEmpty, deleted.nonEmpty) match {
      case (true, any) =>
        val actions =
          inserted.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajatByOids(oid, deleted.map(_.tarjoajaOid))
        DBIOHelpers.sumIntDBIOs(actions)
      case (false, true) =>
        // if changes were deletions, database trigger won't update muokkaaja of toteutus
        DBIOHelpers.sumIntDBIOs(
          Seq(updateToteutuksenMuokkaaja(oid, muokkaaja), deleteTarjoajatByOids(oid, deleted.map(_.tarjoajaOid)))
        )
      case _ => DBIO.successful(0)
    }
  }

  override def getTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): Seq[OrganisaatioOid] =
    KoutaDatabase.runBlocking(selectTarjoajatByHakukohdeOid(hakukohdeOid))

  override def getByKoulutusOid(koulutusOid: KoulutusOid, tilaFilter: TilaFilter): Seq[Toteutus] = {
    KoutaDatabase
      .runBlockingTransactionally(
        for {
          toteutukset <- selectToteutuksetByKoulutusOid(koulutusOid, tilaFilter).as[Toteutus]
          tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList)
        } yield (toteutukset, tarjoajat)
      )
      .map { case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(
            tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList
          ).withEnrichedData(ToteutusEnrichedData(esitysnimi = ToteutusService.generateToteutusEsitysnimi(t)))
            .withoutRelatedData()
        )
      }
      .get
  }

  private def listWithTarjoajat(selectListItems: () => DBIO[Seq[ToteutusListItem]]): Seq[ToteutusListItem] =
    KoutaDatabase
      .runBlockingTransactionally(
        for {
          toteutukset <- selectListItems()
          tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid).toList)
        } yield (toteutukset, tarjoajat)
      )
      .map { case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.toString).map(_.tarjoajaOid).toList)
        )
      }
      .get

  override def listByAllowedOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      vainHakukohteeseenLiitettavat: Boolean = false,
      tilaFilter: TilaFilter
  ): Seq[ToteutusListItem] = (organisaatioOids, vainHakukohteeseenLiitettavat) match {
    case (Nil, _)   => Seq()
    case (_, false) => listWithTarjoajat(() => selectByCreatorOrTarjoaja(organisaatioOids, tilaFilter))
    case (_, true) =>
      listWithTarjoajat(() => selectHakukohteeseenLiitettavatByCreatorOrTarjoaja(organisaatioOids, tilaFilter))
  }

  override def listByKoulutusOid(koulutusOid: KoulutusOid, tilaFilter: TilaFilter): Seq[ToteutusListItem] =
    listWithTarjoajat(() => selectByKoulutusOid(koulutusOid, tilaFilter))

  override def listByKoulutusOidAndAllowedOrganisaatiot(
      koulutusOid: KoulutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[ToteutusListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => listWithTarjoajat(() => selectByKoulutusOidAndCreatorOrTarjoaja(koulutusOid, organisaatioOids))
  }

  override def listByHakuOid(hakuOid: HakuOid): Seq[ToteutusListItem] =
    listWithTarjoajat(() => selectByHakuOid(hakuOid))

  override def listOpintojaksotByAllowedOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): Seq[ToteutusListItem] =
    KoutaDatabase.runBlocking(selectOpintojaksot(organisaatioOids, tilaFilter))

  def getToteutuksetByOids(toteutusOids: List[ToteutusOid]): Seq[Toteutus] = {
    KoutaDatabase
      .runBlockingTransactionally(
        for {
          toteutukset <- selectToteutuksetOrderedByOids(toteutusOids).as[Toteutus]
          tarjoajat   <- selectToteutustenTarjoajat(toteutukset.map(_.oid.get).toList)
        } yield (toteutukset, tarjoajat)
      )
      .map { case (toteutukset, tarjoajat) =>
        toteutukset.map(t =>
          t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.get.toString).map(_.tarjoajaOid).toList)
        )
      }
      .get
  }

  def getToteutustenTarjoajatByKoulutusOid(
      koulutusOid: KoulutusOid,
      tilaFilter: TilaFilter = TilaFilter.onlyOlemassaolevatAndArkistoimattomat()
  ): Map[ToteutusOid, Seq[OrganisaatioOid]] = {
    val convert: ((ToteutusOid, Vector[Tarjoaja])) => (ToteutusOid, Seq[OrganisaatioOid]) = {
      case (toteutusOid, tarjoajat) =>
        toteutusOid -> tarjoajat.map(_.tarjoajaOid)
    }
    KoutaDatabase
      .runBlocking(selectToteutustenTarjoajatByKoulutusOid(koulutusOid, tilaFilter))
      .groupBy(tarjoaja => ToteutusOid(tarjoaja.oid.s))
      .map(convert)
  }

  def getOidsByTarjoajat(tarjoajaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter): Seq[ToteutusOid] = {
    KoutaDatabase.runBlocking(selectByCreatorOrTarjoaja(tarjoajaOids, tilaFilter)).map(toteutus => toteutus.oid)
  }

  def getOpintokokonaisuudet(oids: Seq[ToteutusOid]): Seq[OidAndNimi] = {
    KoutaDatabase.runBlocking(selectOpintokokonaisuudet(oids, TilaFilter.onlyJulkaistut))
  }
}

trait ToteutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: ToteutusOid): DBIO[Option[Instant]] = {
    sql"""select t.last_modified from toteutukset t where t.oid = $oid""".as[Instant].headOption
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[ToteutusOid]] = {
    sql"""select oid from toteutukset where $since < last_modified
          union
          select oid from toteutukset_history where $since <@ system_time
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
              t.last_modified,
              k.metadata,
              k.koulutukset_koodi_uri
       from toteutukset t
           inner join (select oid, koulutukset_koodi_uri, metadata from koulutukset) k on k.oid = t.koulutus_oid"""

  def selectToteutus(oid: ToteutusOid, tilaFilter: TilaFilter) =
    sql"""#$selectToteutusSql
          where t.oid = $oid #${tilaConditions(tilaFilter, "t.tila")}"""

  def selectToteutuksetByKoulutusOid(oid: KoulutusOid, tilaFilter: TilaFilter) =
    sql"""#$selectToteutusSql
          where t.koulutus_oid = $oid
          #${tilaConditions(tilaFilter, "t.tila")}
          """

  def selectToteutuksenTarjoajat(oid: ToteutusOid) =
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid = $oid"""

  def selectToteutustenTarjoajat(oids: List[ToteutusOid]): DBIO[Vector[Tarjoaja]] = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid in (#${createOidInParams(
      oids
    )})""".as[Tarjoaja]
  }

  def selectToteutustenTarjoajatByKoulutusOid(
      koulutusOid: KoulutusOid,
      tilaFilter: TilaFilter
  ): DBIO[Vector[Tarjoaja]] = {
    sql"""select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat where toteutus_oid in
               (select oid from toteutukset where koulutus_oid = $koulutusOid #${tilaConditions(tilaFilter)})"""
      .as[Tarjoaja]
  }

  // with ordinality explained: https://stackoverflow.com/a/35456954
  def selectToteutuksetOrderedByOids(toteutusOids: List[ToteutusOid]) = {
    val oidsAsStr = toteutusOids.map(oid => oid.toString())
    sql"""#$selectToteutusSql
          join unnest($oidsAsStr::text[]) with ordinality o(oid, ord)
          on t.oid = o.oid
          order by o.ord"""
  }

  val selectToteutusEntityListItemSql =
    """select t.oid, t.koulutus_oid, t.nimi, t.tila, t.organisaatio_oid, t.muokkaaja, t.last_modified, t.metadata, k.metadata, k.koulutukset_koodi_uri
              from toteutukset t
              inner join (select oid, metadata, koulutukset_koodi_uri from koulutukset) k on k.oid = t.koulutus_oid"""

  def selectLiitetytToteutuksetOrderedByOids(toteutusOids: List[ToteutusOid]) = {
    val oidsAsStr = toteutusOids.map(oid => oid.toString())
    sql"""#$selectToteutusEntityListItemSql
          join unnest($oidsAsStr::text[]) with ordinality o(oid, ord)
          on t.oid = o.oid
          order by o.ord"""
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
    val inserts =
      toteutus.tarjoajat.map(t => sqlu"""insert into toteutusten_tarjoajat (toteutus_oid, tarjoaja_oid, muokkaaja)
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
            and (koulutus_oid is distinct from ${toteutus.koulutusOid}
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

  def updateToteutuksenMuokkaaja(toteutusOid: Option[ToteutusOid], muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""update toteutukset set
              muokkaaja = ${muokkaaja}
            where oid = ${toteutusOid}"""
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

  def deleteTarjoajatByOids(oid: Option[ToteutusOid], deletedOids: List[OrganisaatioOid]): DBIO[Int] = {
    sqlu"""delete from toteutusten_tarjoajat
           where toteutus_oid = $oid and tarjoaja_oid in (#${createOidInParams(deletedOids)})"""
  }

  def deleteTarjoajat(oid: Option[ToteutusOid]): DBIO[Int] = {
    sqlu"""delete from toteutusten_tarjoajat
           where toteutus_oid = $oid"""
  }

  val selectToteutusListSql =
    """select distinct t.oid, t.koulutus_oid, t.nimi, t.tila, t.organisaatio_oid, t.muokkaaja, t.last_modified, t.metadata, k.metadata, k.koulutukset_koodi_uri
         from toteutukset t
         inner join (select oid, metadata, koulutukset_koodi_uri from koulutukset) k on k.oid = t.koulutus_oid"""

  def selectByCreatorOrTarjoaja(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
             or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
              #${tilaConditions(tilaFilter, "t.tila")}""".as[ToteutusListItem]
  }

  def selectHakukohteeseenLiitettavatByCreatorOrTarjoaja(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
                 or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
                and (((t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AmmTutkinnonOsa.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AmmOsaamisala.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${VapaaSivistystyoMuu.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AmmMuu.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${AikuistenPerusopetus.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${TaiteenPerusopetus.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${KkOpintojakso.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${KkOpintokokonaisuus.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${Erikoistumiskoulutus.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${Muu.toString}::koulutustyyppi
                       and (t.metadata->>'tyyppi')::koulutustyyppi is distinct from ${VapaaSivistystyoOsaamismerkki.toString}::koulutustyyppi)
                     or (t.metadata->>'isHakukohteetKaytossa')::boolean = true
                     or (t.metadata->>'hakulomaketyyppi')::hakulomaketyyppi = ${Ataru.toString}::hakulomaketyyppi)
                    #${tilaConditions(tilaFilter, "t.tila")}""".as[ToteutusListItem]
  }

  def selectByKoulutusOid(koulutusOid: KoulutusOid, tilaFilter: TilaFilter): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          where t.koulutus_oid = $koulutusOid
          #${tilaConditions(tilaFilter, "t.tila")}
          """.as[ToteutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          inner join hakukohteet h on h.toteutus_oid = t.oid
          where h.haku_oid = $hakuOid and t.tila != 'poistettu'::julkaisutila""".as[ToteutusListItem]
  }

  def selectByKoulutusOidAndCreatorOrTarjoaja(
      koulutusOid: KoulutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(
      organisaatioOids
    )}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and t.koulutus_oid = $koulutusOid and t.tila != 'poistettu'::julkaisutila""".as[ToteutusListItem]
  }

  def selectTarjoajatByHakukohdeOid(hakukohdeOid: HakukohdeOid): DBIO[Vector[OrganisaatioOid]] = {
    sql"""select distinct tt.tarjoaja_oid
          from toteutusten_tarjoajat tt
          inner join hakukohteet h on tt.toteutus_oid = h.toteutus_oid
          where h.oid = $hakukohdeOid""".as[OrganisaatioOid]
  }

  def selectOpintojaksot(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): DBIO[Vector[ToteutusListItem]] = {
    sql"""#$selectToteutusListSql
          left join toteutusten_tarjoajat tt on t.oid = tt.toteutus_oid
          where (t.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
             or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
             and t.metadata->>'tyyppi' = 'kk-opintojakso'
              #${tilaConditions(tilaFilter, "t.tila")}""".as[ToteutusListItem]
  }

  def selectOpintokokonaisuudet(oids: Seq[ToteutusOid], tilaFilter: TilaFilter): DBIO[Vector[OidAndNimi]] = {
    val oidsAsStr = oids.map(oid => oid.toString())
    sql"""select oid, nimi
          from toteutukset t
          where metadata->>'tyyppi' = 'kk-opintokokonaisuus'
          and array(select jsonb_array_elements_text(metadata->'liitetytOpintojaksot')) && $oidsAsStr::text[]
          #${tilaConditions(tilaFilter, "t.tila")}""".as[OidAndNimi]
  }
}
