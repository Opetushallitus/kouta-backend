package fi.oph.kouta.util

import fi.oph.kouta.domain.{
  AlkamiskausiJaVuosi,
  Haku,
  Hakukohde,
  HenkilökohtainenSuunnitelma,
  KoulutuksenAlkamiskausi,
  Koulutus,
  Koulutustyyppi,
  PaateltyAlkamisajankohta,
  PaateltyAlkamiskausi,
  Tallennettu,
  TarkkaAlkamisajankohta,
  Toteutus,
  ToteutusMetadata,
  TuvaToteutusMetadata,
  YhdenPaikanSaanto
}

import java.time.format.DateTimeFormatter
import scala.util.Try

object HakukohdeServiceUtil {
  def getJarjestetaanErityisopetuksena(toteutuksenMetadata: ToteutusMetadata): Boolean = {
    toteutuksenMetadata match {
      case tuva: TuvaToteutusMetadata =>
        tuva.jarjestetaanErityisopetuksena
      case _ => false
    }
  }

  // Päätellään hakukohteen koulutuksen alkamiskausi järjestyksessä hakukohteelta, haulta tai toteutukselta.
  // Vastaa aiempaa kouta-indeksoijan logiikkaa (assoc-paatelty-alkamiskausi-for-hakukohde).
  def paatteleAlkamiskausi(
      hakukohde: Hakukohde,
      haku: Option[Haku],
      toteutus: Option[Toteutus]
  ): Option[PaateltyAlkamiskausi] =
    paatteleAlkamiskausi(hakukohde.oid.map(_.s), hakukohde.metadata.flatMap(_.koulutuksenAlkamiskausi), haku, toteutus)

  def paatteleAlkamiskausi(
      hakukohdeOid: Option[String],
      hakukohteenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
      haku: Option[Haku],
      toteutus: Option[Toteutus]
  ): Option[PaateltyAlkamiskausi] =
    parseAlkamiskausi(hakukohteenAlkamiskausi, hakukohdeOid)
      .orElse(parseAlkamiskausi(haku.flatMap(_.metadata).flatMap(_.koulutuksenAlkamiskausi), haku.flatMap(_.oid).map(_.s)))
      .orElse(
        parseAlkamiskausi(
          toteutus.flatMap(_.metadata).flatMap(_.opetus).flatMap(_.koulutuksenAlkamiskausi),
          toteutus.flatMap(_.oid).map(_.s)
        )
      )

  // Päätellään hakukohteen koulutuksen alkamisajankohta (päivämäärä) samassa järjestyksessä kuin alkamiskausi.
  // Henkilökohtainen suunnitelma -tieto otetaan vain hakukohteelta. Jos alkamisajankohtaa ei löydy,
  // palautetaan tieto ilman päivämäärää. Vastaa aiempaa kouta-indeksoijan logiikkaa
  // (assoc-paatelty-alkamisaika-for-hakukohde).
  def paatteleAlkamisajankohta(
      hakukohde: Hakukohde,
      haku: Option[Haku],
      toteutus: Option[Toteutus]
  ): Option[PaateltyAlkamisajankohta] =
    paatteleAlkamisajankohta(hakukohde.oid.map(_.s), hakukohde.metadata.flatMap(_.koulutuksenAlkamiskausi), haku, toteutus)

  def paatteleAlkamisajankohta(
      hakukohdeOid: Option[String],
      hakukohteenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
      haku: Option[Haku],
      toteutus: Option[Toteutus]
  ): Option[PaateltyAlkamisajankohta] =
    hakukohdeOid.map { hakukohdeOid =>
      val henkilokohtainenSuunnitelma =
        hakukohteenAlkamiskausi.flatMap(_.alkamiskausityyppi).contains(HenkilökohtainenSuunnitelma)
      parseAlkamisajankohta(hakukohteenAlkamiskausi, Some(hakukohdeOid))
        .orElse(parseAlkamisajankohta(haku.flatMap(_.metadata).flatMap(_.koulutuksenAlkamiskausi), haku.flatMap(_.oid).map(_.s)))
        .orElse(
          parseAlkamisajankohta(
            toteutus.flatMap(_.metadata).flatMap(_.opetus).flatMap(_.koulutuksenAlkamiskausi),
            toteutus.flatMap(_.oid).map(_.s)
          )
        )
        .map(_.copy(henkilokohtainenSuunnitelma = henkilokohtainenSuunnitelma))
        .getOrElse(PaateltyAlkamisajankohta(None, None, hakukohdeOid, henkilokohtainenSuunnitelma))
    }

  // Hakukohteen tutkintoon johtavuus päätellään hakukohteen toteutuksen koulutukselta.
  // Vastaa aiempaa kouta-indeksoijan logiikkaa (johtaa-tutkintoon?).
  def paatteleJohtaaTutkintoon(koulutus: Option[Koulutus]): Option[Boolean] =
    koulutus.map(_.johtaaTutkintoon)

  // Haun kohdejoukon tarkenteet, joilla haun hakukohteet voivat kuulua yhden paikan säännön piiriin.
  // Muut tarkenteet (esim. Högskolan på Åland) rajaavat hakukohteet yhden paikan säännön ulkopuolelle.
  private val YpsKohdejoukonTarkenteet: Seq[String] = Seq(
    "haunkohdejoukontarkenne_3#",   // jatkotutkintohaku
    "haunkohdejoukontarkenne_010#", // Erasmus Mundus
    "haunkohdejoukontarkenne_11#"   // hakemusmaksulliset kv-kaksoistutkinto-ohjelmat
  )

  val YpsVoimassa: YhdenPaikanSaanto = YhdenPaikanSaanto(voimassa = true, syy = "Hakukohde on yhden paikan säännön piirissä")

  private def eiYps(syy: String): YhdenPaikanSaanto = YhdenPaikanSaanto(voimassa = false, syy = syy)

  // Päätellään, kuuluuko hakukohde yhden paikan säännön (YPS) piiriin.
  // Vastaa aiempaa kouta-indeksoijan logiikkaa (assoc-yps).
  def paatteleYhdenPaikanSaanto(
      hakukohde: Hakukohde,
      haku: Option[Haku],
      koulutus: Option[Koulutus],
      paateltyAlkamiskausi: Option[PaateltyAlkamiskausi]
  ): YhdenPaikanSaanto = {
    val kohdejoukonTarkenne = haku.flatMap(_.kohdejoukonTarkenneKoodiUri).filter(_.trim.nonEmpty)
    if (haku.isEmpty)
      eiYps("Hakukohteen hakua ei löytynyt")
    else if (haku.exists(_.tila == Tallennettu))
      eiYps("Haku on luonnos tilassa")
    else if (hakukohde.tila == Tallennettu)
      eiYps("Hakukohde on luonnos tilassa")
    else if (!koulutus.exists(k => Koulutustyyppi.isKorkeakoulu(k.koulutustyyppi)))
      eiYps("Ei korkeakoulutus koulutusta")
    else if (!koulutus.exists(_.johtaaTutkintoon))
      eiYps("Ei tutkintoon johtavaa koulutusta")
    else if (alkamiskausiEnnenSyksya2016(paateltyAlkamiskausi))
      eiYps("Koulutuksen alkamiskausi on ennen syksyä 2016")
    else
      kohdejoukonTarkenne match {
        case Some(tarkenne) if !YpsKohdejoukonTarkenteet.exists(tarkenne.startsWith) =>
          eiYps(s"Haun kohdejoukon tarkenne on $tarkenne")
        case _ => YpsVoimassa
      }
  }

  private def alkamiskausiEnnenSyksya2016(paateltyAlkamiskausi: Option[PaateltyAlkamiskausi]): Boolean =
    paateltyAlkamiskausi.exists { ak =>
      Try(ak.vuosi.trim.toInt).toOption.exists(vuosi => vuosi < 2016 || (vuosi == 2016 && ak.kausiUri.startsWith("kausi_k#")))
    }

  private def parseAlkamiskausi(
      alkamiskausi: Option[KoulutuksenAlkamiskausi],
      sourceOid: Option[String]
  ): Option[PaateltyAlkamiskausi] =
    for {
      ak     <- alkamiskausi
      tyyppi <- ak.alkamiskausityyppi
      (kausiUri, vuosi) <- tyyppi match {
        case TarkkaAlkamisajankohta =>
          ak.koulutuksenAlkamispaivamaara.map(pvm =>
            (if (pvm.getMonthValue < 8) "kausi_k#1" else "kausi_s#1", pvm.getYear.toString)
          )
        case AlkamiskausiJaVuosi =>
          for {
            kausi <- ak.koulutuksenAlkamiskausiKoodiUri
            vuosi <- ak.koulutuksenAlkamisvuosi
          } yield (kausi, vuosi)
        case _ => None
      }
      source <- sourceOid
    } yield PaateltyAlkamiskausi(tyyppi, kausiUri, vuosi, source)

  // Sama muoto kuin päivämäärillä JSONissa (GenericKoutaFormats.ISO_LOCAL_DATE_TIME_FORMATTER)
  private val AlkamisajankohtaFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  private def parseAlkamisajankohta(
      alkamiskausi: Option[KoulutuksenAlkamiskausi],
      sourceOid: Option[String]
  ): Option[PaateltyAlkamisajankohta] =
    for {
      ak     <- alkamiskausi
      tyyppi <- ak.alkamiskausityyppi
      pvm <- tyyppi match {
        case TarkkaAlkamisajankohta =>
          ak.koulutuksenAlkamispaivamaara.map(AlkamisajankohtaFormatter.format)
        case AlkamiskausiJaVuosi =>
          for {
            kausi <- ak.koulutuksenAlkamiskausiKoodiUri
            vuosi <- ak.koulutuksenAlkamisvuosi
          } yield s"$vuosi-${if (kausi == "kausi_k#1") "01-01" else "08-01"}"
        case _ => None
      }
      source <- sourceOid
    } yield PaateltyAlkamisajankohta(Some(tyyppi), Some(pvm), source, henkilokohtainenSuunnitelma = false)
}
