package fi.oph.kouta.domain

sealed trait Opetusaika extends EnumType

object Opetusaika extends Enum[Opetusaika] {
  override def name: String = "opetusaika"
  def values() = List(Iltaopetus, Paivaopetus, Viikonloppuopetus)
}
case object Iltaopetus extends Opetusaika { val name = "iltaopetus" }
case object Paivaopetus extends Opetusaika { val name = "päiväopetus" }
case object Viikonloppuopetus extends Opetusaika { val name = "viikonloppuopetus" }

case class Osaamisala(koodiUri:String, linkki:Kielistetty = Map(), otsikko:Kielistetty = Map())
case class Maksullisuus(maksullinen:Boolean, maksunMaara:Kielistetty = Map())
case class Opetus(opetuskielet:List[String],
                  lahiopetus:Option[Boolean],
                  opetusajat:List[Opetusaika],
                  maksullisuus:Option[Maksullisuus],
                  kuvaus: Kielistetty = Map())


case class ToteutusMetadata(kuvaus:Kielistetty = Map(),
                            osaamisalat:List[Osaamisala] = List(),
                            opetus:Option[Opetus] = None,
                            asiasanat: List[String] = List(),
                            ammattinimikkeet: List[String] = List(),
                            yhteystieto: Option[Yhteystieto])

case class Toteutus(oid:Option[String] = None,
                    koulutusOid:String,
                    tila:Julkaisutila = Tallennettu,
                    tarjoajat:List[String] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja:String,
                    kielivalinta:Seq[Kieli] = Seq())