package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.{KoutaJsonFormats, MiscUtils}
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, queryFailed}
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasClientException, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.defaultClient
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode
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

trait HakemusPalveluClient extends KoutaJsonFormats {
  def isExistingAtaruId(ataruId: UUID): ExternalQueryResult
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

  implicit val ataruFormCache = CaffeineCache[Seq[AtaruForm]]

  override def isExistingAtaruId(ataruId: UUID): ExternalQueryResult = {
    var existingFormsInCache = ataruFormCache.get("")
    var querySuccess = true
    if (existingFormsInCache.isEmpty) {
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
                    val forms = parseForms(responseBody)
                    existingFormsInCache = Some(forms)
                    ataruFormCache.put("")(forms, Some(15.minutes))
                  })
              case r =>
                r.bodyAsText
                  .runLog
                  .map(_.mkString)
                  .flatMap(_ => Task.fail(new RuntimeException(s"Failed to fetch forms from Hakemuspalvelu, statusCode: ${r.status.code}")))
            }
          }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
      } catch {
        case error: CasClientException =>
          querySuccess = false
          logger.error(s"Authentication to CAS failed: ${error}")
        case error: RuntimeException =>
          querySuccess = false
          logger.error(error.toString)
      }
    }
    querySuccess match {
      case true =>
        fromBoolean(existingFormsInCache.getOrElse(Seq()).exists((form: AtaruForm) => form.key.equals(ataruId.toString)))
      case _ => queryFailed
    }
  }

  override def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult = {
    val existingQuery = isExistingAtaruId(ataruId);
    if (existingQuery != itemFound) {
      return existingQuery
    }
    fromBoolean(ataruFormCache.get().get
      .find((form: AtaruForm) => form.key.equals(ataruId.toString))
      .map((form: AtaruForm) => formAllowsHakuTapa(form, hakutapaKoodiUri))
      .getOrElse(false))
  }

  def formAllowsHakuTapa(form: AtaruForm, hakutapaKoodiUri: Option[String]): Boolean =
    !form.formAllowsOnlyYhteisHaut || MiscUtils.isYhteishakuHakutapa(hakutapaKoodiUri)

  def parseForms(responseAsString: String): Seq[AtaruForm] =
    (parse(responseAsString) \\ "forms").extract[List[AtaruForm]].filter(_.isActive)
}
