package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, splitToBaseAndVersion}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain._
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.properties.OphProperties
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Right, Success, Try}

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
    koodiUriFromString(uri).copy(nimi = nimi map { case (kieli, nimiItem) =>
      (Kieli.withName(kieli.toLowerCase), nimiItem)
    })
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
  }

  private def getKoulutusKoodiUritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getKoulutusKoodiUritForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(koodiUris) => koodiUris
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get ePerusteet with id $ePerusteId, retrying once...")
        Try[Seq[KoodiUri]] {
          getKoulutusKoodiUritForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(koodiUris)    => koodiUris
          case Failure(t: Throwable) => throw exception(t, ePerusteId, "ePerusteet", true)
        }
      case Failure(t: Throwable) =>
        throw exception(t, ePerusteId, "ePerusteet", false)
    }
  }

  def getKoulutusKoodiUritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    try {
      val koodiUrit =
        ePerusteToKoodiuritCache.get(ePerusteId, ePerusteId => getKoulutusKoodiUritForEPeruste(ePerusteId))
      Right(koodiUrit)
    } catch {
      case _: KoodistoNotFoundException => Right(Seq())
      case error: Throwable             => Left(error)
    }
  }

  private def getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId: Long): Seq[TutkinnonOsaServiceItem] = {
    get(
      urlProperties.url("eperusteet-service.tutkinnonosat-by-eperuste", ePerusteId.toString),
      errorHandler,
      followRedirects = true
    ) { response =>
      {
        parse(response)
          .extract[List[TutkinnonosaViite]]
          .filter(_.tutkinnonOsa.isDefined)
          .map(viite => new TutkinnonOsaServiceItem(viite))
      }
    }
  }

  private def getTutkinnonosatForEPeruste(ePerusteId: Long): Seq[TutkinnonOsaServiceItem] = {
    Try[Seq[TutkinnonOsaServiceItem]] {
      getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(tutkinnonOsat) => tutkinnonOsat
      case Failure(exp: KoodistoQueryException) if exp.status == 404 =>
        logger.warn(
          s"Unable to find tutkinnonosat for ePeruste with id $ePerusteId, got response ${exp.status}, ${exp.message}"
        )
        Seq()
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get tutkinnonosat for ePeruste with id $ePerusteId, retrying once...")
        Try[Seq[TutkinnonOsaServiceItem]] {
          getTutkinnonosatForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(tutkinnonOsat) => tutkinnonOsat
          case Failure(t: Throwable)  => throw exception(t, ePerusteId, "tutkinnonosat for ePeruste", true)
        }
      case Failure(t: Throwable) => throw exception(t, ePerusteId, "tutkinnonosat for ePeruste", false)
    }
  }

  def getTutkinnonosatForEPerusteetFromCache(
      ePerusteIdt: Seq[Long]
  ): Either[Throwable, Map[Long, Seq[TutkinnonOsaServiceItem]]] = {
    Try[Map[Long, Seq[TutkinnonOsaServiceItem]]] {
      ePerusteIdt
        .map(ePerusteId =>
          ePerusteId -> ePerusteToTutkinnonosaCache
            .get(ePerusteId, ePerusteId => getTutkinnonosatForEPeruste(ePerusteId))
        )
        .toMap
    } match {
      case Success(tutkinnonOsatByIdt) => Right(tutkinnonOsatByIdt)
      case Failure(exception)          => Left(exception)
    }
  }

  private def getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId: Long): Seq[KoodiUri] = {
    get(
      urlProperties.url("eperusteet-service.osaamisalat-by-eperuste", ePerusteId.toString),
      errorHandler,
      followRedirects = true
    ) { response =>
      {
        (parse(response) \\ "reformi")
          .extract[Map[String, List[OsaamisalaTopItem]]]
          .values
          .filter(item => item.nonEmpty && item.head.osaamisala.isDefined)
          .map(_.head.osaamisala.get.toKoodiUri())
          .toSeq
      }
    }
  }

  private def getOsaamisalaKoodiuritForEPeruste(ePerusteId: Long): Seq[KoodiUri] = {
    Try[Seq[KoodiUri]] {
      getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId)
    } match {
      case Success(koodiUrit) => koodiUrit
      case Failure(exp: KoodistoQueryException) if retryStatusCodes.contains(exp.status) =>
        logger.warn(s"Failed to get osaamisalat for ePeruste with id $ePerusteId, retrying once...")
        Try[Seq[KoodiUri]] {
          getOsaamisalaKoodiuritForEPerusteFromEPerusteetService(ePerusteId)
        } match {
          case Success(koodiUrit)    => koodiUrit
          case Failure(t: Throwable) => throw exception(t, ePerusteId, "osaamisalat for ePeruste", true)
        }
      case Failure(t: Throwable) => throw exception(t, ePerusteId, "osaamisalat for ePeruste", false)
    }
  }

  def getOsaamisalaKoodiuritForEPerusteFromCache(ePerusteId: Long): Either[Throwable, Seq[KoodiUri]] = {
    try {
      val osaamisalaKoodiUritForEPeruste =
        ePerusteToOsaamisalaCache.get(ePerusteId, ePerusteId => getOsaamisalaKoodiuritForEPeruste(ePerusteId))
      Right(osaamisalaKoodiUritForEPeruste)
    } catch {
      case _: KoodistoNotFoundException => Right(Seq())
      case error: RuntimeException      => Left(error)
    }
  }

  private def exception(throwable: Throwable, ePerusteId: Long, contentDesc: String, retryDone: Boolean): Throwable = {
    val retryDoneMsg = if (retryDone) " after retry" else ""
    throwable match {
      case exp: KoodistoQueryException if exp.status == 404 =>
        KoodistoNotFoundException(
          s"Unable to find $contentDesc with id $ePerusteId, got response ${exp.status}, ${exp.message}"
        )
      case exp: KoodistoQueryException =>
        new RuntimeException(
          s"Failed to get $contentDesc with id $ePerusteId$retryDoneMsg, got response ${exp.status}, ${exp.message}"
        )
      case _ =>
        new RuntimeException(
          s"Failed to get $contentDesc with id $ePerusteId$retryDoneMsg, got response ${throwable.getMessage()}"
        )
    }
  }
}
