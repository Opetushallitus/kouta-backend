package fi.oph.kouta.util

import fi.oph.kouta.domain.oid.OrganisaatioOid

object KoulutusServiceValidationUtil {
  def getUnremovableTarjoajat(removedTarjoajat: Map[OrganisaatioOid, Seq[OrganisaatioOid]], toteutustenTarjoajat: Seq[OrganisaatioOid]) : List[OrganisaatioOid] = {
    removedTarjoajat.filter(tarjoajaAndOids => {
      tarjoajaAndOids._2.exists(oid =>
        toteutustenTarjoajat.contains(oid))
    }).keys.toList
  }
}
