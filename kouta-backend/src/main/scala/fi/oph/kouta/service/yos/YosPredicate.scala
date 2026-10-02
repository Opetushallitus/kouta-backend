package fi.oph.kouta.service.yos

import fi.oph.kouta.service.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

object YosPredicate {

  def kuuluukoHakutoiveYosinPiiriin(hakutoive: YosHakutoive, tarkistaVoimassaOlo: Boolean = true): Boolean = {
    if (tarkistaVoimassaOlo && !onkoYosVoimassa(hakutoive.haunAlkamisaika, hakutoive.koulutuksenAlkamisvuosi)) {
      false
    } else {
      hakutoive match {
        case YosHakutoive(true, true, false, false, _, ALEMMAT_ASTEET, _, _) =>
          kuuluukoOrganisaatioYosinPiiriin(hakutoive.organisaatioJaVanhemmat)
        case YosHakutoive(true, true, false, false, _, YLEMMAT_JA_ALEMMAT_ASTEET, _, _) =>
          kuuluukoOrganisaatioYosinPiiriin(hakutoive.organisaatioJaVanhemmat)
        case _ =>
          false
      }
    }
  }

  private val helsinkiZone = ZoneId.of("Europe/Helsinki")
  private val YOS_HAKUAIKA_ALKU_RAJA = LocalDateTime.parse("2026-08-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(helsinkiZone)
  private val YOS_KOULUTUKSEN_ALKAMISVUOSI_ALKU_RAJA = "2027"

  private def onkoYosVoimassa(haunAlkamisaika: Option[LocalDateTime], koulutuksenAlkamisvuosi: Option[String]): Boolean = {
    val yosVoimassaHakuajanPerusteella = haunAlkamisaika.map(_.atZone(helsinkiZone))
      .exists(!_.isBefore(YOS_HAKUAIKA_ALKU_RAJA))
    val yosVoimassaKoulutuksenAlkamisajanPerusteella = koulutuksenAlkamisvuosi.exists(_ >= YOS_KOULUTUKSEN_ALKAMISVUOSI_ALKU_RAJA)
    yosVoimassaHakuajanPerusteella && yosVoimassaKoulutuksenAlkamisajanPerusteella
  }


  def kuuluukoOrganisaatioYosinPiiriin(organisaatiot: List[String]): Boolean =
    YosConstants.YOS_BLACK_LISTED_ORGANIZATION_OIDS.intersect(organisaatiot).isEmpty
}

