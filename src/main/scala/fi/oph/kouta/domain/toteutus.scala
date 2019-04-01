package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

case class Toteutus(oid: Option[ToteutusOid] = None,
                    koulutusOid: KoulutusOid,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     assertValid(koulutusOid),
     validateIfDefined[ToteutusOid](oid, assertValid(_)),
     validateOidList(tarjoajat)
  )
}

case class ToteutusListItem(oid: ToteutusOid,
                            koulutusOid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem
