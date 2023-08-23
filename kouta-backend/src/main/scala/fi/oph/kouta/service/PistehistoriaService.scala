package fi.oph.kouta.service

import fi.oph.kouta.client.{JononAlimmatPisteet, LegacyTarjontaClient, ValintaTulosServiceClient, ValintaperusteetServiceClient, ValintatapajonoDTO}
import fi.oph.kouta.domain.{Hakukohde, HakukohteenLinja, TilaFilter}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, PistehistoriaDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.utils.slf4j.Logging

import scala.collection.parallel.ForkJoinTaskSupport

case class LegacyHakukohde(
    oid: String,
    tarjoajaOids: List[String],
    hakukohteenNimiUri: Option[String],
    hakukohdekoodi: Option[String],
    hakukausiVuosi: Option[String]
)

object PistehistoriaService extends PistehistoriaService(ValintaTulosServiceClient, ValintaperusteetServiceClient)

case class Pistetieto(
    tarjoaja: OrganisaatioOid,
    hakukohdekoodi: String,
    pisteet: Double,
    vuosi: String,
    valintatapajonoOid: String,
    valintatapajonoTyyppi: String,
    hakukohdeOid: HakukohdeOid,
    hakuOid: HakuOid
)

class PistehistoriaService(valintaTulosServiceClient: ValintaTulosServiceClient, valintaperusteetServiceClient: ValintaperusteetServiceClient) extends Logging {
  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdeKoodi: String): Seq[Pistetieto] = {
    PistehistoriaDAO.getPistehistoria(tarjoaja, hakukohdeKoodi)
  }

  def getPistehistoriaForLukiolinja(tarjoaja: OrganisaatioOid, lukiolinjaKoodi: String): Seq[Pistetieto] = {
    logger.info(s"Haetaan pistetiedot tarjoajalle $tarjoaja, lukiolinja $lukiolinjaKoodi")
    val hakukohdekoodit = lukiolinjaToHakukohdekoodi(
      lukiolinjaKoodi
    ) //Näitä voi periaatteessa olla useita, mutta useimmiten käytännössä yksi.
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
      KoodistoService
        .getRinnasteisetKooditInKoodisto(koodiUri, "hakukohteet")
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
        case None     => Seq()
      }
    }
  }

  private def syncPistehistoriaForKoutaHaku(hakuOid: HakuOid): Int = {
    logger.info(s"Käsitellään kouta-haku ${hakuOid}")

    val rawPistetiedot: Seq[JononAlimmatPisteet] = valintaTulosServiceClient.fetchPisteet(hakuOid)
    logger.info(s"Saatiin ${rawPistetiedot.size} pistetietoa Valinta-tulos-servicestä kouta-haulle $hakuOid")

    val haku = HakuDAO
      .get(hakuOid, TilaFilter.all())
      .getOrElse(throw new RuntimeException(s"Hakua $hakuOid ei löytynyt kannasta!"))
      ._1
    val alkamisvuosi: String = haku.metadata
      .flatMap(_.koulutuksenAlkamiskausi.flatMap(_.koulutuksenAlkamisvuosi))
      .getOrElse(throw new RuntimeException(s"Haulle $hakuOid ei löytynyt koulutuksen alkamiskautta. Haku: $haku"))

    val result = rawPistetiedot.flatMap(pt => {
      val result: Seq[Pistetieto] = HakukohdeDAO.get(HakukohdeOid(pt.hakukohdeOid), TilaFilter.all()) match {
        case Some((hakukohde, _)) =>
          logger.info(s"syncPistehistoria: Käsitellään haun $hakuOid hakukohteen ${hakukohde.oid} pistetieto $pt")
          val valintatapajono: ValintatapajonoDTO = valintaperusteetServiceClient.getValintatapajono(pt.valintatapajonoOid)
          for {
            hakukohdekoodi: String <- getHakukohdekoodiUris(hakukohde)
            tarjoaja               <- hakukohde.jarjestyspaikkaOid
          } yield {
            Pistetieto(
              tarjoaja = tarjoaja,
              hakukohdekoodi = hakukohdekoodi,
              pt.alinHyvaksyttyPistemaara,
              vuosi = alkamisvuosi,
              valintatapajonoOid = pt.valintatapajonoOid,
              valintatapajonoTyyppi = valintatapajono.tyyppi,
              hakuOid = hakuOid,
              hakukohdeOid = HakukohdeOid(pt.hakukohdeOid)
            )
          }
        case None =>
          logger.warn(
            s"syncPistehistoria: Ei löytynyt hakukohdetta oidilla ${pt.hakukohdeOid}, ei voida käsitellä pistetietoa $pt"
          )
          Seq()
      }
      logger.info(
        s"syncPistehistoria: syntyi ${result.size} pistetieto(a) haun $hakuOid hakukohteelle ${pt.hakukohdeOid}"
      )
      result
    })
    val changed = PistehistoriaDAO.savePistehistorias(result)
    logger.info(
      s"syncPistehistoria: tallennettiin $changed uutta pistehistoriatietoa ${rawPistetiedot.size} jonon ${result.size} tulokselle"
    )
    changed
  }

  private def syncPistehistoriaForLegacyHaku(hakuOid: HakuOid) = {
    logger.info(s"Käsitellään legacy-haku ${hakuOid}")
    val parallelism       = 8
    lazy val forkJoinPool = new java.util.concurrent.ForkJoinPool(parallelism)
    val pistetiedot       = ValintaTulosServiceClient.fetchPisteet(hakuOid).par
    pistetiedot.tasksupport = new ForkJoinTaskSupport(forkJoinPool)

    logger.info(
      s"Saatiin ${pistetiedot.size} pistetietoa Valinta-tulos-servicestä haulle $hakuOid, parallelism $parallelism"
    )
    val haku     = LegacyTarjontaClient.getHaku(hakuOid.toString)
    var progress = 0
    val result = pistetiedot
      .flatMap(pt => {
        val hakukohde = LegacyTarjontaClient.getHakukohde(pt.hakukohdeOid)
        val valintatapajono: ValintatapajonoDTO = valintaperusteetServiceClient.getValintatapajono(pt.valintatapajonoOid)
        progress += 1
        if (progress % 100 == 0) logger.info(s"handled $progress/${pistetiedot.size} pistetietos")
        hakukohde.tarjoajaOids.map(tarjoaja => {
          Pistetieto(
            tarjoaja = OrganisaatioOid(tarjoaja),
            hakukohdekoodi = hakukohde.hakukohteenNimiUri.map(_.split("#").head).getOrElse("SOS"),
            pt.alinHyvaksyttyPistemaara,
            vuosi = haku.hakukausiVuosi.get,
            valintatapajonoOid = pt.valintatapajonoOid,
            valintatapajonoTyyppi = valintatapajono.tyyppi,
            hakuOid = hakuOid,
            hakukohdeOid = HakukohdeOid(pt.hakukohdeOid)
          )
        })
      })
      .toList
      .filter(p => !p.vuosi.equals("9999") && !p.hakukohdekoodi.equals("SOS"))
    val changed = PistehistoriaDAO.savePistehistorias(result)
    logger.info(s"result: $changed")
    changed
  }

  //2023: 1.2.246.562.29.00000000000000021303
  //2022: 1.2.246.562.29.00000000000000005368
  //2021: 1.2.246.562.29.15658556293
  //2020: 1.2.246.562.29.54537554997
  //2019: 1.2.246.562.29.676633696010
  //2018: 1.2.246.562.29.557 39081531
  def syncDefaults()(implicit authenticated: Authenticated): String = {
    val hakuOids = Seq(
      HakuOid("1.2.246.562.29.55739081531"),
      HakuOid("1.2.246.562.29.676633696010"),
      HakuOid("1.2.246.562.29.54537554997"),
      HakuOid("1.2.246.562.29.15658556293"),
      HakuOid("1.2.246.562.29.00000000000000005368"),
      HakuOid("1.2.246.562.29.00000000000000021303")
    )
    logger.info(s"Synkataan alimmat pisteet oletushauille: $hakuOids")
    hakuOids.map(oid => {
      syncPistehistoriaForHaku(oid)
    }).mkString("\n")
  }

  def syncPistehistoriaForHaku(hakuOid: HakuOid)(implicit authenticated: Authenticated): String = {
    if (!authenticated.session.roleMap.exists(r => r._1.equals(Role.Indexer) && r._2.contains(RootOrganisaatioOid))) {
      logger.error(s"Käyttäjällä ${authenticated.session.personOid} ei ole rekisterinpitäjän oikeuksia, joten ei voida synkata pistehistoriaa haulle $hakuOid.")
      throw OrganizationAuthorizationFailedException(Seq(RootOrganisaatioOid), Seq.empty)
    }
    try {
      val result = hakuOid.toString match {
        case oid if oid.length != 35 => syncPistehistoriaForLegacyHaku(hakuOid)
        case _ => syncPistehistoriaForKoutaHaku(hakuOid)
      }
      s"Tallennettiin haulle $hakuOid yhteensä $result pistetietoa."
    } catch {
      case t: Throwable =>
        logger.error(s"Ei saatu synkattua hakua $hakuOid: ${t.getMessage}.")
        s"Haun $hakuOid pistetietojen tallennuksessa tapahtui virhe: ${t.getMessage}"
    }
  }
}
