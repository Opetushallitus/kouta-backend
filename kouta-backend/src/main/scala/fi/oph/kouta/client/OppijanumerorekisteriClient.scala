package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasClientException, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.client.blaze.defaultClient
import org.http4s.{Request, Uri}
import org.json4s.jackson.JsonMethods._
import scalaz.concurrent.Task

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

case class Henkilo(kutsumanimi: Option[String],
                   sukunimi: Option[String],
                   etunimet: Option[String])

trait OppijanumerorekisteriClient {
  def getHenkilöFromCache(oid: UserOid): Henkilo
}

object OppijanumerorekisteriClient
  extends OppijanumerorekisteriClient
    with CallerId
    with Logging
    with KoutaJsonFormats {
  private val config = KoutaConfigurationFactory.configuration.oppijanumerorekisteriClientConfiguration
  private val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private val params = CasParams(
    urlProperties.url("oppijanumerorekisteri-service"),
    config.username,
    config.password
  )

  private val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "JSESSIONID"
  )

  implicit val OppijanumeroCache: Cache[UserOid, Henkilo] = Scaffeine()
    .expireAfterWrite(60.minutes)
    .build()

  private def getHenkilö(oid: UserOid): Henkilo = {
    val oppijanumerorekisteriUrl: String =
      urlProperties.url(
        "oppijanumerorekisteri-service.henkilo",
        oid
      )

    try {
      Uri.fromString(oppijanumerorekisteriUrl)
        .fold(Task.fail, url => {
          client.fetch(Request(method = GET, uri = url)) {
            case r if r.status.code == 200 =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .map(responseBody => {
                  parse(responseBody).extract[Henkilo]
                })
            case r =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .flatMap(_ => Task.fail(new RuntimeException(s"Failed to get henkilö $oid: ${r.toString()}")))
          }
        }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
    } catch {
      case error: CasClientException =>
        logger.error(s"Authentication to CAS failed: ${error}")
        Henkilo(kutsumanimi = None, sukunimi = None, etunimet = None)
    }
  }

  override def getHenkilöFromCache(oid: UserOid): Henkilo = {
    OppijanumeroCache.get(oid, oid => getHenkilö(oid))
  }
}
