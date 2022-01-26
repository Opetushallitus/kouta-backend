package fi.oph.kouta.domain

case class KoulutustyyppiToOppilaitostyyppiResult(
    koulutustyypitToOppilaitostyypit: Seq[KoulutustyyppiToOppilaitostyypit]
)

case class KoulutustyyppiToOppilaitostyypit(koulutustyyppi: Koulutustyyppi, oppilaitostyypit: Seq[String])
