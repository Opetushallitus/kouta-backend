package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Kielistetty, Koulutustyyppi, Modified, Sisalto, Tallennettu}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}

import java.util.UUID

case class ValintaperusteEnrichedDataRaporttiItem(muokkaajanNimi: Option[String] = None)

case class ValintaperusteRaporttiItem(
    id: UUID,
    externalId: Option[String] = None,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Option[Boolean] = None,
    koulutustyyppi: Koulutustyyppi,
    hakutapaKoodiUri: Option[String] = None,
    kohdejoukkoKoodiUri: Option[String] = None,
    nimi: Kielistetty = Map(),
    julkinen: Option[Boolean] = None,
    valintakokeet: Seq[ValintakoeRaporttiItem] = Seq(),
    metadata: Option[ValintaperusteMetadataRaporttiItem] = None,
    organisaatioOid: Option[OrganisaatioOid],
    muokkaaja: UserOid,
    kielivalinta: Seq[Kieli] = Seq(),
    modified: Option[Modified],
    enrichedData: Option[ValintaperusteEnrichedDataRaporttiItem] = None
)

case class ValintaperusteMetadataRaporttiItem(
    tyyppi: Koulutustyyppi,
    valintatavat: Seq[ValintatapaRaporttiItem],
    kuvaus: Kielistetty,
    hakukelpoisuus: Kielistetty = Map(),
    lisatiedot: Kielistetty = Map(),
    sisalto: Seq[Sisalto] = Seq(),
    valintakokeidenYleiskuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
)

case class ValintatapaRaporttiItem(
    nimi: Kielistetty = Map(),
    valintatapaKoodiUri: Option[String] = None,
    sisalto: Seq[Sisalto],
    kaytaMuuntotaulukkoa: Option[Boolean] = None,
    kynnysehto: Kielistetty = Map(),
    enimmaispisteet: Option[Double] = None,
    vahimmaispisteet: Option[Double] = None
)
