package fi.oph.kouta.domain

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
      |          properties:
      |           fi:
      |             type: object
      |             $ref: '#/components/schemas/OrganisaatioYhteystieto'
      |             description: "Organisaation suomenkieliset yhteystiedot"
      |           sv:
      |             type: object
      |             $ref: '#/components/schemas/OrganisaatioYhteystieto'
      |             description: "Organisaation ruotsinkieliset yhteystiedot"
      |           en:
      |             type: object
      |             $ref: '#/components/schemas/OrganisaatioYhteystieto'
      |             description: "Organisaation englanninkieliset yhteystiedot"
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

  val OrganisaatioYhteystietoModel =
    """    OrganisaatioYhteystieto:
      |      type: object
      |      properties:
      |        email:
      |          type: string
      |          description: "Organisaation sähköpostiosoite"
      |        puhelinnumero:
      |          type: string
      |          description: "Organisaation puhelinnumero"
      |        wwwOsoite:
      |          type: string
      |          description: "Organisaation www-sivujen osoite"
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
      |          type: string
      |          description: "Osoite"
      |        postinumeroUri:
      |          type: string
      |          description: "Postinumeron koodiUri"
      |        postitoimipaikka:
      |          type: string
      |          description: "Postitoimipaikka"
      |""".stripMargin

  def models = Seq(OrganisaatioModel, OrganisaatioHierarkiaModel, OrganisaatioYhteystietoModel, OrganisaatioOsoiteModel)
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
                        yhteystiedot: Map[Kieli, OrganisaatioYhteystieto] = Map()) {
  def isOppilaitos: Boolean = (organisaatiotyypit ++ tyypit).contains("organisaatiotyyppi_02")
  def isPassivoitu: Boolean = status == "PASSIIVINEN"
}

case class OrganisaatioYhteystieto(email: Option[String],
                                   puhelinnumero: Option[String],
                                   wwwOsoite: Option[String],
                                   postiosoite: OrganisaatioOsoite,
                                   kayntiosoite: OrganisaatioOsoite)

case class OrganisaatioOsoite(osoite: Option[String],
                              postinumeroUri: Option[String],
                              postitoimipaikka: Option[String])

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

  def toOsoite(osoiteTyyppi: String, organisaatiopalveluYhteystiedotKielelle: List[OrganisaatiopalveluYhteystieto]): OrganisaatioOsoite = {
    val yhteystieto = organisaatiopalveluYhteystiedotKielelle.find(yt => yt.osoiteTyyppi.contains(osoiteTyyppi))
    OrganisaatioOsoite(osoite = yhteystieto.flatMap(_.osoite),
                       postinumeroUri = yhteystieto.flatMap(_.postinumeroUri),
                       postitoimipaikka = yhteystieto.flatMap(_.postitoimipaikka))
  }
  def toYhteystieto(kieli: Kieli, organisaatiopalveluYhteystiedot: List[OrganisaatiopalveluYhteystieto]): OrganisaatioYhteystieto = {
    val organisaatiopalveluYhteystiedotKielelle = organisaatiopalveluYhteystiedot.filter(_.kieli.startsWith("kieli_" + kieli.toString))
    val email: Option[String] = organisaatiopalveluYhteystiedotKielelle.find(_.email.isDefined).flatMap(_.email)
    val puhelinnumero: Option[String] = organisaatiopalveluYhteystiedotKielelle.find(yt => yt.numero.isDefined && yt.tyyppi.exists("numero".equals(_))).flatMap(_.numero)
    val wwwOsoite: Option[String] = organisaatiopalveluYhteystiedotKielelle.find(_.www.isDefined).flatMap(_.www)
    val postiosoite = toOsoite(if(kieli.equals(En)) "ulkomainen_posti" else "posti", organisaatiopalveluYhteystiedotKielelle)
    val kayntiosoite = toOsoite(if(kieli.equals(En)) "ulkomainen_kaynti" else "kaynti", organisaatiopalveluYhteystiedotKielelle)
    OrganisaatioYhteystieto(
      email = email,
      puhelinnumero = puhelinnumero,
      wwwOsoite = wwwOsoite,
      postiosoite = postiosoite,
      kayntiosoite = kayntiosoite
    )
  }

  def toOrganisaatio(): Organisaatio = {
    val yhteystiedot = Kieli.values.map(kieli => kieli -> toYhteystieto(kieli, this.yhteystiedot)).toMap
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
      yhteystiedot = yhteystiedot
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
