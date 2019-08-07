package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

case class Sorakuvaus(id: Option[UUID] = None,
                      tila: Julkaisutila = Tallennettu,
                      nimi: Kielistetty = Map(),
                      koulutustyyppi: Koulutustyyppi,
                      julkinen: Boolean = false,
                      kielivalinta: Seq[Kieli] = Seq(),
                      metadata: Option[SorakuvausMetadata] = None,
                      organisaatioOid: OrganisaatioOid,
                      muokkaaja: UserOid,
                      modified: Option[LocalDateTime]) extends PerustiedotWithId with Validatable {
  override def validate(): IsValid = and(
    super.validate()
  )
}

case class SorakuvausMetadata(kuvaus: Kielistetty = Map())

case class SorakuvausListItem(id: UUID,
                              nimi: Kielistetty,
                              tila: Julkaisutila,
                              organisaatioOid: OrganisaatioOid,
                              muokkaaja: UserOid,
                              modified: LocalDateTime) extends IdListItem