package fi.oph.kouta.repository

import java.time.Instant
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.instantToModified
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

trait KoulutusDAO extends EntityModificationDAO[KoulutusOid] {
  def getPutActions(koulutus: Koulutus): DBIO[Koulutus]
  def getUpdateActions(koulutus: Koulutus): DBIO[Option[Koulutus]]
  def getUpdateTarjoajatActions(koulutus: Koulutus): DBIO[Koulutus]

  def get(oid: KoulutusOid, myosPoistetut: Boolean = false): Option[(Koulutus, Instant)]
  def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean, myosPoistetut: Boolean = false): Seq[KoulutusListItem]
  def listAllowedByOrganisaatiotAndKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean): Seq[KoulutusListItem]
  def listByHakuOid(hakuOid: HakuOid) :Seq[KoulutusListItem]
  def getJulkaistutByTarjoajaOids(organisaatioOids: Seq[OrganisaatioOid]): Seq[Koulutus]
  def listBySorakuvausId(sorakuvausId: UUID, myosPoistetut: Boolean = false): Seq[String]
}

object KoulutusDAO extends KoulutusDAO with KoulutusSQL {

  override def getPutActions(koulutus: Koulutus): DBIO[Koulutus] =
    for {
      oid <- insertKoulutus(koulutus)
      _   <- insertKoulutuksenTarjoajat(koulutus.withOid(oid))
      m   <- selectLastModified(oid)
    } yield koulutus.withOid(oid).withModified(m.get)

  override def get(oid: KoulutusOid, myosPoistetut: Boolean = false): Option[(Koulutus, Instant)] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        k <- selectKoulutus(oid, myosPoistetut).as[Koulutus].headOption
        t <- selectKoulutuksenTarjoajat(oid).as[Tarjoaja]
        l <- selectLastModified(oid)
      } yield (k, t, l)
    ).get match {
      case (Some(k), t, Some(l)) =>
        Some((k.copy(modified = Some(instantToModified(l)), tarjoajat = t.map(_.tarjoajaOid).toList), l))
      case _ => None
    }
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

  private def updateKoulutuksenTarjoajat(koulutus: Koulutus): DBIO[Int] = {
    val (oid, tarjoajat, muokkaaja) = (koulutus.oid, koulutus.tarjoajat, koulutus.muokkaaja)
    if (tarjoajat.nonEmpty) {
      val actions = tarjoajat.map(insertTarjoaja(oid, _, muokkaaja)) :+ deleteTarjoajat(oid, tarjoajat)
      DBIOHelpers.sumIntDBIOs(actions)
    } else {
      deleteTarjoajat(oid)
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

  override def listAllowedByOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean, myosPoistetut: Boolean = false): Seq[KoulutusListItem] =
    (organisaatioOids, koulutustyypit) match {
      case (Nil, _) => Seq()
      case (_, Nil) => listWithTarjoajat { selectByCreatorAndNotOph(organisaatioOids, myosArkistoidut, myosPoistetut) } //OPH:lla pitäisi olla aina kaikki koulutustyypit
      case _        => listWithTarjoajat { selectByCreatorOrJulkinenForKoulutustyyppi(organisaatioOids, koulutustyypit, myosArkistoidut, myosPoistetut) }
    }

  override def listAllowedByOrganisaatiotAndKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean): Seq[KoulutusListItem] =
    (organisaatioOids, koulutustyyppi) match {
      case (Nil, _) => Seq()
      case _        => listWithTarjoajat { selectByCreatorOrJulkinenForSpecificKoulutustyyppi(organisaatioOids, koulutustyyppi, myosArkistoidut) }
    }

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

  def getTilaAndTyyppi(koulutusOid: KoulutusOid): (Option[Julkaisutila], Option[Koulutustyyppi]) =
    KoutaDatabase.runBlocking(selectTilaAndTyyppi(koulutusOid)) match {
      case None => (None, None)
      case Some((tila, tyyppi)) => (Some(tila), Some(tyyppi))
    }

  override def listBySorakuvausId(sorakuvausId: UUID, myosPoistetut: Boolean = false): Seq[String] = {
    KoutaDatabase.runBlocking(selectOidBySorakuvausId(sorakuvausId, myosPoistetut))
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
            ${koulutus.ePerusteId}) returning oid""".as[KoulutusOid].head
  }

  def insertKoulutuksenTarjoajat(koulutus: Koulutus): DBIO[Int] = {
    val inserts = koulutus.tarjoajat.map(t =>
      sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values (${koulutus.oid}, $t, ${koulutus.muokkaaja})""")
    DBIOHelpers.sumIntDBIOs(inserts)
  }

  def selectKoulutus(oid: KoulutusOid, myosPoistetut: Boolean = false) = {
    sql"""select oid,
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
                 eperuste_id,
                 lower(system_time)
          from koulutukset
          where oid = $oid
            #${andTilaMaybeNotPoistettu(myosPoistetut)}
          """
  }

  def findJulkaistutKoulutuksetByTarjoajat(organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select distinct k.oid,
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
                          m.modified
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
            and ( johtaa_tutkintoon is distinct from ${koulutus.johtaaTutkintoon}
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

  def insertTarjoaja(oid: Option[KoulutusOid], tarjoaja: OrganisaatioOid, muokkaaja: UserOid ): DBIO[Int] = {
    sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $tarjoaja, $muokkaaja)
             on conflict on constraint koulutusten_tarjoajat_pkey do nothing"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid], exclude: List[OrganisaatioOid]): DBIO[Int] = {
    sqlu"""delete from koulutusten_tarjoajat
          where koulutus_oid = $oid
           and tarjoaja_oid not in (#${createOidInParams(exclude)})"""
  }

  def deleteTarjoajat(oid: Option[KoulutusOid]): DBIO[Int] =
    sqlu"""delete from koulutusten_tarjoajat where koulutus_oid = $oid"""

  val selectKoulutusListSql =
    """select distinct k.oid, k.nimi, k.tila, k.organisaatio_oid, k.muokkaaja, m.modified
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
           group by k.oid) m on k.oid = m.oid"""

  def selectByCreatorAndNotOph(organisaatioOids: Seq[OrganisaatioOid], myosArkistoidut: Boolean, myosPoistetut: Boolean = false) = {
    sql"""#$selectKoulutusListSql
          where (organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and organisaatio_oid <> ${RootOrganisaatioOid})
              #${andTilaMaybeNotPoistettu(myosPoistetut)}
              #${andTilaMaybeNotArkistoitu(myosArkistoidut)}
      """.as[KoulutusListItem]
  }

  def selectByCreatorOrJulkinenForKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], koulutustyypit: Seq[Koulutustyyppi], myosArkistoidut: Boolean, myosPoistetut: Boolean = false) = {
    sql"""#$selectKoulutusListSql
          where ((organisaatio_oid in (#${createOidInParams(organisaatioOids)}) and
                  (organisaatio_oid <> ${RootOrganisaatioOid} or
                   tyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
              or (julkinen = ${true} and tyyppi in (#${createKoulutustyypitInParams(koulutustyypit)})))
              #${andTilaMaybeNotPoistettu(myosPoistetut)}
              #${andTilaMaybeNotArkistoitu(myosArkistoidut)}
      """.as[KoulutusListItem]
  }

  def selectByCreatorOrJulkinenForSpecificKoulutustyyppi(organisaatioOids: Seq[OrganisaatioOid], koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean) = {
    sql"""#$selectKoulutusListSql
          where tyyppi = ${koulutustyyppi.toString}::koulutustyyppi and
              (organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or julkinen = ${true})
              and k.tila != 'poistettu'::julkaisutila
              #${andTilaMaybeNotArkistoitu(myosArkistoidut)}
      """.as[KoulutusListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""#$selectKoulutusListSql
          inner join toteutukset t on k.oid = t.koulutus_oid
          inner join hakukohteet h on t.oid = h.toteutus_oid
          where h.haku_oid = ${hakuOid.toString} and k.tila != 'poistettu'::julkaisutila""".as[KoulutusListItem]
  }

  def selectTilaAndTyyppi(koulutusOid: KoulutusOid): DBIO[Option[(Julkaisutila, Koulutustyyppi)]] =
    sql"""select tila, tyyppi from koulutukset
            where oid = $koulutusOid
            and tila != 'poistettu'::julkaisutila
    """.as[(Julkaisutila, Koulutustyyppi)].headOption

  def selectOidBySorakuvausId(sorakuvausId: UUID, myosPoistetut: Boolean = false) = {
    sql"""select oid
          from koulutukset
          where sorakuvaus_id = ${sorakuvausId.toString}::uuid
          #${andTilaMaybeNotPoistettu(myosPoistetut)}
      """.as[String]
  }
}
