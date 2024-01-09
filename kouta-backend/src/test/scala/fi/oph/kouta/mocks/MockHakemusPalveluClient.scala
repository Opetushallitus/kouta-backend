package fi.oph.kouta.mocks

import fi.oph.kouta.client.{HakemusPalveluClient, HakukohdeInfo}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, itemFound, itemNotFound}

import java.util.UUID

class MockHakemusPalveluClient() extends HakemusPalveluClient {
  var ensisijainenApplicationCounts: HakukohdeApplicationCounts = Map()

  override def isExistingAtaruIdFromCache(ataruId: UUID): ExternalQueryResult = itemFound
  override def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult = itemNotFound

  override def getHakukohdeInfo(hakukohdeOid: HakukohdeOid): HakukohdeInfo = HakukohdeInfo(applicationCount = 0)

  override def getEnsisijainenApplicationCounts(
      hakuOid: HakuOid
  ): HakukohdeApplicationCounts =
    ensisijainenApplicationCounts
}
