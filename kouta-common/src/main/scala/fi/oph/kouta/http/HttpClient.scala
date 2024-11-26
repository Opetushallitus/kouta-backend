package fi.oph.kouta.http

import scalaj.http.{Http, HttpOptions}

trait HttpClient {
  def httpGet(url: String)(clientCallerId: String): HttpRequest
  def httpGet(url: String, options: HttpOptions.HttpOption*)(clientCallerId: String): HttpRequest
  def httpPost(url: String, data: Option[String])(clientCallerId: String)  : HttpRequest
  def httpPost(url: String, data: Option[String], options: HttpOptions.HttpOption*)(clientCallerId: String): HttpRequest
  def httpPut(url: String)(clientCallerId: String): HttpRequest
  def httpPut(url: String, options: HttpOptions.HttpOption*)(clientCallerId: String): HttpRequest
}

object DefaultHttpClient extends HttpClient {
  val defaultOptions: Seq[HttpOptions.HttpOption] = Seq(HttpOptions.connTimeout(10000), HttpOptions.readTimeout(60000))

  def httpGet(url: String)(clientCallerId: String): HttpRequest = {
    httpGet(url, defaultOptions: _*)(clientCallerId)
  }

  def httpGet(url: String, options: HttpOptions.HttpOption*)(clientCallerId: String): HttpRequest = {
    new DefaultHttpRequest(Http(url).method("GET").options(options).header("Caller-Id", clientCallerId))
  }

  def httpPost(url: String, data: Option[String])(clientCallerId: String): HttpRequest = {
    httpPost(url, data, defaultOptions: _*)(clientCallerId)
  }

  def httpPost(url: String, data: Option[String], options: HttpOptions.HttpOption*)(clientCallerId: String): HttpRequest = {
    val postRequest = Http(url).method("POST").options(options)
    data match {
      case None => new DefaultHttpRequest(postRequest.postForm.header("Caller-Id", clientCallerId).header("CSRF", clientCallerId).cookie("CSRF", clientCallerId))
      case Some(data) => new DefaultHttpRequest(postRequest.postData(data).header("Caller-Id", clientCallerId).header("CSRF", clientCallerId).cookie("CSRF", clientCallerId))
    }
  }

  def httpPut(url: String)(clientCallerId: String): HttpRequest = {
    httpPut(url, defaultOptions: _*)(clientCallerId)
  }

  def httpPut(url: String, options: HttpOptions.HttpOption*)(clientCallerId: String) : HttpRequest = {
    new DefaultHttpRequest(Http(url).method("PUT").options(options).header("Caller-Id", clientCallerId).header("CSRF", clientCallerId).cookie("CSRF", clientCallerId))
  }

}
