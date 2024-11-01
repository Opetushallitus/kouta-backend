package fi.oph.kouta.http

import fi.oph.kouta.logging.Logging

import scala.collection.immutable.HashMap
import scalaj.http.HttpResponse

trait HttpRequest{
  def responseWithHeaders(): (Int, Map[String, Seq[String]], String)
  def response(): Option[String]
  def param(key: String, value: String): HttpRequest
  def header(key: String, value: String): HttpRequest
  def getUrl: String
}

class DefaultHttpRequest(private val request: scalaj.http.HttpRequest) extends HttpRequest with Logging {
  def param(key: String, value: String) = {
    new DefaultHttpRequest(request.param(key, value))
  }

  def header(key: String, value: String) = {
    new DefaultHttpRequest(request.header(key, value))
  }

  def responseWithHeaders(): (Int, Map[String, Seq[String]], String) = {
    try {
      val response: HttpResponse[String] = request.asString
      (response.code, response.headers, response.body)
    } catch {
      case t: Throwable =>
        logUnexpectedError(t)
        (500, HashMap(), t.toString)
    }
  }

  def response(): Option[String] = {
    try {
      val response = request.asString
      if(response.isError || response.isRedirect) {
        logger.warn("Unexpected status code ${response.code} from {request.method} to ${request.url}" )
        None
      }
      else {
        Some(response.body)
      }
    } catch {
      case t: Throwable =>
        logUnexpectedError(t)
        None
    }
  }

  private def logUnexpectedError(t: Throwable) {
    logger.error("Unexpected error from " + request.method + " to " + request.url + " : " + t, t)
  }

  def getUrl = request.url
}
