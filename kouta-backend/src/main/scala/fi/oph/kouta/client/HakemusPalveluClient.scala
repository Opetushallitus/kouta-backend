package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.service.HakemuspalveluHakukohdeInfo
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.util.{KoutaJsonFormats, MiscUtils}
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

case class HakukohdeInfo(applicationCount: Int) {
  def toHakemuspalveluHakukohdeInfo: HakemuspalveluHakukohdeInfo = {
    HakemuspalveluHakukohdeInfo(
      hakemustenMaara = applicationCount
    )
  }
}

case class AtaruQueryException(message: String, status: Int) extends RuntimeException(message)

trait HakemusPalveluClient extends KoutaJsonFormats {
  type HakukohdeApplicationCounts = Map[String, Int]
  def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult
  def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult

  def getHakukohdeInfo(hakukohdeOid: HakukohdeOid): HakukohdeInfo

  def getEnsisijainenApplicationCounts(hakuOid: HakuOid): HakukohdeApplicationCounts
}

object HakemusPalveluClient extends HakemusPalveluClient with CallerId with Logging {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private lazy val config        = KoutaConfigurationFactory.configuration.hakemuspalveluClientConfiguration
  private lazy val params = CasParams(
    urlProperties.url("hakemuspalvelu-service"),
    "auth/cas",
    config.username,
    config.password
  )

  private lazy val client = CasAuthenticatingClient(
    casClient =
      new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "ring-session"
  )

  implicit val ataruFormCache: Cache[String, Seq[AtaruForm]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  def defaultParse[R: Manifest](res: String): R = parse(res).extract[R]


  private def getFromHakemuspalvelu[R: Manifest](
      urlStr: String,
      timeout: Duration = Duration(5, TimeUnit.SECONDS),
      parseResponse: (String => R)
  ): R = {
    Uri
      .fromString(urlStr)
      .fold(
        Task.fail,
        url => {
          client.fetch(Request(method = GET, uri = url)) {
            case r if r.status.code == 200 =>
              r.bodyAsText.runLog
                .map(_.mkString)
                .map(parseResponse)
            case r =>
              r.bodyAsText.runLog
                .map(_.mkString)
                .flatMap(_ =>
                  Task.fail(AtaruQueryException(s"Failed to get '${urlStr}' from Hakemuspalvelu", r.status.code))
                )
          }
        }
      )
      .unsafePerformSyncAttemptFor(timeout)
      .fold(throw _, x => x)
  }

  private def getWithRetry[R: Manifest](doGet: () => R, errorMsg: String) = {
    try {
      doGet()
    } catch {
      case error: AtaruQueryException if retryStatusCodes.contains(error.status) =>
        logger.warn(s"${errorMsg}, retrying once...")
        try {
          doGet()
        } catch {
          case error: AtaruQueryException =>
            throw AtaruQueryException(
              s"${errorMsg} after retry, ${error.message}",
              error.status
            )
          case error: CasClientException => throw new RuntimeException(s"Authentication to CAS failed: $error")
        }
      case error: AtaruQueryException => throw error
      case error: CasClientException  => throw new RuntimeException(s"Authentication to CAS failed: $error")
    }
  }

  private def getExistingAtaruIds: Seq[AtaruForm] = {
    getWithRetry(
      doGet = () =>
        getFromHakemuspalvelu[Seq[AtaruForm]](
          urlProperties.url("hakemuspalvelu-service.forms"),
          parseResponse = parseForms
        ),
      errorMsg = "Failed to fetch forms from Hakemuspalvelu"
    )
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
    fromBoolean(
      ataruFormCache
        .get("ALL", _ => getExistingAtaruIds)
        .find((form: AtaruForm) => form.key.equals(ataruId.toString))
        .exists((form: AtaruForm) => formAllowsHakuTapa(form, hakutapaKoodiUri))
    )
  }

  def formAllowsHakuTapa(form: AtaruForm, hakutapaKoodiUri: Option[String]): Boolean =
    !form.formAllowsOnlyYhteisHaut || MiscUtils.isYhteishakuHakutapa(hakutapaKoodiUri)

  def parseForms(responseAsString: String): Seq[AtaruForm] =
    (parse(responseAsString) \\ "forms").extract[List[AtaruForm]].filter(_.isActive)

  override def getHakukohdeInfo(hakukohdeOid: HakukohdeOid): HakukohdeInfo = {
    getWithRetry(
      doGet = () =>
        getFromHakemuspalvelu[HakukohdeInfo](urlProperties.url("hakemuspalvelu-service.hakukohde-info", hakukohdeOid),
          parseResponse = defaultParse[HakukohdeInfo]),
      errorMsg = "Failed to fetch hakukohde information from Hakemuspalvelu"
    )
  }
  override def getEnsisijainenApplicationCounts(
      hakuOid: HakuOid
  ): HakukohdeApplicationCounts = {
    getWithRetry(
      doGet = () =>
        getFromHakemuspalvelu[HakukohdeApplicationCounts](
          urlProperties.url("hakemuspalvelu-service.haku-ensisijainen-counts", hakuOid),
          parseResponse = defaultParse[HakukohdeApplicationCounts]
        ),
      errorMsg = "Failed to fetch ensisijainen application counts from Hakemuspalvelu"
    )
  }
}
