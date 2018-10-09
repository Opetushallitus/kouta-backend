package fi.oph.kouta.repository.dto

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.{read, write}

trait KoulutusDTOs extends KoutaJsonFormats {

  def toJson(data:AnyRef) = write(data)

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

  case class KoulutuksenTekstitDTO(oid:String,
                                   kielikoodi: Kieli,
                                   nimi: String,
                                   kuvaus: String)

  object KoulutuksenTekstitDTO extends fi.oph.kouta.repository.Extractable[KoulutuksenTekstitDTO] {
    import slick.jdbc.GetResult
    implicit val extractor =
      GetResult(r => KoulutuksenTekstitDTO(
        oid = r.nextString,
        kielikoodi = Kieli.withName(r.nextString),
        nimi = r.nextString,
        kuvaus = r.nextString))
  }

  case class KoulutuksenTarjoajatDTO(oid:String,
                                     tarjoajaOid:String)

  object KoulutuksenTarjoajatDTO extends fi.oph.kouta.repository.Extractable[KoulutuksenTarjoajatDTO] {
    import slick.jdbc.GetResult
    implicit val extractor =
      GetResult(r => KoulutuksenTarjoajatDTO(
        oid = r.nextString,
        tarjoajaOid = r.nextString))
  }

  def koulutus(koulutus:KoulutusDTO, tarjoajat:Seq[KoulutuksenTarjoajatDTO]) =
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