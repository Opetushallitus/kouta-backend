package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.{koodiUriExistsInList, koodiUriFromString, kooriUriToString}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode

import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success, Try}

object KoulutusKoodiClient extends KoulutusKoodiClient(KoutaConfigurationFactory.configuration.urlProperties, 15.minutes)

class KoulutusKoodiClient(urlProperties: OphProperties, cacheTtl: Duration = 15.minutes) extends KoodistoClient(urlProperties) {
  implicit val commonKoodiUriCache   = CaffeineCache[Seq[KoodiUri]]
  implicit val koodiuriVersionCache  = CaffeineCache[Int]
  implicit val koulutusKoodiUriCache = CaffeineCache[Seq[KoodiUri]]

  case class CodeElementWithVersion(koodiUri: String, versio: Int)
  case class KoodistoSubElement(koodistoUri: String)
  case class KoodistoElement(
      koodiUri: String,
      versio: Int = 1,
      koodisto: Option[KoodistoSubElement],
      voimassaLoppuPvm: Option[String]
  )
  val emptyKoodistoSubElement = KoodistoSubElement("")

  def getKoodiUriWithLatestVersion(koodiUriWithoutVersion: String): String = {
    var versio = koodiuriVersionCache.get(koodiUriWithoutVersion)
    if (versio.isEmpty) {
      get(urlProperties.url("koodisto-service.latest-koodiuri", koodiUriWithoutVersion), followRedirects = true) {
        response =>
        {
          versio = Some(parse(response).extract[CodeElementWithVersion].versio)
          koodiuriVersionCache.put(koodiUriWithoutVersion)(versio.get, Some(cacheTtl))
        }
      }
    }
    s"$koodiUriWithoutVersion#${versio.get}"
  }

  def koulutusKoodiUritOfKoulutustyypitExist(koulutustyypit: Seq[String], koodiUrit: Seq[String]): Boolean = {
    val now = ZonedDateTime.now().toLocalDateTime
    koodiUrit.forall(koodiUri => {
      koulutustyypit.exists(tyyppi => {
        var koodiUritOfKoulutustyyppi = koulutusKoodiUriCache.get(tyyppi)
        if (koodiUritOfKoulutustyyppi.isEmpty) {
          get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", tyyppi), followRedirects = true) {
            response => {
              val koulutukset = parse(response)
                .extract[List[KoodistoElement]]
                .filter(koulutus =>
                  koulutus.koodisto.getOrElse(emptyKoodistoSubElement).koodistoUri == "koulutus" &&
                    isKoodiVoimassa(tyyppi, koulutus.koodiUri, now, koulutus.voimassaLoppuPvm)
                )
                .map(koulutus => KoodiUri(koulutus.koodiUri, koulutus.versio))
              koodiUritOfKoulutustyyppi = Some(koulutukset)
              koulutusKoodiUriCache.put(tyyppi)(koulutukset, Some(cacheTtl))
            }
          }
        }
        koodiUriExistsInList(koodiUri, koodiUritOfKoulutustyyppi.getOrElse(Seq()))
      })
    })
  }

  // Oletus: koofiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUritExist(koodiUriFilter: Seq[String], koodiUrit: Seq[String]): Boolean = {
    val filterSeq = koodiUriFilter.map(koodiUriFromString(_))

    val koulutusKoodiUrit = getAndUpdateFromKoodiUriCache("koulutus").
      filter(fromCache =>
        filterSeq.exists(filterItem => fromCache.koodiUri == filterItem.koodiUri))
    koodiUrit.forall(koodiUri => koodiUriExistsInList(koodiUri, koulutusKoodiUrit))
  }

  def lisatiedotOtsikkoKoodiUritExist(koodiUrit: Seq[String]): Boolean = {
    val lisatietoOtsikkoKoodiUrit = getAndUpdateFromKoodiUriCache("koulutuksenlisatiedot")
    koodiUrit.forall(koodiUri => koodiUriExistsInList(koodiUri, lisatietoOtsikkoKoodiUrit))
  }

  def koulutusalaKoodiUritExist(koodiUrit: Seq[String]): Boolean = {
    val koulutusalaKoodiUrit =
      getAndUpdateFromKoodiUriCache("kansallinenkoulutusluokitus2016koulutusalataso2")
    koodiUrit.forall(koodiUri => koodiUriExistsInList(koodiUri, koulutusalaKoodiUrit))
  }

  def opintojenLaajuusKoodiUriExists(koodiUri: String): Boolean = {
    val opintojenLaajuusKoodiUrit =
      getAndUpdateFromKoodiUriCache("opintojenlaajuus")
    koodiUriExistsInList(koodiUri, opintojenLaajuusKoodiUrit)
  }

  def opintojenLaajuusyksikkoKoodiUriExists(koodiUri: String): Boolean = {
    val opintojenLaajuusyksikkoKoodiUrit =
      getAndUpdateFromKoodiUriCache("opintojenlaajuusyksikko")
    koodiUriExistsInList(koodiUri, opintojenLaajuusyksikkoKoodiUrit)
  }

  def tutkintoNimikeKoodiUritExist(koodiUrit: Seq[String]): Boolean = {
    val tutkintoNimikeKoodiUrit =
      getAndUpdateFromKoodiUriCache("tutkintonimikekk")
    koodiUrit.forall(koodiUri => koodiUriExistsInList(koodiUri, tutkintoNimikeKoodiUrit))
  }

  private def getAndUpdateFromKoodiUriCache(koodisto: String): Seq[KoodiUri] = {
    var koodiUritFromCache = commonKoodiUriCache.get(koodisto)
    if (koodiUritFromCache.isEmpty) {
      get(
        urlProperties.url("koodisto-service.koodisto-koodit", koodisto),
        followRedirects = true
      ) { response =>
        {
          val koodiUrit = parse(response)
            .extract[List[KoodistoElement]]
            .filter(koodiUri => isKoodiVoimassa(koodisto, koodiUri.koodiUri, dateToCompare = koodiUri.voimassaLoppuPvm))
            .map(koodiUri => KoodiUri(koodiUri.koodiUri, koodiUri.versio))
          koodiUritFromCache = Some(koodiUrit)
          commonKoodiUriCache.put(koodisto)(koodiUrit, Some(cacheTtl))
        }
      }
    }
    koodiUritFromCache.getOrElse(Seq())
  }

  private def isKoodiVoimassa(
      koodisto: String,
      koodiUri: String,
      currentDate: LocalDateTime = ZonedDateTime.now().toLocalDateTime,
      dateToCompare: Option[String]
  ): Boolean =
    if (dateToCompare.isDefined) {
      Try[LocalDate] {
        LocalDate.parse(dateToCompare.get, ISO_LOCAL_DATE_FORMATTER)
      } match {
        case Success(dateVal) => currentDate.isBefore(dateVal.atTime(LocalTime.MAX))
        case Failure(_) =>
          logger.warn(
            s"Failed to parse voimassaLoppuPvm '${dateToCompare.get}' of koodiUri '${koodiUri}' of koodisto '${koodisto}'"
          )
          false
      }
    } else {
      true
    }
}
