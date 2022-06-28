package fi.oph.kouta.service

import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.vm.sade.utils.slf4j.Logging

class ArkistointiService extends Logging {
  def ArchiveHautJaHakukohteet(): Unit = {

    logger.info(s"Hakujen ja hakukohteiden arkistointi aloitettu.")

    HakuDAO.listArchivableHakuOids().toSet match {
      case hakuOids: Set[HakuOid] =>
        logger.info(s"Arkistoidaan haut: $hakuOids ja niiden julkaistut hakukohteet.")
        val archivedHakuCount: Int      = HakuDAO.archiveHakusByHakuOids(hakuOids)
        val archivedHakukohdeCount: Int = HakukohdeDAO.archiveHakukohdesByHakuOids(hakuOids)
        logger.info(
          s"Arkistointi valmis, arkistoitiin $archivedHakuCount hakua ja $archivedHakukohdeCount hakukohdetta."
        )
      case _ =>
        logger.info(s"Ei löytynyt yhtään arkistoitavaa hakua, ei arkistoida myöskään yhtään hakukohdetta.")
    }
  }
}
