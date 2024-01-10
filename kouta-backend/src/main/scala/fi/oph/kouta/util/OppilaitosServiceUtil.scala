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

  def getOidsFromChildren(organisaationOsat: Option[List[Organisaatio]]): List[OrganisaatioOid] = {
    organisaationOsat match {
      case Some(organisaationOsat) =>
      organisaationOsat.flatMap(org => {
           if (org.children.isEmpty) {
             List(OrganisaatioOid(org.oid))
           } else {
             OrganisaatioOid(org.oid) +: getOidsFromChildren(org.children)
           }
         })
      case None => List()
    }
  }

  def filterByOsoitetyyppi(yhteystiedot: List[OrganisaationYhteystieto], osoitetyyppi: String): List[OrgOsoite] = {
    yhteystiedot
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

  def toYhteystieto(nimi: Kielistetty, yhteystiedot: List[OrganisaationYhteystieto]): Option[Yhteystieto] = {
    if (yhteystiedot.nonEmpty) {
      val postiosoitteet = filterByOsoitetyyppi(yhteystiedot, "posti")
      val postiosoite = toOsoite(postiosoitteet)

      val kayntiosoitteet = filterByOsoitetyyppi(yhteystiedot, "kaynti")
      val kayntiosoite = toOsoite(kayntiosoitteet)

      val puhelinnumero = yhteystiedot.filter(_.isInstanceOf[Puhelin]).asInstanceOf[List[Puhelin]].map(
        puhelinnumero => (puhelinnumero.kieli, puhelinnumero.numero)
      ).toMap

      val email = yhteystiedot.filter(_.isInstanceOf[Email]).asInstanceOf[List[Email]].map(
        email => (email.kieli, email.email)
      ).toMap

      val www = yhteystiedot.filter(_.isInstanceOf[Www]).asInstanceOf[List[Www]].map(
        www => (www.kieli, www.www)
      ).toMap

      Some(Yhteystieto(nimi = nimi, postiosoite = postiosoite, kayntiosoite = kayntiosoite, puhelinnumero = puhelinnumero, sahkoposti = email, www = www))
    } else {
      None
    }
  }

  def organisaatioServiceOrgToOrganisaatio(organisaatio: OrganisaatioServiceOrg, children: Seq[OrganisaatioServiceOrg] = List()): Organisaatio = {
    val yhteystiedot = organisaatio.yhteystiedot match {
      case Some(yhteystiedot: List[OrganisaationYhteystieto]) => toYhteystieto(organisaatio.nimi, yhteystiedot)
      case None => None
    }

    val organisaatioChildren = organisaatio.children match {
      case Some(children) => Some(children)
      case None => if (children.nonEmpty) Some(children.toList) else None
    }

    val oppilaitostyyppi = organisaatio.oppilaitostyyppi match {
      case Some(oppilaitostyyppi) => Some(oppilaitostyyppi)
      case _ => organisaatio.oppilaitosTyyppiUri match {
        case Some(oppilaitosTyyppiUri) => Some(oppilaitosTyyppiUri)
        case None => None
      }
    }

    Organisaatio(
      oid = organisaatio.oid,
      parentOids = OppilaitosServiceUtil.getParentOids(organisaatio.parentOidPath),
      nimi = organisaatio.nimi,
      yhteystiedot = yhteystiedot,
      kotipaikkaUri = organisaatio.kotipaikkaUri,
      children = organisaatioChildren match {
        case Some(orgChildren) => Some(orgChildren.map(org => organisaatioServiceOrgToOrganisaatio(org)))
        case None => None
      },
      oppilaitostyyppi = oppilaitostyyppi,
      kieletUris = organisaatio.kieletUris,
      organisaatiotyypit = organisaatio.organisaatiotyypit,
      tyypit = organisaatio.tyypit
    )
  }

  def getParentOids(parentOidPath: String): List[OrganisaatioOid] = {
    parentOidPath.split("(\\||\\/)").toList.filter(_.nonEmpty).map(oid => OrganisaatioOid(oid))
  }
}
