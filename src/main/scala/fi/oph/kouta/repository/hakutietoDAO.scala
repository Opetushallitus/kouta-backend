package fi.oph.kouta.repository

import fi.oph.kouta.domain.{Ajanjakso, Hakutieto, HakutietoHaku, HakutietoHakukohde}
import fi.oph.kouta.domain.oid._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait HakutietoDAO {
  def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Hakutieto]
}

object HakutietoDAO extends HakutietoDAO with HakutietoSQL {
  override def getByKoulutusOid(koulutusOid: KoulutusOid): Seq[Hakutieto] = {
    KoutaDatabase.runBlockingTransactionally( for {
      haut <- selectHakujenHakutiedot(koulutusOid)
      hakujenHakuajat <- selectHakujenHakuajat(haut)
      hakukohteet <- selectHakukohteidenHakutiedot(koulutusOid)
      hakukohteidenHakuajat <- selectHakukohteidenHakuajat(hakukohteet)
    } yield (haut, hakujenHakuajat, hakukohteet, hakukohteidenHakuajat) ) match {
      case Left(t) => throw t
      case Right((h, hh, hk, hkh)) => createHakutieto(h, hh, hk, hkh)
    }
  }

  private def createHakutieto(haut:Seq[(ToteutusOid, HakutietoHaku)],
                              hakujenHakuajat:Seq[Hakuaika],
                              hakukohteet:Seq[(ToteutusOid, HakuOid, HakutietoHakukohde)],
                              hakukohteidenHakuajat:Seq[Hakuaika]) = {

    def mapHakuajat[A <: Oid](hakuajat: Seq[Hakuaika], f: GenericOid => A): Map[A, Seq[Ajanjakso]] =
      hakuajat.groupBy(h => f(h.oid))
        .mapValues(_.map(a => Ajanjakso(a.alkaa, a.paattyy)).toList).map(identity)

    val hakujenHakuajatMap = mapHakuajat[HakuOid](hakujenHakuajat, (oid: GenericOid) => HakuOid(oid.toString))
    val hakukohteidenHakuajatMap = mapHakuajat[HakukohdeOid](hakukohteidenHakuajat, (oid: GenericOid) => HakukohdeOid(oid.toString))
    val hakukohdeMap = hakukohteet.groupBy(h => (h._1, h._2)).mapValues(_.map(h => {
      h._3.copy(hakuajat = hakukohteidenHakuajatMap.getOrElse(h._3.hakukohdeOid, Seq()).toList)
    })).map(identity)
    val hakuMap = haut.groupBy(_._1).mapValues(_.map(h => {h._2.copy(
      hakuajat = hakujenHakuajatMap.getOrElse(h._2.hakuOid, Seq()).toList,
      hakukohteet = hakukohdeMap.getOrElse((h._1, h._2.hakuOid), Seq())
    )})).map(identity)
    haut.map(_._1).map { toteutusOid => {
      Hakutieto(
        toteutusOid,
        hakuMap.getOrElse(toteutusOid, Seq()))
    }}
  }
}

sealed trait  HakutietoSQL extends  HakutietoExtractors with SQLHelpers {

  def selectHakujenHakutiedot(koulutusOid: KoulutusOid) = {
    sql"""select t.oid, h.oid, h.nimi, h.hakutapa_koodi_uri, h.alkamiskausi_koodi_uri, h.alkamisvuosi,
                 h.hakulomaketyyppi, h.hakulomake, h.organisaatio_oid, h.muokkaaja, lower(h.system_time)
          from haut h
          inner join hakukohteet k on k.haku_oid = h.oid and k.tila = 'julkaistu'::julkaisutila
          inner join toteutukset t on t.oid = k.toteutus_oid and t.tila = 'julkaistu'::julkaisutila
          inner join koulutukset o on o.oid = t.koulutus_oid and o.tila = 'julkaistu'::julkaisutila
          where o.oid = ${koulutusOid.toString}
          and h.tila = 'julkaistu'::julkaisutila""".as[(ToteutusOid, HakutietoHaku)]
  }

  def selectHakujenHakuajat(hakutiedot: Seq[(ToteutusOid, HakutietoHaku)]) = {
    sql"""select haku_oid, lower(hakuaika), upper(hakuaika)
          from hakujen_hakuajat
          where haku_oid in (#${createOidInParams(hakutiedot.map(_._2.hakuOid))})""".as[Hakuaika]
  }

  def selectHakukohteidenHakutiedot(koulutusOid: KoulutusOid) = {
    sql"""select t.oid, h.oid, k.oid, k.nimi, k.alkamiskausi_koodi_uri, k.alkamisvuosi,
                 k.hakulomaketyyppi, k.hakulomake, k.aloituspaikat, k.ensikertalaisen_aloituspaikat,
                 k.kaytetaan_haun_aikataulua, k.organisaatio_oid, k.muokkaaja, lower(k.system_time)
          from hakukohteet k
          inner join haut h on k.haku_oid = h.oid and k.tila = 'julkaistu'::julkaisutila
          inner join toteutukset t on t.oid = k.toteutus_oid and t.tila = 'julkaistu'::julkaisutila
          inner join koulutukset o on o.oid = t.koulutus_oid and o.tila = 'julkaistu'::julkaisutila
          where o.oid = ${koulutusOid.toString}
          and k.tila = 'julkaistu'::julkaisutila""".as[(ToteutusOid, HakuOid, HakutietoHakukohde)]
  }

  def selectHakukohteidenHakuajat(hakutiedot: Seq[(ToteutusOid, HakuOid, HakutietoHakukohde)]) = {
    sql"""select hakukohde_oid, lower(hakuaika), upper(hakuaika)
          from hakukohteiden_hakuajat
          where hakukohde_oid in (#${createOidInParams(hakutiedot.map(_._3.hakukohdeOid))})""".as[Hakuaika]
  }
}