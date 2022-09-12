package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, queryFailed}
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasClientException, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.client.blaze.defaultClient
import org.http4s.{Request, Uri}
import org.json4s.jackson.JsonMethods.parse
import scalaz.concurrent.Task

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Success, Try}

case class AtaruForm(key: String, deleted: Option[Boolean]) {
  def isActive(): Boolean = !deleted.getOrElse(false)
}

trait HakemusPalveluClient extends KoutaJsonFormats {
  def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult
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

  implicit val ataruIdCache: Cache[String, Seq[String]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  private def getExistingAtaruIds(): Seq[String] = {
    try {
      Uri.fromString(urlProperties.url("hakemuspalvelu-service.forms"))
        .fold(Task.fail, url => {
          client.fetch(Request(method = GET, uri = url)) {
            case r if r.status.code == 200 =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .map(responseBody => {
                  val ids = parseIds(responseBody)
                  ids
                })
            case r =>
              r.bodyAsText
                .runLog
                .map(_.mkString)
                .flatMap(_ => Task.fail(new RuntimeException(s"Failed to fetch forms from Hakemuspalvelu, statusCode: ${r.status.code}")))
          }
        }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
    } catch {
      case error: CasClientException => throw new RuntimeException(s"Authentication to CAS failed: $error")
    }
  }

  override def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult = {
    Try[Seq[String]] {
    ataruIdCache.get("ALL", _ => getExistingAtaruIds())
  } match {
      case Success(existingIdsInCache) =>
        fromBoolean(existingIdsInCache.contains(ataruId.toString))
      case _ => queryFailed
    }
  }

  def parseIds(responseAsString: String): List[String] =
    (parse(responseAsString) \\ "forms").extract[List[AtaruForm]].filter(_.isActive()).map(_.key)
}
