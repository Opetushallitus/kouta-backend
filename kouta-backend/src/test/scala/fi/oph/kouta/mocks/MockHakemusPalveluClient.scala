package fi.oph.kouta.mocks

import fi.oph.kouta.client.HakemusPalveluClient
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, itemFound}

import java.util.UUID

class MockHakemusPalveluClient extends HakemusPalveluClient {
  override def isExistingAtaruId(ataruId: UUID): ExternalQueryResult = itemFound
}
