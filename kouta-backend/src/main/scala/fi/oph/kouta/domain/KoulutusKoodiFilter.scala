package fi.oph.kouta.domain

import fi.oph.kouta.domain.filterTypes.{filterType, koulutusKoodiUri, koulutusTyyppi}

object filterTypes extends Enumeration {
  type filterType = Value

  val koulutusTyyppi, koulutusKoodiUri = Value
}

trait KoulutusKoodiFilter {
  val koulutusTyypit: Seq[String]
  val koulutusKoodiUrit: Seq[String]

  def filterType(): filterType =
    if (koulutusTyypit.nonEmpty) koulutusTyyppi else koulutusKoodiUri
}

class KoulutustyyppiFilter(override val koulutusTyypit: Seq[String])
    extends KoulutusKoodiFilter {
  val koulutusKoodiUrit = Seq()
}

// Oletus: Filterin koodiUrit eivät sisällä versiotietoa; tarkistuslogiikka ei tällä hetkellä huomioi filterissä annettujen
// koodiUrien versioita.
class KoulutusKoodiUriFilter(override val koulutusKoodiUrit: Seq[String]) extends KoulutusKoodiFilter {
  if (koulutusKoodiUrit.exists(_.contains("#")))
    throw new IllegalArgumentException(
      "Filterin koodiUrit eivät saa sisältää versiotietoa, tarkistuslogiikka ei tällä hetkellä huomioi filterissä annettujen URIen versioita."
    )
  val koulutusTyypit   = Seq()
}

case object AmmatillisetKoulutusKoodit
    extends KoulutustyyppiFilter(
      Seq(
        "koulutustyyppi_1",
        "koulutustyyppi_4",
        "koulutustyyppi_5",
        "koulutustyyppi_7",
        "koulutustyyppi_8",
        "koulutustyyppi_11",
        "koulutustyyppi_12",
        "koulutustyyppi_13",
        "koulutustyyppi_18",
        "koulutustyyppi_19",
        "koulutustyyppi_24",
        "koulutustyyppi_26",
        "koulutustyyppi_42"
      )
    )

case object AmmatillisetKoulutuskooditAllowedForKaksoistutkinto
    extends KoulutustyyppiFilter(
      Seq("koulutustyyppi_26")
    )

case object AmmatillisetPerustutkintoKoodit extends KoulutustyyppiFilter(Seq("koulutustyyppi_1"))

case object YoKoulutusKoodit
    extends KoulutustyyppiFilter(
      Seq(
        "tutkintotyyppi_13",
        "tutkintotyyppi_14",
        "tutkintotyyppi_15",
        "eqf_8"
      )
    )

case object AmkKoulutusKoodit
    extends KoulutustyyppiFilter(
      Seq("tutkintotyyppi_06", "tutkintotyyppi_07", "tutkintotyyppi_12")
    )

case object AmmOpeErityisopeJaOpoKoulutusKoodit
    extends KoulutusKoodiUriFilter(Seq("koulutus_000001", "koulutus_000002", "koulutus_000003"))

case object LukioKoulutusKoodit
    extends KoulutusKoodiUriFilter(
      Seq("koulutus_309902", "koulutus_301102", "koulutus_301101", "koulutus_301103", "koulutus_301104")
    )

case object LukioKoulutusKooditAllowedForKaksoistutkinto
    extends KoulutusKoodiUriFilter(
      Seq("koulutus_301101", "koulutus_309902")
    )

case object ErikoislaakariKoulutusKoodit
    extends KoulutusKoodiUriFilter(Seq("koulutus_775101", "koulutus_775201", "koulutus_775301"))

case object AmmKoulutusKooditWithoutEperuste
    extends KoulutusKoodiUriFilter(Seq("koulutus_381501", "koulutus_381502", "koulutus_381503", "koulutus_381521"))
