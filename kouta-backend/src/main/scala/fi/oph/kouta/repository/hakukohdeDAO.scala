package fi.oph.kouta.repository

import fi.oph.kouta.domain
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.MiscUtils.optionWhen
import fi.oph.kouta.util.TimeUtils.modifiedToInstant
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

trait HakukohdeDAO extends EntityModificationDAO[HakukohdeOid] {
  def getPutActions(hakukohde: Hakukohde): DBIO[Hakukohde]
  def getUpdateActions(hakukohde: Hakukohde): DBIO[Option[Hakukohde]]

  def get(oid: HakukohdeOid, tilaFilter: TilaFilter): Option[(Hakukohde, Instant)]
  def getHakukohteetByHakuOid(hakuOid: HakuOid, tilaFilter: TilaFilter): Seq[Hakukohde]
  def listByToteutusOid(oid: ToteutusOid, tilaFilter: TilaFilter): Seq[HakukohdeListItem]
  def listByToteutusOidAndAllowedOrganisaatiot(
      toteutusOid: ToteutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[HakukohdeListItem]
  def listByHakuOid(hakuOid: HakuOid, tilaFilter: TilaFilter): Seq[HakukohdeListItem]
  def listByHakuOidAndAllowedOrganisaatiot(
      hakuOid: HakuOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[HakukohdeListItem]
  def listByValintaperusteId(valintaperusteId: UUID, tilaFilter: TilaFilter): Seq[HakukohdeListItem]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem]

  def archiveHakukohdesByHakukohdeOids(hakukohdeOids: Seq[HakukohdeOid]): Int
  def listArchivableHakukohdeOidsByHakuOids(hakuOids: Seq[HakuOid]): Seq[HakukohdeOid]
  def getDependencyInformation(hakukohde: Hakukohde): Option[HakukohdeDependencyInformation]
  def removeJarjestaaUrheilijanAmmatillistaKoulutustaByJarjestyspaikkaOid(
      jarjestyspaikkaOid: OrganisaatioOid
  ): DBIO[Int]
}

object HakukohdeDAO extends HakukohdeDAO with HakukohdeSQL {

  override def getPutActions(hakukohde: Hakukohde): DBIO[Hakukohde] =
    for {
      oid <- insertHakukohde(hakukohde)
      _   <- insertHakuajat(hakukohde.withOid(oid))
      _   <- insertValintakokeet(hakukohde.withOid(oid))
      _   <- insertLiitteet(hakukohde.withOid(oid))
      m   <- selectLastModified(oid)
    } yield hakukohde.withOid(oid).withModified(m.get)

  override def getUpdateActions(hakukohde: Hakukohde): DBIO[Option[Hakukohde]] =
    for {
      hk <- updateHakukohde(hakukohde)
      ha <- updateHakuajat(hakukohde)
      vk <- updateValintakokeet(hakukohde)
      l  <- updateLiitteet(hakukohde)
      m  <- selectLastModified(hakukohde.oid.get)
    } yield optionWhen(hk + ha + vk + l > 0)(hakukohde.withModified(m.get))

  override def get(oid: HakukohdeOid, tilaFilter: TilaFilter): Option[(Hakukohde, Instant)] = {
    KoutaDatabase
      .runBlockingTransactionally(for {
        h <- selectHakukohde(oid, tilaFilter)
        a <- selectHakuajat(oid)
        k <- selectValintakokeet(oid)
        i <- selectLiitteet(oid)
      } yield (h, a, k, i))
      .get match {
      case (Some(h), a, k, i) =>
        Some(
          (
            h.copy(
              hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList,
              valintakokeet = k.toList,
              liitteet = i.toList
            ),
            modifiedToInstant(h.modified.get)
          )
        )
      case _ => None
    }
  }

  override def getHakukohteetByHakuOid(
      hakuOid: HakuOid,
      tilaFilter: TilaFilter
  ): Seq[Hakukohde] = KoutaDatabase.runBlocking(selectFullHakukohteetByHakuOid(hakuOid, tilaFilter))

  private def updateHakuajat(hakukohde: Hakukohde): DBIO[Int] = {
    val (oid, hakuajat, muokkaaja) = (hakukohde.oid, hakukohde.hakuajat, hakukohde.muokkaaja)
    if (hakuajat.nonEmpty) {
      val actions = hakuajat.map(t => insertHakuaika(oid, t, muokkaaja)) :+ deleteHakuajat(oid, hakuajat)
      DBIOHelpers.sumIntDBIOs(actions)
    } else {
      deleteHakuajat(oid)
    }
  }

  private def updateValintakokeet(hakukohde: Hakukohde): DBIO[Int] = {
    val (oid, valintakokeet, muokkaaja) = (hakukohde.oid, hakukohde.valintakokeet, hakukohde.muokkaaja)
    val (insert, update)                = valintakokeet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteValintakokeet(oid, update.map(_.id.get)) }
    else { deleteValintakokeet(oid) }
    val insertSQL = insert.map(v => insertValintakoe(oid, v.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateValintakoe(oid, v, muokkaaja))

    deleteSQL.zipWith(DBIOHelpers.sumIntDBIOs(insertSQL ++ updateSQL))(_ + _)
  }

  private def updateLiitteet(hakukohde: Hakukohde): DBIO[Int] = {
    val (oid, liitteet, muokkaaja) = (hakukohde.oid, hakukohde.liitteet, hakukohde.muokkaaja)
    val (insert, update)           = liitteet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteLiitteet(oid, update.map(_.id.get)) }
    else { deleteLiitteet(oid) }
    val insertSQL = insert.map(l => insertLiite(oid, l.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateLiite(oid, v, muokkaaja))

    deleteSQL.zipWith(DBIOHelpers.sumIntDBIOs(insertSQL ++ updateSQL))(_ + _)
  }

  override def listByHakuOidAndAllowedOrganisaatiot(
      hakuOid: HakuOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[HakukohdeListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByHakuOidAndAllowedOrganisaatiot(hakuOid, organisaatioOids))
  }

  override def listByHakuOid(hakuOid: HakuOid, tilaFilter: TilaFilter): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByHakuOid(hakuOid, tilaFilter))

  override def listByToteutusOid(toteutusOid: ToteutusOid, tilaFilter: TilaFilter): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid, tilaFilter))

  override def listByToteutusOidAndAllowedOrganisaatiot(
      toteutusOid: ToteutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): Seq[HakukohdeListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByToteutusOidAndAllowedOrganisaatiot(toteutusOid, organisaatioOids))
  }

  override def listByValintaperusteId(valintaperusteId: UUID, tilaFilter: TilaFilter): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByValintaperusteId(valintaperusteId, tilaFilter))

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem] =
    organisaatioOids match {
      case Nil => Seq()
      case _   => KoutaDatabase.runBlocking(selectByAllowedOrganisaatiot(organisaatioOids))
    }

  override def getDependencyInformation(hakukohde: Hakukohde): Option[HakukohdeDependencyInformation] = {
    val toteutusDependencyInfo: Option[HakukohdeToteutusDependencyInfo] =
      KoutaDatabase.runBlocking(selectToteutusDependencyInformation(hakukohde))
    val valintaperusteDependencyInfo: Option[HakukohdeValintaperusteDependencyInfo] =
      KoutaDatabase.runBlocking(selectValintaperusteDependencyInformation(hakukohde))
    val jarjestyspaikkaDependencyInfo: Option[HakukohdeJarjestyspaikkaDependencyInfo] =
      hakukohde.jarjestyspaikkaOid match {
        case Some(jarjestyspaikkaOid) =>
          KoutaDatabase.runBlocking(selectJarjestyspaikkaDependencyInformation(jarjestyspaikkaOid))
        case None => None
      }
    toteutusDependencyInfo match {
      case Some(toteutusInfo) =>
        Some(HakukohdeDependencyInformation(toteutusInfo, valintaperusteDependencyInfo, jarjestyspaikkaDependencyInfo))
      case None => None
    }
  }

  def getOidsByJarjestyspaikka(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter): Seq[String] = {
    KoutaDatabase.runBlocking(selectOidsByJarjestyspaikkaOids(jarjestyspaikkaOids, tilaFilter))
  }

  def getHakukohdeAndRelatedEntities(hakukohdeOids: List[HakukohdeOid]) = {
    KoutaDatabase
      .runBlockingTransactionally(
        selectHakukohdeAndRelatedEntities(hakukohdeOids)
      )
      .get
  }

  override def listArchivableHakukohdeOidsByHakuOids(hakuOids: Seq[HakuOid]): Seq[HakukohdeOid] = {
    KoutaDatabase.runBlocking(selectArchivableHakukohdeOidsByHakuOids(hakuOids))
  }

  override def archiveHakukohdesByHakukohdeOids(hakukohdeOids: Seq[HakukohdeOid]): Int = {
    KoutaDatabase.runBlocking(updateHakukohdesToArchivedByHakukohdeOids(hakukohdeOids: Seq[HakukohdeOid]))
  }
}

sealed trait HakukohdeModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakukohdeOid]] = {
    sql"""select oid from hakukohteet where $since < last_modified
          union
          select oid from hakukohteet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_hakuajat_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_valintakokeet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_liitteet_history where $since <@ system_time""".as[HakukohdeOid]
  }

  def selectLastModified(oid: HakukohdeOid): DBIO[Option[Instant]] = {
    sql"""select ha.last_modified from hakukohteet ha where ha.oid = $oid""".as[Option[Instant]].head
  }
}

sealed trait HakukohdeSQL extends SQLHelpers with HakukohdeModificationSQL with HakukohdeExtractors {

  def insertHakukohde(hakukohde: Hakukohde): DBIO[HakukohdeOid] = {
    sql"""insert into hakukohteet (
            external_id,
            toteutus_oid,
            haku_oid,
            tila,
            nimi,
            hakukohde_koodi_uri,
            hakulomaketyyppi,
            hakulomake_ataru_id,
            hakulomake_kuvaus,
            hakulomake_linkki,
            kaytetaan_haun_hakulomaketta,
            jarjestyspaikka_oid,
            pohjakoulutusvaatimus_koodi_urit,
            pohjakoulutusvaatimus_tarkenne,
            muu_pohjakoulutusvaatimus_kuvaus,
            toinen_aste_onko_kaksoistutkinto,
            kaytetaan_haun_aikataulua,
            valintaperuste_id,
            liitteet_onko_sama_toimitusaika,
            liitteet_onko_sama_toimitusosoite,
            liitteiden_toimitusaika,
            liitteiden_toimitustapa,
            liitteiden_toimitusosoite,
            esikatselu,
            metadata,
            muokkaaja,
            organisaatio_oid,
            kielivalinta
          ) values (
            ${hakukohde.externalId},
            ${hakukohde.toteutusOid},
            ${hakukohde.hakuOid},
            ${hakukohde.tila.toString}::julkaisutila,
            ${toJsonParam(hakukohde.nimi)}::jsonb,
            ${hakukohde.hakukohdeKoodiUri},
            ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
            ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid,
            ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb,
            ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb,
            ${hakukohde.kaytetaanHaunHakulomaketta},
            ${hakukohde.jarjestyspaikkaOid},
            ${hakukohde.pohjakoulutusvaatimusKoodiUrit},
            ${toJsonParam(hakukohde.pohjakoulutusvaatimusTarkenne)}::jsonb,
            ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
            ${hakukohde.toinenAsteOnkoKaksoistutkinto},
            ${hakukohde.kaytetaanHaunAikataulua},
            ${hakukohde.valintaperusteId.map(_.toString)}::uuid,
            ${hakukohde.liitteetOnkoSamaToimitusaika},
            ${hakukohde.liitteetOnkoSamaToimitusosoite},
            ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp,
            ${hakukohde.liitteidenToimitustapa.map(_.toString)}::liitteen_toimitustapa,
            ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb,
            ${hakukohde.esikatselu},
            ${toJsonParam(hakukohde.metadata)}::jsonb,
            ${hakukohde.muokkaaja},
            ${hakukohde.organisaatioOid},
            ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          ) returning oid""".as[HakukohdeOid].head
  }

  def updateHakukohde(hakukohde: Hakukohde): DBIO[Int] = {
    // note! This updates always when muokkaaja changes, even without other changes
    sqlu"""update hakukohteet set
              external_id = ${hakukohde.externalId},
              toteutus_oid = ${hakukohde.toteutusOid},
              haku_oid = ${hakukohde.hakuOid},
              tila = ${hakukohde.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(hakukohde.nimi)}::jsonb,
              hakukohde_koodi_uri = ${hakukohde.hakukohdeKoodiUri},
              hakulomaketyyppi = ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake_ataru_id = ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid,
              hakulomake_kuvaus = ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb,
              hakulomake_linkki = ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb,
              kaytetaan_haun_hakulomaketta = ${hakukohde.kaytetaanHaunHakulomaketta},
              jarjestyspaikka_oid = ${hakukohde.jarjestyspaikkaOid},
              pohjakoulutusvaatimus_koodi_urit = ${hakukohde.pohjakoulutusvaatimusKoodiUrit},
              pohjakoulutusvaatimus_tarkenne = ${toJsonParam(hakukohde.pohjakoulutusvaatimusTarkenne)}::jsonb,
              muu_pohjakoulutusvaatimus_kuvaus = ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
              toinen_aste_onko_kaksoistutkinto = ${hakukohde.toinenAsteOnkoKaksoistutkinto},
              kaytetaan_haun_aikataulua = ${hakukohde.kaytetaanHaunAikataulua},
              valintaperuste_id = ${hakukohde.valintaperusteId.map(_.toString)}::uuid,
              liitteet_onko_sama_toimitusaika = ${hakukohde.liitteetOnkoSamaToimitusaika},
              liitteet_onko_sama_toimitusosoite = ${hakukohde.liitteetOnkoSamaToimitusosoite},
              liitteiden_toimitusaika = ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp,
              liitteiden_toimitustapa = ${hakukohde.liitteidenToimitustapa.map(_.toString)}::liitteen_toimitustapa,
              liitteiden_toimitusosoite = ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb,
              esikatselu = ${hakukohde.esikatselu},
              metadata = ${toJsonParam(hakukohde.metadata)}::jsonb,
              muokkaaja = ${hakukohde.muokkaaja},
              organisaatio_oid = ${hakukohde.organisaatioOid},
              kielivalinta = ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          where oid = ${hakukohde.oid}
            and ( external_id is distinct from ${hakukohde.externalId}
            or toteutus_oid is distinct from ${hakukohde.toteutusOid}
            or haku_oid is distinct from ${hakukohde.hakuOid}
            or tila is distinct from ${hakukohde.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(hakukohde.nimi)}::jsonb
            or hakukohde_koodi_uri is distinct from ${hakukohde.hakukohdeKoodiUri}
            or hakulomaketyyppi is distinct from ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake_ataru_id is distinct from ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid
            or hakulomake_kuvaus is distinct from ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb
            or hakulomake_linkki is distinct from ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb
            or kaytetaan_haun_hakulomaketta is distinct from ${hakukohde.kaytetaanHaunHakulomaketta}
            or jarjestyspaikka_oid is distinct from ${hakukohde.jarjestyspaikkaOid}
            or pohjakoulutusvaatimus_koodi_urit is distinct from ${hakukohde.pohjakoulutusvaatimusKoodiUrit}
            or pohjakoulutusvaatimus_tarkenne is distinct from ${toJsonParam(hakukohde.pohjakoulutusvaatimusTarkenne)}::jsonb
            or muu_pohjakoulutusvaatimus_kuvaus is distinct from ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb
            or toinen_aste_onko_kaksoistutkinto is distinct from ${hakukohde.toinenAsteOnkoKaksoistutkinto}
            or kaytetaan_haun_aikataulua is distinct from ${hakukohde.kaytetaanHaunAikataulua}
            or valintaperuste_id is distinct from ${hakukohde.valintaperusteId.map(_.toString)}::uuid
            or liitteet_onko_sama_toimitusaika is distinct from ${hakukohde.liitteetOnkoSamaToimitusaika}
            or liitteet_onko_sama_toimitusosoite is distinct from ${hakukohde.liitteetOnkoSamaToimitusosoite}
            or liitteiden_toimitusaika is distinct from ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp
            or liitteiden_toimitustapa is distinct from ${hakukohde.liitteidenToimitustapa
      .map(_.toString)}::liitteen_toimitustapa
            or liitteiden_toimitusosoite is distinct from ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb
            or esikatselu is distinct from ${hakukohde.esikatselu}
            or metadata is distinct from ${toJsonParam(hakukohde.metadata)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(hakukohde.kielivalinta)}::jsonb
            or muokkaaja is distinct from ${hakukohde.muokkaaja}
            or organisaatio_oid is distinct from ${hakukohde.organisaatioOid})"""
  }

  val selectFullHakukohdeSql = """select oid,
external_id,
toteutus_oid,
haku_oid,
tila,
nimi,
hakukohde_koodi_uri,
hakulomaketyyppi,
hakulomake_ataru_id,
hakulomake_kuvaus,
hakulomake_linkki,
kaytetaan_haun_hakulomaketta,
jarjestyspaikka_oid,
pohjakoulutusvaatimus_koodi_urit,
pohjakoulutusvaatimus_tarkenne,
muu_pohjakoulutusvaatimus_kuvaus,
toinen_aste_onko_kaksoistutkinto,
kaytetaan_haun_aikataulua,
valintaperuste_id,
liitteet_onko_sama_toimitusaika,
liitteet_onko_sama_toimitusosoite,
liitteiden_toimitusaika,
liitteiden_toimitustapa,
liitteiden_toimitusosoite,
esikatselu,
metadata,
muokkaaja,
organisaatio_oid,
kielivalinta,
last_modified from hakukohteet
""".stripMargin

  def selectHakukohde(oid: HakukohdeOid, tilaFilter: TilaFilter): DBIO[Option[Hakukohde]] = {
    sql"""#$selectFullHakukohdeSql where oid = $oid #${tilaConditions(tilaFilter)}""".as[Hakukohde].headOption
  }

  def selectFullHakukohteetByHakuOid(hakuOid: HakuOid, tilaFilter: TilaFilter): DBIO[Seq[Hakukohde]] = {
    sql"""#$selectFullHakukohdeSql where haku_oid = $hakuOid #${tilaConditions(tilaFilter)}""".as[Hakukohde]
  }

  def insertHakuajat(hakukohde: Hakukohde): DBIO[Int] = {
    val inserts = hakukohde.hakuajat.map(t =>
      sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
              values (${hakukohde.oid}, tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                                                ${formatTimestampParam(
        t.paattyy
      )}::timestamp, '[)'), ${hakukohde.muokkaaja})"""
    )

    DBIO.sequence(inserts).map(_.sum)
  }

  def insertValintakokeet(hakukohde: Hakukohde): DBIO[Int] = {
    val inserts = hakukohde.valintakokeet.map(k =>
      insertValintakoe(hakukohde.oid, k.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja)
    )
    DBIO.sequence(inserts).map(_.sum)
  }

  def insertValintakoe(oid: Option[HakukohdeOid], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into hakukohteiden_valintakokeet (
             id, hakukohde_oid, tyyppi_koodi_uri, nimi, metadata, tilaisuudet, muokkaaja)
           values (
             ${valintakoe.id.map(_.toString)}::uuid,
             $oid,
             ${valintakoe.tyyppiKoodiUri},
             ${toJsonParam(valintakoe.nimi)}::jsonb,
             ${toJsonParam(valintakoe.metadata)}::jsonb,
             ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
             $muokkaaja)"""
  }

  def insertLiitteet(hakukohde: Hakukohde): DBIO[Int] = {
    val inserts =
      hakukohde.liitteet.map(l => insertLiite(hakukohde.oid, l.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja))
    DBIOHelpers.sumIntDBIOs(inserts)
  }

  def insertLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into hakukohteiden_liitteet (
             id,
             hakukohde_oid,
             tyyppi_koodi_uri,
             nimi,
             kuvaus,
             toimitusaika,
             toimitustapa,
             toimitusosoite,
             muokkaaja
           ) values (
             ${liite.id.map(_.toString)}::uuid,
             $oid,
             ${liite.tyyppiKoodiUri},
             ${toJsonParam(liite.nimi)}::jsonb,
             ${toJsonParam(liite.kuvaus)}::jsonb,
             ${formatTimestampParam(liite.toimitusaika)}::timestamp,
             ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa,
             ${toJsonParam(liite.toimitusosoite)}::jsonb,
             $muokkaaja)"""
  }

  def selectHakuajat(oid: HakukohdeOid): DBIO[Vector[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika) from hakukohteiden_hakuajat where hakukohde_oid = $oid"""
      .as[Hakuaika]
  }

  def selectValintakokeet(oid: HakukohdeOid): DBIO[Vector[Valintakoe]] = {
    sql"""select id, tyyppi_koodi_uri, nimi, metadata, tilaisuudet
          from hakukohteiden_valintakokeet where hakukohde_oid = $oid""".as[Valintakoe]
  }

  def selectLiitteet(oid: HakukohdeOid): DBIO[Vector[Liite]] = {
    sql"""select id, tyyppi_koodi_uri, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
          from hakukohteiden_liitteet where hakukohde_oid = $oid""".as[Liite]
  }

  def insertHakuaika(oid: Option[HakukohdeOid], hakuaika: Ajanjakso, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
              values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                    ${formatTimestampParam(hakuaika.paattyy)}::timestamp, '[)'), $muokkaaja)
              on conflict on constraint hakukohteiden_hakuajat_pkey do nothing"""
  }

  def deleteHakuajat(oid: Option[HakukohdeOid], exclude: Seq[Ajanjakso]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_hakuajat
           where hakukohde_oid = $oid
           and hakuaika not in (#${createRangeInParams(exclude)})"""
  }

  def deleteHakuajat(oid: Option[HakukohdeOid]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid"""
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid], exclude: Seq[UUID]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_valintakokeet
           where hakukohde_oid = $oid
           and id not in (#${createUUIDInParams(exclude)})"""
  }

  def updateValintakoe(oid: Option[HakukohdeOid], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""update hakukohteiden_valintakokeet set
              tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri},
              nimi = ${toJsonParam(valintakoe.nimi)}::jsonb,
              metadata = ${toJsonParam(valintakoe.metadata)}::jsonb,
              tilaisuudet = ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
              muokkaaja = $muokkaaja
           where hakukohde_oid = $oid and id = ${valintakoe.id.map(_.toString)}::uuid and (
              nimi is distinct from ${toJsonParam(valintakoe.nimi)}::jsonb or
              metadata is distinct from ${toJsonParam(valintakoe.metadata)}::jsonb or
              tilaisuudet is distinct from ${toJsonParam(valintakoe.tilaisuudet)}::jsonb or
              tyyppi_koodi_uri is distinct from ${valintakoe.tyyppiKoodiUri})"""
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_valintakokeet where hakukohde_oid = $oid"""
  }

  def deleteLiitteet(oid: Option[HakukohdeOid]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid"""
  }

  def deleteLiitteet(oid: Option[HakukohdeOid], exclude: Seq[UUID]): DBIO[Int] = {
    sqlu"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid and id not in (#${createUUIDInParams(
      exclude
    )})"""
  }

  def updateLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid): DBIO[Int] = {
    sqlu"""update hakukohteiden_liitteet set
                tyyppi_koodi_uri = ${liite.tyyppiKoodiUri},
                nimi = ${toJsonParam(liite.nimi)}::jsonb,
                kuvaus = ${toJsonParam(liite.kuvaus)}::jsonb,
                toimitusaika = ${formatTimestampParam(liite.toimitusaika)}::timestamp,
                toimitustapa = ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa,
                toimitusosoite = ${toJsonParam(liite.toimitusosoite)}::jsonb,
                muokkaaja = $muokkaaja
              where id = ${liite.id.map(_.toString)}::uuid
                and hakukohde_oid = $oid
                and ( tyyppi_koodi_uri is distinct from ${liite.tyyppiKoodiUri}
                      or nimi is distinct from ${toJsonParam(liite.nimi)}::jsonb
                      or kuvaus is distinct from ${toJsonParam(liite.kuvaus)}::jsonb
                      or toimitusaika is distinct from ${formatTimestampParam(liite.toimitusaika)}::timestamp
                      or toimitustapa is distinct from ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa
                      or toimitusosoite is distinct from ${toJsonParam(liite.toimitusosoite)}::jsonb)"""
  }

  val selectHakukohdeListSql =
    """select distinct ha.oid, ha.toteutus_oid, ha.haku_oid, ha.valintaperuste_id, ha.nimi, ha.hakukohde_koodi_uri, ha.tila, ha.jarjestyspaikka_oid, ha.organisaatio_oid, ha.muokkaaja, ha.last_modified, t.metadata
         from hakukohteet ha
         inner join toteutukset t on t.oid = ha.toteutus_oid"""

  def selectByHakuOidAndAllowedOrganisaatiot(
      hakuOid: HakuOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          left join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where (ha.organisaatio_oid in (#${createOidInParams(
      organisaatioOids
    )}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and ha.haku_oid = $hakuOid and ha.tila != 'poistettu'::julkaisutila""".as[HakukohdeListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid, tilaFilter: TilaFilter): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.haku_oid = $hakuOid #${tilaConditions(tilaFilter, "ha.tila")}""".as[HakukohdeListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid, tilaFilter: TilaFilter): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.toteutus_oid = $toteutusOid #${tilaConditions(tilaFilter, "ha.tila")}""".as[HakukohdeListItem]
  }

  def selectByToteutusOidAndAllowedOrganisaatiot(
      toteutusOid: ToteutusOid,
      organisaatioOids: Seq[OrganisaatioOid]
  ): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          left join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where (ha.organisaatio_oid in (#${createOidInParams(
      organisaatioOids
    )}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and ha.toteutus_oid = $toteutusOid and ha.tila != 'poistettu'::julkaisutila""".as[HakukohdeListItem]
  }

  def selectByValintaperusteId(valintaperusteId: UUID, tilaFilter: TilaFilter): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.valintaperuste_id = ${valintaperusteId.toString}::uuid
          #${tilaConditions(tilaFilter, "ha.tila")}""".as[HakukohdeListItem]
  }

  def selectByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          inner join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where ha.tila != 'poistettu'::julkaisutila and
            (ha.organisaatio_oid in (#${createOidInParams(
      organisaatioOids
    )}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))""".as[HakukohdeListItem]
  }

  def selectOidsByJarjestyspaikkaOids(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter) = {
    sql"""select distinct oid from hakukohteet
          where (jarjestyspaikka_oid in (#${createOidInParams(jarjestyspaikkaOids)})
          or jarjestyspaikka_oid in
          (select oppilaitos_oid from oppilaitosten_osat where oid in (#${createOidInParams(jarjestyspaikkaOids)})))
          #${tilaConditions(tilaFilter)}""".as[String]
  }

  def selectToteutusDependencyInformation(hakukohde: Hakukohde): DBIO[Option[HakukohdeToteutusDependencyInfo]] =
    sql"""select t.oid, t.tila, t.nimi, k.tyyppi, t.metadata, k.koulutukset_koodi_uri, tarjoajat.tarjoajaOids
          from toteutukset t
          inner join koulutukset k on t.koulutus_oid = k.oid and k.tila != 'poistettu'::julkaisutila
          left join
             (select toteutus_oid toteutusOid, array_agg(tarjoaja_oid) tarjoajaOids
                from toteutusten_tarjoajat
                group by toteutus_oid) tarjoajat on tarjoajat.toteutusOid = t.oid
          where t.oid = ${hakukohde.toteutusOid} and t.tila != 'poistettu'::julkaisutila
    """.as[HakukohdeToteutusDependencyInfo].headOption

  def selectValintaperusteDependencyInformation(
      hakukohde: Hakukohde
  ): DBIO[Option[HakukohdeValintaperusteDependencyInfo]] =
    sql"""select vp.id::text, vp.tila, vp.koulutustyyppi, vk.ids
          from valintaperusteet vp
            left join
                (select valintaperuste_id vpid, array_agg(id) ids
                    from valintaperusteiden_valintakokeet
                    group by valintaperuste_id) vk on vk.vpid = vp.id
          where id = ${hakukohde.valintaperusteId.map(_.toString)}::uuid and tila != 'poistettu'::julkaisutila
    """.as[HakukohdeValintaperusteDependencyInfo].headOption

  def selectJarjestyspaikkaDependencyInformation(
      jarjestyspaikkaOid: OrganisaatioOid
  ): DBIO[Option[HakukohdeJarjestyspaikkaDependencyInfo]] =
    sql"""select oid, metadata ->> 'jarjestaaUrheilijanAmmKoulutusta' as jarjestaaUrheilijanAmmKoulutusta from oppilaitokset where oid = ${jarjestyspaikkaOid.toString}
          union
          select oid, metadata ->> 'jarjestaaUrheilijanAmmKoulutusta' as jarjestaaUrheilijanAmmKoulutusta from oppilaitosten_osat where oid = ${jarjestyspaikkaOid.toString}
       """.as[HakukohdeJarjestyspaikkaDependencyInfo].headOption

  def selectHakukohdeAndRelatedEntities(hakukohdeOids: List[HakukohdeOid]) =
    sql"""select
       hk.oid,
       hk.external_id,
       hk.toteutus_oid,
       hk.haku_oid,
       hk.tila,
       hk.nimi,
       hk.hakukohde_koodi_uri,
       hk.hakulomaketyyppi,
       hk.hakulomake_ataru_id,
       hk.hakulomake_kuvaus,
       hk.hakulomake_linkki,
       hk.kaytetaan_haun_hakulomaketta,
       hk.jarjestyspaikka_oid,
       hk.pohjakoulutusvaatimus_koodi_urit,
       hk.pohjakoulutusvaatimus_tarkenne,
       hk.muu_pohjakoulutusvaatimus_kuvaus,
       hk.toinen_aste_onko_kaksoistutkinto,
       hk.kaytetaan_haun_aikataulua,
       hk.valintaperuste_id,
       hk.liitteet_onko_sama_toimitusaika,
       hk.liitteet_onko_sama_toimitusosoite,
       hk.liitteiden_toimitusaika,
       hk.liitteiden_toimitustapa,
       hk.liitteiden_toimitusosoite,
       hk.esikatselu,
       hk.metadata,
       hk.muokkaaja,
       hk.organisaatio_oid,
       hk.kielivalinta,
       toteutus.oid,
       toteutus.external_id,
       toteutus.koulutus_oid,
       toteutus.tila,
       toteutus.tarjoajat,
       toteutus.nimi,
       toteutus.metadata,
       toteutus.muokkaaja,
       toteutus.esikatselu,
       toteutus.organisaatio_oid,
       toteutus.kielivalinta,
       toteutus.teemakuva,
       toteutus.sorakuvaus_id,
       liitteet.id,
       liitteet.tyyppi_koodi_uri,
       liitteet.nimi,
       liitteet.kuvaus,
       liitteet.toimitusaika,
       liitteet.toimitustapa,
       liitteet.toimitusosoite,
       valintakokeet.id,
       valintakokeet.tyyppi_koodi_uri,
       valintakokeet.nimi,
       valintakokeet.metadata,
       valintakokeet.tilaisuudet,
       hakuajat.hakukohde_oid,
       lower(hakuajat.hakuaika),
       upper(hakuajat.hakuaika)
    from hakukohteet hk
    inner join
        (select oid,
                external_id,
                koulutus_oid,
                tila,
                nimi,
                metadata,
                muokkaaja,
                esikatselu,
                organisaatio_oid,
                kielivalinta,
                teemakuva,
                sorakuvaus_id,
                array_agg(tt.tarjoaja_oid) as tarjoajat
        from toteutukset t
            left join
                (select toteutus_oid, tarjoaja_oid from toteutusten_tarjoajat) tt on tt.toteutus_oid = t.oid
     	          group by oid,
     	                   external_id,
     	                   koulutus_oid,
     	                   tila,
     	                   nimi,
     	                   metadata,
     	                   muokkaaja,
     	                   esikatselu,
     	                   organisaatio_oid,
     	                   kielivalinta,
     	                   teemakuva,
     	                   sorakuvaus_id) as toteutus on toteutus.oid = hk.toteutus_oid
    left join hakukohteiden_liitteet as liitteet on liitteet.hakukohde_oid = hk.oid
    left join hakukohteiden_valintakokeet as valintakokeet on valintakokeet.hakukohde_oid = hk.oid
    left join hakukohteiden_hakuajat as hakuajat on hakuajat.hakukohde_oid = hk.oid
    where hk.oid in (#${createOidInParams(hakukohdeOids)})"""
      .as[(Hakukohde, Toteutus, Liite, Valintakoe, HakukohdeHakuaika)]

  def selectArchivableHakukohdeOidsByHakuOids(hakuOids: Seq[HakuOid]): DBIO[Seq[HakukohdeOid]] = {
    sql"""select oid from hakukohteet where haku_oid in (#${createOidInParams(hakuOids)}) and tila = 'julkaistu'"""
      .as[HakukohdeOid]
  }

  def updateHakukohdesToArchivedByHakukohdeOids(hakukohdeOids: Seq[HakukohdeOid]): DBIO[Int] = {
    sqlu"""update hakukohteet set tila = 'arkistoitu' where oid in (#${createOidInParams(
      hakukohdeOids
    )}) and tila = 'julkaistu'"""
  }

  def removeJarjestaaUrheilijanAmmatillistaKoulutustaByJarjestyspaikkaOid(
      jarjestyspaikkaOid: OrganisaatioOid
  ): DBIO[Int] = {
    sqlu"""
    update hakukohteet
    set metadata = jsonb_set(metadata, '{jarjestaaUrheilijanAmmKoulutusta}', 'false'::jsonb, false)
    where oid in (
        select hk.oid
        from hakukohteet hk
        join toteutukset t on t.oid = hk.toteutus_oid
        join koulutukset k on k.oid = t.koulutus_oid
        where hk.jarjestyspaikka_oid = ${jarjestyspaikkaOid.toString}
          and hk.metadata ->> 'jarjestaaUrheilijanAmmKoulutusta' = 'true'
          and hk.tila in ('tallennettu', 'julkaistu')
          and k.tyyppi = 'amm'
    );"""
  }
}
