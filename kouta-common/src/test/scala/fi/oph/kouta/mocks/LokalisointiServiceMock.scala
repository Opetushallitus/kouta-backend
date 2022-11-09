package fi.oph.kouta.mocks

import fi.oph.kouta.domain.{Fi, Sv, En}


trait LokalisointiServiceMock extends ServiceMockBase {
  def lResponse(key: String): String = "[" + List(Fi, En, Sv).map(lng => s"""{"category": "kouta", "key": "$key", "locale": "$lng", "value": "$key $lng"}""").mkString(",") + "]"

  def mockLokalisointiResponse(key: String): Unit =
    mockGet(getMockPath("lokalisointi-service.localisation"), Map("key" -> key),
      lResponse(key))
}
