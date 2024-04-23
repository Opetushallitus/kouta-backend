package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{
  Julkaisutila,
  Kieli,
  Kielistetty,
  Koulutustyyppi,
  Modified,
  Sorakuvaus,
  SorakuvausEnrichedData,
  SorakuvausMetadata,
  Tallennettu
}

import java.util.UUID

case class SorakuvausEnrichedDataRaporttiItem(muokkaajanNimi: Option[String] = None)

case class SorakuvausRaporttiItem(
    id: UUID,
    externalId: Option[String] = None,
    tila: Julkaisutila = Tallennettu,
    nimi: Kielistetty = Map(),
    koulutustyyppi: Koulutustyyppi,
    kielivalinta: Seq[Kieli] = Seq(),
    metadata: Option[SorakuvausMetadataRaporttiItem] = None,
    organisaatioOid: Option[OrganisaatioOid],
    muokkaaja: UserOid,
    modified: Option[Modified],
    enrichedData: Option[SorakuvausEnrichedDataRaporttiItem] = None
)

case class SorakuvausMetadataRaporttiItem(
    kuvaus: Kielistetty = Map(),
    koulutusalaKoodiUri: Option[String] = None,
    koulutusKoodiUrit: Seq[String] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
)
