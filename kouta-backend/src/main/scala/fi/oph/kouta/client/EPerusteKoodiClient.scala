package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import scalacache.caffeine.CaffeineCache
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration.DurationInt
import scalacache.modes.sync.mode

import scala.util.Try

object EPerusteKoodiClient extends EPerusteKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val ePerusteToKoodiuritCache       = CaffeineCache[Set[String]]

  case class KoulutusKoodiUri(koulutuskoodiUri: String)
  case class EPeruste(voimassaoloLoppuu: Option[Long], koulutukset: List[KoulutusKoodiUri] = List())

  def ePerusteIdValidForKoulutusKoodiUrit(ePerusteId: Long, koulutusKoodiUrit: Seq[String]): Boolean = {
    var koodiUritForEPeruste = ePerusteToKoodiuritCache.get(ePerusteId)
    if (koodiUritForEPeruste.isEmpty) {
      koodiUritForEPeruste = Some(Set())

      val errorHandler = (_: String, status: Int, response: String) =>
        status match {
          case 404 => throw new IllegalArgumentException()
          case _ => throw new RuntimeException(s"Failed to get ePerusteet with id $ePerusteId, got response $status $response")
        }

      //urlProperties.url("eperusteet-service.peruste-by-id", ePerusteId)
      Try[Set[String]] {
        get[Set[String]]("https://virkailija.untuvaopintopolku.fi/eperusteet-service/api/perusteet/" + ePerusteId, errorHandler, followRedirects = true) {
          response => {
            val ePeruste = parse(response).extract[EPeruste]
            println(ePeruste)
            if (ePeruste.voimassaoloLoppuu.isEmpty || ePeruste.voimassaoloLoppuu.get > System.currentTimeMillis()) {
              ePeruste.koulutukset.map(_.koulutuskoodiUri).toSet
            } else {
              Set()
            }
          }
        }
      }
      ePerusteToKoodiuritCache.put(ePerusteId)(koodiUritForEPeruste.get, Some(15.minutes))
    }

    koulutusKoodiUrit.map(_.split("#").head).toSet.subsetOf(koodiUritForEPeruste.get)
  }
}
