package fi.oph.kouta.mocks

import fi.oph.kouta.domain.{Fi, Sv, En}


trait LokalisointiServiceMock extends ServiceMocks {
  def lResponse(key: String): String = "[" + List(Fi, En, Sv).map(lng => s"""{"category": "kouta", "key": "$key", "locale": "$lng", "value": "$key $lng"}""").mkString(",") + "]"
  lazy val DefaultLokalisointiResponse = "[]"

  val lokalisointiResponses = Map(
    "yleiset.opintopistetta" ->
      s"""[{"category": "kouta", "key": "yleiset.opintopistetta", "locale": "fi", "value": "opintopistettä"}
         ,{"category": "kouta", "key": "yleiset.opintopistetta", "locale": "sv", "value": "studiepoäng"}
         ,{"category": "kouta", "key": "yleiset.opintopistetta", "locale": "en", "value": "credits"}]""",
    "toteutuslomake.lukionYleislinjaNimiOsa" ->
      s"""[{"category": "kouta", "key": "toteutuslomake.lukionYleislinjaNimiOsa", "locale": "fi", "value": "Lukio"}
      ,{"category": "kouta", "key": "toteutuslomake.lukionYleislinjaNimiOsa", "locale": "sv", "value": "Gymnasium"}
      ,{"category": "kouta", "key": "toteutuslomake.lukionYleislinjaNimiOsa", "locale": "en", "value": "High school"}]""",
    "yleiset.vaativanaErityisenaTukena" ->
      s"""[{"category": "kouta", "key": "yleiset.vaativanaErityisenaTukena", "locale": "fi", "value": "vaativana erityisenä tukena"}
         ,{"category": "kouta", "key": "yleiset.vaativanaErityisenaTukena", "locale": "sv", "value": "krävande särskilt stöd"}]""",
  )

  def mockLokalisointiResponse(key: String): Unit =
    mockGet(getMockPath("lokalisointi-service.localisation"), Map("key" -> key),
      lResponse(key))
}

object LokalisointiServiceMock extends LokalisointiServiceMock
