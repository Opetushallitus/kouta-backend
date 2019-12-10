package fi.oph.kouta.client

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Kielistetty
import fi.oph.kouta.domain.oid.{KoulutusOid, Oid, OrganisaatioOid, UserOid}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

object KoutaIndexClient extends KoutaIndexClient

trait KoutaIndexClient extends HttpClient with Logging with KoutaJsonFormats {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def searchKoulutukset(koulutusOids: Seq[KoulutusOid], params: Map[String, String]): KoulutusResult = {
    val queryParams = (params + ("oids" -> koulutusOids.map(_.s).sorted.mkString(",")))
    val url = urlProperties.url("kouta-index.koulutus.filtered-list", toQueryParams(queryParams.toSeq:_*))
      get(url, followRedirects = true) { response =>
        parse(response).extract[KoulutusResult]
      }
  }
}

case class KoulutusResult(totalCount: Int,
                          result: Seq[KoulutusItem])

object KoulutusResult {
  def apply(): KoulutusResult = new KoulutusResult(0, Seq())
}

case class KoulutusItem(oid: KoulutusOid,
                        nimi: Kielistetty,
                        organisaatio: Organisaatio,
                        muokkaaja: Muokkaaja,
                        modified: LocalDateTime,
                        toteutukset: Int = 0)

case class Organisaatio(oid: OrganisaatioOid,
                        nimi: Kielistetty,
                        paikkakunta: Paikkakunta)

case class Paikkakunta(koodiUri: String,
                       nimi: Kielistetty)

case class Muokkaaja(nimi: String,
                     oid: UserOid)