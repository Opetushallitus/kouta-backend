package fi.oph.kouta.domain

sealed trait Opetusaika {
  def name: String
  override def toString() = name
}
object Opetusaika {
  def withName(n:String) = n match {
    case Iltaopetus.name => Iltaopetus
    case Paivaopetus.name => Paivaopetus
    case Viikonloppuopetus.name => Viikonloppuopetus
    case x => throw new NoSuchElementException(x)
  }
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
                            yhteystieto: Yhteystieto)

case class Toteutus(oid:Option[String],
                    koulutusOid:String,
                    tila:Julkaisutila,
                    tarjoajat:List[String],
                    nimi: Kielistetty,
                    metadata: ToteutusMetadata,
                    muokkaaja:String)