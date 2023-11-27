package fi.oph.kouta.util

import fi.oph.kouta.client.OrganisaatioServiceQueryException
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.OrganisaatioServiceImpl
import org.slf4j.Logger

object OppilaitosServiceUtil {
  def getHierarkiaOids(hierarkia: OrganisaatioHierarkia): List[OrganisaatioOid] = {
    hierarkia.organisaatiot.flatMap(org => {
      getOidsFromChildren(org.children) :+ OrganisaatioOid(org.oid)
    }).distinct
  }

  def getOidsFromChildren(organisaationOsat: List[OrganisaatioHierarkiaOrg]): List[OrganisaatioOid] = {
    organisaationOsat.flatMap(org => {
      if (org.children.isEmpty) {
        List(OrganisaatioOid(org.oid))
      } else {
        OrganisaatioOid(org.oid) +: getOidsFromChildren(org.children)
      }
    })
  }

  def filterByOsoitetyyppi(yhteystiedot: List[Option[OrganisaationYhteystieto]], osoitetyyppi: String): List[OrgOsoite] = {
    yhteystiedot.flatten
      .filter(_.isInstanceOf[OrgOsoite]).asInstanceOf[List[OrgOsoite]]
      .filter(_.osoiteTyyppi == osoitetyyppi)
  }

  def toOsoite(osoitteet: List[OrgOsoite]): Option[Osoite] = {
    val kielistettyOsoite = osoitteet.map(osoite => {
      (osoite.kieli, osoite.osoite)
    }).toMap

    kielistettyOsoite match {
      case _ if kielistettyOsoite.isEmpty => None
      case _ =>
        val postinumero = osoitteet.map(_.postinumeroUri) match {
          case Nil => None
          case x :: _ => Some(x)
        }

        Some(Osoite(osoite = kielistettyOsoite, postinumeroKoodiUri = postinumero.get))
    }
  }

  def toYhteystieto(nimi: Kielistetty, yhteystiedot: List[Option[OrganisaationYhteystieto]]): Option[Yhteystieto] = {
    val postiosoitteet = filterByOsoitetyyppi(yhteystiedot, "posti")
    val postiosoite = toOsoite(postiosoitteet)

    val kayntiosoitteet = filterByOsoitetyyppi(yhteystiedot, "kaynti")
    val kayntiosoite = toOsoite(kayntiosoitteet)

    val puhelinnumero = yhteystiedot.flatten.filter(_.isInstanceOf[Puhelin]).asInstanceOf[List[Puhelin]].map(
      puhelinnumero => (puhelinnumero.kieli, puhelinnumero.numero)
    ).toMap

    val email = yhteystiedot.flatten.filter(_.isInstanceOf[Email]).asInstanceOf[List[Email]].map(
      email => (email.kieli, email.email)
    ).toMap

    Some(Yhteystieto(nimi = nimi, postiosoite = postiosoite, kayntiosoite = kayntiosoite, puhelinnumero = puhelinnumero, sahkoposti = email))
  }

  def getYhteystieto(organisaatioService: OrganisaatioServiceImpl, oid: OrganisaatioOid, logger: Logger): Option[Yhteystieto] = {
    organisaatioService.getOrganisaatio(oid) match {
      case Right(organisaatio) =>
        val yhteystiedot = organisaatio.yhteystiedot
        OppilaitosServiceUtil.toYhteystieto(organisaatio.nimi, yhteystiedot)
      case Left(e: OrganisaatioServiceQueryException) if e.status == 404 =>
        logger.warn("Organisaatiota ei löytynyt organisaatiopalvelusta oid:lla: " + oid)
        None
      case Left(e: Exception) =>
        logger.error("Ongelmia organisaation tietojen haussa: " + oid)
        None
    }
  }
}
