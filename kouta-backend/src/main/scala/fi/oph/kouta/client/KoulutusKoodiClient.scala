package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode

import java.time.ZonedDateTime
import scala.concurrent.duration.DurationInt

object KoulutusKoodiClient
    extends KoulutusKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class KoulutusKoodiClient(urlProperties: OphProperties)
    extends KoodistoClient(urlProperties) {
  implicit val commonKoodiUriCache   = CaffeineCache[Seq[KoodiUri]]
  implicit val koodiuriVersionCache  = CaffeineCache[Int]
  implicit val koulutusKoodiUriCache = CaffeineCache[Seq[KoodiUri]]

  case class CodeElementWithVersion(koodiUri: String, versio: Int)

  def getKoodiUriWithLatestVersion(koodiUriWithoutVersion: String): String = {
    var versio = koodiuriVersionCache.get(koodiUriWithoutVersion)
    if (versio.isEmpty) {
      get(urlProperties.url("koodisto-service.latest-koodiuri", koodiUriWithoutVersion), followRedirects = true) {
        response =>
          {
            versio = Some(parse(response).extract[CodeElementWithVersion].versio)
            koodiuriVersionCache.put(koodiUriWithoutVersion)(versio.get, Some(15.minutes))
          }
      }
    }
    s"$koodiUriWithoutVersion#${versio.get}"
  }

  def koulutusKoodiUriOfKoulutustyypitExist(koulutustyypit: Seq[String], koodiUri: String): Boolean = {
    val now = ZonedDateTime.now().toLocalDateTime
    koulutustyypit.exists(tyyppi => {
      var koodiUritOfKoulutustyyppi = koulutusKoodiUriCache.get(tyyppi)
      if (koodiUritOfKoulutustyyppi.isEmpty) {
        get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", tyyppi), followRedirects = true) { response =>
          {
            val koulutukset = parse(response)
              .extract[List[KoodistoElement]]
              .filter(koulutus =>
                koulutus.koodisto.getOrElse(emptyKoodistoSubElement).koodistoUri == "koulutus" &&
                  isKoodiVoimassa(tyyppi, koulutus.koodiUri, now, koulutus.voimassaLoppuPvm)
              )
              .map(koulutus => KoodiUri(koulutus.koodiUri, koulutus.versio))
            koodiUritOfKoulutustyyppi = Some(koulutukset)
            koulutusKoodiUriCache.put(tyyppi)(koulutukset, Some(15.minutes))
          }
        }
      }
      koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritOfKoulutustyyppi.getOrElse(Seq()))
    })
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(koodiUriFilter: Seq[String], koodiUri: String): Boolean = {
    val filterSeq = koodiUriFilter.map(koodiUriFromString(_))

    val koulutusKoodiUrit = getAndUpdateFromKoodiUriCache("koulutus", commonKoodiUriCache).filter(fromCache =>
      filterSeq.exists(filterItem => fromCache.koodiUri == filterItem.koodiUri)
    )
    koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koulutusKoodiUrit)
  }

  def lisatiedotOtsikkoKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("koulutuksenlisatiedot", koodiUri)

  def koulutusalaKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("kansallinenkoulutusluokitus2016koulutusalataso2", koodiUri)

  def opintojenLaajuusKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("opintojenlaajuus", koodiUri)

  def opintojenLaajuusyksikkoKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("opintojenlaajuusyksikko", koodiUri)

  def tutkintoNimikeKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("tutkintonimikekk", koodiUri)

  def opetusKieliKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("oppilaitoksenopetuskieli", koodiUri)

  def opetusAikaKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("opetusaikakk", koodiUri)

  def opetusTapaKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("opetuspaikkakk", koodiUri)

  def osaamisalaKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("osaamisala", koodiUri)

  def lukioPainotusKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("lukiopainotukset", koodiUri)

  def lukioErityinenKoulutustehtavaKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("lukiolinjaterityinenkoulutustehtava", koodiUri)

  def lukioDiplomiKoodiUriExists(koodiUri: String): Boolean =
    koodiUriExistsInKoodisto("moduulikoodistolops2021", koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): Boolean = {
    val koodiUritInKoodisto = getAndUpdateFromKoodiUriCache(koodisto, commonKoodiUriCache)
    koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritInKoodisto)
  }
}
