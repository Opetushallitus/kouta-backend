package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Kielistetty, Modified, NimettyLinkki, OppilaitoksenOsa, OppilaitoksenOsaMetadata, Oppilaitos, OppilaitosMetadata, Osoite, Tallennettu, TietoaOpiskelusta, Yhteystieto}
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
    modified: Modified,
    _enrichedData: Option[OppilaitosEnrichedDataRaporttiItem] = None
) {
  def this(o: Oppilaitos) = this(
    o.oid,
    o.tila,
    o.esikatselu,
    o.metadata.map(new OppilaitosMetadataRaporttiItem(_)),
    o.kielivalinta,
    o.organisaatioOid,
    o.muokkaaja,
    o.teemakuva,
    o.logo,
    o.modified.get,
    o._enrichedData.map(e => OppilaitosEnrichedDataRaporttiItem(e.muokkaajanNimi))
  )
}

case class OppilaitoksenOsaRaporttiItem(
    oid: OrganisaatioOid,
    oppilaitosOid: Option[OrganisaatioOid],
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    metadata: Option[OppilaitoksenOsaMetadataRaporttiItem] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    modified: Modified,
    _enrichedData: Option[OppilaitosEnrichedDataRaporttiItem] = None
) {
  def this(o: OppilaitoksenOsa) = this(
    o.oid,
    o.oppilaitosOid,
    o.tila,
    o.esikatselu,
    o.metadata.map(new OppilaitoksenOsaMetadataRaporttiItem(_)),
    o.kielivalinta,
    o.organisaatioOid,
    o.muokkaaja,
    o.teemakuva,
    o.modified.get,
    o._enrichedData.map(e => OppilaitosEnrichedDataRaporttiItem(e.muokkaajanNimi))
  )
}

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
) {
  def this(m: OppilaitosMetadata) = this(
    m.tietoaOpiskelusta.map(new TietoaOpiskelustaRaporttiItem(_)),
    m.wwwSivu,
    m.some,
    m.hakijapalveluidenYhteystiedot.map(new YhteystietoRaporttiItem(_)),
    m.esittely,
    m.opiskelijoita,
    m.korkeakouluja,
    m.tiedekuntia,
    m.kampuksia,
    m.yksikoita,
    m.toimipisteita,
    m.akatemioita,
    m.isMuokkaajaOphVirkailija,
    m.jarjestaaUrheilijanAmmKoulutusta
  )
}

case class TietoaOpiskelustaRaporttiItem(otsikkoKoodiUri: String, teksti: Kielistetty) {
  def this(t: TietoaOpiskelusta) = this(
    t.otsikkoKoodiUri,
    t.teksti
  )
}

case class OppilaitoksenOsaMetadataRaporttiItem(
    wwwSivu: Option[NimettyLinkki] = None,
    hakijapalveluidenYhteystiedot: Option[YhteystietoRaporttiItem] = None,
    opiskelijoita: Option[Int] = None,
    kampus: Kielistetty = Map(),
    esittely: Kielistetty = Map(),
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) {
  def this(m: OppilaitoksenOsaMetadata) = this(
    m.wwwSivu,
    m.hakijapalveluidenYhteystiedot.map(new YhteystietoRaporttiItem(_)),
    m.opiskelijoita,
    m.kampus,
    m.esittely,
    m.jarjestaaUrheilijanAmmKoulutusta,
    m.isMuokkaajaOphVirkailija
  )
}

case class YhteystietoRaporttiItem(
    nimi: Kielistetty = Map(),
    postiosoite: Option[Osoite] = None,
    kayntiosoite: Option[Osoite] = None,
    puhelinnumero: Kielistetty = Map(),
    sahkoposti: Kielistetty = Map()
) {
  def this(y: Yhteystieto) = this(
    y.nimi,
    y.postiosoite,
    y.kayntiosoite,
    y.puhelinnumero,
    y.sahkoposti
  )
}
