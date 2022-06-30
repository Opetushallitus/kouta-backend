package fi.oph.kouta.mocks

trait KoodistoServiceMock extends ServiceMocks {

  def optionalVoimassaOloString(str: Option[String]): String =
    str match {
      case Some(str) => s""""voimassaLoppuPvm": "$str""""
      case _ => s""""voimassaLoppuPvm": null"""
    }

  def koodiUriResponse(koodisto: String, koodiUrit: Seq[(String, Int, Option[String])]) = "[" + koodiUrit.map(uri =>
    s"""{
        "koodiUri": "${uri._1}",
        "versio": ${uri._2},
        "metadata": [
          {
            "nimi": "${uri._1} fi",
            "kieli": "FI"
          },
          {
            "nimi": "${uri._1} sv",
            "kieli": "SV"
          }],
        "koodisto": {
          "koodistoUri": "$koodisto"
        },""" + optionalVoimassaOloString(uri._3) + "}").mkString(",") + "]"

  def latestKoodiUriResponse(koodiUriWithoutVersion: String, version: Int): String =
    s"""{"koodiUri": "$koodiUriWithoutVersion", "versio": $version}"""

  def ePerusteResponse(voimassaoloLoppuu: Option[Long], koodiUrit: Seq[String]): String = s"""{
    "voimassaoloLoppuu": ${voimassaoloLoppuu.getOrElse(System.currentTimeMillis() + (5 * 60 * 1000))},
    "koulutukset": [""" + koodiUrit.map(uri =>
      s"""{"koulutuskoodiUri": "$uri"}""").mkString(",") + "]}"

  def osaamisalaResponse(osaamisalaKoodiUrit: Seq[String]): String = s"""{"reformi" : {""" +
    osaamisalaKoodiUrit.map(uri => s""""$uri": []""").mkString(",") + "}}"

  def tutkinnonosaResponse(tutkinnonOsat: Seq[(Long, Long)]): String = "[" + tutkinnonOsat.map(osa =>
    s"""{"id": ${osa._1}, "tutkinnonOsa": {"id": ${osa._2}}}""").mkString(",") + "]"

  lazy val DefaultKoodistoResponse = "[]"

  def mockKoodistoResponse(koodisto: String, koodiUrit: Seq[(String, Int, Option[String])]) = {
    val path = getMockPath("koodisto-service.koodisto-koodit", Some(koodisto))
    mockGet(path, Map.empty, koodiUriResponse(koodisto, koodiUrit))
  }

  def mockKoulutustyyppiResponse(matchingKoulutustyyppi: String, matchingKoodiUrit: Seq[(String, Int, Option[String])], otherKoulutustyypit: Seq[String] = Seq()) = {
    mockGet(getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(matchingKoulutustyyppi)), Map.empty, koodiUriResponse("koulutus", matchingKoodiUrit))
    otherKoulutustyypit.foreach(tyyppi =>
      mockGet(getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(tyyppi)), Map.empty, koodiUriResponse("koulutus", Seq()))
    )
  }

  def mockLatestKoodiUriResponse(koodiUriWithoutVersion: String, version: Int): Unit = {
    val path = getMockPath("koodisto-service.latest-koodiuri", Some(koodiUriWithoutVersion))
    mockGet(path, Map.empty, latestKoodiUriResponse(koodiUriWithoutVersion, version))
  }

  def mockKoulutusKoodiUritForEPerusteResponse(ePerusteId: Long, voimassaoloLoppuu: Option[Long], koodiUrit: Seq[String] = Seq()) = {
    val path = getMockPath("eperusteet-service.peruste-by-id", Some(ePerusteId.toString))
    mockGet(path, Map.empty, ePerusteResponse(voimassaoloLoppuu, koodiUrit))
  }

  def mockOsaamisalaKoodiUritByEPeruste(ePerusteId: Long, osaamisalaKoodiUrit: Seq[String]) = {
    val path = getMockPath("eperusteet-service.osaamisalat-by-eperuste", Some(ePerusteId.toString))
    mockGet(path, Map.empty, osaamisalaResponse(osaamisalaKoodiUrit))
  }

  def mockTutkinnonOsatByEPeruste(ePerusteId: Long, tutkinnonOsat: Seq[(Long, Long)]) = {
    val path = getMockPath("eperusteet-service.tutkinnonosat-by-eperuste", Some(ePerusteId.toString))
    mockGet(path, Map.empty, tutkinnonosaResponse(tutkinnonOsat))
  }
}

object KoodistoServiceMock extends KoodistoServiceMock

