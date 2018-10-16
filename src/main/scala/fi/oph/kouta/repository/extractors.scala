package fi.oph.kouta.repository

import java.time.{Instant, OffsetDateTime, ZoneId}

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import slick.jdbc.{GetResult, PositionedResult}

trait ExtractorBase extends KoutaJsonFormats {

  case class Tarjoaja(oid:String, tarjoajaOid:String)
  case class Hakuaika(oid:String, alkaa:Instant, paattyy:Instant)

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  implicit val getTarjoajatResult: GetResult[Tarjoaja] = GetResult(r => new Tarjoaja(r.nextString, r.nextString))

  implicit val getHakuaikaResult: GetResult[Hakuaika] = GetResult(r => {
    new Hakuaika(r.nextString(), r.nextTimestamp.toInstant, r.nextTimestamp.toInstant)
  })
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

trait HakuExtractors extends ExtractorBase {

  implicit val getHakuResult: GetResult[Haku] = GetResult(r => Haku(
    oid = r.nextStringOption,
    tila = Julkaisutila.withName(r.nextString),
    nimi = read[Map[Kieli, String]](r.nextString),
    hakutapa = r.nextStringOption.map(Hakutapa.withName),
    tietojen_muuttaminen_paattyy = r.nextTimestampOption().map(_.toInstant),
    alkamiskausi = r.nextStringOption.map(Alkamiskausi.withName),
    alkamisvuosi = r.nextStringOption,
    kohdejoukko = r.nextStringOption,
    kohdejoukon_tarkenne = r.nextStringOption,
    metadata = r.nextStringOption().map(read[HakuMetadata]),
    organisaatio = r.nextString,
    hakuajat = List(),
    muokkaaja = r.nextString
  ))
}
