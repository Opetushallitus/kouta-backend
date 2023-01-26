package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.KoodistoNimi
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.validation.ExternalQueryResults._
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse

import java.time.ZonedDateTime
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object KoulutusKoodiClient extends KoulutusKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoulutusKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val commonKoodiUriCache: Cache[String, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()
  implicit val koodiuriVersionCache: Cache[String, KoodiUri] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()
  implicit val koulutusKoodiUriCache: Cache[String, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  def getKoodiUriVersionOrLatestFromCache(koodiUriAsString: String): Either[Throwable, KoodiUri] = {
    Try[KoodiUri] {
      koodiuriVersionCache.get(koodiUriAsString, koodiUriAsString => getKoodiUriVersionOrLatest(koodiUriAsString))
    } match {
      case Success(koodiUri) => Right(koodiUri)
      case Failure(exp) => Left(exp)
    }
  }

  private def getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi: String): Seq[KoodiUri] = {
    val now = ZonedDateTime.now().toLocalDateTime
    get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", tyyppi), errorHandler, followRedirects = true) {
      response => {
        parse(response)
          .extract[List[KoodistoElement]]
          .filter(koulutus =>
            koulutus.koodisto.getOrElse(emptyKoodistoSubElement).koodistoUri == "koulutus" &&
              isKoodiVoimassa(tyyppi, koulutus.koodiUri, now, koulutus.voimassaLoppuPvm)
          )
          .map(koulutus => KoodiUri(koulutus.koodiUri, koulutus.versio))
      }
    }
  }

  private def koulutuskoodiUriOfKoulutusTyypit(tyyppi: String): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
    } match {
      case Success(koulutukset) => koulutukset
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koulutusKoodiUris for koulutustyypi $tyyppi from koodisto, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
        } match {
          case Success(koulutukset) => koulutukset
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyypi $tyyppi from koodisto after retry, got response ${exp.status} ${exp.message}")
          case Failure(exp: Throwable) =>
            throw new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyypi $tyyppi from koodisto after retry, got response ${exp.getMessage}")
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyypi $tyyppi from koodisto, got response ${exp.status} ${exp.message}")
      case Failure(exp: Throwable) =>
        throw new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyypi $tyyppi from koodisto, got response ${exp.getMessage}")
    }
  }

  def koulutusKoodiUriOfKoulutustyypitExistFromCache(koulutustyypit: Seq[String], koodiUri: String): ExternalQueryResult = {
    try {
       val exits = koulutustyypit.exists(tyyppi => {
        val koodiUritOfKoulutustyyppi = koulutusKoodiUriCache.get(tyyppi, tyyppi => koulutuskoodiUriOfKoulutusTyypit(tyyppi))
        koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritOfKoulutustyyppi)
      })
      if (exits) itemFound
      else itemNotFound
    } catch {
      case _: Throwable => queryFailed
    }
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(koodiUriFilter: Seq[String], koodiUri: String): ExternalQueryResult = {
    val filterSeq = koodiUriFilter.map(koodiUriFromString)

    val queryResponse = getAndUpdateFromKoodiUriCache("koulutus", commonKoodiUriCache)
    if (queryResponse.success) {
      val koulutusKoodiUrit = queryResponse.koodiUritInKoodisto.filter(fromCache =>
        filterSeq.exists(filterItem => fromCache.koodiUri == filterItem.koodiUri))
      fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koulutusKoodiUrit))
    } else {
      queryFailed
    }
  }

  def osaamisalaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("osaamisala", koodiUri)

  def lukioPainotusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("lukiopainotukset", koodiUri)

  def lukioErityinenKoulutustehtavaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("lukiolinjaterityinenkoulutustehtava", koodiUri)

  def lukioDiplomiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("moduulikoodistolops2021", koodiUri)

  def opinnonTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("opinnontyyppi", koodiUri)

  def erikoistumisKoulutusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("erikoistumiskoulutukset", koodiUri)

  def taiteenalaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("taiteenperusopetustaiteenala", koodiUri)

  def koodiUriExistsInKoodisto(koodisto: KoodistoNimi, koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto(koodisto.toString, koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): ExternalQueryResult = {
    getAndUpdateFromKoodiUriCache(koodisto, commonKoodiUriCache) match {
      case resp if resp.success =>
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, resp.koodiUritInKoodisto))
      case _ => queryFailed
    }
  }
}
