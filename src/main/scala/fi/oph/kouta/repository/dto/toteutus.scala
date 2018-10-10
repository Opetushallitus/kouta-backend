package fi.oph.kouta.repository.dto

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Toteutus, ToteutusMetadata}
import org.json4s.jackson.Serialization.read

trait ToteutusDTOs extends KoutaDTOBase {

  case class ToteutusDTO(oid:String,
                         koulutusOid:String,
                         tila:Julkaisutila,
                         nimi: Map[Kieli, String],
                         metatieto: String,
                         muokkaaja:String)

  object ToteutusDTO extends fi.oph.kouta.repository.Extractable[ToteutusDTO] {
    import slick.jdbc.GetResult
    implicit val extractor =
      GetResult(r => ToteutusDTO(
        oid = r.nextString,
        koulutusOid = r.nextString,
        tila = Julkaisutila.withName(r.nextString),
        nimi = read[Map[Kieli, String]](r.nextString),
        metatieto = r.nextString(),
        muokkaaja = r.nextString))
  }

  def toteutus(toteutus:ToteutusDTO, tarjoajat:Seq[TarjoajaDTO]) =
    new Toteutus(
      Some(toteutus.oid),
      toteutus.koulutusOid,
      toteutus.tila,
      tarjoajat.map(_.tarjoajaOid).toList,
      toteutus.nimi,
      read[ToteutusMetadata](toteutus.metatieto),
      toteutus.muokkaaja
    )
}
