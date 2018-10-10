package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import slick.jdbc.GetResult

trait ExtractorBase extends KoutaJsonFormats {

  case class Tarjoaja(oid:String, tarjoajaOid:String)

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  implicit val getTarjoajatResult: GetResult[Tarjoaja] = GetResult(r => new Tarjoaja(r.nextString, r.nextString))
}

trait KoulutusExtractors extends ExtractorBase {

  implicit val getKoulutusResult: GetResult[Koulutus] = GetResult(r => Koulutus(
    oid = r.nextStringOption(),
    johtaaTutkintoon = r.nextBoolean,
    koulutustyyppi = Koulutustyyppi.withName(r.nextString),
    koulutusKoodiUri = r.nextString,
    tila = Julkaisutila.withName(r.nextString),
    tarjoajat = List(),
    nimi = read[Map[Kieli, String]](r.nextString),
    metadata = read[KoulutusMetadata](r.nextString()),
    muokkaaja = r.nextString))
}

trait ToteutusExtractors extends ExtractorBase {

  implicit val getToteutusResult: GetResult[Toteutus] = GetResult(r => Toteutus(
    oid = r.nextStringOption,
    koulutusOid = r.nextString,
    tila = Julkaisutila.withName(r.nextString),
    tarjoajat = List(),
    nimi = read[Map[Kieli, String]](r.nextString),
    metadata = read[ToteutusMetadata](r.nextString()),
    muokkaaja = r.nextString
  ))
}
