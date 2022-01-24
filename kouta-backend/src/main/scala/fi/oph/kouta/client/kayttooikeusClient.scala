package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails}
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasClientException, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.client.blaze.defaultClient
import org.http4s.{Request, Uri}
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeSync
import scalacache.modes.sync._
import scalaz.concurrent.Task

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

case class OrganisaatioHenkilo(organisaatioOid: String)

object KayttooikeusClient extends KayttooikeusClient

trait KayttooikeusClient extends HttpClient with CallerId with Logging {

  import org.json4s._

  private implicit val formats: DefaultFormats.type = DefaultFormats
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val config = KoutaConfigurationFactory.configuration.oppijanumerorekisteriClientConfiguration
  private lazy val params = CasParams(
    urlProperties.url("kayttooikeus-service"),
    config.username,
    config.password
  )

  private lazy val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "JSESSIONID"
  )

  implicit val KayttooikeusCache = CaffeineCache[List[OrganisaatioHenkilo]]

  def getUserByUsername(username: String): KayttooikeusUserDetails = {
    val url = urlProperties.url(s"kayttooikeus-service.userDetails.byUsername", username)

    val errorHandler = (_: String, status: Int, response: String) =>
      status match {
        case 404 => throw new AuthenticationFailedException(s"User not found with username: $username, got response $status $response")
        case _ => throw new RuntimeException(s"Failed to get username $username details, got response $status $response")
      }

    get(url, errorHandler, followRedirects = true) { response =>
      val kayttooikeusDto = parse(response).extract[KayttooikeusUserResp]
      KayttooikeusUserDetails(
        kayttooikeusDto.authorities
          .map(a => Authority(a.authority.replace("ROLE_", "")))
          .toSet,
        kayttooikeusDto.username
      )
    }
  }

  def getOrganisaatiot(oid: UserOid): List[OrganisaatioHenkilo] = memoizeSync[List[OrganisaatioHenkilo]](Some(60.minutes)) {
    val url = urlProperties.url(s"kayttooikeus-service.organisaatiohenkilo", oid)
    println(url)

    try {
      Uri.fromString(url)
        .fold(Task.fail, url => {
          client.fetch(Request(method = GET, uri = url)) {
            case r if r.status.code == 200 =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .map(responseBody => {
                  println(responseBody)
                  parse(responseBody).extract[List[OrganisaatioHenkilo]]
                })
            case r =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .flatMap(_ => Task.fail(new RuntimeException(s"Failed to fetch organisaatiohenkilot for $oid: ${r.toString()}")))
          }
        }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
    } catch {
      case error: CasClientException =>
        logger.error(s"Authentication to CAS failed: ${error}")
        List()
    }
  }
}

case class KayttooikeusUserResp(authorities: List[GrantedAuthority], username: String)

case class GrantedAuthority(authority: String)
