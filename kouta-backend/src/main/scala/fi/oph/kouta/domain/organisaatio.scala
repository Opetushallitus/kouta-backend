package fi.oph.kouta.domain

import fi.oph.kouta.service.MigrationService.toKieli

package object organisaatio {

  val OrganisaatioModel =
    """    Organisaatio:
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
      |            $ref: '#/components/schemas/Organisaatio'
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
      |        yhteystiedot:
      |          type: object
      |          $ref: '#/components/schemas/OrganisaatioYhteystiedot'
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

  val OrganisaatioYhteystiedotModel =
    """    OrganisaatioYhteystiedot:
      |      type: object
      |      properties:
      |        sahkoposti:
      |          type: object
      |          properties:
      |           fi:
      |             type: string
      |             description: "Organisaation suomenkielinen sähköpostiosoite"
      |           sv:
      |             type: string
      |             description: "Organisaation ruotsinkielinen sähköpostiosoite"
      |           en:
      |             type: string
      |             description: "Organisaation englanninkielinen sähköpostiosoite"
      |        puhelinnumero:
      |          type: object
      |          properties:
      |           fi:
      |             type: string
      |             description: "Organisaation suomenkielinen puhelinnumero"
      |           sv:
      |             type: string
      |             description: "Organisaation ruotsinkielinen puhelinnumero"
      |           en:
      |             type: string
      |             description: "Organisaation englanninkielinen puhelinnumero"
      |        wwwOsoite:
      |          type: object
      |          properties:
      |           fi:
      |             type: string
      |             description: "Organisaation suomenkielinen kotisivun osoite"
      |           sv:
      |             type: string
      |             description: "Organisaation ruotsinkielinen kotisivun osoite"
      |           en:
      |             type: string
      |             description: "Organisaation englanninkielinen kotisivun osoite"
      |        postiosoite:
      |          type: object
      |          $ref: '#/components/schemas/OrganisaatioOsoite'
      |          description: "Organisaation postiosoite"
      |        kayntiosoite:
      |          type: object
      |          $ref: '#/components/schemas/OrganisaatioOsoite'
      |          description: "Organisaation kayntiosoite"
      |""".stripMargin

  val OrganisaatioOsoiteModel =
    """    OrganisaatioOsoite:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          properties:
      |            fi:
      |              type: string
      |              description: "Organisaation suomenkielinen osoite"
      |            sv:
      |              type: string
      |              description: "Organisaation ruotsinkielinen osoite"
      |            en:
      |              type: string
      |              description: "Organisaation englanninkielinen osoite"
      |        postinumeroUri:
      |          type: object
      |          properties:
      |            fi:
      |              type: string
      |              description: "Organisaation suomenkielinen postinumeron koodiUri"
      |            sv:
      |              type: string
      |              description: "Organisaation ruotsinkielinen postinumeron koodiUri"
      |            en:
      |              type: string
      |              description: "Organisaation englanninkielinen postinumeron koodiUri"
      |""".stripMargin

  def models = Seq(OrganisaatioModel, OrganisaatioHierarkiaModel, OrganisaatioYhteystiedotModel, OrganisaatioOsoiteModel)
}

case class Organisaatio(oid: String,
                        parentOidPath: String,
                        oppilaitostyyppi: Option[String] = None,
                        nimi: Kielistetty,
                        status: String,
                        kotipaikkaUri: Option[String] = None,
                        children: List[Organisaatio] = List(),
                        organisaatiotyypit: List[String] = List(),
                        tyypit: List[String] = List(),
                        yhteystiedot: Option[OrganisaatioYhteystiedot] = None) {
  def isOppilaitos: Boolean = (organisaatiotyypit ++ tyypit).contains("organisaatiotyyppi_02")
  def isPassivoitu: Boolean = status == "PASSIIVINEN"
}

case class OrganisaatioYhteystiedot(postiosoite: Option[OrganisaatioOsoite] = None,
                                    kayntiosoite: Option[OrganisaatioOsoite] = None,
                                    puhelinnumero: Kielistetty = Map(),
                                    sahkoposti: Kielistetty = Map(),
                                    wwwOsoite: Kielistetty = Map())

case class OrganisaatioOsoite(osoite: Kielistetty = Map(), postinumeroKoodiUri: Kielistetty = Map())

case class OrganisaatiopalveluOrganisaatio(oid: String,
                                           parentOidPath: String,
                                           oppilaitostyyppi: Option[String] = None,
                                           nimi: Kielistetty,
                                           status: String,
                                           kotipaikkaUri: Option[String] = None,
                                           children: List[Organisaatio] = List(),
                                           organisaatiotyypit: List[String] = List(),
                                           tyypit: List[String] = List(),
                                           yhteystiedot: List[OrganisaatiopalveluYhteystieto] = List()) {

  def toOsoite(osoiteTyyppi: String, organisaatiopalveluYhteystiedot: List[OrganisaatiopalveluYhteystieto]): OrganisaatioOsoite = {
    val osoitetyypit: List[String] = List(osoiteTyyppi, "ulkomainen_".concat(osoiteTyyppi))
    val osoite: Kielistetty = organisaatiopalveluYhteystiedot.filter(yt => yt.osoite.isDefined && yt.osoiteTyyppi.isDefined && osoitetyypit.contains(yt.osoiteTyyppi.get)).map(yt => toKieli(yt.kieli) -> yt.osoite.get).toMap
    val postinumero: Kielistetty = organisaatiopalveluYhteystiedot.filter(yt => yt.postinumeroUri.isDefined && yt.osoiteTyyppi.isDefined && osoitetyypit.contains(yt.osoiteTyyppi.get)).map(yt => toKieli(yt.kieli) -> yt.postinumeroUri.get).toMap
    OrganisaatioOsoite(osoite = osoite,
                       postinumeroKoodiUri = postinumero)
  }

  def toKieli(koodiUri: String): Kieli = {
    koodiUri.split("#").head match {
      case "kieli_en" => En
      case "kieli_sv" => Sv
      case _ => Fi
    }
  }
  def toYhteystiedot(organisaatiopalveluYhteystiedot: List[OrganisaatiopalveluYhteystieto]): OrganisaatioYhteystiedot = {
    val email: Kielistetty = organisaatiopalveluYhteystiedot.filter(_.email.isDefined).map(yt => toKieli(yt.kieli) -> yt.email.get).toMap
    val puhelinnumero: Kielistetty = organisaatiopalveluYhteystiedot.filter(yt => yt.numero.isDefined).map(yt => toKieli(yt.kieli) -> yt.numero.get).toMap
    val wwwOsoite: Kielistetty = organisaatiopalveluYhteystiedot.filter(_.www.isDefined).map(yt => toKieli(yt.kieli) -> yt.www.get).toMap
    val postiosoite = toOsoite("posti", organisaatiopalveluYhteystiedot)
    val kayntiosoite = toOsoite("kaynti", organisaatiopalveluYhteystiedot)
    OrganisaatioYhteystiedot(sahkoposti = email,
                             puhelinnumero = puhelinnumero,
                             wwwOsoite = wwwOsoite,
                             postiosoite = Some(postiosoite),
                             kayntiosoite = Some(kayntiosoite))
  }

  def toOrganisaatio(): Organisaatio = {
    val yhteystiedot = toYhteystiedot(this.yhteystiedot)
    Organisaatio(
      oid = oid,
      parentOidPath = parentOidPath,
      oppilaitostyyppi = oppilaitostyyppi,
      nimi = nimi,
      status = status,
      kotipaikkaUri = kotipaikkaUri,
      children = children,
      organisaatiotyypit = organisaatiotyypit,
      tyypit = tyypit,
      yhteystiedot = Some(yhteystiedot)
    )
  }
}

case class OrganisaatiopalveluYhteystieto(osoiteTyyppi: Option[String],
                                          tyyppi: Option[String],
                                          kieli: String,
                                          email: Option[String],
                                          postinumeroUri: Option[String],
                                          postitoimipaikka: Option[String],
                                          osoite: Option[String],
                                          numero: Option[String],
                                          www: Option[String])

case class OrganisaatioHierarkia(organisaatiot: List[Organisaatio])
