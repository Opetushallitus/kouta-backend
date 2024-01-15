package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Julkaisutila, Kieli, Kielistetty, Koulutustyyppi, Modified, Sorakuvaus, SorakuvausEnrichedData, SorakuvausMetadata, Tallennettu}

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
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified,
    _enrichedData: Option[SorakuvausEnrichedDataRaporttiItem] = None
) {
  def this(s: Sorakuvaus) = this(
    s.id.getOrElse(UUID.fromString("")),
    s.externalId,
    s.tila,
    s.nimi,
    s.koulutustyyppi,
    s.kielivalinta,
    s.metadata.map(new SorakuvausMetadataRaporttiItem(_)),
    s.organisaatioOid,
    s.muokkaaja,
    s.modified.get,
    s._enrichedData.map(e => new SorakuvausEnrichedDataRaporttiItem(e.muokkaajanNimi))
  )
}

case class SorakuvausMetadataRaporttiItem(
    kuvaus: Kielistetty = Map(),
    koulutusalaKoodiUri: Option[String] = None,
    koulutusKoodiUrit: Seq[String] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) {
  def this(m: SorakuvausMetadata) = this(
    m.kuvaus,
    m.koulutusalaKoodiUri,
    m.koulutusKoodiUrit,
    m.isMuokkaajaOphVirkailija
  )
}
