package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, itemNotFound, queryFailed}
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode

import java.time.ZonedDateTime
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object KoulutusKoodiClient extends KoulutusKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoulutusKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val commonKoodiUriCache   = CaffeineCache[Seq[KoodiUri]]
  implicit val koodiuriVersionCache  = CaffeineCache[Int]
  implicit val koulutusKoodiUriCache = CaffeineCache[Seq[KoodiUri]]

  case class CodeElementWithVersion(koodiUri: String, versio: Int)

  def getKoodiUriWithLatestVersion(koodiUriWithoutVersion: String): String = {
    var versio = koodiuriVersionCache.get(koodiUriWithoutVersion)
    if (versio.isEmpty) {
      Try[Int] {
        get(urlProperties.url("koodisto-service.latest-koodiuri", koodiUriWithoutVersion), errorHandler, followRedirects = true) {
          response => parse(response).extract[CodeElementWithVersion].versio
        }
      } match {
        case Success(version) =>
          versio = Some(version)
          koodiuriVersionCache.put(koodiUriWithoutVersion)(versio.get, Some(15.minutes))
        case Failure(exp: KoodistoQueryException) =>
          throw new RuntimeException(s"Failed to get koodiuri-version from koodisto for $koodiUriWithoutVersion, got response ${exp.status} ${exp.message}")
        case Failure(exp: Throwable) =>
          throw new RuntimeException(s"Failed to get koodiuri-version from koodisto for $koodiUriWithoutVersion, got response ${exp.getMessage()}")
      }
    }
    s"$koodiUriWithoutVersion#${versio.get}"
  }

  def koulutusKoodiUriOfKoulutustyypitExist(koulutustyypit: Seq[String], koodiUri: String): ExternalQueryResult = {
    val now          = ZonedDateTime.now().toLocalDateTime
    var querySuccess = true
    val exists = koulutustyypit.exists(tyyppi => {
      var koodiUritOfKoulutustyyppi = koulutusKoodiUriCache.get(tyyppi)
      if (querySuccess && koodiUritOfKoulutustyyppi.isEmpty) {
        Try[Seq[KoodiUri]] {
          get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", tyyppi), errorHandler, followRedirects = true) {
            response =>
              {
                parse(response)
                  .extract[List[KoodistoElement]]
                  .filter(koulutus =>
                    koulutus.koodisto.getOrElse(emptyKoodistoSubElement).koodistoUri == "koulutus" &&
                      isKoodiVoimassa(tyyppi, koulutus.koodiUri, now, koulutus.voimassaLoppuPvm)
                  )
                  .map(koulutus => KoodiUri(koulutus.koodiUri, koulutus.versio))
              }
          }
        } match {
          case Success(koulutukset) =>
            koodiUritOfKoulutustyyppi = Some(koulutukset)
            koulutusKoodiUriCache.put(tyyppi)(koulutukset, Some(15.minutes))
          case Failure(exp: KoodistoQueryException) =>
            querySuccess = false
            koodiUritOfKoulutustyyppi = None
            logger.error(s"Failed to get koulutusKoodiUris for koulutustyypit from koodisto, got response ${exp.status} ${exp.message}")
          case Failure(exp: Throwable) =>
            querySuccess = false
            koodiUritOfKoulutustyyppi = None
            logger.error(s"Failed to get koulutusKoodiUris for koulutustyypit from koodisto, got response ${exp.getMessage()}")
        }
      }
      koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritOfKoulutustyyppi.getOrElse(Seq()))
    })
    if (querySuccess)
      if (exists) itemFound else itemNotFound
    else queryFailed
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(koodiUriFilter: Seq[String], koodiUri: String): ExternalQueryResult = {
    val filterSeq = koodiUriFilter.map(koodiUriFromString(_))

    val queryResponse = getAndUpdateFromKoodiUriCache("koulutus", commonKoodiUriCache)
    queryResponse.success match {
      case true =>
        val koulutusKoodiUrit = queryResponse.koodiUritInKoodisto.filter(fromCache =>
          filterSeq.exists(filterItem => fromCache.koodiUri == filterItem.koodiUri))
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koulutusKoodiUrit))
      case _ => queryFailed
    }
  }

  def lisatiedotOtsikkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("koulutuksenlisatiedot", koodiUri)

  def koulutusalaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kansallinenkoulutusluokitus2016koulutusalataso2", koodiUri)

  def opintojenLaajuusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("opintojenlaajuus", koodiUri)

  def opintojenLaajuusyksikkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("opintojenlaajuusyksikko", koodiUri)

  def tutkintoNimikeKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("tutkintonimikekk", koodiUri)

  def opetusKieliKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("oppilaitoksenopetuskieli", koodiUri)

  def opetusAikaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("opetusaikakk", koodiUri)

  def opetusTapaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("opetuspaikkakk", koodiUri)

  def osaamisalaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("osaamisala", koodiUri)

  def lukioPainotusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("lukiopainotukset", koodiUri)

  def lukioErityinenKoulutustehtavaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("lukiolinjaterityinenkoulutustehtava", koodiUri)

  def lukioDiplomiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("moduulikoodistolops2021", koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): ExternalQueryResult = {
    getAndUpdateFromKoodiUriCache(koodisto, commonKoodiUriCache) match {
      case resp if resp.success =>
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, resp.koodiUritInKoodisto))
      case _ => queryFailed
    }
  }
}
