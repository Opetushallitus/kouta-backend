package fi.oph.kouta.mocks

import fi.oph.kouta.client.HakemusPalveluClient

import java.util.UUID

class MockHakemusPalveluClient extends HakemusPalveluClient {
  override def isExistingAtaruId(ataruId: UUID): Boolean = {
    if (ataruId == UUID.fromString("c6eead54-a075-44db-b0c5-ba99915958d4")) {
      false
    } else {
      true
    }
  }
}
