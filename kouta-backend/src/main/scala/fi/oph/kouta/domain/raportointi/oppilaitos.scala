package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{
  Julkaisutila,
  Kieli,
  Kielistetty,
  Modified,
  NimettyLinkki,
  Osoite,
  Tallennettu,
}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}

case class OppilaitosEnrichedDataRaporttiItem(muokkaajanNimi: Option[String] = None)

case class OppilaitosRaporttiItem(
    oid: OrganisaatioOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    metadata: Option[OppilaitosMetadataRaporttiItem] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    logo: Option[String] = None,
    modified: Option[Modified] = None,
    _enrichedData: Option[OppilaitosEnrichedDataRaporttiItem] = None,
    osat: Seq[OppilaitoksenOsaRaporttiItem] = Seq()
)

case class OppilaitoksenOsaRaporttiItem(
    oid: OrganisaatioOid,
    oppilaitosOid: OrganisaatioOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    metadata: Option[OppilaitoksenOsaMetadataRaporttiItem] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    modified: Option[Modified] = None,
    _enrichedData: Option[OppilaitosEnrichedDataRaporttiItem] = None
)

case class OppilaitosMetadataRaporttiItem(
    tietoaOpiskelusta: Seq[TietoaOpiskelustaRaporttiItem] = Seq(),
    wwwSivu: Option[NimettyLinkki] = None,
    some: Map[String, Option[String]] = Map(),
    hakijapalveluidenYhteystiedot: Option[YhteystietoRaporttiItem] = None,
    esittely: Kielistetty = Map(),
    opiskelijoita: Option[Int] = None,
    korkeakouluja: Option[Int] = None,
    tiedekuntia: Option[Int] = None,
    kampuksia: Option[Int] = None,
    yksikoita: Option[Int] = None,
    toimipisteita: Option[Int] = None,
    akatemioita: Option[Int] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None
)

case class TietoaOpiskelustaRaporttiItem(otsikkoKoodiUri: String, teksti: Kielistetty)

case class OppilaitoksenOsaMetadataRaporttiItem(
    wwwSivu: Option[NimettyLinkki] = None,
    hakijapalveluidenYhteystiedot: Option[YhteystietoRaporttiItem] = None,
    opiskelijoita: Option[Int] = None,
    kampus: Kielistetty = Map(),
    esittely: Kielistetty = Map(),
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
)

case class YhteystietoRaporttiItem(
    nimi: Kielistetty = Map(),
    postiosoite: Option[Osoite] = None,
    kayntiosoite: Option[Osoite] = None,
    puhelinnumero: Kielistetty = Map(),
    sahkoposti: Kielistetty = Map()
)
