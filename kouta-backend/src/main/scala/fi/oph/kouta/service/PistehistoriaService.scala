package fi.oph.kouta.service

import fi.oph.kouta.client.{BasicCachedKoodistoClient, JononAlimmatPisteet, LegacyTarjontaClient, ValintaTulosServiceClient}
import fi.oph.kouta.domain.{Hakukohde, HakukohteenLinja, TilaFilter}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, PistehistoriaDAO}
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.parallel.ForkJoinTaskSupport

case class LegacyHakukohde(oid: String,
                           tarjoajaOids: List[String],
                           hakukohteenNimiUri: Option[String],
                           hakukohdekoodi: Option[String],
                           hakukausiVuosi: Option[String])

object PistehistoriaService extends PistehistoriaService

case class Pistetieto(tarjoaja: OrganisaatioOid,
                      hakukohdekoodi: String,
                      pisteet: Double,
                      vuosi: String,
                      valintatapajonoOid: String,
                      hakukohdeOid: HakukohdeOid,
                      hakuOid: HakuOid)

class PistehistoriaService extends Logging {
  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdeKoodi: String): Seq[Pistetieto] = {
      PistehistoriaDAO.getPistehistoria(tarjoaja, hakukohdeKoodi)
    }

  def getPistehistoriaForLukiolinja(tarjoaja: OrganisaatioOid, lukiolinjaKoodi: String): Seq[Pistetieto] = {
    logger.info(s"Haetaan pistetiedot tarjoajalle $tarjoaja, lukiolinja $lukiolinjaKoodi")
    val hakukohdekoodit = lukiolinjaToHakukohdekoodi(lukiolinjaKoodi) //Näitä voi periaatteessa olla useita, mutta useimmiten käytännössä yksi.
    if (hakukohdekoodit.isEmpty) {
      logger.warn(s"Lukiolinjalle $lukiolinjaKoodi ei löytynyt rinnasteista hakukohdekoodia!")
      Seq()
    } else {
      logger.info(s"Lukiolinjalle $lukiolinjaKoodi löytyi hakukohdekoodit: $hakukohdekoodit")
      hakukohdekoodit.flatMap(hk => PistehistoriaDAO.getPistehistoria(tarjoaja, hk))
    }
  }

  private def lukiolinjaToHakukohdekoodi(koodiUri: String) = {
    val result =
      BasicCachedKoodistoClient.getRinnasteisetCached(koodiUri, "hakukohteet")
        .map(_.koodiUri)
    if (result.nonEmpty) logger.info(s"Löydettiin rinnakkaisia hakukohdekoodeja lukiolinjalle $koodiUri: $result")
    else logger.warn(s"Ei löydetty rinnakkaisia hakukohdekoodeja lukiolinjalle $koodiUri")
    result
  }

  private def getHakukohdekoodiUris(hakukohde: Hakukohde) = {
    if (hakukohde.hakukohdeKoodiUri.isDefined) {
      Seq(hakukohde.hakukohdeKoodiUri.get)
    } else {
      val lukiolinja: Option[HakukohteenLinja] = hakukohde.metadata.flatMap(_.hakukohteenLinja)
      //Jos lukiolinja on määritelty mutta sen linjaUria ei ole määritelty,
      //kyseessä on tavallinen lukiokoulutus eli vastaa hakukohdekoodia hakukohteet_000
      lukiolinja match {
        case Some(ll) => ll.linja.map(lukiolinjaToHakukohdekoodi).getOrElse(Seq("hakukohteet_000"))
        case None => Seq()
      }
    }
  }

  private def syncPistehistoriaForKoutaHaku(hakuOid: HakuOid): Int = {
    logger.info(s"Käsitellään kouta-haku ${hakuOid}")

    val rawPistetiedot: Seq[JononAlimmatPisteet] = ValintaTulosServiceClient.fetchPisteet(hakuOid)
    logger.info(s"Saatiin ${rawPistetiedot.size} pistetietoa Valinta-tulos-servicestä kouta-haulle $hakuOid")

    val haku = HakuDAO.get(hakuOid, TilaFilter.all()).getOrElse(throw new RuntimeException(s"Hakua $hakuOid ei löytynyt kannasta!"))._1
    val alkamisvuosi: String = haku.metadata.flatMap(_.koulutuksenAlkamiskausi.flatMap(_.koulutuksenAlkamisvuosi))
      .getOrElse(throw new RuntimeException(s"Haulle $hakuOid ei löytynyt koulutuksen alkamiskautta. Haku: $haku"))

    val result = rawPistetiedot.flatMap(pt => {
      val result: Seq[Pistetieto] = HakukohdeDAO.get(HakukohdeOid(pt.hakukohdeOid), TilaFilter.all()) match {
        case Some((hakukohde, _)) =>
          logger.info(s"syncPistehistoria: Käsitellään haun $hakuOid hakukohteen ${hakukohde.oid} pistetieto $pt")
          for {
            hakukohdekoodi: String <- getHakukohdekoodiUris(hakukohde)
            tarjoaja <- hakukohde.jarjestyspaikkaOid
          } yield {
            Pistetieto(
              tarjoaja = tarjoaja,
              hakukohdekoodi = hakukohdekoodi,
              pt.alinHyvaksyttyPistemaara,
              vuosi = alkamisvuosi,
              valintatapajonoOid = pt.valintatapajonoOid,
              hakuOid = hakuOid,
              hakukohdeOid = HakukohdeOid(pt.hakukohdeOid))
          }
        case None =>
          logger.warn(s"syncPistehistoria: Ei löytynyt hakukohdetta oidilla ${pt.hakukohdeOid}, ei voida käsitellä pistetietoa $pt")
          Seq()
      }
      logger.info(s"syncPistehistoria: syntyi ${result.size} pistetieto(a) haun $hakuOid hakukohteelle ${pt.hakukohdeOid}")
      result
    })
    val changed = PistehistoriaDAO.savePistehistorias(result)
    logger.info(s"syncPistehistoria: tallennettiin $changed uutta pistehistoriatietoa ${rawPistetiedot.size} jonon ${result.size} tulokselle")
    changed
  }

  private def syncPistehistoriaForLegacyHaku(hakuOid: HakuOid) = {
    logger.info(s"Käsitellään legacy-haku ${hakuOid}")
    val parallelism = 8
    lazy val forkJoinPool = new java.util.concurrent.ForkJoinPool(parallelism)
    val pistetiedot = ValintaTulosServiceClient.fetchPisteet(hakuOid).par
    pistetiedot.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

    logger.info(s"Saatiin ${pistetiedot.size} pistetietoa Valinta-tulos-servicestä haulle $hakuOid, parallelism $parallelism")
    val haku = LegacyTarjontaClient.getHaku(hakuOid.toString)
    var progress = 0
    val result = pistetiedot.flatMap(pt => {
      val hakukohde = LegacyTarjontaClient.getHakukohde(pt.hakukohdeOid)
      progress += 1
      if (progress % 100 == 0) logger.info(s"handled $progress/${pistetiedot.size} pistetietos")
      hakukohde.tarjoajaOids.map(tarjoaja => {
        Pistetieto(
          tarjoaja = OrganisaatioOid(tarjoaja),
          hakukohdekoodi = hakukohde.hakukohteenNimiUri.map(_.split("#").head).getOrElse("SOS"),
          pt.alinHyvaksyttyPistemaara,
          vuosi = haku.hakukausiVuosi.getOrElse("9999"), //todo, fallback to sijoitteluajoId?
          valintatapajonoOid = pt.valintatapajonoOid,
          hakuOid = hakuOid,
          hakukohdeOid = HakukohdeOid(pt.hakukohdeOid))
      })
    }).toList.filter(p => !p.vuosi.equals("9999") && !p.hakukohdekoodi.equals("SOS"))
    val changed = PistehistoriaDAO.savePistehistorias(result)
    logger.info(s"result: $changed")
    changed
  }

  def syncPistehistoriaForHaku(hakuOid: HakuOid): Int = {
    hakuOid.toString match {
      case oid if oid.length != 35 => syncPistehistoriaForLegacyHaku(hakuOid)
      case _ => syncPistehistoriaForKoutaHaku(hakuOid)
    }
  }
}
