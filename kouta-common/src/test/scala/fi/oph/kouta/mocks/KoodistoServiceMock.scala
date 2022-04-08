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

  def koodiUriResponse(koodiUriWithoutVersion: String, version: Int): String =
    s"""{"koodiUri": "$koodiUriWithoutVersion", "versio": $version}"""

  lazy val DefaultKoodistoResponse = "[]"

  def mockKoodistoResponse(koodisto: String, koodiArvot: List[String]): Unit =
    mockGet(getMockPath("koodisto-service.koodisto-koodit", Some(koodisto)), Map.empty, kResponse(koodiArvot))

  def mockKoodiUriResponse(koodiUriWithoutVersion: String, version: Int): Unit = {
    val path = getMockPath("koodisto-service.latest-koodiuri", Some(koodiUriWithoutVersion))
    mockGet(path, Map.empty, koodiUriResponse(koodiUriWithoutVersion, version))
  }
}

object KoodistoServiceMock extends KoodistoServiceMock

