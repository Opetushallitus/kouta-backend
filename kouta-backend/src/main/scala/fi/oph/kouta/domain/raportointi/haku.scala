package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{Ajanjakso, Haku, HakuMetadata, Hakulomaketyyppi, Julkaisutila, Kieli, Kielistetty, KoulutuksenAlkamiskausi, Modified, Tallennettu, Yhteyshenkilo}
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
    organisaatioOid: OrganisaatioOid,
    hakuajat: List[Ajanjakso] = List(),
    muokkaaja: UserOid,
    kielivalinta: Seq[Kieli] = Seq(),
    modified: Modified,
    _enrichedData: Option[HakuEnrichedDataRaporttiItem] = None
) {
  def this(h: Haku) = this(
    h.oid.getOrElse(HakuOid("")),
    h.externalId,
    h.tila,
    h.nimi,
    h.hakutapaKoodiUri,
    h.hakukohteenLiittamisenTakaraja,
    h.hakukohteenMuokkaamisenTakaraja,
    h.ajastettuJulkaisu,
    h.ajastettuHaunJaHakukohteidenArkistointi,
    h.ajastettuHaunJaHakukohteidenArkistointiAjettu,
    h.kohdejoukkoKoodiUri,
    h.kohdejoukonTarkenneKoodiUri,
    h.hakulomaketyyppi,
    h.hakulomakeAtaruId,
    h.hakulomakeKuvaus,
    h.hakulomakeLinkki,
    h.metadata.map(new HakuMetadataRaporttiItem(_)),
    h.organisaatioOid,
    h.hakuajat,
    h.muokkaaja,
    h.kielivalinta,
    h.modified.get,
    h._enrichedData.map(e => new HakuEnrichedDataRaporttiItem(e.muokkaajanNimi))
  )
}

case class HakuMetadataRaporttiItem(
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq(),
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausiRaporttiItem],
    isMuokkaajaOphVirkailija: Option[Boolean]
) {
  def this(h: HakuMetadata) = this(
    h.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    h.tulevaisuudenAikataulu,
    h.koulutuksenAlkamiskausi.map(new KoulutuksenAlkamiskausiRaporttiItem(_)),
    h.isMuokkaajaOphVirkailija
  )
}
