package fi.oph.kouta.servlet

import fi.oph.kouta.domain.keyword._
import fi.oph.kouta.domain.{Fi, Kieli}
import fi.oph.kouta.service.KeywordService._
import org.scalatra.{Ok}
import org.scalatra.swagger.Swagger

import scala.util.Try

class AsiasanaServlet(implicit val swagger:Swagger) extends KeywordServlet {
  override val modelName: String = "Asiasana"
  override val applicationDescription = "Asiasanojen APIt"

  get("/search/:term", operation(apiOperation[List[String]]("Hae asiasanoja")
    tags modelName
    summary "Hae asiasanoja"
    parameter pathParam[String]("term").description("Hakutermi")
    parameter queryParam[Kieli]("kieli").description("Kieli")
    parameter queryParam[Int]("limit").description("Asiasanojen määrä (default = 15)"))) {

    Ok(search(parseAsiasanaSearch()))
  }

  post("/", operation(apiOperation[Int]("Tallenna asiasanoja")
    tags modelName
    summary "Tallenna asiasanoja"
    parameter bodyParam[List[String]]
    parameter queryParam[Kieli]("kieli").description("Kieli"))) {

    val kieli = parseKieliParam("kieli", Fi)
    Ok(store(Asiasana, bodyToKeywords(kieli)))
  }

  private def parseAsiasanaSearch(): KeywordSearch = KeywordSearch(
    params("term"),
    parseKieliParam("kieli", Fi),
    Asiasana,
    parseIntParam("limit", 15))
}

class AmmattinimikeServlet(implicit val swagger:Swagger) extends KeywordServlet {
  override val modelName: String = "Ammattinimike"
  override val applicationDescription = "Ammattinimikkeiden APIt"

  get("/search/:term", operation(apiOperation[List[String]]("Hae ammattinimikkeitä")
    tags modelName
    summary "Hae ammattinimikkeitä"
    parameter pathParam[String]("term").description("Hakutermi")
    parameter queryParam[Kieli]("kieli").description("Kieli")
    parameter queryParam[Int]("limit").description("Asiasanojen määrä (default = 15)"))) {

    Ok(search(parseAmmattinimikeSearch()))
  }

  post("/", operation(apiOperation[Int]("Tallenna ammattinimikkeitä")
    tags modelName
    summary "Tallenna ammattinimikkeitä"
    parameter bodyParam[List[String]]
    parameter queryParam[Kieli]("kieli").description("Kieli"))) {

    val kieli = parseKieliParam("kieli", Fi)
    Ok(store(Ammattinimike, bodyToKeywords(kieli)))
  }

  def parseAmmattinimikeSearch(): KeywordSearch = KeywordSearch(
    params("term"),
    parseKieliParam("kieli", Fi),
    Ammattinimike,
    parseIntParam("limit", 15))
}

sealed trait KeywordServlet extends KoutaServlet {

  def parseKieliParam(name:String, default:Kieli = Fi): Kieli =
    params.get(name).map(Kieli.withName).getOrElse(default)

  def parseIntParam(name:String, default:Int = 15): Int =
    params.get(name).flatMap(l => Try(l.toInt).toOption).getOrElse(default)

  def bodyToKeywords(kieli:Kieli) =
    parsedBody.extract[List[String]].map((kieli, _))
}
