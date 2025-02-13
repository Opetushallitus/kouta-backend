package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.service.HakemuspalveluHakukohdeInfo
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.util.{KoutaJsonFormats, MiscUtils}
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, queryFailed}
import fi.oph.kouta.logging.Logging
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.parse

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
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

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("hakemuspalvelu-service"),
    callerId,
    callerId,
    "/auth/cas")
    .setJsessionName("ring-session").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  implicit val ataruFormCache: Cache[String, Seq[AtaruForm]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  def defaultParse[R: Manifest](res: String): R = parse(res).extract[R]

  private def getFromHakemuspalvelu[R: Manifest](
                                                  urlStr: String,
                                                  timeout: Duration = Duration(60, TimeUnit.SECONDS),
                                                  parseResponse: (String => R)
                                                ): R = {
    val request = new RequestBuilder().setMethod("GET").setUrl(urlStr).build
    val future = Future {
      casClient.executeBlocking(request)
    }.map {
      case r if r.getStatusCode == 200 =>
        parseResponse(r.getResponseBody)
      case r =>
        throw AtaruQueryException(s"Tietojen hakeminen hakemuspalvelusta epäonnistui: $r", r.getStatusCode)
    }
    Await.result(future, timeout)
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
        }
      case error: AtaruQueryException => throw error
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
      case t: Throwable =>
        logger.error(s"Failed to check if ataruId ${ataruId} exists in cache", t)
        queryFailed
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
        getFromHakemuspalvelu[HakukohdeInfo](
          urlProperties.url("hakemuspalvelu-service.hakukohde-info", hakukohdeOid),
          parseResponse = defaultParse[HakukohdeInfo]
        ),
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
