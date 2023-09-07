package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.{contains, getVersio, removeVersio}
import fi.oph.kouta.client.{KoodistoClient, KoodistoElement, KoodistoError, KoodistoWithVersio}
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, itemNotFound, queryFailed}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}
import fi.vm.sade.utils.slf4j.Logging

object KoodistoService extends KoodistoService(KoodistoClient)

class KoodistoService(koodistoClient: KoodistoClient) extends Object with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def getKoodistoKoodit(koodisto: String, versio: Option[Int]): Either[Throwable, Seq[KoodistoElement]] = {
    versio match {
      case Some(v) => koodistoClient.getKoodistoKooditWithVersion(KoodistoWithVersio(koodisto, v))
      case None => koodistoClient.getKoodistoKoodit(koodisto)
    }
  }

  def getKoodistoKaannokset(koodisto: String): Map[String, Kielistetty] = {
    koodistoClient.getKoodistoKoodit(koodisto).map(elements => elements.view
      .filter(isKoodiVoimassa)
      .map(element => (element.koodiUri, element.asKielistetty()))
      .toMap)
      .getOrElse(Map.empty);
  }

  def getKaannokset(koodiUri: String): Either[Throwable, Kielistetty] = {
    val result = getVersio(koodiUri) match {
      case Some(_) => koodistoClient.getKoodistoElementVersion(koodiUri)
      case None => koodistoClient.getKoodistoElementLatestVersion(koodiUri)
    }
    result.map(element => element.asKielistetty())
  }

  def getRinnasteisetKooditInKoodisto(koodiUri: String, koodisto: String): Seq[KoodistoElement] = {
    logger.info(s"Haetaan rinnasteiset koodit koodiUrille $koodiUri")
    koodistoClient.getRinnasteisetKoodit(removeVersio(koodiUri)) match {
      case Right(result) => result.view
        .filter(element => element.belongsToKoodisto(koodisto))
        .filter(isKoodiVoimassa)
      case Left(exp) if exp.status.exists(_ == 404) => Seq.empty
      case Left(exp) => {
        logger.error("Rinnasteisten koodien haku epäonnistui: ", exp)
        throw exp
      }
    }
  }

  def getLatestVersion(koodiUri: String): Either[Throwable, KoodistoElement] = {
    koodistoClient.getKoodistoElementLatestVersion(removeVersio(koodiUri))
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
      case Left(exp) if exp.status.exists(_ == 404) => itemNotFound
      case Left(_) => queryFailed
    }
  }

  def isInLisattavatKoulutukset(ylakoodit: Seq[String], koodiUri: String): ExternalQueryResult = {
    for (koulutusTyyppi <- ylakoodit) {
      getLisattavatKoulutukset(koulutusTyyppi) match {
        case Right(koulutusTyyppiKoodiUrit) => if (contains(koodiUri, koulutusTyyppiKoodiUrit)) return itemFound
        case Left(_) => return queryFailed
      }
    }
    itemNotFound
  }

  def getLisattavatKoulutukset(ylakoodi: String): Either[KoodistoError, Seq[KoodistoElement]] = {
    koodistoClient.getYlakoodit(ylakoodi) match {
      case Right(result) => Right(result.view
        .filter(isKoodiVoimassa)
        .filter(element => element.koodisto.exists(_.koodistoUri == "koulutus"))
        .filter(element => !isKoulutusValiotsikkoKoodiUri(element.koodiArvo)))
      case Left(err) if err.status.exists(_ == 404) =>
        logger.warn(s"No koulutukset were found for yläkoodi ${ylakoodi}")
        Right(Seq.empty)
      case Left(err) => Left(err)
    }
  }

  private def withYlaRelaatiot(koodi: KoodistoElement): KoodistoElement = {
    koodistoClient.getYlakoodit(koodi.koodiUri) match {
      case Right(result) => koodi.withYlaRelaatiot(result)
      case Left(_) => koodi
    }
  }

  def getValintakokeenTyypit(koulutusKoodit: Seq[String],
                             hakutapaKoodi: Option[String],
                             haunkohdejoukkoKoodi: Option[String],
                             osaamisalaKoodit: Seq[String]): Either[KoodistoError, Seq[KoodistoElement]] = {
    koodistoClient.getKoodistoKoodit(ValintakoeTyyppiKoodisto.name) match {
      case Right(result) => Right(result.view
      .filter(isKoodiVoimassa)
      .map(withYlaRelaatiot)
      .filter(koodi => {
        val noYlaKoodiWithKoulutuksetAndOsaamisAla = !koodi.hasYlakoodiWithinKoodisto(KoulutusKoodisto.name) &&
          !koodi.hasYlakoodiWithinKoodisto(OsaamisalaKoodisto.name)
        val koulutuksetValid = noYlaKoodiWithKoulutuksetAndOsaamisAla ||
          (koulutusKoodit.nonEmpty && koulutusKoodit.forall(k => koodi.containsYlaKoodiWithKoodisto(k, KoulutusKoodisto.name)))
        val hakutapaValid = !koodi.hasYlakoodiWithinKoodisto(HakutapaKoodisto.name) ||
          hakutapaKoodi.exists(k => koodi.containsYlaKoodiWithKoodisto(k, HakutapaKoodisto.name))
        val kohdejoukkoValid = !koodi.hasYlakoodiWithinKoodisto(HaunKohdejoukkoKoodisto.name) ||
          haunkohdejoukkoKoodi.exists(k => koodi.containsYlaKoodiWithKoodisto(k, HaunKohdejoukkoKoodisto.name))
        val osaamisalatValid = noYlaKoodiWithKoulutuksetAndOsaamisAla ||
          (osaamisalaKoodit.nonEmpty && osaamisalaKoodit.forall(k => koodi.containsYlaKoodiWithKoodisto(k, OsaamisalaKoodisto.name)))
        val koulutusOrOsaamisalaValid = koulutuksetValid || osaamisalatValid
        koulutusOrOsaamisalaValid && hakutapaValid && kohdejoukkoValid
      })
      .map(koodi => koodi.withYlaRelaatiot(Seq.empty)))
      case Left(err) => Left(err)
    }
  }

  // Oletus: sallitutKoodiUrit eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata sallituissa koodiUreissa
  // mahdollisesti annettuihin versioihin.
  def isLisattavaKoulutus(sallitutKoodiUrit: Seq[String], koodiUri: String): ExternalQueryResult = {
    koodistoClient.getKoodistoKoodit("koulutus") match {
      case Right(elements) =>
        val koulutusKoodiUrit = elements.view
          .filter(isKoodiVoimassa)
          .filter(element => !isKoulutusValiotsikkoKoodiUri(element.koodiArvo))
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

  def isKoulutusValiotsikkoKoodiUri(koodiArvo: String): Boolean = {
    koodiArvo.endsWith("00");
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

  def invalidateCaches() = koodistoClient.invalidateCaches()
}
