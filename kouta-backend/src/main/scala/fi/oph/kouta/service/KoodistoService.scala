package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.{contains, getVersio, removeVersio}
import fi.oph.kouta.client.{KoodistoClient, KoodistoElement, KoodistoError}
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, itemNotFound, queryFailed}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}
import fi.vm.sade.utils.slf4j.Logging

object KoodistoService extends KoodistoService(KoodistoClient)

class KoodistoService(koodistoClient: KoodistoClient) extends Object with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def getKoodistoKaannoksetFromCache(koodisto: String): Map[String, Kielistetty] = {
    koodistoClient.getKoodistoKoodit(koodisto).map(elements => elements.view
      .filter(isKoodiVoimassa)
      .map(element => (element.koodiUri, element.asKielistetty()))
      .toMap)
      .getOrElse(Map.empty);
  }

  def getRinnasteisetKooditInKoodisto(koodiUri: String, koodisto: String): Seq[KoodistoElement] = {
    logger.info(s"Haetaan rinnasteiset koodit koodiUrille $koodiUri")
    koodistoClient.getRinnasteisetKoodit(removeVersio(koodiUri)) match {
      case Right(result) => result.view
        .filter(element => element.belongsToKoodisto(koodisto))
        .filter(isKoodiVoimassa)
      case Left(exp) if exp.status.isDefined && exp.status == 404 => Seq.empty
      case Left(exp) => {
        logger.error("Rinnasteisten koodien haku epäonnistui: ", exp)
        throw exp
      }
    }
  }

  def getKoodistoElementVersionOrLatest(koodiUri: String): Either[Throwable, KoodistoElement] = {
    getVersio(koodiUri) match {
      case Some(_) => koodistoClient.getKoodistoElementVersion(koodiUri)
      case None => koodistoClient.getKoodistoElementLatestVersion(koodiUri)
    }
  }

  def getKoulutuksetByTutkintotyyppi(tutkintotyyppi: String): Either[Throwable, Seq[KoodistoElement]] = {
    koodistoClient.getYlakoodit(tutkintotyyppi) match {
      case Right(result) => Right(result.filter(isKoodiVoimassa))
      case Left(_) => Left(new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyyppi $tutkintotyyppi from koodisto."))
    }
  }

  def koodiUriExistsInKoodisto(koodisto: KoodistoNimi, koodiUri: String): ExternalQueryResult = {
    koodistoClient.getKoodistoKoodit(koodisto.toString) match {
      case Right(elements: Seq[KoodistoElement]) => fromBoolean(contains(koodiUri, elements.filter(isKoodiVoimassa)))
      case Left(exp) if exp.status.isDefined && exp.status.get == 404 => itemNotFound
      case Left(_) => queryFailed
    }
  }

  def koulutusKoodiUriOfKoulutustyypitExist(koulutustyypit: Seq[String], koodiUri: String): ExternalQueryResult = {
    for (koulutusTyyppi <- koulutustyypit) {
      koodistoClient.getYlakoodit(koulutusTyyppi).map(result => result.view
        .filter(isKoodiVoimassa)
        .filter(element => element.koodisto.isDefined && element.koodisto.get.koodistoUri == "koulutus")) match {
        case Right(koulutusTyyppiKoodiUrit) => if (contains(koodiUri, koulutusTyyppiKoodiUrit)) return itemFound
        case Left(_) => return queryFailed
      }
    }
    itemNotFound
  }

  // Oletus: sallitutKoodiUrit eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata sallituissa koodiUreissa
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(sallitutKoodiUrit: Seq[String], koodiUri: String): ExternalQueryResult = {
    koodistoClient.getKoodistoKoodit("koulutus") match {
      case Right(elements) =>
        val koulutusKoodiUrit = elements.view
          .filter(isKoodiVoimassa)
          .filter(koodistoElement => sallitutKoodiUrit.exists(sallittuKoodiUri => koodistoElement.koodiUri == sallittuKoodiUri)
          )
        fromBoolean(contains(koodiUri, koulutusKoodiUrit))
      case Left(_) => queryFailed
    }
  }

  def oppiaineArvoExists(oppiaineArvo: String): ExternalQueryResult = {
    if (oppiaineKielitasoKoodiUriEtuliitteet contains oppiaineArvo) {
      itemFound
    } else {
      koodiUriExistsInKoodisto(OppiaineKoodisto, oppiaineArvo)
    }
  }

  def getLisattavatKoulutukset(ylakoodi: String): Either[KoodistoError, Seq[KoodistoElement]] = {
    koodistoClient.getYlakoodit(ylakoodi) match {
      case Right(result) => Right(result.view
        .filter(isKoodiVoimassa)
        .filter(element => element.koodisto.isDefined && element.koodisto.get.koodistoUri == "koulutus")
        .filter(element => !isKoulutusValiotsikkoKoodiUri(element.koodiArvo)))
      case Left(err) if err.status.isDefined && err.status.get == 404 =>
        logger.warn(s"No koulutukset were found for yläkoodi ${ylakoodi}")
        Right(Seq.empty)
      case Left(err) => Left(err)
    }
  }

  protected def isKoodiVoimassa(koodistoElement: KoodistoElement) = {
    val dateToCompare = koodistoElement.voimassaLoppuPvm
    val currentDate = ZonedDateTime.now().toLocalDateTime
    if (koodistoElement.voimassaLoppuPvm.isDefined) {
      Try[LocalDate] {
        LocalDate.parse(dateToCompare.get, ISO_LOCAL_DATE_FORMATTER)
      } match {
        case Success(dateVal) => currentDate.isBefore(dateVal.atTime(LocalTime.MAX))
        case Failure(_) =>
          logger.warn(
            s"Failed to parse voimassaLoppuPvm '${dateToCompare.get}' of koodiUri '${koodistoElement.koodiUri}' of koodisto '${koodistoElement.koodisto}'"
          )
          false
      }
    } else {
      true
    }
  }

  def isKoulutusValiotsikkoKoodiUri(koodiArvo: String): Boolean = {
    koodiArvo.endsWith("00");
  }

  def invalidateCaches() = koodistoClient.invalidateCaches()
}
