package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.TimeUtils.instantToLocalDateTime
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakukohdeDAO extends EntityModificationDAO[HakukohdeOid] {
  def getPutActions(hakukohde: Hakukohde): DBIO[Hakukohde]
  def getUpdateActions(hakukohde: Hakukohde, notModifiedSince: Instant): DBIO[Option[Hakukohde]]

  def put(hakukohde: Hakukohde): Hakukohde
  def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)]
  def update(haku: Hakukohde, notModifiedSince: Instant): Option[Hakukohde]

  def listByToteutusOid(oid: ToteutusOid): Seq[HakukohdeListItem]
  def listByToteutusOidAndAllowedOrganisaatiot(toteutusOid: ToteutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem]
  def listByHakuOid(hakuOid: HakuOid): Seq[HakukohdeListItem]
  def listByHakuOidAndAllowedOrganisaatiot(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem]
  def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem]
  def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem]
}

object HakukohdeDAO extends HakukohdeDAO with HakukohdeSQL {

  override def getPutActions(hakukohde: Hakukohde): DBIO[Hakukohde] =
    for {
      (oid, hk) <- insertHakukohde(hakukohde)
      ha <- insertHakuajat(hakukohde.withOid(oid))
      vk <- insertValintakokeet(hakukohde.withOid(oid))
      l  <- insertLiitteet(hakukohde.withOid(oid))
    } yield hakukohde.withOid(oid).withModified((hk :: ha ++ vk ++ l).max)

  override def getUpdateActions(hakukohde: Hakukohde, notModifiedSince: Instant): DBIO[Option[Hakukohde]] =
    checkNotModified(hakukohde.oid.get, notModifiedSince).andThen(
      for {
        hk <- updateHakukohde(hakukohde)
        ha <- updateHakuajat(hakukohde)
        vk <- updateValintakokeet(hakukohde)
        l  <- updateLiitteet(hakukohde)
      } yield {
        val modified = (hk ++ ha ++ vk ++ l).sorted.lastOption
        modified.map(hakukohde.withModified)
      }
    )

  override def put(hakukohde: Hakukohde): Hakukohde =
    KoutaDatabase.runBlockingTransactionally(getPutActions(hakukohde)).get

  override def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)] = {
    KoutaDatabase.runBlockingTransactionally( for {
      h <- selectHakukohde(oid)
      a <- selectHakuajat(oid)
      k <- selectValintakokeet(oid)
      i <- selectLiitteet(oid)
      l <- selectLastModified(oid)
    } yield (h, a, k, i, l) ).get match {
      case (Some(h), a, k, i, Some(l)) => Some((h.copy(
        modified = Some(instantToLocalDateTime(l)),
        hakuajat = a.map(x => domain.Ajanjakso(x.alkaa, x.paattyy)).toList,
        valintakokeet = k.toList,
        liitteet = i.toList), l))
      case _ => None
    }
  }

  override def update(hakukohde: Hakukohde, notModifiedSince: Instant): Option[Hakukohde] =
    KoutaDatabase.runBlockingTransactionally(getUpdateActions(hakukohde, notModifiedSince)).get

  private def updateHakuajat(hakukohde: Hakukohde) = {
    val (oid, hakuajat, muokkaaja) = (hakukohde.oid, hakukohde.hakuajat, hakukohde.muokkaaja)
    if(hakuajat.nonEmpty) {
      val actions = hakuajat.map(t => insertHakuaika(oid, t, muokkaaja)) :+ deleteHakuajat(oid, hakuajat)
      combineInstants(actions)
    } else {
      deleteHakuajat(oid)
    }
  }

  private def updateValintakokeet(hakukohde: Hakukohde) = {
    val (oid, valintakokeet, muokkaaja) = (hakukohde.oid, hakukohde.valintakokeet, hakukohde.muokkaaja)
    val (insert, update) = valintakokeet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteValintakokeet(oid, update.map(_.id.get)) } else { deleteValintakokeet(oid) }
    val insertSQL = insert.map(v => insertValintakoe(oid, v.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateValintakoe(oid, v, muokkaaja))

    combineInstants(deleteSQL :: insertSQL ++ updateSQL)
  }

  private def updateLiitteet(hakukohde: Hakukohde) = {
    val (oid, liitteet, muokkaaja) = (hakukohde.oid, hakukohde.liitteet, hakukohde.muokkaaja)
    val (insert, update) = liitteet.partition(_.id.isEmpty)

    val deleteSQL = if (update.nonEmpty) { deleteLiitteet(oid, update.map(_.id.get)) } else { deleteLiitteet(oid) }
    val insertSQL = insert.map(l => insertLiite(oid, l.copy(id = Some(UUID.randomUUID())), muokkaaja))
    val updateSQL = update.map(v => updateLiite(oid, v, muokkaaja))

    combineInstants(deleteSQL :: insertSQL ++ updateSQL)
  }

  override def listByHakuOidAndAllowedOrganisaatiot(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByHakuOidAndAllowedOrganisaatiot(hakuOid, organisaatioOids))
  }

  override def listByHakuOid(hakuOid: HakuOid): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByHakuOid(hakuOid))

  override def listByToteutusOid(toteutusOid: ToteutusOid): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByToteutusOid(toteutusOid))

  override def listByToteutusOidAndAllowedOrganisaatiot(toteutusOid: ToteutusOid, organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   => KoutaDatabase.runBlocking(selectByToteutusOidAndAllowedOrganisaatiot(toteutusOid, organisaatioOids))
  }

  override def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem] =
    KoutaDatabase.runBlocking(selectByValintaperusteId(valintaperusteId))

  override def listByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Seq[HakukohdeListItem] = organisaatioOids match {
    case Nil => Seq()
    case _   =>  KoutaDatabase.runBlocking(selectByAllowedOrganisaatiot(organisaatioOids))
  }
}

sealed trait HakukohdeModificationSQL extends SQLHelpers {
  this: ExtractorBase =>

  def selectModifiedSince(since: Instant): DBIO[Seq[HakukohdeOid]] = {
    sql"""select oid from hakukohteet where $since < lower(system_time)
          union
          select oid from hakukohteet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_hakuajat where $since < lower(system_time)
          union
          select hakukohde_oid from hakukohteiden_hakuajat_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_valintakokeet where $since < lower(system_time)
          union
          select hakukohde_oid from hakukohteiden_valintakokeet_history where $since <@ system_time
          union
          select hakukohde_oid from hakukohteiden_liitteet where $since < lower(system_time)
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

  def insertHakukohde(hakukohde: Hakukohde): DBIO[(HakukohdeOid, Instant)] = {
    sql"""insert into hakukohteet (
            toteutus_oid,
            haku_oid,
            tila,
            nimi,
            alkamiskausi_koodi_uri,
            alkamisvuosi,
            kaytetaan_haun_alkamiskautta,
            hakulomaketyyppi,
            hakulomake_ataru_id,
            hakulomake_kuvaus,
            hakulomake_linkki,
            kaytetaan_haun_hakulomaketta,
            aloituspaikat,
            min_aloituspaikat,
            max_aloituspaikat,
            ensikertalaisen_aloituspaikat,
            min_ensikertalaisen_aloituspaikat,
            max_ensikertalaisen_aloituspaikat,
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
            ${hakukohde.kaytetaanHaunAlkamiskautta},
            ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
            ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid,
            ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb,
            ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb,
            ${hakukohde.kaytetaanHaunHakulomaketta},
            ${hakukohde.aloituspaikat},
            ${hakukohde.minAloituspaikat},
            ${hakukohde.maxAloituspaikat},
            ${hakukohde.ensikertalaisenAloituspaikat},
            ${hakukohde.minEnsikertalaisenAloituspaikat},
            ${hakukohde.maxEnsikertalaisenAloituspaikat},
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
            ${hakukohde.muokkaaja},
            ${hakukohde.organisaatioOid},
            ${toJsonParam(hakukohde.kielivalinta)}::jsonb
          ) returning oid, lower(system_time)""".as[(HakukohdeOid, Instant)].head
  }

  def updateHakukohde(hakukohde: Hakukohde): DBIO[Vector[Instant]] = {
    sql"""update hakukohteet set
              toteutus_oid = ${hakukohde.toteutusOid},
              haku_oid = ${hakukohde.hakuOid},
              tila = ${hakukohde.tila.toString}::julkaisutila,
              nimi = ${toJsonParam(hakukohde.nimi)}::jsonb,
              alkamiskausi_koodi_uri = ${hakukohde.alkamiskausiKoodiUri},
              alkamisvuosi = ${hakukohde.alkamisvuosi},
              kaytetaan_haun_alkamiskautta = ${hakukohde.kaytetaanHaunAlkamiskautta},
              hakulomaketyyppi = ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi,
              hakulomake_ataru_id = ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid,
              hakulomake_kuvaus = ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb,
              hakulomake_linkki = ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb,
              kaytetaan_haun_hakulomaketta = ${hakukohde.kaytetaanHaunHakulomaketta},
              aloituspaikat = ${hakukohde.aloituspaikat},
              min_aloituspaikat = ${hakukohde.minAloituspaikat},
              max_aloituspaikat = ${hakukohde.maxAloituspaikat},
              ensikertalaisen_aloituspaikat = ${hakukohde.ensikertalaisenAloituspaikat},
              min_ensikertalaisen_aloituspaikat = ${hakukohde.minEnsikertalaisenAloituspaikat},
              max_ensikertalaisen_aloituspaikat = ${hakukohde.maxEnsikertalaisenAloituspaikat},
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
            or kaytetaan_haun_alkamiskautta is distinct from ${hakukohde.kaytetaanHaunAlkamiskautta}
            or hakulomaketyyppi is distinct from ${hakukohde.hakulomaketyyppi.map(_.toString)}::hakulomaketyyppi
            or hakulomake_ataru_id is distinct from ${hakukohde.hakulomakeAtaruId.map(_.toString)}::uuid
            or hakulomake_kuvaus is distinct from ${toJsonParam(hakukohde.hakulomakeKuvaus)}::jsonb
            or hakulomake_linkki is distinct from ${toJsonParam(hakukohde.hakulomakeLinkki)}::jsonb
            or kaytetaan_haun_hakulomaketta is distinct from ${hakukohde.kaytetaanHaunHakulomaketta}
            or aloituspaikat is distinct from ${hakukohde.aloituspaikat}
            or min_aloituspaikat is distinct from ${hakukohde.minAloituspaikat}
            or max_aloituspaikat is distinct from ${hakukohde.maxAloituspaikat}
            or ensikertalaisen_aloituspaikat is distinct from ${hakukohde.ensikertalaisenAloituspaikat}
            or min_ensikertalaisen_aloituspaikat is distinct from ${hakukohde.minEnsikertalaisenAloituspaikat}
            or max_ensikertalaisen_aloituspaikat is distinct from ${hakukohde.maxEnsikertalaisenAloituspaikat}
            or pohjakoulutusvaatimus_koodi_urit is distinct from ${hakukohde.pohjakoulutusvaatimusKoodiUrit}
            or pohjakoulutusvaatimus_tarkenne is distinct from ${toJsonParam(hakukohde.pohjakoulutusvaatimusTarkenne)}::jsonb
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
            or organisaatio_oid is distinct from ${hakukohde.organisaatioOid})
          returning lower(system_time)""".as[Instant]
  }

  def selectHakukohde(oid: HakukohdeOid): DBIO[Option[Hakukohde]] = {
    sql"""select oid,
             toteutus_oid,
             haku_oid,
             tila,
             nimi,
             alkamiskausi_koodi_uri,
             alkamisvuosi,
             kaytetaan_haun_alkamiskautta,
             hakulomaketyyppi,
             hakulomake_ataru_id,
             hakulomake_kuvaus,
             hakulomake_linkki,
             kaytetaan_haun_hakulomaketta,
             aloituspaikat,
             min_aloituspaikat,
             max_aloituspaikat,
             ensikertalaisen_aloituspaikat,
             min_ensikertalaisen_aloituspaikat,
             max_ensikertalaisen_aloituspaikat,
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
             muokkaaja,
             organisaatio_oid,
             kielivalinta,
             lower(system_time) from hakukohteet where oid = $oid""".as[Hakukohde].headOption
  }

  def insertHakuajat(hakukohde: Hakukohde): DBIO[List[Instant]] = {
    DBIO.sequence(
      hakukohde.hakuajat.map(t =>
        sql"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
              values (${hakukohde.oid}, tsrange(${formatTimestampParam(Some(t.alkaa))}::timestamp,
                                                ${formatTimestampParam(Some(t.paattyy))}::timestamp, '[)'), ${hakukohde.muokkaaja})
              returning lower(system_time)""".as[Instant].head))
  }

  def insertValintakokeet(hakukohde: Hakukohde): DBIO[Vector[Instant]] = {
    val inserts = hakukohde.valintakokeet.map(k => insertValintakoe(hakukohde.oid, k.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja))
    combineInstants(inserts)
  }

  def insertValintakoe(oid: Option[HakukohdeOid], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Vector[Instant]] = {
    sql"""insert into hakukohteiden_valintakokeet (id, hakukohde_oid, tyyppi_koodi_uri, tilaisuudet, muokkaaja)
               values (${valintakoe.id.map(_.toString)}::uuid, $oid, ${valintakoe.tyyppiKoodiUri}, ${toJsonParam(valintakoe.tilaisuudet)}::jsonb, $muokkaaja)
           returning lower(system_time)""".as[Instant]
  }

  def insertLiitteet(hakukohde: Hakukohde): DBIO[Vector[Instant]] = {
    val inserts = hakukohde.liitteet.map(l => insertLiite(hakukohde.oid, l.copy(id = Some(UUID.randomUUID())), hakukohde.muokkaaja))
    combineInstants(inserts)
  }

  def insertLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid): DBIO[Vector[Instant]] = {
    sql"""insert into hakukohteiden_liitteet (
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
            $muokkaaja)
          returning lower(system_time)""".as[Instant]
  }

  def selectHakuajat(oid: HakukohdeOid): DBIO[Vector[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika) from hakukohteiden_hakuajat where hakukohde_oid = $oid""".as[Hakuaika]
  }

  def selectValintakokeet(oid: HakukohdeOid): DBIO[Vector[Valintakoe]] = {
    sql"""select id, tyyppi_koodi_uri, tilaisuudet from hakukohteiden_valintakokeet where hakukohde_oid = $oid""".as[Valintakoe]
  }

  def selectLiitteet(oid: HakukohdeOid): DBIO[Vector[Liite]] = {
    sql"""select id, tyyppi_koodi_uri, nimi, kuvaus, toimitusaika, toimitustapa, toimitusosoite
          from hakukohteiden_liitteet where hakukohde_oid = $oid""".as[Liite]
  }

  def insertHakuaika(oid: Option[HakukohdeOid], hakuaika: Ajanjakso, muokkaaja: UserOid): DBIO[Vector[Instant]] = {
    sql"""insert into hakukohteiden_hakuajat (hakukohde_oid, hakuaika, muokkaaja)
              values ($oid, tsrange(${formatTimestampParam(Some(hakuaika.alkaa))}::timestamp,
                                    ${formatTimestampParam(Some(hakuaika.paattyy))}::timestamp, '[)'), $muokkaaja)
              on conflict on constraint hakukohteiden_hakuajat_pkey do nothing
              returning lower(system_time)""".as[Instant]
  }

  def deleteHakuajat(oid: Option[HakukohdeOid], exclude: List[Ajanjakso]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_hakuajat
          where hakukohde_oid = $oid
           and hakuaika not in (#${createRangeInParams(exclude)})
          returning now()""".as[Instant]
  }

  def deleteHakuajat(oid: Option[HakukohdeOid]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_hakuajat where hakukohde_oid = $oid returning now()""".as[Instant]
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid], exclude: List[UUID]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_valintakokeet
          where hakukohde_oid = $oid
            and id not in (#${createUUIDInParams(exclude)})
          returning now()""".as[Instant]
  }

  def updateValintakoe(oid: Option[HakukohdeOid], valintakoe: Valintakoe, muokkaaja: UserOid): DBIO[Vector[Instant]] = {
    sql"""update hakukohteiden_valintakokeet set
              tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri},
              tilaisuudet = ${toJsonParam(valintakoe.tilaisuudet)}::jsonb,
              muokkaaja = $muokkaaja
          where hakukohde_oid = $oid and id = ${valintakoe.id.map(_.toString)}::uuid and (
              tilaisuudet is distinct from ${toJsonParam(valintakoe.tilaisuudet)}::jsonb or
              tyyppi_koodi_uri is distinct from ${valintakoe.tyyppiKoodiUri})
          returning lower(system_time)""".as[Instant]
  }

  def deleteValintakokeet(oid: Option[HakukohdeOid]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_valintakokeet where hakukohde_oid = $oid returning now()""".as[Instant]
  }

  def deleteLiitteet(oid: Option[HakukohdeOid]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid returning now()""".as[Instant]
  }

  def deleteLiitteet(oid: Option[HakukohdeOid], exclude: List[UUID]): DBIO[Vector[Instant]] = {
    sql"""delete from hakukohteiden_liitteet where hakukohde_oid = $oid and id not in (#${createUUIDInParams(exclude)}) returning now()""".as[Instant]
  }

  def updateLiite(oid: Option[HakukohdeOid], liite: Liite, muokkaaja: UserOid): DBIO[Vector[Instant]] = {
    sql"""update hakukohteiden_liitteet set
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
                      or toimitusosoite is distinct from ${toJsonParam(liite.toimitusosoite)}::jsonb)
          returning lower(system_time)""".as[Instant]
  }

  val selectHakukohdeListSql =
    """select distinct ha.oid, ha.toteutus_oid, ha.haku_oid, ha.valintaperuste_id, ha.nimi, ha.tila, ha.organisaatio_oid, ha.muokkaaja, m.modified
         from hakukohteet ha
         inner join (
           select ha.oid oid, greatest(
             max(lower(ha.system_time)),
             max(lower(hh.system_time)),
             max(lower(hv.system_time)),
             max(lower(hl.system_time)),
             max(upper(hah.system_time)),
             max(upper(hhh.system_time)),
             max(upper(hvh.system_time)),
             max(upper(hlh.system_time))) modified
           from hakukohteet ha
           left join hakukohteet_history hah on ha.oid = hah.oid
           left join hakukohteiden_hakuajat hh on ha.oid = hh.hakukohde_oid
           left join hakukohteiden_hakuajat_history hhh on ha.oid = hhh.hakukohde_oid
           left join hakukohteiden_valintakokeet hv on ha.oid = hv.hakukohde_oid
           left join hakukohteiden_valintakokeet_history hvh on ha.oid = hvh.hakukohde_oid
           left join hakukohteiden_liitteet hl on ha.oid = hl.hakukohde_oid
           left join hakukohteiden_liitteet_history hlh on ha.oid = hlh.hakukohde_oid
           group by ha.oid) m on m.oid = ha.oid"""

  def selectByHakuOidAndAllowedOrganisaatiot(hakuOid: HakuOid, organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          inner join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where (ha.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and ha.haku_oid = $hakuOid""".as[HakukohdeListItem]
  }

  def selectByHakuOid(hakuOid: HakuOid): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.haku_oid = $hakuOid""".as[HakukohdeListItem]
  }

  def selectByToteutusOid(toteutusOid: ToteutusOid): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.toteutus_oid = $toteutusOid""".as[HakukohdeListItem]
  }

  def selectByToteutusOidAndAllowedOrganisaatiot(toteutusOid: ToteutusOid, organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          inner join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where (ha.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))
          and ha.toteutus_oid = $toteutusOid""".as[HakukohdeListItem]
  }

  def selectByValintaperusteId(valintaperusteId: UUID): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          where ha.valintaperuste_id = ${valintaperusteId.toString}::uuid""".as[HakukohdeListItem]
  }

  def selectByAllowedOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): DBIO[Vector[HakukohdeListItem]] = {
    sql"""#$selectHakukohdeListSql
          inner join toteutusten_tarjoajat tt on ha.toteutus_oid = tt.toteutus_oid
          where (ha.organisaatio_oid in (#${createOidInParams(organisaatioOids)}) or tt.tarjoaja_oid in (#${createOidInParams(organisaatioOids)}))""".as[HakukohdeListItem]
  }
}
