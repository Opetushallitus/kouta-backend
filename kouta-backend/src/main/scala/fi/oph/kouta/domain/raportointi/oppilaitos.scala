package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Kielistetty, Modified, NimettyLinkki, Osoite, Tallennettu}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}

case class OppilaitosOrOsaEnrichedDataRaporttiItem(muokkaajanNimi: Option[String] = None)

sealed trait OppilaitosOrOsaMetadataRaporttiItem {
  val wwwSivu: Option[NimettyLinkki]
  val esittelyvideo: Option[NimettyLinkki]
  val hakijapalveluidenYhteystiedot: Option[YhteystietoRaporttiItem]
  val opiskelijoita: Option[Int]
  val esittely: Kielistetty
  val jarjestaaUrheilijanAmmKoulutusta: Option[Boolean]
  val isMuokkaajaOphVirkailija: Option[Boolean]
}

case class OppilaitosOrOsaRaporttiItem(
    oid: OrganisaatioOid,
    parentOppilaitosOid: Option[OrganisaatioOid],
    tila: Julkaisutila = Tallennettu,
    esikatselu: Option[Boolean] = None,
    metadata: Option[OppilaitosOrOsaMetadataRaporttiItem] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: Option[OrganisaatioOid],
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    logo: Option[String] = None,
    modified: Option[Modified],
    enrichedData: Option[OppilaitosOrOsaEnrichedDataRaporttiItem] = None
)

case class OppilaitosMetadataRaporttiItem(
    tietoaOpiskelusta: Seq[TietoaOpiskelustaRaporttiItem] = Seq(),
    wwwSivu: Option[NimettyLinkki] = None,
    esittelyvideo: Option[NimettyLinkki] = None,
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
) extends OppilaitosOrOsaMetadataRaporttiItem

case class OppilaitoksenOsaMetadataRaporttiItem(
    wwwSivu: Option[NimettyLinkki] = None,
    esittelyvideo: Option[NimettyLinkki] = None,
    hakijapalveluidenYhteystiedot: Option[YhteystietoRaporttiItem] = None,
    esittely: Kielistetty = Map(),
    kampus: Kielistetty = Map(),
    opiskelijoita: Option[Int] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None
) extends OppilaitosOrOsaMetadataRaporttiItem

case class TietoaOpiskelustaRaporttiItem(otsikkoKoodiUri: Option[String], teksti: Kielistetty)

case class YhteystietoRaporttiItem(
    nimi: Kielistetty = Map(),
    postiosoite: Option[Osoite] = None,
    kayntiosoite: Option[Osoite] = None,
    puhelinnumero: Kielistetty = Map(),
    sahkoposti: Kielistetty = Map()
)
