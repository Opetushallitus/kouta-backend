package fi.oph.kouta.mocks

import fi.oph.kouta.client.HakemusPalveluClient

import java.util.UUID

class MockHakemusPalveluClient extends HakemusPalveluClient {
  override def isExistingAtaruId(ataruId: UUID): Boolean = false
}
