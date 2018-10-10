package fi.oph.kouta.domain

case class KoulutusMetadata(kuvaus:Map[Kieli, String] = Map())

case class Koulutus(oid:Option[String] = None,
                    johtaaTutkintoon:Boolean,
                    koulutustyyppi:Koulutustyyppi,
                    koulutusKoodiUri:String,
                    tila:Julkaisutila,
                    tarjoajat:List[String],
                    nimi: Kielistetty,
                    metadata: KoulutusMetadata,
                    muokkaaja:String)