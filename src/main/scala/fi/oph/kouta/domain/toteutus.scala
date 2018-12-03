package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.validation.{IsValid, Validatable}

case class Osaamisala(koodi:String, linkki:Kielistetty = Map(), otsikko:Kielistetty = Map())
case class Osio(otsikko:Kielistetty, teksti:Kielistetty)

case class Opetus(opetuskielet:List[String],
                  opetusaikaKoodiUri:Option[String],
                  opetustapaKoodiUri:Option[String],
                  onkoMaksullinen: Option[Boolean],
                  maksunMaara: Kielistetty = Map(),
                  kuvaus: Kielistetty = Map(),
                  osiot: List[Osio] = List())


case class ToteutusMetadata(kuvaus:Kielistetty = Map(),
                            osaamisalat:List[Osaamisala] = List(),
                            opetus:Option[Opetus] = None,
                            asiasanat: List[Keyword] = List(),
                            ammattinimikkeet: List[Keyword] = List(),
                            yhteystieto: Option[Yhteystieto] = None)

case class Toteutus(oid:Option[String] = None,
                    koulutusOid:String,
                    tila:Julkaisutila = Tallennettu,
                    tarjoajat:List[String] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja:String,
                    organisaatioOid:String,
                    kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate():IsValid = and(
     super.validate(),
     assertMatch(koulutusOid, KoulutusOidPattern),
     validateIfDefined[String](oid, assertMatch(_, ToteutusOidPattern)),
     validateOidList(tarjoajat)
  )
}