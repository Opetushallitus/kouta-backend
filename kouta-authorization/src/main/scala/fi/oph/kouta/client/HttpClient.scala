package fi.oph.kouta.client

import java.util.{Map => JavaMap}

import fi.vm.sade.utils.http.DefaultHttpClient
import org.json4s.jackson.Serialization.write
import org.json4s.Formats
import scalaj.http.HttpOptions._

trait HttpClient {
  def callerId: String

  private val DefaultConnTimeout = 30000
  private val DefaultReadTimeout = 120000

  private def defaultOptions(doFollowRedirects: Boolean = false): Seq[HttpOption] = Seq(
    connTimeout(DefaultConnTimeout),
    readTimeout(DefaultReadTimeout),
    followRedirects(doFollowRedirects)
  )

  //TODO: Caller-Id konfiguraatiosta, että tämän luokan perijä voi käyttää omaa tunnustaan
  private val HeaderClientSubSystemCode = ("clientSubSystemCode", callerId)
  private val HeaderContentTypeJson = ("Content-Type", "application/json; charset=utf-8")
  private val HeaderAcceptJson = ("Accept", "application/json")

  def get[T](url: String, errorHandler: (String, Int, String) => Nothing = defaultErrorHandler, followRedirects: Boolean = false)(parse: String => T): T =
    DefaultHttpClient.httpGet(url, defaultOptions(followRedirects):_*)(callerId)
      .header(HeaderClientSubSystemCode._1, HeaderClientSubSystemCode._2)
      .responseWithHeaders match {
      case (200, _, response) => parse(response)
      case (xxx, _, response) => errorHandler(url, xxx, response)
    }

  def post[B <: AnyRef, T](url: String, body: B, errorHandler: (String, Int, String) => Nothing = defaultErrorHandler, followRedirects: Boolean = false)(parse: String => T)(implicit jsonFormats: Formats): T =
    DefaultHttpClient.httpPost(url, Some(write[B](body)), defaultOptions(followRedirects):_*)(callerId)
      .header(HeaderClientSubSystemCode._1, HeaderClientSubSystemCode._2)
      .header(HeaderContentTypeJson._1, HeaderContentTypeJson._2)
      .header(HeaderAcceptJson._1, HeaderAcceptJson._2)
      .responseWithHeaders match {
      case (200, _, response) => parse(response)
      case (xxx, _, response) => errorHandler(url, xxx, response)
    }

  private def defaultErrorHandler(url: String, statusCode: Int, response: String) =
    throw new RuntimeException(s"Url $url returned status code $statusCode $response")

  def toQueryParams(params: (String, String)*): JavaMap[String, String] = scala.collection.JavaConverters.mapAsJavaMap(Map(params:_*))
}
