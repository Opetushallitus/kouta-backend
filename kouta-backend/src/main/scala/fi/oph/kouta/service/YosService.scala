package fi.oph.kouta.service

import fi.oph.kouta.domain.{Haku, Hakukohde, Organisaatio, PaateltyAlkamiskausi, Tallennettu}
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.service.yos.YosConstants.{KOULUTUSASTE_ALEMMAT, KOULUTUSASTE_YLEMMAT}
import fi.oph.kouta.service.yos.{YosHakutoive, YosPredicate}
import fi.oph.kouta.service.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_JA_ALEMMAT_ASTEET, YosKoulutusAsteLuokka}
import fi.oph.kouta.util.OrganisaatioServiceUtil


object YosService
  extends YosService(
    OrganisaatioServiceImpl,
    KoodistoService
  )

class YosService (
                   val organisaatioService: OrganisaatioServiceImpl,
                   koodistoService: KoodistoService) extends Logging {


  val korkeakouluHaunKohdeJoukkoUri = "haunkohdejoukko_12"

  val erasmusMundusTaiKaksoisTutkintoKohdejoukonTarkenneUri = "haunkohdejoukontarkenne_010"
  val jatkotutkintoKohdejoukonTarkenneUri = "haunkohdejoukontarkenne_3"

  def kuuluukoHakutoiveYossinpiiriin(haku: Option[Haku], hakukohde: Hakukohde, paateltyAlkamiskausi: Option[PaateltyAlkamiskausi], johtaaTutkintoon: Option[Boolean], koulutusasteKoodiUrit: Seq[String]): Boolean = {
    try {
      (haku, hakukohde.jarjestyspaikkaOid) match {
        case (None, _) =>
          logger.warn(s"Virhe yos-päättelyssä: hakukohteelle ${hakukohde.oid} ei löytynyt hakua")
          false
        case (_, None) =>
          logger.warn(s"Virhe yos-päättelyssä: hakukohteelle ${hakukohde.oid} ei löytynyt järjestyspaikkaa")
          false
        case (Some(h), _) if h.tila == Tallennettu || hakukohde.tila == Tallennettu =>
          logger.info(s"Hakutoive ${hakukohde.oid} haussa ${hakukohde.hakuOid} ei kuulu YOS piiriin, koska haku tai hakukohde on luonnos")
          false
        case _ =>
          val organisaatio = organisaatioService.getOrganisaatio(hakukohde.jarjestyspaikkaOid.get) match {
            case Right(organisaatio) =>
              Some(OrganisaatioServiceUtil.organisaatioServiceOrgToOrganisaatio(organisaatio))
            case Left(_) => None
          }
          if (organisaatio.isEmpty)
            false
          else {
            val yosHakutoive = muodostaYosHakutoive(haku.get, hakukohde, organisaatio.get, paateltyAlkamiskausi, johtaaTutkintoon, koulutusasteKoodiUrit)
            logger.info(
              s"""Tarkistetaan kuuluuko hakutoive ${hakukohde.oid} haussa ${hakukohde.hakuOid} YOSin piiriin.
                 |Hakutoiveen arvot ovat:
                 | korkeakoulutus: ${yosHakutoive.korkeakoulutus}, tutkintoonJohtava: ${yosHakutoive.tutkintoonJohtava},
                 | jatkoTutkinto: ${yosHakutoive.jatkoTutkinto}, kaksoisTutkinto: ${yosHakutoive.kaksoisTutkinto},
                 | organisaatioJaVanhemmat: ${yosHakutoive.organisaatioJaVanhemmat.mkString(", ")}, koulutusAste: ${yosHakutoive.koulutusAste},
                 | haunAlkamisaika: ${yosHakutoive.haunAlkamisaika.map(_.toString).orNull}, koulutuksenAlkamisvuosi: ${yosHakutoive.koulutuksenAlkamisvuosi.orNull}""".stripMargin)
            val kuuluukoYOSsinPiiriin = YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive)
            logger.info(s"Hakutoive ${hakukohde.oid} haussa ${hakukohde.hakuOid} ${if (kuuluukoYOSsinPiiriin) "kuuluu" else "ei kuulu"} YOS piiriin: $yosHakutoive")
            kuuluukoYOSsinPiiriin
          }
      }
    }
    catch {
      case e: Exception =>
        logger.error(s"Virhe yos-päättelyssä haulle ${hakukohde.hakuOid} ja hakukohteelle ${hakukohde.oid} hakukohdeOid", e)
        false
    }
  }

  private def muodostaYosHakutoive(haku: Haku, hakukohde: Hakukohde, organisaatio: Organisaatio, paateltyAlkamiskausi: Option[PaateltyAlkamiskausi], johtaaTutkintoon: Option[Boolean], koulutusasteKoodiUrit: Seq[String]): YosHakutoive = {

    val koulutusAste = getKoulutusAsteHakutoiveelle(koulutusasteKoodiUrit)

    val haunAlkamisaika = haku.hakuajat.map(_.alkaa).sortWith(_.isBefore(_)).headOption
    val koulutuksenAlkamisvuosi = paateltyAlkamiskausi.map(_.vuosi)
    val organisaatioJaVanhemmat: List[String] = organisaatio.parentOids.map(_.s) :+ organisaatio.oid

    YosHakutoive(isKorkeakouluHaku(haku), johtaaTutkintoon.getOrElse(false), isJatkotutkinto(haku),
      isErasmusMundusTaiKaksoistutkinto(haku), organisaatioJaVanhemmat, koulutusAste, haunAlkamisaika, koulutuksenAlkamisvuosi)
  }

  private def getKoulutusAsteHakutoiveelle(koulutusasteKoodiUrit: Seq[String]): YosKoulutusAsteLuokka = {
    val koodit = koulutusasteKoodiUrit.map(_.split("_").last)
    val containsAlempi: Boolean = koodit.exists(k => KOULUTUSASTE_ALEMMAT.contains(k))
    val containsYlempi: Boolean = koodit.exists(k => KOULUTUSASTE_YLEMMAT.contains(k))
    (containsAlempi, containsYlempi) match {
      case (_, true) =>
        YLEMMAT_JA_ALEMMAT_ASTEET
      case (true, false) =>
        ALEMMAT_ASTEET
      case _ =>
        EI_YOS_KOULUTUSASTETTA
    }
  }

  def isKorkeakouluHaku(haku: Haku): Boolean = {
    val kohdejoukkoPrefix = haku.kohdejoukkoKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    kohdejoukkoPrefix.equals(korkeakouluHaunKohdeJoukkoUri)
  }

  def isErasmusMundusTaiKaksoistutkinto(haku: Haku): Boolean = {
    val kohdejoukkoPrefix = haku.kohdejoukonTarkenneKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    kohdejoukkoPrefix.equals(erasmusMundusTaiKaksoisTutkintoKohdejoukonTarkenneUri)
  }

  def isJatkotutkinto(haku: Haku): Boolean = {
    val kohdejoukkoPrefix = haku.kohdejoukonTarkenneKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    kohdejoukkoPrefix.equals(jatkotutkintoKohdejoukonTarkenneUri)
  }

}
