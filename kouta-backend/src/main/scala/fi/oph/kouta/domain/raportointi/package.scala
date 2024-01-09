package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}

import java.util.UUID

package object raportointi {

  case class TutkinnonOsaRaporttiItem(
      ePerusteId: Option[Long] = None,
      koulutusKoodiUri: Option[String] = None,
      tutkinnonosaId: Option[Long] = None,
      tutkinnonosaViite: Option[Long] = None
  ) {
    def this(t: TutkinnonOsa) = {
      this(t.ePerusteId, t.koulutusKoodiUri, t.tutkinnonosaId, t.tutkinnonosaViite)
    }
  }

  case class ValintakoeRaporttiItem(
      id: UUID,
      parentOidOrUUID: String,
      tyyppiKoodiUri: Option[String] = None,
      nimi: Kielistetty = Map(),
      metadata: Option[ValintakoeMetadata] = None,
      tilaisuudet: Seq[Valintakoetilaisuus] = Seq(),
      muokkaaja: String
  )

  case class PistetietoRaporttiItem(
      tarjoaja: OrganisaatioOid,
      hakukohdekoodi: String,
      pisteet: Double,
      vuosi: String,
      valintatapajonoOid: String,
      hakukohdeOid: HakukohdeOid,
      hakuOid: HakuOid,
      valintatapajonoTyyppi: String
  )

}
