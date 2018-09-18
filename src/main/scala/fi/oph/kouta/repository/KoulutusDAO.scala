package fi.oph.kouta.repository

import fi.oph.kouta.domain.Koulutus
import slick.jdbc.PostgresProfile.api._

object KoulutusDAO extends PgSetters {

  private implicit val getResult = Koulutus.extractor

  def put(koulutus:Koulutus) = {
    val Koulutus(oid, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, tarjoajat, muokkaaja) = koulutus

    KoutaDatabase.runBlocking(
      sqlu"""insert into koulutukset (oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, tarjoajat, muokkaaja)
             values ($oid, $johtaaTutkintoon, ${koulutustyyppi.toString}::koulutustyyppi,
             $koulutusKoodiUri, ${tila.toString}::julkaisutila, ${tarjoajat}, $muokkaaja)""")}

  def get(oid:String): Option[Koulutus] = {
    KoutaDatabase.runBlocking(
      sql"""select oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, tarjoajat, muokkaaja
            from koulutukset
            where oid=$oid"""
        .as[Koulutus].headOption)
  }
}