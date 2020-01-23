package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.keyword._
import fi.oph.kouta.domain.{Fi, Kieli}
import fi.oph.kouta.service.KeywordService
import org.scalatra.Ok

import scala.util.Try

class AsiasanaServlet(keywordService: KeywordService) extends KeywordServlet {

  def this() = this(KeywordService)

  registerPath("/asiasana/search/{term}",
    """    get:
      |      summary: Hakee asiasanoja annetulla hakutermillä
      |      operationId: Hae asiasanoja
      |      description: Hakee asiasanoja annetulla hakutermillä
      |      tags:
      |        - Asiasana
      |      parameters:
      |        - in: path
      |          name: term
      |          schema:
      |            type: string
      |          required: true
      |          description: hakutermi
      |          example: robo
      |        - in: query
      |          name: kieli
      |          description: Haettavan asiasanan kieli
      |          schema:
      |            type: string
      |            enum:
      |              - fi
      |              - sv
      |              - en
      |        - in: query
      |          name: limit
      |          description: Palautettavien asiasanojen maksimimäärä
      |          schema:
      |            type: integer
      |          default: 15
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: string
      |                  example:
      |                    - robotiikka
      |                    - robotti
      |
      |""".stripMargin)
  get("/search/:term") {

    implicit val authenticated: Authenticated = authenticate

    Ok(keywordService.search(parseAsiasanaSearch()))
  }

  registerPath("/asiasana/",
    """    post:
      |      summary: Tallenna asiasanoja
      |      operationId: Tallenna asiasanoja
      |      description: Tallenna asiasanoja
      |      tags:
      |        - Asiasana
      |      parameters:
      |        - in: query
      |          name: kieli
      |          description: Tallennettavan asiasanan kieli
      |          schema:
      |            type: string
      |            enum:
      |              - fi
      |              - sv
      |              - en
      |      requestBody:
      |        description: Lista tallennettavia asiasanoja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example:
      |                  - biologia
      |                  - genetiikka
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate

    val kieli = parseKieliParam("kieli", Fi)
    Ok(keywordService.store(Asiasana, bodyToKeywords(kieli)))
  }

  private def parseAsiasanaSearch(): KeywordSearch = KeywordSearch(
    params("term"),
    parseKieliParam("kieli", Fi),
    Asiasana,
    parseIntParam("limit", 15))
}

class AmmattinimikeServlet(keywordService: KeywordService) extends KeywordServlet {

  def this() = this(KeywordService)

  registerPath("/ammattinimike/search/{term}",
    """    get:
      |      summary: Hakee ammattinimikkeitä annetulla hakutermillä
      |      operationId: Hae ammattinimikkeita
      |      description: Hakee ammattinimikkeitä annetulla hakutermillä
      |      tags:
      |        - Ammattinimike
      |      parameters:
      |        - in: path
      |          name: term
      |          schema:
      |            type: string
      |          required: true
      |          description: hakutermi
      |          example: kone
      |        - in: query
      |          name: kieli
      |          description: Haettavan ammattinimikkeen kieli
      |          schema:
      |            type: string
      |            enum:
      |              - fi
      |              - sv
      |              - en
      |        - in: query
      |          name: limit
      |          description: Palautettavien ammattinimikkeiden maksimimäärä
      |          schema:
      |            type: integer
      |          default: 15
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: string
      |                  example:
      |                    - koneinsinööri
      |                    - koneistaja
      |
      |""".stripMargin)
  get("/search/:term") {

    implicit val authenticated: Authenticated = authenticate

    Ok(keywordService.search(parseAmmattinimikeSearch()))
  }

  registerPath("/ammattinimike/",
    """    post:
      |      summary: Tallenna ammattinimikkeitä
      |      operationId: Tallenna ammattinimikkeita
      |      description: Tallenna ammattinimikkeitä
      |      tags:
      |        - Ammattinimike
      |      parameters:
      |        - in: query
      |          name: kieli
      |          description: Tallennettavan ammattinimikkeen kieli
      |          schema:
      |            type: string
      |            enum:
      |              - fi
      |              - sv
      |              - en
      |      requestBody:
      |        description: Lista tallennettavia ammattinimikkeitä
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example:
      |                  - lähihoitaja
      |                  - jalkahoitaja
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate

    val kieli = parseKieliParam("kieli", Fi)
    Ok(keywordService.store(Ammattinimike, bodyToKeywords(kieli)))
  }

  def parseAmmattinimikeSearch(): KeywordSearch = KeywordSearch(
    params("term"),
    parseKieliParam("kieli", Fi),
    Ammattinimike,
    parseIntParam("limit", 15))
}

sealed trait KeywordServlet extends KoutaServlet {
  def parseKieliParam(name: String, default: Kieli = Fi): Kieli =
    params.get(name).map(Kieli.withName).getOrElse(default)

  def parseIntParam(name: String, default: Int = 15): Int =
    params.get(name).flatMap(l => Try(l.toInt).toOption).getOrElse(default)

  def bodyToKeywords(kieli: Kieli) =
    parsedBody.extract[List[String]].map(Keyword(kieli, _))
}
