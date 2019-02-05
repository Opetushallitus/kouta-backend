package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

case class Osaamisala(koodi: String, linkki: Kielistetty = Map(), otsikko: Kielistetty = Map())
case class Osio(otsikko: Kielistetty, teksti: Kielistetty)

case class Opetus(opetuskieliKoodiUrit: Seq[String],
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUri: Option[String],
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String],
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean],
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Kielistetty = Map(),
                  osiot: List[Osio] = List())


case class ToteutusMetadata(kuvaus: Kielistetty = Map(),
                            osaamisalat: List[Osaamisala] = List(),
                            opetus: Option[Opetus] = None,
                            asiasanat: List[Keyword] = List(),
                            ammattinimikkeet: List[Keyword] = List(),
                            yhteystieto: Option[Yhteystieto] = None)

case class Toteutus(oid: Option[ToteutusOid] = None,
                    koulutusOid: KoulutusOid,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     assertValid(koulutusOid),
     validateIfDefined[ToteutusOid](oid, assertValid(_)),
     validateOidList(tarjoajat)
  )
}