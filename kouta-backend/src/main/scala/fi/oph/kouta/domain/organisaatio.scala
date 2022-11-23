package fi.oph.kouta.domain

package object organisaatio {

  val OrganisaationOsaModel =
    """    OrganisaationOsa:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          example: 1.2.246.562.10.66634895666
      |        parentOidPath:
      |          type: string
      |          example: 1.2.246.562.10.66634895666/1.2.246.562.10.66634895871/1.2.246.562.10.594252633210/1.2.246.562.10.00000000001
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
      |            $ref: '#/components/schemas/OrganisaationOsa'
      |        status:
      |          type: string
      |          example: AKTIIVINEN
      |        organisaatiotyypit:
      |          items:
      |            type: string
      |            example:
      |              - organisaatiotyyppi_1
      |              - organisaatiotyyppi_2
      |""".stripMargin

  val OrganisaatioModel =
    """    OrgServiceOrganisaatio:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          example: 1.2.246.562.10.66634895871
      |        parentOidPath:
      |          type: string
      |          example: 1.2.246.562.10.66634895871/1.2.246.562.10.594252633210/1.2.246.562.10.00000000001
      |        oppilaitostyyppi:
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
      |            $ref: '#/components/schemas/OrganisaationOsa'
      |        status:
      |          type: string
      |          example: AKTIIVINEN
      |        organisaatiotyypit:
      |          items:
      |            type: string
      |            example:
      |              - organisaatiotyyppi_1
      |              - organisaatiotyyppi_2
      |        tyypit:
      |          items:
      |            type: string
      |            example:
      |              - organisaatiotyyppi_1
      |              - organisaatiotyyppi_2
      |""".stripMargin

  val OrganisaatioHierarkiaModel =
    """    OrganisaatioHierarkia:
      |      type: object
      |      properties:
      |        organisaatiot:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/OrgServiceOrganisaatio'
      |""".stripMargin

  def models = Seq(OrganisaationOsaModel, OrganisaatioModel, OrganisaatioHierarkiaModel)
}

case class Organisaatio(oid: String,
                        parentOidPath: String,
                        oppilaitostyyppi: Option[String] = None,
                        nimi: Kielistetty,
                        kotipaikkaUri: Option[String] = None,
                        children: List[OrganisaationOsa] = List(),
                        organisaatiotyypit: Option[List[String]] = None,
                        tyypit: List[String] = List())

case class OrganisaationOsa(oid: String,
                            parentOidPath: String,
                            nimi: Kielistetty,
                            kotipaikkaUri: Option[String] = None,
                            children: List[OrganisaationOsa] = List(),
                            organisaatiotyypit: List[String] = List())

case class OrganisaatioHierarkia(organisaatiot: List[Organisaatio])
