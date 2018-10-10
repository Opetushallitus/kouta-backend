package fi.oph.kouta.repository.dto

import fi.oph.kouta.domain._
import org.json4s.jackson.Serialization.{read}

trait KoulutusDTOs extends KoutaDTOBase {

  case class KoulutusDTO(oid:String,
                         johtaaTutkintoon:Boolean,
                         koulutustyyppi:Koulutustyyppi,
                         koulutusKoodiUri:String,
                         tila:Julkaisutila,
                         nimi: Map[Kieli, String],
                         metatieto: String,
                         muokkaaja:String)

  object KoulutusDTO extends fi.oph.kouta.repository.Extractable[KoulutusDTO] {
    import slick.jdbc.GetResult
    implicit val extractor =
      GetResult(r => KoulutusDTO(
        oid = r.nextString,
        johtaaTutkintoon = r.nextBoolean,
        koulutustyyppi = Koulutustyyppi.withName(r.nextString),
        koulutusKoodiUri = r.nextString,
        tila = Julkaisutila.withName(r.nextString),
        nimi = read[Map[Kieli, String]](r.nextString),
        metatieto = r.nextString(),
        muokkaaja = r.nextString))
  }

  def koulutus(koulutus:KoulutusDTO, tarjoajat:Seq[TarjoajaDTO]) =
    new Koulutus(
      Some(koulutus.oid),
      koulutus.johtaaTutkintoon,
      koulutus.koulutustyyppi,
      koulutus.koulutusKoodiUri,
      koulutus.tila,
      tarjoajat.map(_.tarjoajaOid).toList,
      koulutus.nimi,
      read[KoulutusMetadata](koulutus.metatieto),
      koulutus.muokkaaja
    )
}