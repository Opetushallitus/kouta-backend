package fi.oph.kouta.domain

import fi.oph.kouta.validation.Validatable

case class KoulutusMetadata(kuvaus:Map[Kieli, String] = Map())

case class Koulutus(oid:Option[String] = None,
                    johtaaTutkintoon:Boolean,
                    koulutustyyppi:Option[Koulutustyyppi] = None,
                    koulutusKoodiUri:Option[String] = None,
                    tila:Julkaisutila = Tallennettu,
                    tarjoajat:List[String] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[KoulutusMetadata] = None,
                    muokkaaja:String,
                    kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate() = for {
    _ <- super.validate().right
    _ <- validateKoulutusOid(oid).right
    x <- validateIfTrue(Julkaistu == tila, () => for {
      _ <- validateKoulutustyyppi(koulutustyyppi).right
      _ <- validateTutkintoonjohtavuus(koulutustyyppi.get, johtaaTutkintoon).right
      _ <- validateKoulutusKoodi(koulutusKoodiUri).right
      y <- validateTarjoajat(tarjoajat).right
    } yield y).right
  } yield x

}