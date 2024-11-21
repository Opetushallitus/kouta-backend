package fi.oph.kouta.security

import fi.oph.kouta.config.{KoutaConfigurationFactory, SecurityConfiguration}
import fi.oph.kouta.client.CallerId
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}

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
    val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
      "",
      "",
      config.casUrl,
      "",
      callerId,
      callerId,
      "").build

    val casClient: CasClient = CasClientBuilder.build(casConfig)
    ProductionSecurityContext(config.casUrl, casClient, config.casServiceIdentifier)
  }
}
