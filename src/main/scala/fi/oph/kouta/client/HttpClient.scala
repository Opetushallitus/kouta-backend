package fi.oph.kouta.client

import java.util.{Map => JavaMap}
import java.util.NoSuchElementException

import fi.vm.sade.utils.http.DefaultHttpClient
import scalaj.http.HttpOptions._

trait HttpClient {
  private val DefaultConnTimeout = 30000
  private val DefaultReadTimeout = 120000

  private val DefaultOptions: Seq[HttpOption] = Seq(
    connTimeout(DefaultConnTimeout),
    readTimeout(DefaultReadTimeout)
  )

  private val HeaderCallerId            = ("Caller-id", "kouta-backend")
  private val HeaderClientSubSystemCode = ("clientSubSystemCode", "kouta-backend")

  def get[T](url: String, parse: (String) => T): T =
    DefaultHttpClient.httpGet(url, DefaultOptions:_*)
      .header(HeaderClientSubSystemCode._1, HeaderClientSubSystemCode._2)
      .header(HeaderCallerId._1, HeaderCallerId._2)
      .responseWithHeaders match {
      case (200, _, result) => parse(result)
      case (xxx, _, result) => handleError(url, xxx, result)
    }

  private def handleError(url: String, statusCode: Int, result: String) = (statusCode, result) match {
    case (404, result) => throw new NoSuchElementException(s"Url $url returned status code 404 $result")
    case (500, result) => throw new InternalError(s"Url $url returned status code 500 $result")
    case (xxx, result) => throw new InternalError(s"Url $url returned status code $xxx $result")
  }

  def toQueryParams(params: (String, String)*): JavaMap[String, String] = scala.collection.JavaConverters.mapAsJavaMap(Map(params:_*))
}
