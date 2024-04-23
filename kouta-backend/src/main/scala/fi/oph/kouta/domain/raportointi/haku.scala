package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{
  Ajanjakso,
  Haku,
  HakuMetadata,
  Hakulomaketyyppi,
  Julkaisutila,
  Kieli,
  Kielistetty,
  KoulutuksenAlkamiskausi,
  Modified,
  Tallennettu,
  Yhteyshenkilo
}
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid, UserOid}

import java.time.LocalDateTime
import java.util.UUID

case class HakuEnrichedDataRaporttiItem(muokkaajanNimi: Option[String] = None)

case class HakuRaporttiItem(
    oid: HakuOid,
    externalId: Option[String] = None,
    tila: Julkaisutila = Tallennettu,
    nimi: Kielistetty = Map(),
    hakutapaKoodiUri: Option[String] = None,
    hakukohteenLiittamisenTakaraja: Option[LocalDateTime] = None,
    hakukohteenMuokkaamisenTakaraja: Option[LocalDateTime] = None,
    hakukohteenLiittajaOrganisaatiot: Seq[OrganisaatioOid] = Seq(),
    ajastettuJulkaisu: Option[LocalDateTime] = None,
    ajastettuHaunJaHakukohteidenArkistointi: Option[LocalDateTime] = None,
    ajastettuHaunJaHakukohteidenArkistointiAjettu: Option[LocalDateTime] = None,
    kohdejoukkoKoodiUri: Option[String] = None,
    kohdejoukonTarkenneKoodiUri: Option[String] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeAtaruId: Option[UUID] = None,
    hakulomakeKuvaus: Kielistetty = Map(),
    hakulomakeLinkki: Kielistetty = Map(),
    metadata: Option[HakuMetadataRaporttiItem] = None,
    organisaatioOid: Option[OrganisaatioOid],
    hakuajat: List[Ajanjakso] = List(),
    muokkaaja: UserOid,
    kielivalinta: Seq[Kieli] = Seq(),
    modified: Option[Modified],
    enrichedData: Option[HakuEnrichedDataRaporttiItem] = None
)

case class HakuMetadataRaporttiItem(
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq(),
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausiRaporttiItem],
    isMuokkaajaOphVirkailija: Option[Boolean]
)
