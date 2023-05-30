package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{contains, getVersio, removeVersio}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Kieli, Kielistetty, KoodistoNimi, OppiaineKoodisto, oppiaineKielitasoKoodiUriEtuliitteet}
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, itemNotFound, queryFailed}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZonedDateTime}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

case class KoodistoQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class KoodistoNotFoundException(message: String)                        extends RuntimeException(message)

case class KoodistoSubElement(koodistoUri: String)

case class KoodistoMetadataElement(
    nimi: String = "",
    kieli: String = ""
)

case class KoodistoElement(
    koodiUri: String,
    versio: Int = 1,
    koodisto: Option[KoodistoSubElement] = None,
    voimassaLoppuPvm: Option[String] = None,
    metadata: List[KoodistoMetadataElement] = List.empty
) {

  def belongsToKoodisto(koodistoUri: String): Boolean = {
    koodisto.exists(k => koodistoUri.equals(k.koodistoUri))
  }

  def asKielistetty(): Kielistetty = {
    metadata.map(element => Kieli.withName(element.kieli.toLowerCase) -> element.nimi).toMap
  }
}

case class KoodistoQueryResponse(success: Boolean, elements: Seq[KoodistoElement])

object KoodistoUtils {

  def removeVersio(koodiUri: String): String = {
    koodiUri.split("#")(0);
  }

  def getVersio(koodiUri: String): Option[Int] = {
    koodiUri.split("#") match {
      case parts if parts.length == 2 => Some(parts(1).toInt)
      case _ => None
    }
  }

  def contains(koodiUri: String, koodistoElements: Seq[KoodistoElement]): Boolean = {
    val versio = getVersio(koodiUri).getOrElse(Int.MinValue);
    val base = removeVersio(koodiUri);
    koodistoElements.exists(element => element.koodiUri.equals(base) && element.versio>=versio);
  }
}

object CachedKoodistoClient extends CachedKoodistoClient(KoutaConfigurationFactory.configuration.urlProperties)

class CachedKoodistoClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {

  implicit val koodistoElementCache: Cache[String, Seq[KoodistoElement]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val koodistoElementVersionCache: Cache[String, KoodistoElement] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val rinnasteinenKoodistoElementCache: Cache[(String, String), Seq[KoodistoElement]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  implicit val koulutuksetByTutkintotyyppiCache: Cache[String, Seq[KoodistoElement]] = Scaffeine()
    .expireAfterWrite(10.minutes)
    .build()

  def getKoodistoKaannoksetFromCache(koodisto: String): Map[String, Kielistetty] = {
    val res: KoodistoQueryResponse = getAndUpdateFromKoodistoElementCache(koodisto, koodistoElementCache, getKoodistoKoodit)

    if (res.success) {
      res.elements.map(element => (element.koodiUri, element.asKielistetty)).toMap
    } else {
      Map.empty
    }
  }

  def getRinnasteisetCached(koodiUri: String, koodisto: String): Seq[KoodistoElement] = {
    rinnasteinenKoodistoElementCache.get((koodiUri, koodisto), params => getRinnasteisetKooditInKoodisto(params._1, params._2))
  }

  def getKoodistoElementVersionOrLatestFromCache(koodiUriAsString: String): Either[Throwable, KoodistoElement] = {
    Try[KoodistoElement] {
      koodistoElementVersionCache.get(koodiUriAsString, koodiUriAsString => getKoodistoElementVersionOrLatest(koodiUriAsString))
    } match {
      case Success(koodistoElement) => Right(koodistoElement)
      case Failure(exp)      => Left(exp)
    }
  }

  def getKoulutuksetByTutkintotyyppiCached(tutkintotyyppi: String): Either[Throwable, Seq[KoodistoElement]] = {
    val res: KoodistoQueryResponse = getAndUpdateFromKoodistoElementCache(
      tutkintotyyppi,
      koulutuksetByTutkintotyyppiCache,
      getVoimassaOlevatYlakoodit)

    if (res.success) {
      Right(res.elements)
    } else {
      Left(new RuntimeException(s"Failed to get koulutusKoodiUris for koulutustyyppi $tutkintotyyppi from koodisto."))
    }
  }

  def koodiUriExistsInKoodisto(koodisto: KoodistoNimi, koodiUri: String): ExternalQueryResult =
    getAndUpdateFromKoodistoElementCache(koodisto.toString, koodistoElementCache, getKoodistoKoodit) match {
      case resp if resp.success =>
        fromBoolean(contains(koodiUri, resp.elements))
      case _ => queryFailed
    }

  private def getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi: String): Seq[KoodistoElement] = {
    getVoimassaOlevatYlakoodit(tyyppi).filter(element => element.koodisto.map(koodisto => koodisto.koodistoUri == "koulutus").getOrElse(false))
  }

  private def getKoulutusKoodistoElementsOfKoulutusTyypit(tyyppi: String): Seq[KoodistoElement] = {
    Try[Seq[KoodistoElement]] {
      getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
    } match {
      case Success(koulutukset) => koulutukset
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, retrying once...")
        Try[Seq[KoodistoElement]] {
          getKoulutuskoodiUriOfKoulutusTyypitFromKoodistoService(tyyppi)
        } match {
          case Success(koulutukset) => koulutukset
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(
              s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto after retry, got response ${exp.status} ${exp.message}"
            )
          case Failure(exp: Throwable) =>
            throw new RuntimeException(
              s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto after retry, got response ${exp.getMessage}"
            )
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(
          s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, got response ${exp.status} ${exp.message}"
        )
      case Failure(exp: Throwable) =>
        throw new RuntimeException(
          s"Failed to get koulutusKoodiUris for koulutustyyppi $tyyppi from koodisto, got response ${exp.getMessage}"
        )
    }
  }

  def koulutusKoodiUriOfKoulutustyypitExistFromCache(
      koulutustyypit: Seq[String],
      koodiUri: String
  ): ExternalQueryResult = {
    try {
      val exits = koulutustyypit.exists(tyyppi => {
        val koodiUritOfKoulutustyyppi =
          koodistoElementCache.get(tyyppi, tyyppi => getKoulutusKoodistoElementsOfKoulutusTyypit(tyyppi))
        contains(koodiUri, koodiUritOfKoulutustyyppi)
      })
      if (exits) itemFound
      else itemNotFound
    } catch {
      case _: Throwable => queryFailed
    }
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  def koulutusKoodiUriExists(sallitutKoodiUrit: Seq[String], koodiUri: String): ExternalQueryResult = {
    val queryResponse = getAndUpdateFromKoodistoElementCache("koulutus", koodistoElementCache, getKoodistoKoodit)
    if (queryResponse.success) {
      val koulutusKoodiUrit = queryResponse.elements.filter(koodistoElement =>
        sallitutKoodiUrit.exists(sallittuKoodiUri => koodistoElement.koodiUri == sallittuKoodiUri)
      )
      fromBoolean(contains(koodiUri, koulutusKoodiUrit))
    } else {
      queryFailed
    }
  }

  def oppiaineArvoExists(oppiaineArvo: String): ExternalQueryResult = {
    if (oppiaineKielitasoKoodiUriEtuliitteet contains oppiaineArvo) {
      itemFound
    } else {
      koodiUriExistsInKoodisto(OppiaineKoodisto, oppiaineArvo)
    }
  }
}

abstract class KoodistoClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {

  val ISO_LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val formats = DefaultFormats

  val errorHandler = (url: String, status: Int, response: String) => throw KoodistoQueryException(url, status, response)

  protected def getKoodistoKoodit(koodisto: String): Seq[KoodistoElement] =
    get(
      urlProperties.url("koodisto-service.koodisto-koodit", koodisto),
      errorHandler,
      followRedirects = true
    ) { response =>
      {
        parse(response)
          .extract[List[KoodistoElement]]
          .filter(isKoodiVoimassa)
      }
    }

  protected def getVoimassaOlevatYlakoodit(koodi: String): List[KoodistoElement] = {
    get(urlProperties.url("koodisto-service.sisaltyy-ylakoodit", koodi), errorHandler, followRedirects = true) {
      response => {
        parse(response)
          .extract[List[KoodistoElement]]
          .filter(isKoodiVoimassa)
      }
    }
  }

  def getRinnasteisetKooditInKoodisto(koodiUri: String, koodisto: String): Seq[KoodistoElement] = {
    logger.info(s"Haetaan rinnasteiset koodit koodiUrille $koodiUri")
    try {
      get(
        urlProperties.url("koodisto-service.koodisto-koodit.rinnasteiset", removeVersio(koodiUri)),
        errorHandler,
        followRedirects = true
      ) { response =>
        {
          parse(response)
            .extract[List[KoodistoElement]]
            .filter((koodi: KoodistoElement) =>
              koodi.belongsToKoodisto(koodisto) &&
                isKoodiVoimassa(koodi)
            )
        }
      }
    } catch {
      case e: KoodistoQueryException if e.status == 404 => List.empty
      case e: Throwable =>
        logger.error("Rinnasteisten koodien haku epäonnistui: ", e)
        throw e
    }
  }

  private def getAndUpdateFromKoodistoElement(koodisto: String, getFromKoodistoFunction: String => Seq[KoodistoElement] = getKoodistoKoodit): Seq[KoodistoElement] = {
    val contentDesc = s"koodiuris from koodisto $koodisto"
    Try[Seq[KoodistoElement]] {
      getFromKoodistoFunction(koodisto)
    } match {
      case Success(koodiUrit) => koodiUrit
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get koodiuris from koodisto $koodisto, retrying once...")
        Try[Seq[KoodistoElement]] {
          getFromKoodistoFunction(koodisto)
        } match {
          case Success(koodiUrit)    => koodiUrit
          case Failure(t: Throwable) => throw exception(t, contentDesc, true)
        }
      case Failure(t: Throwable) => throw exception(t, contentDesc, false)
    }
  }

  protected def getAndUpdateFromKoodistoElementCache(
                                               koodisto: String,
                                               koodiUriCache: Cache[String, Seq[KoodistoElement]],
                                               getFromKoodistoFunction: String => Seq[KoodistoElement]
                                             ): KoodistoQueryResponse = {
    try {
      val koodiUritFromCache = koodiUriCache.get(koodisto, koodisto => getAndUpdateFromKoodistoElement(koodisto, getFromKoodistoFunction))
      KoodistoQueryResponse(success = true, koodiUritFromCache)
    } catch {
      case _: KoodistoNotFoundException => KoodistoQueryResponse(success = true, Seq())
      case _: Throwable                 => KoodistoQueryResponse(success = false, Seq())
    }
  }

  protected def isKoodiVoimassa(koodistoElement: KoodistoElement) = {
    val dateToCompare = koodistoElement.voimassaLoppuPvm
    val currentDate   = ZonedDateTime.now().toLocalDateTime
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

  private def getKoodistoElementVersionOrLatestFromKoodistoService(koodiUriAsString: String): KoodistoElement = {
    val base = removeVersio(koodiUriAsString)
    val versio = getVersio(koodiUriAsString);
    get(
      if (versio.isDefined)
        urlProperties.url("koodisto-service.koodiuri-version", base, versio.get.toString)
      else
        urlProperties.url("koodisto-service.latest-koodiuri", base),
      errorHandler,
      followRedirects = true
    ) { response =>
      parse(response).extract[KoodistoElement]
    }
  }

  protected def getKoodistoElementVersionOrLatest(koodiUriAsString: String): KoodistoElement = {
    val contentDesc = s"koodiuri-version from koodisto for $koodiUriAsString"
    Try[KoodistoElement] {
      getKoodistoElementVersionOrLatestFromKoodistoService(koodiUriAsString)
    } match {
      case Success(koodiUri) => koodiUri
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get $contentDesc, retrying once...")
        Try[KoodistoElement] {
          getKoodistoElementVersionOrLatestFromKoodistoService(koodiUriAsString)
        } match {
          case Success(koodiUri)     => koodiUri
          case Failure(t: Throwable) => throw exception(t, contentDesc, true)
        }
      case Failure(t: Throwable) => throw exception(t, contentDesc, false)
    }
  }

  private def exception(throwable: Throwable, contentDesc: String, retryDone: Boolean): Throwable = {
    val retryDoneMsg = if (retryDone) " after retry" else ""
    throwable match {
      case exp: KoodistoQueryException if exp.status == 404 =>
        KoodistoNotFoundException(s"Unable to find $contentDesc, got response ${exp.status}, ${exp.message}")
      case exp: KoodistoQueryException =>
        new RuntimeException(s"Failed to get $contentDesc$retryDoneMsg, got response ${exp.status}, ${exp.message}")
      case _ =>
        new RuntimeException(s"Failed to get $contentDesc$retryDoneMsg, got response ${throwable.getMessage()}")
    }
  }

}
