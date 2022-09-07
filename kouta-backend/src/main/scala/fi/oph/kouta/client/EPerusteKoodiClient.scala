package fi.oph.kouta.client

import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import org.json4s.JsonAST.{JNothing, JObject}
import scalacache.caffeine.CaffeineCache
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.{Duration, DurationInt}
import scalacache.modes.sync.mode

import scala.util.{Failure, Success, Try}

object EPerusteKoodiClient extends EPerusteKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val ePerusteToKoodiuritCache    = CaffeineCache[Seq[KoodiUri]]
  implicit val ePerusteToTutkinnonosaCache = CaffeineCache[Seq[(Long, Long)]]
  implicit val ePerusteToOsaamisalaCache   = CaffeineCache[Seq[KoodiUri]]

  case class KoulutusKoodiUri(koulutuskoodiUri: String)
  case class EPeruste(voimassaoloLoppuu: Option[Long], koulutukset: List[KoulutusKoodiUri] = List())
  case class Tutkinnonosa(id: Long)
  case class TutkinnonosaViite(id: Long, tutkinnonOsa: Option[Tutkinnonosa])

  def getKoulutusKoodiUritForEPeruste(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    var koodiUritForEPeruste                          = ePerusteToKoodiuritCache.get(ePerusteId)
    var returnValue: Either[Throwable, Seq[KoodiUri]] = Right(Seq())
    if (koodiUritForEPeruste.isEmpty) {
      Try[Seq[KoodiUri]] {
        get(
          urlProperties.url("eperusteet-service.peruste-by-id", ePerusteId.toString),
          errorHandler,
          followRedirects = true
        ) { response =>
          {
            val ePeruste = parse(response).extract[EPeruste]
            if (ePeruste.voimassaoloLoppuu.isEmpty || ePeruste.voimassaoloLoppuu.get > System.currentTimeMillis()) {
              ePeruste.koulutukset.map(uri => koodiUriFromString(uri.koulutuskoodiUri))
            } else {
              Seq()
            }
          }
        }
      } match {
        case Success(koodiUris) =>
          koodiUritForEPeruste = Some(koodiUris)
          ePerusteToKoodiuritCache.put(ePerusteId)(koodiUris, Some(15.minutes))
        case Failure(exp: KoodistoQueryException) if exp.status == 404 => koodiUritForEPeruste = None
        case Failure(exp: KoodistoQueryException) =>
          returnValue = Left(exp)
          logger.error(s"Failed to get ePerusteet with id $ePerusteId, got response ${exp.status} ${exp.message}")
      }
    }

    if (returnValue.left.toOption.isDefined) returnValue else Right(koodiUritForEPeruste.getOrElse(Seq()))
  }

  def getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId: Long): Either[Throwable, Seq[(Long, Long)]] = {
    var viitteetAndIdtForEPeruste                         = ePerusteToTutkinnonosaCache.get(ePerusteId)
    var returnValue: Either[Throwable, Seq[(Long, Long)]] = Right(Seq())
    if (viitteetAndIdtForEPeruste.isEmpty) {
      Try[Seq[(Long, Long)]] {
        get(
          urlProperties.url("eperusteet-service.tutkinnonosat-by-eperuste", ePerusteId.toString),
          errorHandler,
          followRedirects = true
        ) { response =>
          {
            parse(response)
              .extract[List[TutkinnonosaViite]]
              .filter(_.tutkinnonOsa.isDefined)
              .map(viite => (viite.id, viite.tutkinnonOsa.get.id))
          }
        }
      } match {
        case Success(viiteAndIdt) =>
          viitteetAndIdtForEPeruste = Some(viiteAndIdt)
          ePerusteToTutkinnonosaCache.put(ePerusteId)(viiteAndIdt, Some(15.minutes))
        case Failure(exp: KoodistoQueryException) if exp.status == 404 => viitteetAndIdtForEPeruste = None
        case Failure(exp: KoodistoQueryException) =>
          returnValue = Left(exp)
          logger.error(
            s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId, got response ${exp.status} ${exp.message}"
          )
      }
    }
    if (returnValue.left.toOption.isDefined) returnValue else Right(viitteetAndIdtForEPeruste.getOrElse(Seq()))
  }

  def getOsaamisalaKoodiuritForEPeruste(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    var osaamisalaKoodiUritForEPeruste                = ePerusteToOsaamisalaCache.get(ePerusteId)
    var returnValue: Either[Throwable, Seq[KoodiUri]] = Right(Seq())
    if (osaamisalaKoodiUritForEPeruste.isEmpty) {
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
        case Success(koodiUrit) =>
          osaamisalaKoodiUritForEPeruste = Some(koodiUrit)
          ePerusteToOsaamisalaCache.put(ePerusteId)(koodiUrit, Some(15.minutes))
        case Failure(exp: KoodistoQueryException) if exp.status == 404 => osaamisalaKoodiUritForEPeruste = None
        case Failure(exp: KoodistoQueryException) =>
          returnValue = Left(exp)
          logger.error(
            s"Failed to get osaamisalat for ePeruste with id $ePerusteId, got response ${exp.status} ${exp.message}"
          )
      }
    }
    if (returnValue.left.toOption.isDefined) returnValue else Right(osaamisalaKoodiUritForEPeruste.getOrElse(Seq()))
  }
}
