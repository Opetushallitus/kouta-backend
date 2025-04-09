package fi.oph.kouta.repository

import java.time.Instant
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.modifiedToInstant
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlStreamingAction

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO extends EntityModificationDAO[KoulutusOid] {
  def getPutActions(koulutus: Koulutus): DBIO[Koulutus]
  def getUpdateActions(koulutus: Koulutus): DBIO[Option[Koulutus]]
  def getUpdateTarjoajatActions(koulutus: Koulutus): DBIO[Koulutus]

  def get(oid: KoulutusOid, tilaFilter: TilaFilter): Option[(Koulutus, Instant)]
  def get(oid: KoulutusOid): Option[Koulutus]
  def get(oids: List[KoulutusOid]): Seq[KoulutusLiitettyListItem]
  def listAllowedByOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyypit: Seq[Koulutustyyppi],
      tilaFilter: TilaFilter
  ): Seq[KoulutusListItem]
  def listAllowedByOrganisaatiotAndKoulutustyyppi(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyyppi: Koulutustyyppi,
      tilaFilter: TilaFilter
  ): Seq[KoulutusListItem]
  def listJulkaistutWithKoulutusOidsAllowedByOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutusOids: Seq[KoulutusOid] = Seq()
  ): Seq[KoulutusListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[KoulutusListItem]
  def getJulkaistutByTarjoajaOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[KoulutusWithMaybeToteutus]

  def listBySorakuvausId(sorakuvausId: UUID, tilaFilter: TilaFilter): Seq[String]
  def listTarjoajaOids(oid: KoulutusOid): Seq[OrganisaatioOid]
  def listUsedEPerusteIds(): Seq[String]
}

object KoulutusDAO extends KoulutusDAO with KoulutusSQL {

  override def getPutActions(koulutus: Koulutus): DBIO[Koulutus] =
    for {
      oid <- insertKoulutus(koulutus)
      _   <- insertKoulutuksenTarjoajat(koulutus.withOid(oid))
      m   <- selectLastModified(oid)
    } yield koulutus.withOid(oid).withModified(m.get)

  override def get(oid: KoulutusOid, tilaFilter: TilaFilter): Option[(Koulutus, Instant)] = {
    KoutaDatabase
      .runBlockingTransactionally(
        for {
          k <- selectKoulutus(oid, tilaFilter).as[Koulutus].headOption
          t <- selectKoulutuksenTarjoajat(oid).as[Tarjoaja]
        } yield (k, t)
      )
      .get match {
      case (Some(k), t) =>
        Some((k.copy(tarjoajat = t.map(_.tarjoajaOid).toList), modifiedToInstant(k.modified.get)))
      case _ => None
    }
  }

  override def get(oid: KoulutusOid): Option[Koulutus] = {
    KoutaDatabase
      .runBlockingTransactionally(selectKoulutus(oid, TilaFilter.onlyOlemassaolevat()).as[Koulutus].headOption)
      .get
  }

  override def get(oids: List[KoulutusOid]) = {
    KoutaDatabase.runBlocking(selectKoulutuksetOrderedByOids(oids).as[KoulutusLiitettyListItem])
  }

  override def getUpdateActions(koulutus: Koulutus): DBIO[Option[Koulutus]] =
    for {
      k <- updateKoulutus(koulutus)
      t <- updateKoulutuksenTarjoajat(koulutus)
      m <- selectLastModified(koulutus.oid.get)
    } yield optionWhen(k + t > 0)(koulutus.withModified(m.get))

  def updateJustKoulutus(koulutus: Koulutus): DBIO[Koulutus] =
    for {
      _ <- updateKoulutus(koulutus)
      m <- selectLastModified(koulutus.oid.get)
    } yield koulutus.withModified(m.get)

  override def getUpdateTarjoajatActions(koulutus: Koulutus): DBIO[Koulutus] =
    for {
      _ <- updateKoulutuksenTarjoajat(koulutus)
      m <- selectLastModified(koulutus.oid.get)
    } yield koulutus.withModified(m.get)

  def updateKoulutuksenTarjoajat(koulutus: Koulutus): DBIO[Int] = {
    val (oid, tarjoajat, muokkaaja) = (koulutus.oid, koulutus.tarjoajat, koulutus.muokkaaja)
    val oldTarjoajat = KoutaDatabase.runBlockingTransactionally(
      for {
        t <- selectKoulutuksenTarjoajat(oid.get).as[Tarjoaja]
      } yield t.toList)
    val inserted = tarjoajat.filterNot(oid => oldTarjoajat.get.map(_.tarjoajaOid).contains(oid))
    val deleted = oldTarjoajat.get.filterNot(t => tarjoajat.contains(t.tarjoajaOid))
    (inserted.nonEmpty, deleted.nonEmpty) match {
      case (true, any) =>
        val actions = inserted.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajatByOids(oid, deleted.map(_.tarjoajaOid))
        DBIOHelpers.sumIntDBIOs(actions)
      case (false, true) =>
        // if changes were deletions, database trigger won't update muokkaaja of toteutus
        DBIOHelpers.sumIntDBIOs(Seq(updateKoulutuksenMuokkaaja(oid, muokkaaja), deleteTarjoajatByOids(oid, deleted.map(_.tarjoajaOid))))
      case _ => DBIO.successful(0)
    }
  }

  private def listWithTarjoajat(selectListItems: => DBIO[Seq[KoulutusListItem]]): Seq[KoulutusListItem] =
    KoutaDatabase
      .runBlockingTransactionally(for {
        koulutukset <- selectListItems
        tarjoajat   <- selectKoulutustenTarjoajat(koulutukset.map(_.oid).toList).as[Tarjoaja]
      } yield (koulutukset, tarjoajat))
      .map {
        case (toteutukset, tarjoajat) => {
          toteutukset.map(t =>
            t.copy(tarjoajat = tarjoajat.filter(_.oid.toString == t.oid.toString).map(_.tarjoajaOid).toList)
          )
        }
      }
      .get

  override def listAllowedByOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyypit: Seq[Koulutustyyppi],
      tilaFilter: TilaFilter
  ): Seq[KoulutusListItem] =
    (organisaatioOids, koulutustyypit) match {
      case (Nil, _) => Seq()
      case (_, Nil) =>
        listWithTarjoajat {
          selectByCreatorAndNotOph(organisaatioOids, tilaFilter)
        } //OPH:lla pitäisi olla aina kaikki koulutustyypit
      case _ =>
        listWithTarjoajat { selectByCreatorOrJulkinenForKoulutustyyppi(organisaatioOids, koulutustyypit, tilaFilter) }
    }

  override def listAllowedByOrganisaatiotAndKoulutustyyppi(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyyppi: Koulutustyyppi,
      tilaFilter: TilaFilter
  ): Seq[KoulutusListItem] =
    (organisaatioOids, koulutustyyppi) match {
      case (Nil, _) => Seq()
      case _ =>
        listWithTarjoajat {
          selectByCreatorOrJulkinenForSpecificKoulutustyyppi(organisaatioOids, koulutustyyppi, tilaFilter)
        }
    }

  override def listJulkaistutWithKoulutusOidsAllowedByOrganisaatiot(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutusOids: Seq[KoulutusOid] = Seq()
  ): Seq[KoulutusListItem] = {
    (organisaatioOids, koulutusOids) match {
      case (Nil, _) => Seq()
      case _ =>
        listWithTarjoajat {
          selectByCreatorAndNotOph(organisaatioOids, TilaFilter.onlyJulkaistut(), koulutusOids)
        }
    }
  }

  override def listByHakuOid(hakuOid: HakuOid): Seq[KoulutusListItem] =
    listWithTarjoajat(selectByHakuOid(hakuOid))

  override def getJulkaistutByTarjoajaOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[KoulutusWithMaybeToteutus] = {
    KoutaDatabase
      .runBlocking(
        findJulkaistutKoulutuksetJaToteutuksetByTarjoajat(organisaatioOids).as[KoulutusWithMaybeToteutus])
  }

  override def listBySorakuvausId(sorakuvausId: UUID, tilaFilter: TilaFilter): Seq[String] = {
    KoutaDatabase.runBlocking(selectOidBySorakuvausId(sorakuvausId, tilaFilter))
  }

  override def listTarjoajaOids(oid: KoulutusOid): Seq[OrganisaatioOid] = {
    KoutaDatabase.runBlocking(selectKoulutuksenTarjoajat(oid).as[Tarjoaja]).map(_.tarjoajaOid)
  }

  def getOidsByTarjoajat(tarjoajaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter): Seq[KoulutusOid] = {
    KoutaDatabase.runBlocking(selectByCreatorOrTarjoaja(tarjoajaOids, tilaFilter))
  }

  def listUsedEPerusteIds(): Seq[String] = KoutaDatabase.runBlocking(selectEPerusteIds())
}

sealed trait KoulutusModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectLastModified(oid: KoulutusOid): DBIO[Option[Instant]] = {
    sql"""select k.last_modified from koulutukset k where k.oid = $oid""".as[Instant].headOption
  }

  def selectModifiedSince(since: Instant): DBIO[Seq[KoulutusOid]] = {
    sql"""select oid from koulutukset where $since < last_modified
          union
          select oid from koulutukset_history where $since <@ system_time
          union
          select koulutus_oid from koulutusten_tarjoajat_history where $since <@ system_time""".as[KoulutusOid]
  }
}

sealed trait KoulutusSQL extends KoulutusExtractors with KoulutusModificationSQL with SQLHelpers {

  def insertKoulutus(koulutus: Koulutus): DBIO[KoulutusOid] = {
    sql"""insert into koulutukset (
            external_id,
            johtaa_tutkintoon,
            tyyppi,
            koulutukset_koodi_uri,
            tila,
            nimi,
            sorakuvaus_id,
            metadata,
            julkinen,
            muokkaaja,
            organisaatio_oid,
            esikatselu,
            kielivalinta,
            teemakuva,
            eperuste_id)
          values (
            ${koulutus.externalId},
            ${koulutus.johtaaTutkintoon},
            ${koulutus.koulutustyyppi.toString}::koulutustyyppi,
            ${koulutus.koulutuksetKoodiUri},
            ${koulutus.tila.toString}::julkaisutila,
            ${toJsonParam(koulutus.nimi)}::jsonb,
            ${koulutus.sorakuvausId.map(_.toString)}::uuid,
            ${toJsonParam(koulutus.metadata)}::jsonb,
            ${koulutus.julkinen},
            ${koulutus.muokkaaja},
            ${koulutus.organisaatioOid},
            ${koulutus.esikatselu},
            ${toJsonParam(koulutus.kielivalinta)}::jsonb,
            ${koulutus.teemakuva},
            ${koulutus.ePerusteId}
            ) returning oid""".as[KoulutusOid].head
  }

  def insertKoulutuksenTarjoajat(koulutus: Koulutus): DBIO[Int] = {
    val inserts =
      koulutus.tarjoajat.map(t => sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values (${koulutus.oid}, $t, ${koulutus.muokkaaja})""")
    DBIOHelpers.sumIntDBIOs(inserts)
  }

  val selectKoulutusSql =
    """select k.oid,
              k.external_id,
              k.johtaa_tutkintoon,
              k.tyyppi,
              k.koulutukset_koodi_uri,
              k.tila,
              k.nimi,
              k.sorakuvaus_id,
              k.metadata,
              k.julkinen,
              k.muokkaaja,
              k.organisaatio_oid,
              k.esikatselu,
              k.kielivalinta,
              k.teemakuva,
              k.eperuste_id,
              k.last_modified
       from koulutukset k"""

  def selectKoulutus(oid: KoulutusOid, tilaFilter: TilaFilter) = {
    sql"""#$selectKoulutusSql
          where k.oid = $oid
            #${tilaConditions(tilaFilter)}
          """
  }

  def findJulkaistutKoulutuksetJaToteutuksetByTarjoajat(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select koulutus.koulutus_oid,
                 koulutus.johtaa_tutkintoon,
                 koulutus.tyyppi,
                 koulutus.koulutukset_koodi_uri,
                 koulutus.tila,
                 koulutus.koulutuksen_tarjoajat,
                 koulutus.nimi,
                 koulutus.metadata,
                 koulutus.organisaatio_oid,
                 koulutus.kielivalinta,
                 koulutus.teemakuva,
                 toteutus.oid,
                 toteutus.external_id,
                 toteutus.koulutus_oid,
                 toteutus.tila,
                 toteutus.toteutuksen_tarjoajat,
                 toteutus.nimi,
                 toteutus.metadata,
                 toteutus.muokkaaja,
                 toteutus.esikatselu,
                 toteutus.organisaatio_oid,
                 toteutus.kielivalinta,
                 toteutus.teemakuva,
                 toteutus.sorakuvaus_id
    from (
      select k.oid as koulutus_oid,
             k.johtaa_tutkintoon,
             k.tyyppi,
             k.koulutukset_koodi_uri,
             k.tila,
             array_agg(distinct kt.tarjoaja_oid) as koulutuksen_tarjoajat,
             k.nimi,
             k.metadata,
             k.organisaatio_oid,
             k.kielivalinta,
             k.teemakuva
      from koulutukset k, koulutusten_tarjoajat kt
      where oid = kt.koulutus_oid
      #${tilaConditions(TilaFilter.onlyJulkaistut(), "k.tila")}
      and kt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)})
      group by k.oid) as koulutus
    left join (
      select t.oid,
             t.external_id,
             t.koulutus_oid as koulutus_oid,
             t.tila,
             array_agg(distinct tt.tarjoaja_oid) as toteutuksen_tarjoajat,
             t.nimi,
             t.metadata,
             t.muokkaaja,
             t.esikatselu,
             t.organisaatio_oid,
             t.kielivalinta,
             t.teemakuva,
             t.sorakuvaus_id
      from toteutukset t, toteutusten_tarjoajat tt
      where t.oid = tt.toteutus_oid
      #${tilaConditions(TilaFilter.onlyOlemassaolevatAndArkistoimattomat(), "t.tila")}
      and tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)})
      group by t.oid) as toteutus
    using (koulutus_oid);"""
  }

  def selectKoulutuksenTarjoajat(oid: KoulutusOid) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  def selectKoulutustenTarjoajat(oids: List[KoulutusOid]) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid in (#${createOidInParams(
      oids
    )})"""
  }

  def updateKoulutus(koulutus: Koulutus): DBIO[Int] = {
    sqlu"""update koulutukset set
              external_id = ${koulutus.externalId},
              johtaa_tutkintoon = ${koulutus.johtaaTutkintoon},
              tyyppi = ${koulutus.koulutustyyppi.toString}::koulutustyyppi,
              koulutukset_koodi_uri = ${koulutus.koulutuksetKoodiUri},
              tila = ${koulutus.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(koulutus.nimi)}::jsonb,
              sorakuvaus_id = ${koulutus.sorakuvausId.map(_.toString)}::uuid,
              metadata = ${toJsonParam(koulutus.metadata)}::jsonb,
              julkinen = ${koulutus.julkinen},
              muokkaaja = ${koulutus.muokkaaja},
              organisaatio_oid = ${koulutus.organisaatioOid},
              esikatselu = ${koulutus.esikatselu},
              kielivalinta = ${toJsonParam(koulutus.kielivalinta)}::jsonb,
              teemakuva = ${koulutus.teemakuva},
              eperuste_id = ${koulutus.ePerusteId}
            where oid = ${koulutus.oid}
            and (johtaa_tutkintoon is distinct from ${koulutus.johtaaTutkintoon}
            or external_id is distinct from ${koulutus.externalId}
            or tyyppi is distinct from ${koulutus.koulutustyyppi.toString}::koulutustyyppi
            or koulutukset_koodi_uri is distinct from ${koulutus.koulutuksetKoodiUri}
            or tila is distinct from ${koulutus.tila.toString}::julkaisutila
            or sorakuvaus_id is distinct from ${koulutus.sorakuvausId.map(_.toString)}::uuid
            or nimi is distinct from ${toJsonParam(koulutus.nimi)}::jsonb
            or julkinen is distinct from ${koulutus.julkinen}
            or metadata is distinct from ${toJsonParam(koulutus.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(koulutus.kielivalinta)}::jsonb
            or teemakuva is distinct from ${koulutus.teemakuva}
            or esikatselu is distinct from ${koulutus.esikatselu}
            or eperuste_id is distinct from ${koulutus.ePerusteId}
            or organisaatio_oid is distinct from ${koulutus.organisaatioOid})"""
  }

  def updateKoulutuksenMuokkaaja(koulutusOid: Option[KoulutusOid], muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""update koulutukset set
              muokkaaja = ${muokkaaja}
            where oid = ${koulutusOid}"""
  }

  def insertTarjoaja(oid: Option[KoulutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint koulutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajatByOids(oid: Option[KoulutusOid], deletedOids: List[OrganisaatioOid]): DBIO[Int] = {
    sqlu"""delete from koulutusten_tarjoajat
           where koulutus_oid = $oid and tarjoaja_oid in (#${createOidInParams(deletedOids)})"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid], exclude: List[OrganisaatioOid]): DBIO[Int] = {
    sqlu"""delete from koulutusten_tarjoajat
          where koulutus_oid = $oid
           and tarjoaja_oid not in (#${createOidInParams(exclude)})"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid]): DBIO[Int] = {
    sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  val selectKoulutusListSql =
    s"""select distinct k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, k.last_modified from koulutukset k"""

  def selectByCreatorAndNotOph(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter,
      koulutusOids: Seq[KoulutusOid] = Seq()
  ) = {
    sql"""#$selectKoulutusListSql
          where (organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and organisaatio_oid <> ${RootOrganisaatioOid})
              #${tilaConditions(tilaFilter)}
              #${if (koulutusOids.nonEmpty) s"and oid in (${createOidInParams(koulutusOids)})" else ""}
      """.as[KoulutusListItem]
  }

  def selectByCreatorOrJulkinenForKoulutustyyppi(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyypit: Seq[Koulutustyyppi],
      tilaFilter: TilaFilter
  ) = {

    sql"""#$selectKoulutusListSql
            left join koulutusten_tarjoajat ta on k.oid = ta.koulutus_oid
        where
            -- Listauksissa halutaan näyttää..
            -- 1. koulutukset, jotka omistaa jokin annetuista organisaatioista, mutta OPH:n omistamat vain, jos koulutustyyppi täsmää.
            -- TODO: Mahdollisesti, jos OPH:n omistama, pitäisi katsoa, että se on lisäksi julkinen ja mätsää oppilaitostyyppeihin
            ((k.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
                and (k.organisaatio_oid <> ${RootOrganisaatioOid} or k.tyyppi in (#${createKoulutustyypitInParams(
      koulutustyypit
    )})))
            -- 2. koulutustyyppeihin täsmäävät koulutukset, jotka ovat julkisia
            or (k.julkinen = ${true} and k.tyyppi in (#${createKoulutustyypitInParams(koulutustyypit)}))
            -- 3. jotka ovat avointa korkeakoulutusta ja tarjoajista (järjestäjistä) löytyy annettuja organisaatioita
            or (k.metadata ->> 'isAvoinKorkeakoulutus' = 'true'
                and ta.tarjoaja_oid in (#${createOidInParams(organisaatioOids)})))
    #${tilaConditions(tilaFilter, glueWord = "and")}
""".as[KoulutusListItem]
  }

  def selectByCreatorOrJulkinenForSpecificKoulutustyyppi(
      organisaatioOids: Seq[OrganisaatioOid],
      koulutustyyppi: Koulutustyyppi,
      tilaFilter: TilaFilter
  ) = {
    sql"""#$selectKoulutusListSql
          where tyyppi = ${koulutustyyppi.toString}::koulutustyyppi and
              (organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or julkinen = ${true})
              and k.tila != 'poistettu'::julkaisutila
              #${tilaConditions(tilaFilter)}
      """.as[KoulutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""#$selectKoulutusListSql
          inner join toteutukset t on k.oid = t.koulutus_oid
          inner join hakukohteet h on t.oid = h.toteutus_oid
          where h.haku_oid = ${hakuOid.toString} and k.tila != 'poistettu'::julkaisutila""".as[KoulutusListItem]
  }

  def selectOidBySorakuvausId(sorakuvausId: UUID, tilaFilter: TilaFilter) = {
    sql"""select oid
          from koulutukset
          where sorakuvaus_id = ${sorakuvausId.toString}::uuid
          #${tilaConditions(tilaFilter)}
      """.as[String]
  }

  def selectByCreatorOrTarjoaja(
      organisaatioOids: Seq[OrganisaatioOid],
      tilaFilter: TilaFilter
  ): DBIO[Vector[KoulutusOid]] = {
    sql"""select distinct k.oid
          from koulutukset k, koulutusten_tarjoajat kt
          where k.oid = kt.koulutus_oid
          and (k.organisaatio_oid in (#${createOidInParams(organisaatioOids)})
             or kt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
              #${tilaConditions(tilaFilter, "k.tila")}""".as[KoulutusOid]
  }

  val selectKoulutusListItemSql =
    """select k.oid,
              k.nimi,
              k.tila,
              k.organisaatio_oid,
              k.muokkaaja,
              k.last_modified,
              k.tyyppi,
              k.julkinen,
              k.metadata
       from koulutukset k"""

  def selectKoulutuksetOrderedByOids(koulutusOids: List[KoulutusOid]) = {
    val oidsAsStr = koulutusOids.map(oid => oid.toString())
    sql"""#$selectKoulutusListItemSql
          join unnest($oidsAsStr::text[]) with ordinality o(oid, ord)
          on k.oid = o.oid
          where #${tilaConditions(TilaFilter.onlyOlemassaolevatAndArkistoimattomat(), "k.tila", "")}
          order by o.ord
          """
  }

  def selectEPerusteIds(): SqlStreamingAction[Vector[String], String, Effect] = {
    sql"""select distinct eperusteid from (
            select tila, eperuste_id::text as eperusteid from koulutukset where eperuste_id is not null
            union all
            select tila, tutkinnonOsa ->> 'ePerusteId' as eperusteid from koulutukset,
              LATERAL jsonb_array_elements(metadata -> 'tutkinnonOsat') as tutkinnonOsa) as eperusteidt
          where #${tilaConditions(TilaFilter.onlyOlemassaolevatAndArkistoimattomat(), "tila", "")}
          """.as[String]
  }
}
