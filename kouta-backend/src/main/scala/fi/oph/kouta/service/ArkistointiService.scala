package fi.oph.kouta.service

import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.vm.sade.utils.slf4j.Logging

class ArkistointiService extends Logging {
  def ArchiveHautJaHakukohteet(): Unit = {
    var archivedHakuCount: Int      = 0
    var archivedHakukohdeCount: Int = 0

    logger.info(s"Aloitetaan hakujen ja hakukohteiden arkistointi.")

    HakuDAO.listArchivableHakuOids().toSet match {
      case hakuOids: Set[HakuOid] =>
        HakukohdeDAO.listArchivableHakukohdeOidsByHakuOids(hakuOids).toSet match {
          case hakukohdeOids: Set[HakukohdeOid] =>
            logger.info(s"Arkistoidaan julkaistut haut: $hakuOids ja niiden julkaistut hakukohteet: $hakukohdeOids.")
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
            archivedHakukohdeCount = HakukohdeDAO.archiveHakukohdesByHakukohdeOids(hakukohdeOids)
          case _ =>
            logger.info(s"Ei löytynyt arkistoitavia hakukohteita hakuOideille: $hakuOids, arkistoidaan haut.")
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
        }
      case _ =>
        logger.info(s"Ei löytynyt yhtään arkistoitavaa hakua, ei arkistoida myöskään yhtään hakukohdetta.")
    }
    logger.info(s"Arkistointi valmis, arkistoitiin $archivedHakuCount hakua ja $archivedHakukohdeCount hakukohdetta.")
  }
}
