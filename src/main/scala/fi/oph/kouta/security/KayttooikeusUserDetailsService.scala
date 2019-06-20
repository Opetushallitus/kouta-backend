package fi.oph.kouta.security

import fi.oph.kouta.client.HttpClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

object KayttooikeusUserDetailsService extends KayttooikeusUserDetailsService

trait KayttooikeusUserDetailsService extends HttpClient with Logging {

  import org.json4s._

  private implicit val formats: DefaultFormats.type = DefaultFormats
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def getUserByUsername(username: String): KayttooikeusUserDetails = {
    val url = urlProperties.url(s"kayttooikeus-service.userDetails.byUsername", username)

    val errorHandler = (_: String, status: Int, response: String) =>
      status match {
        case 404 => throw new AuthenticationFailedException(s"User not found with username: $username, got response $status $response")
        case _ => throw new InternalError(s"Failed to get username $username details, got response $status $response")
      }

    get(url, errorHandler) { response =>
      val kayttooikeusDto = parse(response).extract[KayttooikeusUserResp]
      KayttooikeusUserDetails(
        kayttooikeusDto.authorities
          .map(a => Authority(a.authority.replace("ROLE_", "")))
          .toSet,
        kayttooikeusDto.username
      )
    }
  }
}

case class KayttooikeusUserResp(authorities: List[GrantedAuthority], username: String)

case class GrantedAuthority(authority: String)
