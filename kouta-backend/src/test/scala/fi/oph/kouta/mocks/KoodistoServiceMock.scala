package fi.oph.kouta.mocks

import org.mockserver.model.HttpRequest

trait KoodistoServiceMock extends ServiceMockBase {
  def optionalVoimassaOloString(str: Option[String]): String =
    str match {
      case Some(str) => s""""voimassaLoppuPvm": "$str""""
      case _         => s""""voimassaLoppuPvm": null"""
    }

  def singleKoodiuriResponse(koodisto: String, uri: (String, Int, Option[String])): String =
    s"""{
        "koodiUri": "${uri._1}",
        "versio": ${uri._2},
        "metadata": [
          {
            "nimi": "nimi",
            "kieli": "FI"
          },
          {
            "nimi": "nimi sv",
            "kieli": "SV"
          }],
        "koodisto": {
          "koodistoUri": "$koodisto"
        },""" + optionalVoimassaOloString(uri._3) + "}"

  def koodiUriResponse(koodisto: String, koodiUrit: Seq[(String, Int, Option[String])]) =
    "[" + koodiUrit.map(uri => singleKoodiuriResponse(koodisto, uri)).mkString(",") + "]"

  def ePerusteResponse(voimassaoloAlkaa: Option[Long], voimassaoloLoppuu: Option[Long], koodiUrit: Seq[String], epId: String): String = {
    val alkaa = voimassaoloAlkaa.getOrElse(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30))
    val loppuu = voimassaoloLoppuu.getOrElse(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 90))
    s"""{
        "id": $epId,
        "diaarinumero": "$epId-OPH-2021",
        "voimassaoloAlkaa": $alkaa,
        "voimassaoloLoppuu": $loppuu,
        "koulutukset": [""" + koodiUrit.map(uri => s"""{"koulutuskoodiUri": "$uri"}""").mkString(",") + "]}"
  }

  def osaamisalaResponse(osaamisalaKoodiUrit: Seq[String]): String = s"""{"reformi" : {""" +
    osaamisalaKoodiUrit
      .map(uri =>
        s""""$uri": [{"osaamisala": {"uri": "$uri", "nimi": {"fi": "nimi", "sv": "nimi sv", "en": "nimi en"}}}]"""
      )
      .mkString(",") + "}}"

  def tutkinnonosaResponse(tutkinnonOsat: Seq[(Long, Long)]): String = "[" + tutkinnonOsat
    .map(osa =>
      s"""{"id": ${osa._1}, "tutkinnonOsa": {"id": ${osa._2}, "nimi": {"fi": "nimi", "sv": "nimi sv", "en": "nimi en"}}}"""
    )
    .mkString(",") + "]"

  lazy val DefaultKoodistoResponse = "[]"

  def mockKoodistoResponse(koodisto: String, koodiUrit: Seq[(String, Int, Option[String])]) = {
    val path = getMockPath("koodisto-service.koodisto-koodit", Some(koodisto))
    mockGet(path, Map.empty, koodiUriResponse(koodisto, koodiUrit))
  }

  def mockKoodistoNotFound(koodisto: String): HttpRequest = {
    val path = getMockPath("koodisto-service.koodisto-koodit", Some(koodisto))
    mockGet(path, Map.empty, s"Koodisto $koodisto not found", 404)
  }

  def mockKoodistoFailure(koodisto: String): HttpRequest = {
    val path = getMockPath("koodisto-service.koodisto-koodit", Some(koodisto))
    mockGet(path, Map.empty, s"Failure in koodisto-service for koodisto $koodisto", 500)
  }

  def mockKoulutusByTutkintotyyppiResponse(koodisto: String, koodiUrit: Seq[(String, Int, Option[String])]) = {
    val path = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(koodisto))
    mockGet(path, Map.empty, koodiUriResponse(koodisto, koodiUrit))
  }

  def mockKoulutusByTutkintotyyppiFailure(koodisto: String) = {
    val path = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(koodisto))
    mockGet(path, Map.empty, s"Failure in koodisto-service for koodisto $koodisto", 500)
  }

  def mockKoulutustyyppiResponse(
      matchingKoulutustyyppi: String,
      matchingKoodiUrit: Seq[(String, Int, Option[String])],
      otherKoulutustyypit: Seq[String] = Seq()
  ) = {
    mockGet(
      getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(matchingKoulutustyyppi)),
      Map.empty,
      koodiUriResponse("koulutus", matchingKoodiUrit)
    )
    otherKoulutustyypit.foreach(tyyppi =>
      mockGet(
        getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(tyyppi)),
        Map.empty,
        koodiUriResponse("koulutus", Seq())
      )
    )
  }

  def mockKoulutustyyppiFailure(koulutustyyppi: String) = {
    mockGet(
      getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(koulutustyyppi)),
      Map.empty,
      s"Failure in koodisto-service for koulutustyyppi $koulutustyyppi",
      500
    )
  }

  def mockLatestKoodiUriResponse(koodiUriWithoutVersion: String, version: Int): Unit = {
    val path = getMockPath("koodisto-service.latest-koodiuri", Some(koodiUriWithoutVersion))
    mockGet(path, Map.empty, singleKoodiuriResponse("notUsedInThisCase", (koodiUriWithoutVersion, version, None)))
  }

  def mockKoodiUriVersionResponse(koodiUriWithoutVersion: String, version: Int): Unit = {
    val path = getMockPath("koodisto-service.koodiuri-version", Seq(koodiUriWithoutVersion, version.toString))
    mockGet(path, Map.empty, singleKoodiuriResponse("notUsedInThisCase", (koodiUriWithoutVersion, version, None)))
  }

  def mockLatestKoodiUriFailure(koodiUriWithoutVersion: String) = {
    mockGet(
      getMockPath("koodisto-service.latest-koodiuri", Some(koodiUriWithoutVersion)),
      Map.empty,
      s"Failure in koodisto-service for koodiuri-base $koodiUriWithoutVersion",
      500
    )
  }

  def mockKoodiUriVersionFailure(koodiUriWithoutVersion: String, version: Int) = {
    mockGet(
      getMockPath("koodisto-service.koodiuri-version", Seq(koodiUriWithoutVersion, version.toString)),
      Map.empty,
      s"Failure in koodisto-service for koodiuri-base $koodiUriWithoutVersion",
      500
    )
  }

  def mockKoulutusKoodiUritForEPerusteResponse(
      ePerusteId: Long,
      voimassaoloAlkaa: Option[Long],
      voimassaoloLoppuu: Option[Long],
      koodiUrit: Seq[String] = Seq()
  ) = {
    val path = getMockPath("eperusteet-service.peruste-by-id", Some(ePerusteId.toString))
    mockGet(path, Map.empty, ePerusteResponse(voimassaoloAlkaa, voimassaoloLoppuu, koodiUrit, ePerusteId.toString))
  }

  def mockKoulutusKoodiUritForEPerusteFailure(ePerusteId: Long) = {
    mockGet(
      getMockPath("eperusteet-service.peruste-by-id", Some(ePerusteId.toString)),
      Map.empty,
      s"Failure in eperuste-service for ePerusteId $ePerusteId",
      500
    )
  }

  def mockOsaamisalaKoodiUritByEPeruste(ePerusteId: Long, osaamisalaKoodiUrit: Seq[String]) = {
    val path = getMockPath("eperusteet-service.osaamisalat-by-eperuste", Some(ePerusteId.toString))
    mockGet(path, Map.empty, osaamisalaResponse(osaamisalaKoodiUrit))
  }

  def mockOsaamisalaKoodiUritFailure(ePerusteId: Long) = {
    mockGet(
      getMockPath("eperusteet-service.osaamisalat-by-eperuste", Some(ePerusteId.toString)),
      Map.empty,
      s"Failure in eperuste-service for osaamisalat by ePerusteId $ePerusteId",
      500
    )
  }

  def mockTutkinnonOsatByEPeruste(ePerusteId: Long, tutkinnonOsat: Seq[(Long, Long)]) = {
    val path = getMockPath("eperusteet-service.tutkinnonosat-by-eperuste", Some(ePerusteId.toString))
    mockGet(path, Map.empty, tutkinnonosaResponse(tutkinnonOsat))
  }

  def mockTutkinnonOsatFailure(ePerusteId: Long) = {
    mockGet(
      getMockPath("eperusteet-service.tutkinnonosat-by-eperuste", Some(ePerusteId.toString)),
      Map.empty,
      s"Failure in eperuste-service for tutkinnonosat by ePerusteId $ePerusteId",
      500
    )
  }
}