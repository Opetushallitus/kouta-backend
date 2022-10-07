package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.{KoutaJsonFormats, MiscUtils}
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, queryFailed}
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

case class AtaruFormProperties(allowOnlyYhteisHaut: Option[Boolean]) {
  def allowsOnlyYhteisHaut: Boolean = allowOnlyYhteisHaut.getOrElse(false);
}

case class AtaruForm(key: String, deleted: Option[Boolean], properties: Option[AtaruFormProperties]) {
  def isActive: Boolean = !deleted.getOrElse(false)

  def formAllowsOnlyYhteisHaut: Boolean = properties.exists(_.allowsOnlyYhteisHaut);
}

case class AtaruQueryException(message: String, status: Int) extends RuntimeException(message)

trait HakemusPalveluClient extends KoutaJsonFormats {
  def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult
  def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult
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

  implicit val ataruFormCache: Cache[String, Seq[AtaruForm]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  private def getExistingAtaruIdsFromAtaruService: Seq[AtaruForm] = {
    Uri.fromString(urlProperties.url("hakemuspalvelu-service.forms"))
      .fold(Task.fail, url => {
        client.fetch(Request(method = GET, uri = url)) {
          case r if r.status.code == 200 =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .map(responseBody => {
                parseForms(responseBody)
              })
          case r =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .flatMap(_ => Task.fail(AtaruQueryException(s"Failed to fetch forms from Hakemuspalvelu", r.status.code)))
        }
      }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
  }

  private def getExistingAtaruIds: Seq[AtaruForm] = {
    try {
      getExistingAtaruIdsFromAtaruService
    } catch {
      case error: AtaruQueryException if retryStatusCodes.contains(error.status) =>
        logger.warn(s"Failed to fetch forms from Hakemuspalvelu, retrying once...")
        try {
          getExistingAtaruIdsFromAtaruService
        } catch {
          case error: AtaruQueryException => throw new AtaruQueryException(s"Failed to fetch forms from Hakemuspalvelu after retry, ${error.message}", error.status)
          case error: CasClientException => throw new RuntimeException(s"Authentication to CAS failed: $error")
        }
      case error: AtaruQueryException => throw error
      case error: CasClientException => throw new RuntimeException(s"Authentication to CAS failed: $error")
    }
  }

  override def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult = {
    try {
    val existingFormsInCache = ataruFormCache.get("ALL", _ => getExistingAtaruIds)
      fromBoolean(existingFormsInCache.exists((form: AtaruForm) => form.key.equals(ataruId.toString)))
  } catch {
      case _: Throwable => queryFailed
    }
  }

  override def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult = {
    val existingQuery = isExistingAtaruIdFromCache(ataruId);
    if (existingQuery != itemFound) {
      return existingQuery
    }
    fromBoolean(ataruFormCache.get("ALL", _ => getExistingAtaruIds)
      .find((form: AtaruForm) => form.key.equals(ataruId.toString)).exists((form: AtaruForm) => formAllowsHakuTapa(form, hakutapaKoodiUri)))
  }

  def formAllowsHakuTapa(form: AtaruForm, hakutapaKoodiUri: Option[String]): Boolean =
    !form.formAllowsOnlyYhteisHaut || MiscUtils.isYhteishakuHakutapa(hakutapaKoodiUri)

  def parseForms(responseAsString: String): Seq[AtaruForm] =
    (parse(responseAsString) \\ "forms").extract[List[AtaruForm]].filter(_.isActive)
}
