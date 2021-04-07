package fi.oph.kouta.repository

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Ajanjakso, Hakutieto, HakutietoHaku, HakutietoHakukohde}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait HakutietoDAO {
  def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Hakutieto]
}

object HakutietoDAO extends HakutietoDAO with HakutietoSQL {
  override def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Hakutieto] = {
    KoutaDatabase.runBlockingTransactionally(
      for {
        haut <- selectHakujenHakutiedot(koulutusOid)
        hakujenHakuajat <- selectHakujenHakuajat(haut)
        hakukohteet <- selectHakukohteidenHakutiedot(koulutusOid)
        hakukohteidenHakuajat <- selectHakukohteidenHakuajat(hakukohteet)
      } yield (haut, hakujenHakuajat, hakukohteet, hakukohteidenHakuajat)
    ).get match {
      case (h, hh, hk, hkh) => createHakutieto(h, hh, hk, hkh)
    }
  }

  private def createHakutieto(haut:Seq[(ToteutusOid, HakutietoHaku)],
                              hakujenHakuajat:Seq[Hakuaika],
                              hakukohteet:Seq[(ToteutusOid, HakuOid, HakutietoHakukohde)],
                              hakukohteidenHakuajat:Seq[Hakuaika]) = {

    def mapHakuajat[A <: Oid](hakuajat: Seq[Hakuaika], f: GenericOid => A): Map[A, Seq[Ajanjakso]] =
      hakuajat.groupBy(h => f(h.oid))
        .mapValues(_.map(a => Ajanjakso(a.alkaa, a.paattyy)))

    val hakujenHakuajatMap = mapHakuajat[HakuOid](hakujenHakuajat, (oid: GenericOid) => HakuOid(oid.toString))
    val hakukohteidenHakuajatMap = mapHakuajat[HakukohdeOid](hakukohteidenHakuajat, (oid: GenericOid) => HakukohdeOid(oid.toString))

    val hakukohdeMap = hakukohteet
      .groupBy { case (toteutusOid, hakuOid, _) => (toteutusOid, hakuOid) }
      .mapValues(_.map { case (_, _, hakukohde) =>
        hakukohde.copy(hakuajat = hakukohteidenHakuajatMap.getOrElse(hakukohde.hakukohdeOid, Seq()))
      })

    val hakuMap = haut
      .groupBy { case (toteutusOid, _) => toteutusOid }
      .mapValues(_.map { case (toteutusOid, haku) =>
        haku.copy(
          hakuajat = hakujenHakuajatMap.getOrElse(haku.hakuOid, Seq()),
          hakukohteet = hakukohdeMap.getOrElse((toteutusOid, haku.hakuOid), Seq())
        )
      })

    haut.map(_._1).map { toteutusOid => {
      Hakutieto(
        toteutusOid,
        hakuMap.getOrElse(toteutusOid, Seq()))
    }}
  }
}

sealed trait HakutietoSQL extends HakutietoExtractors with SQLHelpers {

  def selectHakujenHakutiedot(koulutusOid: KoulutusOid): DBIO[Vector[(ToteutusOid, HakutietoHaku)]] = {
    sql"""select distinct t.oid,
                          h.oid,
                          h.nimi,
                          h.hakutapa_koodi_uri,
                          h.metadata -> 'koulutuksenAlkamiskausi' as koulutuksen_alkamiskausi,
                          h.hakulomaketyyppi,
                          h.hakulomake_ataru_id,
                          h.hakulomake_kuvaus,
                          h.hakulomake_linkki,
                          h.organisaatio_oid,
                          h.muokkaaja,
                          lower(h.system_time)
          from haut h
                   inner join hakukohteet k on k.haku_oid = h.oid and k.tila = 'julkaistu'::julkaisutila
                   inner join toteutukset t on t.oid = k.toteutus_oid and t.tila = 'julkaistu'::julkaisutila
                   inner join koulutukset o on o.oid = t.koulutus_oid and o.tila = 'julkaistu'::julkaisutila
          where o.oid = ${koulutusOid.toString}
            and h.tila = 'julkaistu'::julkaisutila""".as[(ToteutusOid, HakutietoHaku)]
  }

  def selectHakujenHakuajat(hakutiedot: Seq[(ToteutusOid, HakutietoHaku)]): DBIO[Vector[Hakuaika]] = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika)
          from hakujen_hakuajat
          where haku_oid in (#${createOidInParams(hakutiedot.map(_._2.hakuOid))})""".as[Hakuaika]
  }

  def selectHakukohteidenHakutiedot(koulutusOid: KoulutusOid): DBIO[Vector[(ToteutusOid, HakuOid, HakutietoHakukohde)]] = {
    sql"""select t.oid,
                 h.oid,
                 hk.oid,
                 hk.nimi,
                 hk.valintaperuste_id,
                 hk.metadata -> 'koulutuksenAlkamiskausi' as koulutuksen_alkamiskausi,
                 hk.metadata ->> 'kaytetaanHaunAlkamiskautta' as kaytetaan_haun_alkamiskautta_uusi,
                 hk.jarjestyspaikka_oid,
                 hk.hakulomaketyyppi,
                 hk.hakulomake_ataru_id,
                 hk.hakulomake_kuvaus,
                 hk.hakulomake_linkki,
                 hk.kaytetaan_haun_hakulomaketta,
                 hk.metadata -> 'aloituspaikat' as aloituspaikat_UUSI,
                 hk.metadata -> 'aloituspaikat' ->> 'lukumaara' as aloituspaikat,
                 hk.metadata -> 'aloituspaikat' ->> 'ensikertalaisille' as ensikertalaisen_aloituspaikat,
                 hk.kaytetaan_haun_aikataulua,
                 hk.pohjakoulutusvaatimus_koodi_urit,
                 hk.pohjakoulutusvaatimus_tarkenne,
                 hk.organisaatio_oid,
                 hk.muokkaaja,
                 array(select jsonb_array_elements(v.metadata -> 'valintatavat') ->> 'valintatapaKoodiUri') as valintatapa_koodi_urit,
                 lower(hk.system_time)
          from hakukohteet hk
                   inner join haut h on hk.haku_oid = h.oid and hk.tila = 'julkaistu'::julkaisutila
                   inner join toteutukset t on t.oid = hk.toteutus_oid and t.tila = 'julkaistu'::julkaisutila
                   inner join koulutukset k on k.oid = t.koulutus_oid and k.tila = 'julkaistu'::julkaisutila
                   left join valintaperusteet v on v.id = hk.valintaperuste_id and v.tila = 'julkaistu'::julkaisutila
          where k.oid = ${koulutusOid.toString}
            and hk.tila = 'julkaistu'::julkaisutila""".as[(ToteutusOid, HakuOid, HakutietoHakukohde)]
  }

  def selectHakukohteidenHakuajat(hakutiedot: Seq[(ToteutusOid, HakuOid, HakutietoHakukohde)]): DBIO[Vector[Hakuaika]] = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika)
          from hakukohteiden_hakuajat
          where hakukohde_oid in (#${createOidInParams(hakutiedot.map(_._3.hakukohdeOid))})""".as[Hakuaika]
  }
}
