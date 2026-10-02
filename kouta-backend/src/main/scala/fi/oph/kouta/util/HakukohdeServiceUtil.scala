package fi.oph.kouta.util

import fi.oph.kouta.domain.{
  AlkamiskausiJaVuosi,
  Haku,
  Hakukohde,
  KoulutuksenAlkamiskausi,
  Koulutus,
  PaateltyAlkamiskausi,
  TarkkaAlkamisajankohta,
  Toteutus,
  ToteutusMetadata,
  TuvaToteutusMetadata
}

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
    parseAlkamiskausi(hakukohde.metadata.flatMap(_.koulutuksenAlkamiskausi), hakukohde.oid.map(_.s))
      .orElse(parseAlkamiskausi(haku.flatMap(_.metadata).flatMap(_.koulutuksenAlkamiskausi), haku.flatMap(_.oid).map(_.s)))
      .orElse(
        parseAlkamiskausi(
          toteutus.flatMap(_.metadata).flatMap(_.opetus).flatMap(_.koulutuksenAlkamiskausi),
          toteutus.flatMap(_.oid).map(_.s)
        )
      )

  // Hakukohteen tutkintoon johtavuus päätellään hakukohteen toteutuksen koulutukselta.
  // Vastaa aiempaa kouta-indeksoijan logiikkaa (johtaa-tutkintoon?).
  def paatteleJohtaaTutkintoon(koulutus: Option[Koulutus]): Option[Boolean] =
    koulutus.map(_.johtaaTutkintoon)

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
}
