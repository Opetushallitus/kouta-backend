package fi.oph.kouta.domain

import fi.oph.kouta.validation.Validatable

sealed trait Opetusaika extends EnumType

case class Osaamisala(koodiUri:String, linkki:Kielistetty = Map(), otsikko:Kielistetty = Map())
case class Maksullisuus(maksullinen:Boolean, maksunMaara:Kielistetty = Map())
case class Opetus(opetuskielet:List[String],
                  lahiopetus:Option[Boolean],
                  opetusajat:List[String],
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
                    kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate(): Either[String, Unit] = for {
    _ <- super.validate().right
    _ <- validateKoulutusOid(koulutusOid).right
    _ <- validateToteutusOid(oid).right
    x <- validateIfTrue(tila == Julkaistu, () => for {
      y <- validateTarjoajat(tarjoajat).right
    } yield y ).right
  } yield x

}