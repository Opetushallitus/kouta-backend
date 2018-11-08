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

  override def validate() = {
    and(super.validate(),
        validateIfDefined[String](oid, assertMatch(_, KoulutusOidPattern)),
        validateOidList(tarjoajat),
        validateIfDefined[String](koulutusKoodiUri, assertMatch(_, KoulutusKoodiPattern)),
        validateIfTrue(Julkaistu == tila, () => and(
          assertNotOptional(koulutustyyppi, "koulutustyyppi"),
          validateIfDefined[Koulutustyyppi](koulutustyyppi, (k) => assertTrue(k == Muu | johtaaTutkintoon, invalidTutkintoonjohtavuus(k.toString))),
          assertNotOptional(koulutusKoodiUri, "koulutusKoodiUri"))))
  }
}