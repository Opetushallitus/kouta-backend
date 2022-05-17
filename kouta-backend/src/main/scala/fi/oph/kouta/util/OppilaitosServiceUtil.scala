package fi.oph.kouta.util

import fi.oph.kouta.client.{OrganisaatioHierarkia, OrganisaationOsa}
import fi.oph.kouta.domain.oid.OrganisaatioOid

object OppilaitosServiceUtil {
  def getHierarkiaOids(hierarkia: OrganisaatioHierarkia): List[OrganisaatioOid] = {
    hierarkia.organisaatiot.flatMap(org => {
      getOidsFromChildren(org.children) :+ OrganisaatioOid(org.oid)
    }).distinct
  }

  def getOidsFromChildren(organisaationOsat: List[OrganisaationOsa]): List[OrganisaatioOid] = {
    organisaationOsat.flatMap(org => {
      if (org.children.isEmpty) {
        List(OrganisaatioOid(org.oid))
      } else {
        OrganisaatioOid(org.oid) +: getOidsFromChildren(org.children)
      }
    })
  }
}
