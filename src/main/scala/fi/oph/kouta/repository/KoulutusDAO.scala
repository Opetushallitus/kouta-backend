package fi.oph.kouta.repository

import fi.oph.kouta.domain.Koulutus
import slick.jdbc.PostgresProfile.api._

//import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusDAO extends PgSetters {

  private implicit val getResult = Koulutus.extractor

  def put(koulutus:Koulutus) = {
    val Koulutus(oid, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, tarjoajat, nimi, kuvaus, muokkaaja) = koulutus

    val kielet = nimi.keys ++ kuvaus.keys

    KoutaDatabase.runBlockingTransactionally(
      sqlu"""insert into koulutukset (oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, tarjoajat, muokkaaja)
             values ($oid, $johtaaTutkintoon, ${koulutustyyppi.toString}::koulutustyyppi,
             $koulutusKoodiUri, ${tila.toString}::julkaisutila, ${tarjoajat}, $muokkaaja)""".andThen(
        DBIO.sequence( kielet.map(k => sqlu"""insert into koulutusten_tekstit (koulutus_oid, kielikoodi, nimi, kuvaus, muokkaaja)
             values ($oid, ${k.toString}, ${nimi.get(k)}, ${kuvaus.get(k)}, $muokkaaja)"""))))
  }

  def get(oid:String): Option[Koulutus] = {
    KoutaDatabase.runBlocking(
      sql"""select k.oid, k.johtaa_tutkintoon, k.tyyppi, k.koulutus_koodi_uri,
              k.tila, k.tarjoajat, json_object(array_agg(t.kielikoodi), array_agg(t.nimi)),
              json_object(array_agg(t.kielikoodi), array_agg(t.kuvaus)), k.muokkaaja
            from koulutukset k
            inner join koulutusten_tekstit t on k.oid = t.koulutus_oid
            where k.oid=$oid
            group by k.oid"""
        .as[Koulutus].headOption)
  }
}