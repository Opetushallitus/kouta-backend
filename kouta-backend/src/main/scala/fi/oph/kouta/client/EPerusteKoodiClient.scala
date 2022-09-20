package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.properties.OphProperties
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Right, Success, Try}
import fi.oph.kouta.domain.{En, Fi, Kieli, Kielistetty, Sv}
import fi.vm.sade.properties.OphProperties
import scalacache.caffeine.CaffeineCache
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scalacache.modes.sync.mode

import scala.util.{Failure, Success, Try}

case class KoulutusKoodiUri(koulutuskoodiUri: String)
case class EPeruste(voimassaoloLoppuu: Option[Long], koulutukset: List[KoulutusKoodiUri] = List())
case class TutkinnonOsaNimi(fi: Option[String], sv: Option[String], en: Option[String])
case class Tutkinnonosa(id: Long, nimi: Option[TutkinnonOsaNimi])
case class TutkinnonosaViite(id: Long, tutkinnonOsa: Option[Tutkinnonosa])
case class TutkinnonOsaServiceItem(id: Long, viiteId: Long, nimi: Kielistetty) {
  def this(viite: TutkinnonosaViite) = {
    this(
      viite.tutkinnonOsa.get.id,
      viite.id,
      viite.tutkinnonOsa.get.nimi match {
        case Some(nimi) =>
          Map(Fi -> nimi.fi.getOrElse(""), Sv -> nimi.sv.getOrElse(""), En -> nimi.en.getOrElse("")).filter(v =>
            v._2.nonEmpty
          )
        case _ => Map()
      }
    )
  }
}

case class OsaamisalaItem(nimi: Map[String, String], uri: String) {
  def toKoodiUri(): KoodiUri =
    KoodiUri(uri, 1, nimi map { case (kieli, nimiItem) => (Kieli.withName(kieli.toLowerCase), nimiItem) })
}

case class OsaamisalaTopItem(osaamisala: Option[OsaamisalaItem])

object EPerusteKoodiClient extends EPerusteKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val cacheTTL = 15.minutes

  implicit val ePerusteToKoodiuritCache: Cache[Long, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()
  implicit val ePerusteToTutkinnonosaCache: Cache[Long, Seq[TutkinnonOsaServiceItem]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()
  implicit val ePerusteToOsaamisalaCache: Cache[Long, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(cacheTTL)
    .build()

  private def getKoulutusKoodiUritForEPerusteFromEPerusteetService(ePerusteId: Long): Seq[KoodiUri] = {
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
  }

  private def getKoulutusKoodiUritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getKoulutusKoodiUritForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(koodiUris) => koodiUris
      case Failure(exp: KoodistoQueryException) if exp.status == 404 =>
        throw KoodistoNotFoundException(s"Failed to find ePerusteet with id $ePerusteId, got response ${exp.status}, ${exp.message}")
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get ePerusteet with id $ePerusteId, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoulutusKoodiUritForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(koodiUris) => koodiUris
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(s"Failed to get ePerusteet with id $ePerusteId after retry, got response ${exp.status}, ${exp.message}")
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(s"Failed to get ePerusteet with id $ePerusteId, got response ${exp.status}, ${exp.message}")
    }
  }

  def getKoulutusKoodiUritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    try {
      val koodiUrit = ePerusteToKoodiuritCache.get(ePerusteId, ePerusteId => getKoulutusKoodiUritForEPeruste(ePerusteId))
      Right(koodiUrit)
    } catch {
      case _: KoodistoNotFoundException => Right(Seq())
      case error: Throwable => Left(error)
    }
  }

  private def getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId: Long): Seq[TutkinnonOsaServiceItem] = {
    get(urlProperties.url("eperusteet-service.tutkinnonosat-by-eperuste", ePerusteId.toString), errorHandler, followRedirects = true) {
      response => {
        parse(response).extract[List[TutkinnonosaViite]].
          filter(_.tutkinnonOsa.isDefined).map(viite => new TutkinnonOsaServiceItem(viite))
      }
    }
  }

  private def getTutkinnonosatForEPeruste(ePerusteId: Long): Seq[TutkinnonOsaServiceItem] = {
    Try[Seq[TutkinnonOsaServiceItem]] {
      getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(viiteAndIdt) => viiteAndIdt
      case Failure(exp: KoodistoQueryException) if exp.status == 404 =>
        throw KoodistoNotFoundException(s"Failed to find tutkinnonosat with id $ePerusteId, got response ${exp.status}, ${exp.message}")
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId, retrying once...")
        Try[Seq[TutkinnonOsaServiceItem]] {
          getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(viiteAndIdt) => viiteAndIdt
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId after retry, got response ${exp.status}, ${exp.message}")
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId, got response ${exp.status}, ${exp.message}")
    }
  }

  def getTutkinnonosatForEPerusteetFromCache(ePerusteIdt: Seq[Long]): Either[Throwable, Map[Long, Seq[TutkinnonOsaServiceItem]]] = {
    val ePerusteId = ePerusteIdt.head
    try {
      val viitteetAndIdtForEPeruste = ePerusteToTutkinnonosaCache.get(ePerusteId, ePerusteId => getTutkinnonosatForEPeruste(ePerusteId))
      Right(viitteetAndIdtForEPeruste)
    } catch {
      case _: KoodistoNotFoundException => Right(Map())
      case error: Throwable => Left(error)
    }
  }

  private def getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId: Long): Seq[KoodiUri] = {
    get(
      urlProperties.url("eperusteet-service.osaamisalat-by-eperuste", ePerusteId.toString),
      errorHandler,
      followRedirects = true
    ) {
      response => {
        (parse(response) \\ "reformi").extract[JObject].values.keySet.map(uri => koodiUriFromString(uri)).toSeq
      }
    }
  }

  private def getOsaamisalaKoodiuritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(koodiUrit) => koodiUrit
      case Failure(exp: KoodistoQueryException) if exp.status == 404 =>
        throw KoodistoNotFoundException(s"Failed to find osaamisalat with id $ePerusteId, got response ${exp.status}, ${exp.message}")
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get osaamisalat for ePeruste with id $ePerusteId, retrying once...")
        Try[Seq[KoodiUri]] {
          getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(koodiUrit) => koodiUrit
          case Failure(exp: KoodistoQueryException) =>
            throw new RuntimeException(
              s"Failed to get osaamisalat for ePeruste with id $ePerusteId after retry, got response ${exp.status}, ${exp.message}"
            )
        }
      case Failure(exp: KoodistoQueryException) =>
        throw new RuntimeException(
          s"Failed to get osaamisalat for ePeruste with id $ePerusteId, got response ${exp.status}, ${exp.message}"
        )
    }
  }

  def getOsaamisalaKoodiuritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    try {
      val osaamisalaKoodiUritForEPeruste = ePerusteToOsaamisalaCache.get(ePerusteId, ePerusteId => getOsaamisalaKoodiuritForEPeruste(ePerusteId))
      Right(osaamisalaKoodiUritForEPeruste)
    } catch {
      case _: KoodistoNotFoundException => Right(Seq())
      case error: RuntimeException => Left(error)
    }
  }
}
