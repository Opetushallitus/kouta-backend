package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasClientException, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.defaultClient
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode
import scalaz.concurrent.Task

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt}

object HakemusPalveluClient extends HakemusPalveluClient

case class AtaruForm(key: UUID, deleted: Option[Boolean], locked: Option[Boolean]) {
  def isActive(): Boolean =
    !deleted.getOrElse(false) && !locked.getOrElse(false)
}

trait HakemusPalveluClient extends HttpClient with CallerId with Logging {
  private implicit val formats: DefaultFormats.type = DefaultFormats
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val config = KoutaConfigurationFactory.configuration.hakemuspalveluClientConfiguration
  private lazy val params = CasParams(
    urlProperties.url("hakemuspalvelu-service"),
    "j_spring_cas_security_check",
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

  implicit val ataruIdCache = CaffeineCache[Seq[UUID]]

  def isExistingAtaruId(ataruId: UUID): Boolean = {
    var existingIdsInCache = ataruIdCache.get("")
    if (existingIdsInCache.isEmpty) {
      val url = urlProperties.url("hakemuspalvelu-service.forms")
      try {
        Uri.fromString(url)
          .fold(Task.fail, url => {
            client.fetch(Request(method = GET, uri = url)) {
              case r if r.status.code == 200 =>
                r.bodyAsText
                  .runLog
                  .map(_.mkString)
                  .map(responseBody => {
                    val ids = (parse(responseBody) \\ "forms").extract[List[AtaruForm]].filter(_.isActive()).map(_.key)
                    existingIdsInCache = Some(ids)
                    ataruIdCache.put("")(ids, Some(15.minutes))
                  })
              case r =>
                r.bodyAsText
                  .runLog
                  .map(_.mkString)
                  .flatMap(_ => Task.fail(new RuntimeException("Failed to fetch forms from Hakemuspalvelu")))
            }
          }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
      } catch {
        case error: CasClientException =>
          logger.error(s"Authentication to CAS failed: ${error}")
      }
    }
    existingIdsInCache.getOrElse(Seq()).contains(ataruId)
  }
}