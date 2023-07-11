package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.KoodistoElement
import fi.oph.kouta.service.KoodistoService
import org.scalatra.{BadRequest, InternalServerError, Ok}
import scala.util.matching.Regex

class KoodistoServlet(koodistoService: KoodistoService) extends KoutaServlet {

  def this() = this(KoodistoService)

  val tyyppiPattern: Regex = "^[A-Za-z0-9]+_[0-9]+$".r

  registerPath("/koodisto/koulutukset/{ylakoodi}",
    """    get:
      |      summary: Hae koulutukset yläkoodin perusteella
      |      operationId: Hae koulutukset
      |      description: Hakee voimassa olevat koulutukset ja niiden metadatan versioimattoman yläkoodin perusteella. Väliotsikkokoodit (päättyvät "00") on suodatettu pois.
      |      tags:
      |        - Koodisto
      |      parameters:
      |        - in: path
      |          name: tyyppi
      |          schema:
      |            type: string
      |          required: true
      |          description: yläkoodin koodiUri ilman versiotietoa
      |          example: koulutustyyppi_06
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: object
      |                  properties:
      |                    koodiUri:
      |                      type: string
      |                      description: koodiUri.
      |                      example: "koulutusTyyppi_25"
      |                    koodiArvo:
      |                      type: string
      |                      description: Koodiurin numeerinen tunniste
      |                    versio:
      |                      type: number
      |                      description: Koodin versio
      |                      example: 1
      |                    koodisto:
      |                      type: object
      |                      description: Koodisto johon koodi kuuluu
      |                      properties:
      |                        koodistoUri:
      |                          type: string
      |                          description: Koodiston uri, tässä tapauksessa "koulutus"
      |                    voimassaLoppuPvm:
      |                      type: string
      |                      description: Päivämäärä johon asti koodi on voimassa
      |                      example: "2015-04-24"
      |                    metadata:
      |                      type: array
      |                      description: Koodin metadata
      |                      items:
      |                        type: object
      |                        properties:
      |                          nimi:
      |                            type: string
      |                            description: Koodin nimi
      |                            example: "Insinööri (AMK), bio- ja elintarviketekniikka"
      |                          kieli:
      |                            type: string
      |                            description: Metadataelementin kielitunniste
      |                            example: "FI"
      |""".stripMargin)
  get("/koulutukset/:ylakoodi") {
    authenticate() // vain autentikoidut käyttäjät saavat ladata koulutuksia, roolilla ei ole väliä

    val ylakoodi = params.get("ylakoodi").get
    tyyppiPattern.findFirstIn(ylakoodi) match {
      case Some(tutkintoTyyppi) => koodistoService.getLisattavatKoulutukset(tutkintoTyyppi) match {
        case Right(koulutukset: Seq[KoodistoElement]) => Ok(koulutukset)
        case Left(error: Throwable) =>
          logger.error(s"Error fetching koulutukset for yläkoodi $tutkintoTyyppi", error)
          InternalServerError("500 Internal Server Error")
      }
      case None => BadRequest(s"parameter $ylakoodi is not a valid non-versioned yläkoodi")
    }
  }

  registerPath("/valintakokeentyypit/",
    """    get:
      |      summary: Hakee valintakokeentyypit, kaikki tai suodattaa niitä koodirelaatioiden kautta annetuilla parametreilla
      |      operationId: Hae valintakokeentyypit
      |      description: Hakee voimassa olevat valintakokeentyypit ja niiden metadatat. Suodatetaan yläkoodirelaatioden kautta annetuilla parametreilla. Jos valintakokeentyyppikoodilla ei ole ollenkaan relaatiota yläkoodiston koodiin, niin silloin se on mukana palautettavassa listassa.
      |      tags:
      |        - Koodisto
      |      parameters:
      |        - in: query
      |          name: koulutuskoodi
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: koulutuskoodit, millä rajataan valintakokeentyyppejä
      |          example: ["koulutus_354345"]
      |        - in: query
      |          name: hakutapakoodi
      |          schema:
      |            type: string
      |          required: false
      |          description: hakutapakoodi, millä rajataan valintakokeentyyppejä
      |          example: hakutapa_03
      |        - in: query
      |          name: "haunkohdejoukkokoodi"
      |          schema:
      |            type: string
      |          required: false
      |          description: haunkohdejoukkokoodi, millä rajataan valintakokeentyyppejä
      |          example: "haunkohdejoukko_23"
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: object
      |                  properties:
      |                    koodiUri:
      |                      type: string
      |                      description: koodiUri.
      |                      example: "koulutusTyyppi_25"
      |                    koodiArvo:
      |                      type: string
      |                      description: Koodiurin numeerinen tunniste
      |                    versio:
      |                      type: number
      |                      description: Koodin versio
      |                      example: 1
      |                    koodisto:
      |                      type: object
      |                      description: Koodisto johon koodi kuuluu
      |                      properties:
      |                        koodistoUri:
      |                          type: string
      |                          description: Koodiston uri, tässä tapauksessa "koulutus"
      |                    voimassaLoppuPvm:
      |                      type: string
      |                      description: Päivämäärä johon asti koodi on voimassa
      |                      example: "2015-04-24"
      |                    metadata:
      |                      type: array
      |                      description: Koodin metadata
      |                      items:
      |                        type: object
      |                        properties:
      |                          nimi:
      |                            type: string
      |                            description: Koodin nimi
      |                            example: "Insinööri (AMK), bio- ja elintarviketekniikka"
      |                          kieli:
      |                            type: string
      |                            description: Metadataelementin kielitunniste
      |                            example: "FI"
      |""".stripMargin)
  get("/koulutukset/valintakokeentyypit/") {
    authenticate()

    val koulutuskoodit = multiParams.get("koulutuskoodi")
    val hakutapakoodi = params.get("hakutapakoodi")
    val haunkohdejoukkokoodi = params.get("haunkohdejoukkokoodi")
    koodistoService.getValintakokeenTyypit(koulutuskoodit.getOrElse(Seq.empty), hakutapakoodi, haunkohdejoukkokoodi) match {
      case Right(valintakokeenTyypit: Seq[KoodistoElement]) => Ok(valintakokeenTyypit)
      case Left(error: Throwable) =>
        logger.error(s"Error fetching valintakokeentyypit", error)
        InternalServerError("500 Internal Server Error")
    }
  }
}
