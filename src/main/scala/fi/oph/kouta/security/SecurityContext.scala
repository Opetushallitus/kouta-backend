package fi.oph.kouta.security

import fi.oph.kouta.config.SecurityConfiguration
import fi.vm.sade.utils.cas.CasClient

trait SecurityContext {
  def casUrl: String
  def casServiceIdentifier: String
  def casClient: CasClient
  def requiredRoles: Set[Role]
}

case class ProductionSecurityContext(casUrl: String,
                                     casClient: CasClient,
                                     casServiceIdentifier: String,
                                     requiredRoles: Set[Role]) extends SecurityContext

object ProductionSecurityContext {
  def apply(config: SecurityConfiguration): ProductionSecurityContext = {
    val casClient = new CasClient(config.casUrl, org.http4s.client.blaze.defaultClient)
    ProductionSecurityContext(config.casUrl, casClient, config.casServiceIdentifier, config.requiredRoles)
  }
}
