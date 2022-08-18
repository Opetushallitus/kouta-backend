package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
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

case class AtaruForm(key: UUID, deleted: Option[Boolean], locked: Option[Boolean]) {
  def isActive(): Boolean =
    !deleted.getOrElse(false) && !locked.getOrElse(false)
}

trait HakemusPalveluClient extends KoutaJsonFormats {
  def isExistingAtaruId(ataruId: UUID): Boolean
}
object HakemusPalveluClient extends HakemusPalveluClient with HttpClient with CallerId with Logging {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val config = KoutaConfigurationFactory.configuration.hakemuspalveluClientConfiguration
  private lazy val params = CasParams(
    urlProperties.url("hakemuspalvelu-service"),
    "auth/cas",
    config.username,
    config.password
  )

  private lazy val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "ring-session"
  )

  implicit val ataruIdCache = CaffeineCache[Seq[UUID]]

  override def isExistingAtaruId(ataruId: UUID): Boolean = {
    logger.info("Checking AtaruId: " + ataruId)
    var existingIdsInCache = ataruIdCache.get("")
    if (existingIdsInCache.isEmpty) {
      val url = urlProperties.url("hakemuspalvelu-service.forms")
      logger.info("Fetching from url " + url)
      try {
        Uri.fromString(url)
          .fold(Task.fail, url => {
            client.fetch(Request(method = GET, uri = url)) {
              case r if r.status.code == 200 =>
                r.bodyAsText
                  .runLog
                  .map(_.mkString)
                  .map(responseBody => {
                    val ids = parseIds(responseBody)
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
    logger.info("Existing AtaruIDs: " + existingIdsInCache.getOrElse(Seq()))
    existingIdsInCache.getOrElse(Seq()).contains(ataruId)
  }

  def parseIds(responseAsString: String): List[UUID] =
    (parse(responseAsString) \\ "forms").extract[List[AtaruForm]].filter(_.isActive()).map(_.key)
}
