package fi.oph.kouta.mocks

trait KoodistoServiceMock extends ServiceMocks {
  def kResponse(keys:List[String]) = "[" + keys.map(key => s"""{
    "koodiUri": "$key",
    "metadata": [
    {
      "nimi": "$key fi",
      "kieli": "FI"
    },
    {
      "nimi": "$key sv",
      "kieli": "SV"
    }]}""").mkString(",") + "]"

  lazy val DefaultKoodistoResponse = "[]"

  def mockKoodistoResponse(koodisto: String, koodiArvot: List[String]): Unit =
    mockGet(getMockPath("koodisto-service.koodisto-koodit", Some(koodisto)), Map.empty, kResponse(koodiArvot))
}

object KoodistoServiceMock extends KoodistoServiceMock

