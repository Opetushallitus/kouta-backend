package fi.oph.kouta.domain

import fi.oph.kouta.domain.Julkaisutila.Julkaisutila
import fi.oph.kouta.domain.Koulutustyyppi.Koulutustyyppi

object Koulutustyyppi extends Enumeration {
  type Koulutustyyppi = Value
  val amm, kk, lk, muu = Value
}

object Julkaisutila extends Enumeration {
  type Julkaisutila = Value
  val julkaisematta, julkaistu, poistettu = Value
}

case class Koulutus( oid:String,
                     johtaaTutkintoon:Boolean,
                     koulutustyyppi:Koulutustyyppi,
                     koulutusKoodiUri:String,
                     tila:Julkaisutila,
                     tarjoajat:List[String],
                     muokkaaja:String)

object Koulutus extends fi.oph.kouta.repository.Extractable[Koulutus] {
  import slick.jdbc.GetResult

  val extractor =
    GetResult(r => Koulutus(
      oid = r.nextString,
      johtaaTutkintoon = r.nextBoolean,
      koulutustyyppi = Koulutustyyppi.withName(r.nextString),
      koulutusKoodiUri = r.nextString,
      tila = Julkaisutila.withName(r.nextString),
      tarjoajat = extractArray[String](r.nextObject()),
      muokkaaja = r.nextString))
}