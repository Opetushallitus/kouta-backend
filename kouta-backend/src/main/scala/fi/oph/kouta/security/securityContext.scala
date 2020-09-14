package fi.oph.kouta.security

import fi.oph.kouta.config.SecurityConfiguration
import fi.vm.sade.utils.cas.CasClient
import fi.oph.kouta.client.CallerId

trait SecurityContext {
  def casUrl: String
  def casServiceIdentifier: String
  def casClient: CasClient
}

case class ProductionSecurityContext(casUrl: String,
                                     casClient: CasClient,
                                     casServiceIdentifier: String) extends SecurityContext

object ProductionSecurityContext extends CallerId {
  def apply(config: SecurityConfiguration): ProductionSecurityContext = {
    val casClient = new CasClient(config.casUrl, org.http4s.client.blaze.defaultClient, callerId)
    ProductionSecurityContext(config.casUrl, casClient, config.casServiceIdentifier)
  }
}
