package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails}
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.parse

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

case class OrganisaatioHenkilo(organisaatioOid: String)

object KayttooikeusClient extends KayttooikeusClient

trait KayttooikeusClient extends HttpClient with CallerId with Logging {

  import org.json4s._

  private implicit val formats: DefaultFormats.type = DefaultFormats
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val config = KoutaConfigurationFactory.configuration.kayttooikeusClientConfiguration

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("kayttooikeus-service"),
    callerId,
    callerId,
    "j_spring_cas_security_check")
    .setJsessionName("JSESSIONID").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  implicit val kayttooikeusCache: Cache[UserOid, List[OrganisaatioHenkilo]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

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

  private def getOrganisaatiot(oid: UserOid): List[OrganisaatioHenkilo] =  {
    val url = urlProperties.url(s"kayttooikeus-service.organisaatiohenkilo", oid)

    /*
    try {
      Uri.fromString(url)
        .fold(Task.fail, url => {
          client.fetch(Request(method = GET, uri = url)) {
            case r if r.status.code == 200 =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .map(responseBody => {
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
        logger.error(s"Authentication to CAS failed: $error")
        throw error
    }
    */
    val request = new RequestBuilder().setMethod("GET").setUrl(url).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[List[OrganisaatioHenkilo]]
      case r =>
        throw new RuntimeException(s"Henkilön $oid käyttöoikeustietojen hakeminen käyttöoikeus-servicestä epäonnistui: $r")
    }
    Await.result(result, Duration(1, TimeUnit.MINUTES))
  }

  def getOrganisaatiotFromCache(oid: UserOid): List[OrganisaatioHenkilo] = {
    try {
      kayttooikeusCache.get(oid, oid => getOrganisaatiot(oid))
    } catch {
      case error: Throwable => throw error
    }
  }
}

case class KayttooikeusUserResp(authorities: List[GrantedAuthority], username: String)

case class GrantedAuthority(authority: String)
