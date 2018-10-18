package fi.oph.kouta.domain

case class KoulutusMetadata(kuvaus:Map[Kieli, String] = Map())

case class Koulutus(oid:Option[String] = None,
                    johtaaTutkintoon:Boolean,
                    koulutustyyppi:Option[Koulutustyyppi] = None,
                    koulutusKoodiUri:Option[String] = None,
                    tila:Julkaisutila = Tallennettu,
                    tarjoajat:List[String] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[KoulutusMetadata] = None,
                    muokkaaja:String)