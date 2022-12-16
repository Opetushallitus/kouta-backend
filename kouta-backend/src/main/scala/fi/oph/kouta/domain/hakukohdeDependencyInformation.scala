package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}

import java.util.UUID

case class HakukohdeToteutusDependencyInfo(
    oid: ToteutusOid,
    tila: Julkaisutila,
    nimi: Kielistetty,
    koulutustyyppi: Koulutustyyppi,
    metadata: Option[ToteutusMetadata],
    koulutusKoodiUrit: Seq[String] = Seq(),
    tarjoajat: Seq[OrganisaatioOid] = Seq()
)

case class HakukohdeValintaperusteDependencyInfo(
    valintaperusteId: UUID,
    tila: Julkaisutila,
    koulutustyyppi: Koulutustyyppi,
    valintakoeIdt: Seq[UUID]
)

case class HakukohdeJarjestyspaikkaDependencyInfo(
    oid: OrganisaatioOid,
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean]
)

case class HakukohdeDependencyInformation(
    toteutus: HakukohdeToteutusDependencyInfo,
    valintaperuste: Option[HakukohdeValintaperusteDependencyInfo],
    jarjestyspaikka: Option[HakukohdeJarjestyspaikkaDependencyInfo]
)
