package fi.oph.kouta.repository

import java.time.Instant
import java.util.{ConcurrentModificationException, UUID}

import fi.oph.kouta.domain
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.sql.SqlAction

import scala.concurrent.ExecutionContext.Implicits.global

trait HakukohdeDAO extends EntityModificationDAO[HakukohdeOid] {
  def put(hakukohde: Hakukohde): Option[HakukohdeOid]
  def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)]
  def update(haku: Hakukohde, notModifiedSince: Instant): Boolean

  def listByToteutusOid(oid: ToteutusOid): Seq[HakukohdeListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[HakukohdeListItem]
  def listByHakuOidAndOrganisaatioOids(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem]
  def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem]
}

object HakukohdeDAO extends HakukohdeDAO with HakukohdeSQL {

  override def put(hakukohde: Hakukohde): Option[HakukohdeOid] = {
    KoutaDatabase.runBlockingTransactionally( for {
      oid <- insertHakukohde(hakukohde)
      _ <- insertHakuajat(hakukohde.copy(oid = oid))
      _ <- insertValintakokeet(hakukohde.copy(oid = oid))
      x <- insertLiitteet(hakukohde.copy(oid = oid))
    } yield (oid, x) ) match {
      case Left(t) => throw t
      case Right((oid, _)) => oid
    }
  }

  override def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHakukohde(oid)
      a <- selectHakuajat(oid)
      k <- selectValintakokeet(oid)
      i <- selectLiitteet(oid)
      l <- selectLastModified(oid)
    } yield (h, a, k, i, l) ) match {
      case Left(t) => throw t
      case Right((None, _, _, _, _)) | Right((_, _, _, _, None)) => None
      case Right((Some(h), a, k, i, Some(l))) => Some((h.copy(
        hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList,
        valintakokeet = k.toList,
        liitteet = i.toList), l))
    }
  }

  override def update(hakukohde: Hakukohde, notModifiedSince: Instant): Boolean = {
    KoutaDatabase.runBlockingTransactionally( selectLastModified(hakukohde.oid.get).flatMap(_ match {
      case None => DBIO.failed(new NoSuchElementException(s"Unknown hakukohde oid ${hakukohde.oid.get}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(new ConcurrentModificationException(s"Joku oli muokannut hakukohdetta ${hakukohde.oid.get} samanaikaisesti"))
      case Some(time) => DBIO.successful(time)
    }).andThen(updateHakukohde(hakukohde))
      .zip(updateHakuajat(hakukohde))
      .zip(updateValintakokeet(hakukohde))
      .zip(updateLiitteet(hakukohde))) match {
      case Left(t) => throw t
      case Right((((a, b), c), d)) => 0 < (a + b.sum + c.sum + d.sum)
    }
  }

  private def updateHakuajat(hakukohde: Hakukohde) = {
    val (oid, hakuajat, muokkaaja) = (hakukohde.oid, hakukohde.hakuajat, hakukohde.muokkaaja)
    if(hakuajat.nonEmpty) {
      DBIO.sequence( hakuajat.map(t => insertHakuaika(oid, t, muokkaaja)) :+ deleteHakuajat(oid, hakuajat))
    } else {
      DBIO.sequence(List(deleteHakuajat(oid)))
    }
  }

  private def updateValintakokeet(hakukohde: Hakukohde) = {
    val (oid, valintakokeet, muokkaaja) = (hakukohde.oid, hakukohde.valintakokeet, hakukohde.muokkaaja)
    val (insert, update) = valintakokeet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteValintakokeet(oid, update.map(_.id.get)) } else { deleteValintakokeet(oid) }
    val insertSQL = insert.map(v => insertValintakoe(oid, v.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateValintakoe(oid, v, muokkaaja))

    DBIO.sequence(List(deleteSQL) ++ insertSQL ++ updateSQL)
  }

  private def updateLiitteet(hakukohde: Hakukohde) = {
    val (oid, liitteet, muokkaaja) = (hakukohde.oid, hakukohde.liitteet, hakukohde.muokkaaja)
    val (insert, update) = liitteet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteLiitteet(oid, update.map(_.id.get)) } else { deleteLiitteet(oid) }
    val insertSQL = insert.map(l => insertLiite(oid, l.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateLiite(oid, v, muokkaaja))

    DBIO.sequence(List(deleteSQL) ++ insertSQL ++ updateSQL)
  }

  override def listByHakuOidAndOrganisaatioOids(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByHakuOidAndOrganisaatioOids(hakuOid, organisaatioOids))

  override def listByHakuOid(hakuOid: HakuOid): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByHakuOid(hakuOid))

  override def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid))

  override def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByValintaperusteId(valintaperusteId))
}

sealed trait HakukohdeModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakukohdeOid]] = {
    sql"""select oid from hakukohteet where ${since} < lower(system_time)
          union
          select oid from hakukohteet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_hakuajat where ${since} < lower(system_time)
          union
          select hakukohde_oid from hakukohteiden_hakuajat_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_valintakokeet where ${since} < lower(system_time)
          union
          select hakukohde_oid from hakukohteiden_valintakokeet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_liitteet where ${since} < lower(system_time)
          union
          select hakukohde_oid from hakukohteiden_liitteet_history where $since <@ system_time""".as[HakukohdeOid]
  }

  def selectLastModified(oid: HakukohdeOid): DBIO[Option[Instant]] = {
    sql"""select greatest(
            max(lower(ha.system_time)),
            max(lower(hh.system_time)),
            max(lower(hv.system_time)),
            max(lower(hl.system_time)),
            max(upper(hah.system_time)),
            max(upper(hhh.system_time)),
            max(upper(hvh.system_time)),
            max(upper(hlh.system_time)))
          from hakukohteet ha
          left join hakukohteet_history hah on ha.oid = hah.oid
          left join hakukohteiden_hakuajat hh on ha.oid = hh.hakukohde_oid
          left join hakukohteiden_hakuajat_history hhh on ha.oid = hhh.hakukohde_oid
          left join hakukohteiden_valintakokeet hv on ha.oid = hv.hakukohde_oid
          left join hakukohteiden_valintakokeet_history hvh on ha.oid = hvh.hakukohde_oid
          left join hakukohteiden_liitteet hl on ha.oid = hl.hakukohde_oid
          left join hakukohteiden_liitteet_history hlh on ha.oid = hlh.hakukohde_oid
          where ha.oid = $oid""".as[Option[Instant]].head
  }
}

sealed trait HakukohdeSQL extends SQLHelpers with HakukohdeModificationSQL with HakukohdeExctractors {

  def insertHakukohde(hakukohde: Hakukohde) = {
    sql"""insert into hakukohteet (
            toteutus_oid,
            haku_oid,
            tila,
            nimi,
            alkamiskausi_koodi_uri,
            alkamisvuosi,
            hakulomaketyyppi,
            hakulomake,
            aloituspaikat,
            ensikertalaisen_aloituspaikat,
            pohjakoulutusvaatimus_koodi_urit,
            muu_pohjakoulutusvaatimus_kuvaus,
            toinen_aste_onko_kaksoistutkinto,
            kaytetaan_haun_aikataulua,
            valintaperuste_id,
            liitteet_onko_sama_toimitusaika,
            liitteet_onko_sama_toimitusosoite,
            liitteiden_toimitusaika,
            liitteiden_toimitustapa,
            liitteiden_toimitusosoite,
            muokkaaja,
            organisaatio_oid,
            kielivalinta
          ) values (
            ${hakukohde.toteutusOid},
            ${hakukohde.hakuOid},
            ${hakukohde.tila.toString}::julkaisutila,
            ${toJsonParam(hakukohde.nimi)}::jsonb,
            ${hakukohde.alkamiskausiKoodiUri},
            ${hakukohde.alkamisvuosi},
            ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
            ${toJsonParam(hakukohde.hakulomake)}::jsonb,
            ${hakukohde.aloituspaikat},
            ${hakukohde.ensikertalaisenAloituspaikat},
            ${toJsonParam(hakukohde.pohjakoulutusvaatimusKoodiUrit)}::jsonb,
            ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
            ${hakukohde.toinenAsteOnkoKaksoistutkinto},
            ${hakukohde.kaytetaanHaunAikataulua},
            ${hakukohde.valintaperusteId.map(_.toString)}::uuid,
            ${hakukohde.liitteetOnkoSamaToimitusaika},
            ${hakukohde.liitteetOnkoSamaToimitusosoite},
            ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp,
            ${hakukohde.liitteidenToimitustapa.map(_.toString)}::liitteen_toimitustapa,
            ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb,
            ${hakukohde.muokkaaja},
            ${hakukohde.organisaatioOid},
            ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          ) returning oid""".as[HakukohdeOid].headOption
  }

  def updateHakukohde(hakukohde: Hakukohde) = {
    sqlu"""update hakukohteet set
              toteutus_oid = ${hakukohde.toteutusOid},
              haku_oid = ${hakukohde.hakuOid},
              tila = ${hakukohde.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(hakukohde.nimi)}::jsonb,
              alkamiskausi_koodi_uri = ${hakukohde.alkamiskausiKoodiUri},
              alkamisvuosi = ${hakukohde.alkamisvuosi},
              hakulomaketyyppi = ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake = ${toJsonParam(hakukohde.hakulomake)}::jsonb,
              aloituspaikat = ${hakukohde.aloituspaikat},
              ensikertalaisen_aloituspaikat = ${hakukohde.ensikertalaisenAloituspaikat},
              pohjakoulutusvaatimus_koodi_urit = ${toJsonParam(hakukohde.pohjakoulutusvaatimusKoodiUrit)}::jsonb,
              muu_pohjakoulutusvaatimus_kuvaus = ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb,
              toinen_aste_onko_kaksoistutkinto = ${hakukohde.toinenAsteOnkoKaksoistutkinto},
              kaytetaan_haun_aikataulua = ${hakukohde.kaytetaanHaunAikataulua},
              valintaperuste_id = ${hakukohde.valintaperusteId.map(_.toString)}::uuid,
              liitteet_onko_sama_toimitusaika = ${hakukohde.liitteetOnkoSamaToimitusaika},
              liitteet_onko_sama_toimitusosoite = ${hakukohde.liitteetOnkoSamaToimitusosoite},
              liitteiden_toimitusaika = ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp,
              liitteiden_toimitustapa = ${hakukohde.liitteidenToimitustapa.map(_.toString)}::liitteen_toimitustapa,
              liitteiden_toimitusosoite = ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb,
              muokkaaja = ${hakukohde.muokkaaja},
              organisaatio_oid = ${hakukohde.organisaatioOid},
              kielivalinta = ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          where oid = ${hakukohde.oid}
            and ( toteutus_oid is distinct from ${hakukohde.toteutusOid}
            or haku_oid is distinct from ${hakukohde.hakuOid}
            or tila is distinct from ${hakukohde.tila.toString}::julkaisutila
            or nimi is distinct from ${toJsonParam(hakukohde.nimi)}::jsonb
            or alkamiskausi_koodi_uri is distinct from ${hakukohde.alkamiskausiKoodiUri}
            or alkamisvuosi is distinct from ${hakukohde.alkamisvuosi}
            or hakulomaketyyppi is distinct from ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake is distinct from ${toJsonParam(hakukohde.hakulomake)}::jsonb
            or aloituspaikat is distinct from ${hakukohde.aloituspaikat}
            or ensikertalaisen_aloituspaikat is distinct from ${hakukohde.ensikertalaisenAloituspaikat}
            or pohjakoulutusvaatimus_koodi_urit is distinct from ${toJsonParam(hakukohde.pohjakoulutusvaatimusKoodiUrit)}::jsonb
            or muu_pohjakoulutusvaatimus_kuvaus is distinct from ${toJsonParam(hakukohde.muuPohjakoulutusvaatimus)}::jsonb
            or toinen_aste_onko_kaksoistutkinto is distinct from ${hakukohde.toinenAsteOnkoKaksoistutkinto}
            or kaytetaan_haun_aikataulua is distinct from ${hakukohde.kaytetaanHaunAikataulua}
            or valintaperuste_id is distinct from ${hakukohde.valintaperusteId.map(_.toString)}::uuid
            or liitteet_onko_sama_toimitusaika is distinct from ${hakukohde.liitteetOnkoSamaToimitusaika}
            or liitteet_onko_sama_toimitusosoite is distinct from ${hakukohde.liitteetOnkoSamaToimitusosoite}
            or liitteiden_toimitusaika is distinct from ${formatTimestampParam(hakukohde.liitteidenToimitusaika)}::timestamp
            or liitteiden_toimitustapa is distinct from ${hakukohde.liitteidenToimitustapa.map(_.toString)}::liitteen_toimitustapa
            or liitteiden_toimitusosoite is distinct from ${toJsonParam(hakukohde.liitteidenToimitusosoite)}::jsonb
            or kielivalinta is distinct from ${toJsonParam(hakukohde.kielivalinta)}::jsonb
            or organisaatio_oid is distinct from ${hakukohde.organisaatioOid})"""
  }

  def selectHakukohde(oid: HakukohdeOid) = {
    sql"""select oid,
             toteutus_oid,
             haku_oid,
             tila,
             nimi,
             alkamiskausi_koodi_uri,
             alkamisvuosi,
             hakulomaketyyppi,
             hakulomake,
             aloituspaikat,
             ensikertalaisen_aloituspaikat,
             pohjakoulutusvaatimus_koodi_urit,
             muu_pohjakoulutusvaatimus_kuvaus,
             toinen_aste_onko_kaksoistutkinto,
             kaytetaan_haun_aikataulua,
             valintaperuste_id,
             liitteet_onko_sama_toimitusaika,
             liitteet_onko_sama_toimitusosoite,
             liitteiden_toimitusaika,
             liitteiden_toimitustapa,
             liitteiden_toimitusosoite,
             muokkaaja,
             organisaatio_oid,
             kielivalinta,
             lower(system_time) from hakukohteet where oid = $oid""".as[Hakukohde].headOption
  }

  def insertHakuajat(hakukohde: Hakukohde) = {
    DBIO.sequence(
      hakukohde.hakuajat.map(t =>
        sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
               values (${hakukohde.oid}, tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                                                 ${formatTimestampParam(Some(t.paattyy))}::timestamp, '[)'), ${hakukohde.muokkaaja})"""))
  }

  def insertValintakokeet(hakukohde: Hakukohde) = {
    DBIO.sequence(
      hakukohde.valintakokeet.map(k => insertValintakoe(hakukohde.oid, k.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja)))
  }

  def insertValintakoe(oid: Option[HakukohdeOid], valintakoe:Valintakoe, muokkaaja:UserOid) = {
    sqlu"""insert into hakukohteiden_valintakokeet (id, hakukohde_oid, tyyppi, tilaisuudet, muokkaaja)
               values (${valintakoe.id.map(_.toString)}::uuid, ${oid}, ${valintakoe.tyyppi}, ${toJsonParam(valintakoe.tilaisuudet)}::jsonb, ${muokkaaja})"""
  }

  def insertLiitteet(hakukohde: Hakukohde) = {
    DBIO.sequence(
      hakukohde.liitteet.map(l => insertLiite(hakukohde.oid, l.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja)))
  }

  def insertLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid) = {
      sqlu"""insert into hakukohteiden_liitteet (
                id,
                hakukohde_oid,
                tyyppi,
                nimi,
                kuvaus,
                toimitusaika,
                toimitustapa,
                toimitusosoite,
                muokkaaja
             ) values (
                ${liite.id.map(_.toString)}::uuid,
                ${oid},
                ${liite.tyyppi},
                ${toJsonParam(liite.nimi)}::jsonb,
                ${toJsonParam(liite.kuvaus)}::jsonb,
                ${formatTimestampParam(liite.toimitusaika)}::timestamp,
                ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa,
                ${toJsonParam(liite.toimitusosoite)}::jsonb,
                ${muokkaaja})"""
  }

  def selectHakuajat(oid: HakukohdeOid) = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika) from hakukohteiden_hakuajat where hakukohde_oid = $oid""".as[Hakuaika]
  }

  def selectValintakokeet(oid: HakukohdeOid) = {
    sql"""select id, tyyppi, tilaisuudet from hakukohteiden_valintakokeet where hakukohde_oid = $oid""".as[Valintakoe]
  }

  def selectLiitteet(oid: HakukohdeOid) = {
    sql"""select id, tyyppi, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
          from hakukohteiden_liitteet where hakukohde_oid = $oid""".as[Liite]
  }

  def insertHakuaika(oid: Option[HakukohdeOid], hakuaika: Ajanjakso, muokkaaja: UserOid) = {
    sqlu"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
               values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                     ${formatTimestampParam(Some(hakuaika.paattyy))}::timestamp, '[)'), $muokkaaja)
               on conflict on constraint hakukohteiden_hakuajat_pkey do nothing"""
  }

  def deleteHakuajat(oid: Option[HakukohdeOid], exclude: List[Ajanjakso]): SqlAction[Int, NoStream, Effect] = {
    sqlu"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid and hakuaika not in (#${createRangeInParams(exclude)})"""
  }

  def deleteHakuajat(oid: Option[HakukohdeOid]) = {
    sqlu"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid"""
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid], exclude: List[UUID]) = {
    sqlu"""delete from hakukohteiden_valintakokeet where hakukohde_oid = $oid and id not in (#${createUUIDInParams(exclude)})"""
  }

  def updateValintakoe(oid: Option[HakukohdeOid], valintakoe: Valintakoe, muokkaaja: UserOid) = {
    sqlu"""update hakukohteiden_valintakokeet set
              tyyppi = ${valintakoe.tyyppi},
              tilaisuudet = ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
              muokkaaja = ${muokkaaja}
           where hakukohde_oid = $oid and id = ${valintakoe.id.map(_.toString)}::uuid and (
              tilaisuudet is distinct from ${toJsonParam(valintakoe.tilaisuudet)}::jsonb or
              tyyppi is distinct from ${valintakoe.tyyppi})"""
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid]) = {
    sqlu"""delete from hakukohteiden_valintakokeet where hakukohde_oid = $oid"""
  }

  def deleteLiitteet(oid: Option[HakukohdeOid]) = {
    sqlu"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid"""
  }

  def deleteLiitteet(oid: Option[HakukohdeOid], exclude: List[UUID]) = {
    sqlu"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid and id not in (#${createUUIDInParams(exclude)})"""
  }

  def updateLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid) = {
    sqlu"""update hakukohteiden_liitteet set
                tyyppi = ${liite.tyyppi},
                nimi = ${toJsonParam(liite.nimi)}::jsonb,
                kuvaus = ${toJsonParam(liite.kuvaus)}::jsonb,
                toimitusaika = ${formatTimestampParam(liite.toimitusaika)}::timestamp,
                toimitustapa = ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa,
                toimitusosoite = ${toJsonParam(liite.toimitusosoite)}::jsonb,
                muokkaaja = ${muokkaaja}
              where id = ${liite.id.map(_.toString)}::uuid
                and hakukohde_oid = ${oid}
                and ( tyyppi is distinct from ${liite.tyyppi}
                      or nimi is distinct from ${toJsonParam(liite.nimi)}::jsonb
                      or kuvaus is distinct from ${toJsonParam(liite.kuvaus)}::jsonb
                      or toimitusaika is distinct from ${formatTimestampParam(liite.toimitusaika)}::timestamp
                      or toimitustapa is distinct from ${liite.toimitustapa.map(_.toString)}::liitteen_toimitustapa
                      or toimitusosoite is distinct from ${toJsonParam(liite.toimitusosoite)}::jsonb)"""
  }

  def selectByHakuOidAndOrganisaatioOids(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]) = {
    sql"""select oid, toteutus_oid, haku_oid, valintaperuste_id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from hakukohteet
          where organisaatio_oid in (#${createOidInParams(organisaatioOids)})
          and haku_oid = $hakuOid""".as[HakukohdeListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid) = {
    sql"""select oid, toteutus_oid, haku_oid, valintaperuste_id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from hakukohteet
          where haku_oid = $hakuOid""".as[HakukohdeListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid) = {
    sql"""select oid, toteutus_oid, haku_oid, valintaperuste_id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from hakukohteet
          where toteutus_oid = $toteutusOid""".as[HakukohdeListItem]
  }

  def selectByValintaperusteId(valintaperusteId: UUID) = {
    sql"""select oid, toteutus_oid, haku_oid, valintaperuste_id, nimi, tila, organisaatio_oid, muokkaaja, lower(system_time)
          from hakukohteet
          where valintaperuste_id = ${valintaperusteId.toString}::uuid""".as[HakukohdeListItem]
  }
}
