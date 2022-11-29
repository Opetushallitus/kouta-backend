package fi.oph.kouta.service

import fi.oph.kouta.client.{LegacyTarjontaClient, ValintaTulosServiceClient}
import fi.oph.kouta.domain.{HakukohdeListItem, TilaFilter}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.repository.{HakukohdeDAO, PistehistoriaDAO}
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

  private def syncPistehistoriaForKoutaHaku(hakuOid: HakuOid): Int = {
    logger.info(s"Käsitellään kouta-haku ${hakuOid}")
    val pistetiedot = ValintaTulosServiceClient.fetchPisteet(hakuOid)
    val koutaHakukohteet: Seq[HakukohdeListItem] = HakukohdeDAO.listByHakuOid(hakuOid, TilaFilter.all()).toList

    val result = pistetiedot.map(pt => {
      val hakukohde = koutaHakukohteet.find(hk => hk.oid.toString.equals(pt.hakukohdeOid)).getOrElse(throw new RuntimeException(s"No hakukohde ${pt.hakukohdeOid} found"))
      Pistetieto(
        tarjoaja = hakukohde.jarjestyspaikkaOid.get, //option, mutta onko tämä käytännössä aina tiedossa?
        hakukohdekoodi = hakukohde.hakukohdeKoodiUri.map(_.split("#").head).getOrElse("SOS"), //todo lukiolinja -> hakukohdeKoodiUri?
        pt.alinHyvaksyttyPistemaara,
        vuosi = "2022", //fixme, haetaan haun tiedoista
        valintatapajonoOid = pt.valintatapajonoOid,
        hakuOid = hakuOid,
        hakukohdeOid = HakukohdeOid(pt.hakukohdeOid))

    }).filter(p => p.hakukohdekoodi != "SOS")
    val changed = PistehistoriaDAO.savePistehistorias(result)
    logger.info(s"result: $changed")
    changed
  }

  private def syncPistehistoriaForLegacyHaku(hakuOid: HakuOid) = {
    val parallelism = 8
    lazy val forkJoinPool = new java.util.concurrent.ForkJoinPool(parallelism)
    logger.info(s"Käsitellään legacy-haku ${hakuOid}")
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

  def syncPistehistoriaForHaku(hakuOid: HakuOid) = {
    hakuOid.toString match {
      case oid if oid.length != 35 => syncPistehistoriaForLegacyHaku(hakuOid)
      case _ => syncPistehistoriaForKoutaHaku(hakuOid)
    }
  }
}
