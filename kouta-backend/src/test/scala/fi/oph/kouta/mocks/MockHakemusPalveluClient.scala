package fi.oph.kouta.mocks

import fi.oph.kouta.client.HakemusPalveluClient
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, itemFound, itemNotFound}

import java.util.UUID

class MockHakemusPalveluClient extends HakemusPalveluClient {
  override def isExistingAtaruId(ataruId: UUID): ExternalQueryResult = itemFound
  override def isFormAllowedForHakutapa(ataruId: UUID, hakutapaKoodiUri: Option[String]): ExternalQueryResult = itemNotFound
}
