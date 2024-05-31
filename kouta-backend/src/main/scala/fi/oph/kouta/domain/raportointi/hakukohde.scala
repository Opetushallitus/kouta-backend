package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{Ajanjakso, Aloituspaikat, HakukohdeEnrichedData, Hakulomaketyyppi, Julkaisutila, Kieli, Kielistetty, KoulutuksenAlkamiskausi, LiitteenToimitustapa, Modified, Osoite, Tallennettu, ValintakokeenLisatilaisuudet}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid, ToteutusOid, UserOid}

import java.time.LocalDateTime
import java.util.UUID

case class HakukohdeEnrichedDataRaporttiItem(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None) {
  def this(e: HakukohdeEnrichedData) = this(
    e.esitysnimi,
    e.muokkaajanNimi
  )
}

case class HakukohdeRaporttiItem(
    oid: HakukohdeOid,
    externalId: Option[String] = None,
    toteutusOid: ToteutusOid,
    hakuOid: HakuOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Option[Boolean] = None,
    nimi: Kielistetty = Map(),
    hakukohdeKoodiUri: Option[String] = None,
    jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeAtaruId: Option[UUID] = None,
    hakulomakeKuvaus: Kielistetty = Map(),
    hakulomakeLinkki: Kielistetty = Map(),
    kaytetaanHaunHakulomaketta: Option[Boolean] = None,
    pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
    pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
    muuPohjakoulutusvaatimus: Kielistetty = Map(),
    toinenAsteOnkoKaksoistutkinto: Option[Boolean] = None,
    kaytetaanHaunAikataulua: Option[Boolean] = None,
    valintaperusteId: Option[UUID] = None,
    liitteetOnkoSamaToimitusaika: Option[Boolean] = None,
    liitteetOnkoSamaToimitusosoite: Option[Boolean] = None,
    liitteidenToimitusaika: Option[LocalDateTime] = None,
    liitteidenToimitustapa: Option[LiitteenToimitustapa] = None,
    liitteidenToimitusosoite: Option[LiitteenToimitusosoiteRaporttiItem] = None,
    liitteet: Seq[HakukohdeLiiteRaporttiItem] = Seq(),
    valintakokeet: Seq[ValintakoeRaporttiItem] = Seq(),
    hakuajat: Seq[Ajanjakso] = Seq(),
    metadata: Option[HakukohdeMetadataRaporttiItem] = None,
    muokkaaja: UserOid,
    organisaatioOid: Option[OrganisaatioOid],
    kielivalinta: Seq[Kieli] = Seq(),
    modified: Option[Modified] = None,
    enrichedData: Option[HakukohdeEnrichedDataRaporttiItem] = None
)

case class HakukohdeLiiteRaporttiItem(
    id: UUID,
    hakukohdeOid: HakukohdeOid,
    tyyppiKoodiUri: Option[String],
    nimi: Kielistetty = Map(),
    kuvaus: Kielistetty = Map(),
    toimitusaika: Option[LocalDateTime] = None,
    toimitustapa: Option[LiitteenToimitustapa] = None,
    toimitusosoite: Option[LiitteenToimitusosoiteRaporttiItem] = None
)

case class LiitteenToimitusosoiteRaporttiItem(
    osoite: Osoite,
    sahkoposti: Option[String] = None,
    verkkosivu: Option[String] = None
)

case class OppiaineKoodiUritRaporttiItem(oppiaine: Option[String], kieli: Option[String])

case class PainotettuOppiaineRaporttiItem(
    koodiUrit: Option[OppiaineKoodiUritRaporttiItem] = None,
    painokerroin: Option[Double]
)

case class HakukohteenLinjaRaporttiItem(
    linja: Option[String] = None, // NOTE: None tarkoittaa Yleislinjaa
    alinHyvaksyttyKeskiarvo: Option[Double] = None,
    lisatietoa: Kielistetty = Map(),
    painotetutArvosanat: Seq[PainotettuOppiaineRaporttiItem] = Seq()
)

case class HakukohdeMetadataRaporttiItem(
    valintakokeidenYleiskuvaus: Kielistetty = Map(),
    valintaperusteenValintakokeidenLisatilaisuudet: Seq[ValintakokeenLisatilaisuudet] = Seq(),
    kynnysehto: Kielistetty = Map(),
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausiRaporttiItem] = None,
    kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
    aloituspaikat: Option[Aloituspaikat] = None,
    hakukohteenLinja: Option[HakukohteenLinjaRaporttiItem] = None,
    uudenOpiskelijanUrl: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None
)
