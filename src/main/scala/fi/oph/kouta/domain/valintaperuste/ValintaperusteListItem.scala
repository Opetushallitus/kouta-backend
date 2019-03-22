package fi.oph.kouta.domain.valintaperuste

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.{IdListItem, Julkaisutila, Kielistetty}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}

case class ValintaperusteListItem(id: UUID,
                                  nimi: Kielistetty,
                                  tila: Julkaisutila,
                                  organisaatioOid: OrganisaatioOid,
                                  muokkaaja: UserOid,
                                  modified: LocalDateTime) extends IdListItem
