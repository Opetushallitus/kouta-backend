package fi.oph.kouta.service

import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.repository.HakuDAO

class ArkistointiService {
  def ArchiveHautJaHakukohteet(): Unit = {
    val hakuOids: Set[HakuOid] = HakuDAO.listArchivableHakuOids().toSet
//    HakuDAO.archieveHakus(hakuOids);
//    HakukohdeDAO.archieveHakukohdes(hakuOids);
  }

}
