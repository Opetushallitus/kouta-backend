package fi.oph.kouta.service

import fi.oph.kouta.domain.{Haku, Hakukohde, Organisaatio, PaateltyAlkamiskausi, Tallennettu}
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.service.yos.YosConstants.{KOULUTUSASTE_ALEMMAT, KOULUTUSASTE_YLEMMAT}
import fi.oph.kouta.service.yos.{YosHakukohde, YosPredicate}
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
  val hakemusmaksullinenKaksoistutkintoKohdejoukonTarkenneUri = "haunkohdejoukontarkenne_11"
  val jatkotutkintoKohdejoukonTarkenneUri = "haunkohdejoukontarkenne_3"

  def kuuluukoHakukohdeYossinpiiriin(haku: Option[Haku], hakukohde: Hakukohde, paateltyAlkamiskausi: Option[PaateltyAlkamiskausi], johtaaTutkintoon: Option[Boolean], koulutusasteKoodiUrit: Seq[String]): Boolean = {
    try {
      (haku, hakukohde.jarjestyspaikkaOid) match {
        case (None, _) =>
          logger.warn(s"Virhe yos-päättelyssä: hakukohteelle ${hakukohde.oid} ei löytynyt hakua")
          false
        case (_, None) =>
          logger.warn(s"Virhe yos-päättelyssä: hakukohteelle ${hakukohde.oid} ei löytynyt järjestyspaikkaa")
          false
        case (Some(h), _) if h.tila == Tallennettu || hakukohde.tila == Tallennettu =>
          logger.info(s"Hakukohde ${hakukohde.oid} haussa ${hakukohde.hakuOid} ei kuulu YOS piiriin, koska haku tai hakukohde on luonnos")
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
            val yosHakukohde = muodostaYosHakukohde(haku.get, hakukohde, organisaatio.get, paateltyAlkamiskausi, johtaaTutkintoon, koulutusasteKoodiUrit)
            logger.info(
              s"""Tarkistetaan kuuluuko hakukohde ${hakukohde.oid} haussa ${hakukohde.hakuOid} YOSin piiriin.
                 |Hakukohteen arvot ovat:
                 | korkeakoulutus: ${yosHakukohde.korkeakoulutus}, tutkintoonJohtava: ${yosHakukohde.tutkintoonJohtava},
                 | jatkoTutkinto: ${yosHakukohde.jatkoTutkinto}, kaksoisTutkinto: ${yosHakukohde.kaksoisTutkinto},
                 | organisaatioJaVanhemmat: ${yosHakukohde.organisaatioJaVanhemmat.mkString(", ")}, koulutusAste: ${yosHakukohde.koulutusAste},
                 | haunAlkamisaika: ${yosHakukohde.haunAlkamisaika.map(_.toString).orNull}, koulutuksenAlkamisvuosi: ${yosHakukohde.koulutuksenAlkamisvuosi.orNull}""".stripMargin)
            val kuuluukoYOSsinPiiriin = YosPredicate.kuuluukoHakukohdeYosinPiiriin(yosHakukohde)
            logger.info(s"Hakukohde ${hakukohde.oid} haussa ${hakukohde.hakuOid} ${if (kuuluukoYOSsinPiiriin) "kuuluu" else "ei kuulu"} YOS piiriin: $yosHakukohde")
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

  private def muodostaYosHakukohde(haku: Haku, organisaatio: Organisaatio, paateltyAlkamiskausi: Option[PaateltyAlkamiskausi], johtaaTutkintoon: Option[Boolean], koulutusasteKoodiUrit: Seq[String]): YosHakukohde = {

    val koulutusAste = getKoulutusAsteHakukohteelle(koulutusasteKoodiUrit)

    val haunAlkamisaika = haku.hakuajat.map(_.alkaa).sortWith(_.isBefore(_)).headOption
    val koulutuksenAlkamisvuosi = paateltyAlkamiskausi.map(_.vuosi)
    val organisaatioJaVanhemmat: List[String] = organisaatio.parentOids.map(_.s) :+ organisaatio.oid

    YosHakukohde(isKorkeakouluHaku(haku), johtaaTutkintoon.getOrElse(false), isJatkotutkinto(haku),
      isErasmusMundusTaiKaksoistutkinto(haku), organisaatioJaVanhemmat, koulutusAste, haunAlkamisaika, koulutuksenAlkamisvuosi)
  }

  private def getKoulutusAsteHakukohteelle(koulutusasteKoodiUrit: Seq[String]): YosKoulutusAsteLuokka = {
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
    kohdejoukkoPrefix.equals(erasmusMundusTaiKaksoisTutkintoKohdejoukonTarkenneUri) ||
      kohdejoukkoPrefix.equals(hakemusmaksullinenKaksoistutkintoKohdejoukonTarkenneUri)
  }

  def isJatkotutkinto(haku: Haku): Boolean = {
    val kohdejoukkoPrefix = haku.kohdejoukonTarkenneKoodiUri.flatMap(_.split("#").headOption).getOrElse("")
    kohdejoukkoPrefix.equals(jatkotutkintoKohdejoukonTarkenneUri)
  }

}
