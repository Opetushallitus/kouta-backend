package fi.oph.kouta.domain

import fi.oph.kouta.domain.Julkaisutila.Julkaisutila
import fi.oph.kouta.domain.Koulutustyyppi.Koulutustyyppi

object Koulutustyyppi extends Enumeration {
  type Koulutustyyppi = Value
  val amm, kk, lk, muu = Value
}

case class KoulutusMetadata(kuvaus:Map[Kieli.Kieli, String] = Map())

case class Koulutus(oid:Option[String] = None,
                    johtaaTutkintoon:Boolean,
                    koulutustyyppi:Koulutustyyppi,
                    koulutusKoodiUri:String,
                    tila:Julkaisutila,
                    tarjoajat:List[String],
                    nimi: Map[Kieli.Kieli, String],
                    metadata: KoulutusMetadata,
                    muokkaaja:String)