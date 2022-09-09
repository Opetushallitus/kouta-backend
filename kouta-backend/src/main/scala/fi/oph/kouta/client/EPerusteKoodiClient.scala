package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object EPerusteKoodiClient extends EPerusteKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val cacheTTL = 15.minutes

  implicit val ePerusteToKoodiuritCache: Cache[Long, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()
  implicit val ePerusteToTutkinnonosaCache: Cache[Long, Seq[(Long, Long)]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()
  implicit val ePerusteToOsaamisalaCache: Cache[Long, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()

  case class KoulutusKoodiUri(koulutuskoodiUri: String)
  case class EPeruste(voimassaoloLoppuu: Option[Long], koulutukset: List[KoulutusKoodiUri] = List())
  case class Tutkinnonosa(id: Long)
  case class TutkinnonosaViite(id: Long, tutkinnonOsa: Option[Tutkinnonosa])

  private def getKoulutusKoodiUritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      get(urlProperties.url("eperusteet-service.peruste-by-id", ePerusteId.toString), errorHandler, followRedirects = true) {
        response => {
          val ePeruste = parse(response).extract[EPeruste]
          if (ePeruste.voimassaoloLoppuu.isEmpty || ePeruste.voimassaoloLoppuu.get > System.currentTimeMillis()) {
            ePeruste.koulutukset.map(uri => koodiUriFromString(uri.koulutuskoodiUri))
          } else {
            Seq()
          }
        }
      }
    } match {
      case Success(koodiUris) => koodiUris
      case Failure(exp: KoodistoQueryException) if exp.status == 404 => Seq()
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(s"Failed to get ePerusteet with id $ePerusteId, got response ${exp.status} ${exp.message}")
    }
  }

  def getKoulutusKoodiUritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    var returnValue: Either[Throwable, Seq[KoodiUri]] = Right(Seq())
    ePerusteToKoodiuritCache.get(ePerusteId, ePerusteId => getKoulutusKoodiUritForEPeruste(ePerusteId))
    if (returnValue.left.toOption.isDefined) returnValue else Right(koodiUritForEPeruste.getOrElse(Seq()))
  }

  private def getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId: Long): Seq[(Long, Long)] = {
    Try[Seq[(Long, Long)]] {
      get(urlProperties.url("eperusteet-service.tutkinnonosat-by-eperuste", ePerusteId.toString), errorHandler, followRedirects = true) {
        response => {
          parse(response).extract[List[TutkinnonosaViite]].
            filter(_.tutkinnonOsa.isDefined).map(viite => (viite.id, viite.tutkinnonOsa.get.id))
        }
      }
    } match {
      case Success(viiteAndIdt) => viiteAndIdt
      case Failure(exp: KoodistoQueryException) if exp.status == 404 => Seq()
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId, got response ${exp.status} ${exp.message}")
    }
  }

  def getTutkinnonosaViitteetAndIdtForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[(Long, Long)]] = {
    var returnValue: Either[Throwable, Seq[(Long, Long)]] = Right(Seq())
    ePerusteToTutkinnonosaCache.get(ePerusteId, ePerusteId => getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId))
    if (returnValue.left.toOption.isDefined) returnValue else Right(viitteetAndIdtForEPeruste.getOrElse(Seq()))
  }

  private def getOsaamisalaKoodiuritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
      Try[Seq[KoodiUri]] {
        get(
          urlProperties.url("eperusteet-service.osaamisalat-by-eperuste", ePerusteId.toString),
          errorHandler,
          followRedirects = true
        ) { response =>
          {
            (parse(response) \\ "reformi").extract[JObject].values.keySet.map(uri => koodiUriFromString(uri)).toSeq
          }
        }
      } match {
        case Success(koodiUrit) => koodiUrit
        case Failure(exp: KoodistoQueryException) if exp.status == 404 => Seq()
        case Failure(exp: KoodistoQueryException) =>
          returnValue = Left(exp)
          logger.error(
            s"Failed to get osaamisalat for ePeruste with id $ePerusteId, got response ${exp.status} ${exp.message}"
          )
      }
  }

  def getOsaamisalaKoodiuritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    var returnValue: Either[Throwable, Seq[KoodiUri]] = Right(Seq())
    ePerusteToOsaamisalaCache.get(ePerusteId, ePerusteId => getOsaamisalaKoodiuritForEPeruste(ePerusteId))
    if (returnValue.left.toOption.isDefined) returnValue else Right(osaamisalaKoodiUritForEPeruste.getOrElse(Seq()))
  }
}
