package fi.oph.kouta.security

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.http.DefaultHttpClient
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import scalaj.http.HttpOptions

import scala.util.Try
import scala.util.control.NonFatal
import scala.util.matching.Regex

object KayttooikeusUserDetailsService extends KayttooikeusUserDetailsService

trait KayttooikeusUserDetailsService extends Logging {

  import org.json4s._

  private implicit val formats: DefaultFormats.type = DefaultFormats

  def getUserByUsername(username: String): Either[Throwable, KayttooikeusUserDetails] = {
    val url = s"${KoutaConfigurationFactory.configuration.securityConfiguration.kayttooikeusUrl}$username"

    fetch(url) { response =>
      // response username field contains actually oid because of historical ldap reasons
      val koDto = parse(response).extract[KayttooikeusUserResp]
      KayttooikeusUserDetails(
        koDto.authorities
          .map(ga => Role(ga.role))
          .toSet,
        koDto.username
      )
    }.left.map {
      case e: IllegalArgumentException =>
        new AuthenticationFailedException(s"User not found with username: $username", e)
      case e: Exception =>
        new RuntimeException(s"Failed to get username $username details", e)
    }
  }

  private def fetch[T](url: String)(parse: String => T): Either[Throwable, T] = {
    Try(
      DefaultHttpClient.httpGet(url, HttpOptions.connTimeout(5000), HttpOptions.readTimeout(10000))
        .header("Caller-id", "kouta-backend")
        .responseWithHeaders match {
        case (200, _, resultString) =>
          Try(Right(parse(resultString))).recover {
            case NonFatal(e) => Left(new IllegalStateException(s"Parsing result $resultString of GET $url failed", e))
          }.get
        case (404, _, resultString) =>
          Left(new IllegalArgumentException(s"User not found"))
        case (responseCode, _, resultString) =>
          Left(new RuntimeException(s"GET $url failed with status $responseCode: $resultString"))
      }
    ).recover {
      case NonFatal(e) => Left(new RuntimeException(s"GET $url failed", e))
    }.get
  }

}

case class KayttooikeusUserResp(authorities: List[GrantedAuthority], username: String)

case class GrantedAuthority(authority: String) {

  lazy val role: String = GrantedAuthority.organisaatioRegex
    .replaceAllIn(authority, "")
    .replace("ROLE_", "")

  lazy val organisaatio: Option[String] = GrantedAuthority.organisaatioRegex.findFirstIn(authority).map(_.filterNot(_ == '_'))
}

object GrantedAuthority {
  val organisaatioRegex: Regex = """_1\.2\.246\.562\.10.[\d]+$""".r
}
