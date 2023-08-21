package fi.oph.kouta.mocks

import fi.oph.kouta.client.{KoodistoElement, KoodistoMetadataElement, KoodistoSubElement}
import fi.oph.kouta.domain.{HakutapaKoodisto, HaunKohdejoukkoKoodisto, Kielistetty, KoulutusKoodisto, OsaamisalaKoodisto, ValintakoeTyyppiKoodisto}
import org.mockserver.model.HttpRequest

object TestKoodistoElement {
  def apply(koodiUri: String, version: Int, nimi: Kielistetty): KoodistoElement = {
    KoodistoElement(koodiUri, koodiUri.split("_")(1), version, Some(KoodistoSubElement("notUsedInThisCase")), None, nimi
      .map(tuple => KoodistoMetadataElement(tuple._2, tuple._1.toString.toUpperCase)).toList, Seq.empty)
  }
}

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
        "koodiArvo": "${uri._1.split("_")(1)}",
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

  def koodiUriResponseWithMultipleKoodisto(koodiUritWithKoodisto: Seq[(String, String, Int, Option[String])]) =
    "[" + koodiUritWithKoodisto.map(uri => singleKoodiuriResponse(uri._1, (uri._2, uri._3, uri._4))).mkString(",") + "]"

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
    mockGet(path, Map.empty, koodiUriResponse("koulutus", koodiUrit))
  }

  def mockKoulutusByTutkintotyyppiFailure(koodisto: String) = {
    val path = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(koodisto))
    mockGet(path, Map.empty, s"Failure in koodisto-service for koodisto $koodisto", 500)
  }

  def mockValintakoeKoodit() = {
    val path1 = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some("valintakokeentyyppi_1"))
    val path2 = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some("valintakokeentyyppi_2"))
    val path3 = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some("valintakokeentyyppi_3"))
    val path4 = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some("valintakokeentyyppi_4"))
    val path5 = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some("valintakokeentyyppi_always-in"))
    mockGet(path1, Map.empty, koodiUriResponseWithMultipleKoodisto(Seq(
      (KoulutusKoodisto.name, "koulutus_11", 1, None),
      (HakutapaKoodisto.name, "hakutapa_02", 1, None),
      (HaunKohdejoukkoKoodisto.name, "haunkohdejoukko_13", 1, None))))
    mockGet(path2, Map.empty, koodiUriResponseWithMultipleKoodisto(Seq((HakutapaKoodisto.name, "hakutapa_01", 1, None))))
    mockGet(path3, Map.empty, koodiUriResponseWithMultipleKoodisto(Seq((HaunKohdejoukkoKoodisto.name, "haunkohdejoukko_12", 1, None))))
    mockGet(path4, Map.empty, koodiUriResponseWithMultipleKoodisto(Seq(
      (OsaamisalaKoodisto.name, "osaamisala_1791", 1, None),
      (HakutapaKoodisto.name, "hakutapa_04", 1, None),
      (HaunKohdejoukkoKoodisto.name, "haunkohdejoukko_14", 1, None))))
    mockGet(path5, Map.empty, "[]")
    val koetyypitReponse = Seq("valintakokeentyyppi_1", "valintakokeentyyppi_2", "valintakokeentyyppi_3", "valintakokeentyyppi_4", "valintakokeentyyppi_always-in")
      .map(tyyppi => (tyyppi, 1, None))
    mockKoodistoResponse("valintakokeentyyppi", koetyypitReponse)
  }

  def mockValintakoeKooditWithEmptyRelations(valintaKokeenTyypit: Seq[(String, Int, Option[String])] = Seq.empty) = {
    valintaKokeenTyypit.foreach(koe => {
      val path = getMockPath("koodisto-service.sisaltyy-ylakoodit", Some(koe._1))

      mockGet(path, Map.empty, "[]")
    })
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