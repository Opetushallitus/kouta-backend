package fi.oph.kouta.repository

import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.repository.dto._
import fi.vm.sade.utils.slf4j.Logging
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusDAO extends KoulutusDTOs with Logging {

  private def insertKoulutus(koulutus:Koulutus) = {
    val Koulutus(oid, johtaaTutkintoon, koulutustyyppi, koulutusKoodiUri, tila, _, _, _, muokkaaja) = koulutus
    sqlu"""insert into koulutukset (oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, muokkaaja)
             values ($oid, $johtaaTutkintoon, ${koulutustyyppi.toString}::koulutustyyppi,
             $koulutusKoodiUri, ${tila.toString}::julkaisutila, $muokkaaja)"""
  }

  private def insertKoulutuksenTekstit(koulutus:Koulutus) = {
    val Koulutus(oid, _, _, _, _, _, nimi, kuvaus, muokkaaja) = koulutus
    val kielet = nimi.keys ++ kuvaus.keys
    DBIO.sequence( kielet.map(k => sqlu"""insert into koulutusten_tekstit (koulutus_oid, kielikoodi, nimi, kuvaus, muokkaaja)
             values ($oid, ${k.toString}, ${nimi.get(k)}, ${kuvaus.get(k)}, $muokkaaja)"""))
  }

  private def insertKoulutuksenTarjoajat(koulutus:Koulutus) = {
    val Koulutus(oid, _, _, _, _, tarjoajat, _, _, muokkaaja) = koulutus
    DBIO.sequence( tarjoajat.map(t =>
      sqlu"""insert into koulutusten_tarjoajat (koulutus_oid, tarjoaja_oid, muokkaaja)
             values ($oid, $t, $muokkaaja)"""))
  }

  def put(koulutus:Koulutus) = {
    KoutaDatabase.runBlockingTransactionally(
      insertKoulutus(koulutus).andThen(
        insertKoulutuksenTekstit(koulutus)).andThen(
          insertKoulutuksenTarjoajat(koulutus))) match {
      case Left(t) => logger.error("FAILURE", t); throw t
      case Right(x) => logger.error(x.toList.mkString(":")); x
    }
  }

  private def selectKoulutus(oid:String) = {
    sql"""select oid, johtaa_tutkintoon, tyyppi, koulutus_koodi_uri, tila, muokkaaja from koulutukset where oid = $oid"""
  }

  private def selectKoulutuksenTekstit(oid:String) = {
    sql"""select koulutus_oid, kielikoodi, nimi, kuvaus from koulutusten_tekstit where koulutus_oid = $oid"""
  }

  private def selectKoulutuksenTarjoajat(oid:String) = {
    sql"""select koulutus_oid, tarjoaja_oid from koulutusten_tarjoajat where koulutus_oid = $oid"""
  }

  def get(oid:String): Option[Koulutus] = {
    KoutaDatabase.runBlockingTransactionally( for {
      x <- selectKoulutus(oid).as[KoulutusDTO].headOption
      y <- selectKoulutuksenTekstit(oid).as[KoulutuksenTekstitDTO]
      z <- selectKoulutuksenTarjoajat(oid).as[KoulutuksenTarjoajatDTO]
    } yield (x, y, z) ) match {
      case Left(t) => throw t
      case Right((None, _, _)) => None
      case Right((Some(k), y, z)) => Some(koulutus(k, y, z))
    }
  }
}