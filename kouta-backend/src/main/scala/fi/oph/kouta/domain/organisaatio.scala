package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.OrganisaatioOid

package object organisaatio {

  val OrganisaatioModel =
    """    Organisaatio:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          example: 1.2.246.562.10.66634895871
      |        parentOids:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.10.66634895871
      |              - 1.2.246.562.10.594252633210
      |              - 1.2.246.562.10.00000000001
      |        oppilaitostyyppiUri:
      |          type: string
      |          example: oppilaitostyyppi_21
      |        nimi:
      |          type: object
      |          $ref: '#/components/schemas/Nimi'
      |        kotipaikkaUri:
      |          type: string
      |          example:
      |            - kunta_091
      |            - kunta_398
      |        children:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/Organisaatio'
      |        status:
      |          type: string
      |          example: AKTIIVINEN
      |        organisaatiotyypit:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - organisaatiotyyppi_1
      |              - organisaatiotyyppi_2
      |        tyypit:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - organisaatiotyyppi_1
      |              - organisaatiotyyppi_2
      |        kieletUris:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - oppilaitoksenopetuskieli_1#1
      |              - oppilaitoksenopetuskieli_4#1
      |""".stripMargin

  val OrganisaatioHierarkiaModel =
    """    OrganisaatioHierarkia:
      |      type: object
      |      properties:
      |        organisaatiot:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/Organisaatio'
      |""".stripMargin

  def models = Seq(OrganisaatioModel, OrganisaatioHierarkiaModel)
}

sealed trait OrganisaationYhteystieto {
  val kieli: Kieli
}

case class OrgOsoite(osoiteTyyppi: String,
                     kieli: Kieli,
                     osoite: String,
                     postinumeroUri: Option[String]) extends OrganisaationYhteystieto

case class Email(kieli: Kieli, email: String) extends OrganisaationYhteystieto

case class Puhelin(kieli: Kieli, numero: String) extends OrganisaationYhteystieto

case class Www(kieli: Kieli, www: String) extends OrganisaationYhteystieto

sealed trait OrganisaatioBase {
  val oid: String
  val nimi: Kielistetty
}

case class OrganisaatioServiceOrg(oid: String,
                                  parentOidPath: String,
                                  oppilaitostyyppi: Option[String] = None,
                                  nimi: Kielistetty,
                                  yhteystiedot: Option[List[OrganisaationYhteystieto]] = None,
                                  status: String,
                                  kotipaikkaUri: Option[String] = None,
                                  children: Option[List[OrganisaatioServiceOrg]] = None,
                                  organisaatiotyypit: Option[List[String]] = None,
                                  tyypit: Option[List[String]] = None,
                                  oppilaitosTyyppiUri: Option[String] = None,
                                  kieletUris: List[String] = List()
                       ) extends OrganisaatioBase {
  def isOppilaitos: Boolean = (organisaatiotyypit.getOrElse(List()) ++ tyypit.getOrElse(List())).contains("organisaatiotyyppi_02")
}

case class OrgServiceOrganisaatioHierarkia(organisaatiot: List[OrganisaatioServiceOrg])

case class Organisaatio(oid: String,
                        parentOids: List[OrganisaatioOid] = List(),
                        nimi: Kielistetty,
                        yhteystiedot: Option[Yhteystieto] = None,
                        kotipaikkaUri: Option[String] = None,
                        children: Option[List[Organisaatio]] = None,
                        oppilaitostyyppiUri: Option[String] = None,
                        kieletUris: List[String] = List(),
                        organisaatiotyyppiUris: Option[List[String]] = None) extends OrganisaatioBase

case class OrganisaatioHierarkia(organisaatiot: List[Organisaatio])
