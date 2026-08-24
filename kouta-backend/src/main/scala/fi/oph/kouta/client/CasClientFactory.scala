package fi.oph.kouta.client

import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl.asyncHttpClient

import java.util.concurrent.ThreadFactory

object CasClientFactory {

  // Vastaa CasClientBuilder.build:iä, mutta HTTP/2 on kytketty pois päältä: async-http-client 3.x
  // ottaa sen oletuksena käyttöön, eikä asetusta voi muuttaa propertyillä vaan ainoastaan
  // kutsumalla setHttp2Enabled(false).
  def build(casConfig: CasConfig): CasClient = {
    val threadFactory: ThreadFactory = BasicThreadFactory
      .builder()
      .namingPattern("async-cas-client-thread-%d")
      .daemon(true)
      .priority(Thread.NORM_PRIORITY)
      .build()

    val httpClient = asyncHttpClient(
      new DefaultAsyncHttpClientConfig.Builder()
        .setThreadFactory(threadFactory)
        .setHttp2Enabled(false)
        .build
    )

    CasClientBuilder.buildFromConfigAndHttpClient(casConfig, httpClient)
  }
}
