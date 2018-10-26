package fi.oph.kouta.repository

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import slick.jdbc._

trait ExtractorBase extends KoutaJsonFormats {

  case class Tarjoaja(oid:String, tarjoajaOid:String)
  case class Hakuaika(oid:String, alkaa:Instant, paattyy:Instant)

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  implicit val getTarjoajatResult: GetResult[Tarjoaja] = GetResult(r => new Tarjoaja(r.nextString, r.nextString))

  implicit val getHakuaikaResult: GetResult[Hakuaika] = GetResult(r => {
    new Hakuaika(r.nextString(), r.nextTimestamp.toInstant, r.nextTimestamp.toInstant)
  })

  def extractKielistetty(json:Option[String]): Kielistetty = json.map(read[Map[Kieli, String]]).getOrElse(Map())
  def extractKielivalinta(json:Option[String]): Seq[Kieli] = json.map(read[Seq[Kieli]]).getOrElse(Seq())
}

trait KoulutusExtractors extends ExtractorBase {

  implicit val getKoulutusResult: GetResult[Koulutus] = GetResult(r => Koulutus(
    oid = r.nextStringOption(),
    johtaaTutkintoon = r.nextBoolean,
    koulutustyyppi = r.nextStringOption.map(Koulutustyyppi.withName),
    koulutusKoodiUri = r.nextStringOption,
    tila = Julkaisutila.withName(r.nextString),
    tarjoajat = List(),
    nimi = extractKielistetty(r.nextStringOption),
    metadata = r.nextStringOption.map(read[KoulutusMetadata]),
    muokkaaja = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)))
}

trait ToteutusExtractors extends ExtractorBase {

  implicit val getToteutusResult: GetResult[Toteutus] = GetResult(r => Toteutus(
    oid = r.nextStringOption,
    koulutusOid = r.nextString,
    tila = Julkaisutila.withName(r.nextString),
    tarjoajat = List(),
    nimi = extractKielistetty(r.nextStringOption),
    metadata = r.nextStringOption.map(read[ToteutusMetadata]),
    muokkaaja = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))
}

trait HakuExtractors extends ExtractorBase {

  implicit val getHakuResult: GetResult[Haku] = GetResult(r => Haku(
    oid = r.nextStringOption,
    tila = Julkaisutila.withName(r.nextString),
    nimi = extractKielistetty(r.nextStringOption),
    hakutapaKoodiUri = r.nextStringOption,
    hakukohteenLiittamisenTakaraja = r.nextTimestampOption().map(_.toInstant),
    hakukohteenMuokkaamisenTakaraja = r.nextTimestampOption().map(_.toInstant),
    alkamiskausiKoodiUri = r.nextStringOption,
    alkamisvuosi = r.nextStringOption,
    kohdejoukkoKoodiUri = r.nextStringOption,
    kohdejoukonTarkenneKoodiUri = r.nextStringOption,
    hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
    hakulomake = r.nextStringOption(),
    metadata = r.nextStringOption().map(read[HakuMetadata]),
    organisaatio = r.nextString,
    hakuajat = List(),
    muokkaaja = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))
}

trait ValintaperusteExtractors extends ExtractorBase {

  implicit val getValintaperusteResult: GetResult[Valintaperuste] = GetResult(r => Valintaperuste(
    id = r.nextStringOption.map(UUID.fromString),
    tila = Julkaisutila.withName(r.nextString),
    hakutapaKoodiUri = r.nextStringOption,
    kohdejoukkoKoodiUri = r.nextStringOption,
    kohdejoukonTarkenneKoodiUri = r.nextStringOption,
    nimi = extractKielistetty(r.nextStringOption),
    onkoJulkinen = r.nextBoolean,
    metadata = r.nextStringOption().map(read[ValintaperusteMetadata]),
    organisaatio = r.nextString,
    muokkaaja = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))
}