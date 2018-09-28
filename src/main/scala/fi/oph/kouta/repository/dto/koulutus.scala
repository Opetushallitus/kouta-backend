package fi.oph.kouta.repository.dto

import fi.oph.kouta.domain.Julkaisutila.Julkaisutila
import fi.oph.kouta.domain.{Julkaisutila, Kieli, Koulutus, Koulutustyyppi}
import fi.oph.kouta.domain.Koulutustyyppi.Koulutustyyppi

trait KoulutusDTOs {

  case class KoulutusDTO(oid:String,
                         johtaaTutkintoon:Boolean,
                         koulutustyyppi:Koulutustyyppi,
                         koulutusKoodiUri:String,
                         tila:Julkaisutila,
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
        muokkaaja = r.nextString))
  }

  case class KoulutuksenTekstitDTO(oid:String,
                                   kielikoodi: Kieli.Kieli,
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

  def koulutus(koulutus:KoulutusDTO, tekstit:Seq[KoulutuksenTekstitDTO], tarjoajat:Seq[KoulutuksenTarjoajatDTO]) =
    new Koulutus(
      Some(koulutus.oid),
      koulutus.johtaaTutkintoon,
      koulutus.koulutustyyppi,
      koulutus.koulutusKoodiUri,
      koulutus.tila,
      tarjoajat.map(_.tarjoajaOid).toList,
      tekstit.map(t => (t.kielikoodi, t.nimi)).filter(_._2 != null).toMap,
      tekstit.map(t => (t.kielikoodi, t.kuvaus)).filter(_._2 != null).toMap,
      koulutus.muokkaaja
    )
}