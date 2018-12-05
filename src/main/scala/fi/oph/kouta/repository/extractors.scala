package fi.oph.kouta.repository

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.read
import slick.jdbc._

trait ExtractorBase extends KoutaJsonFormats {

  case class Tarjoaja(oid:String, tarjoajaOid:String)
  case class Hakuaika(oid:String, alkaa:LocalDateTime, paattyy:LocalDateTime)

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  implicit val getTarjoajatResult: GetResult[Tarjoaja] = GetResult(r => new Tarjoaja(r.nextString, r.nextString))

  implicit val getHakuaikaResult: GetResult[Hakuaika] = GetResult(r => {
    new Hakuaika(r.nextString(), r.nextTimestamp.toLocalDateTime, r.nextTimestamp.toLocalDateTime)
  })

  def extractKielistetty(json:Option[String]): Kielistetty = json.map(read[Map[Kieli, String]]).getOrElse(Map())
  def extractKielivalinta(json:Option[String]): Seq[Kieli] = json.map(read[Seq[Kieli]]).getOrElse(Seq())

  implicit val getUUIDResult: GetResult[UUID] = GetResult(r => {
    UUID.fromString(r.nextString())
  })
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
    organisaatioOid = r.nextString,
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
    organisaatioOid = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))
}

trait HakuExtractors extends ExtractorBase {

  implicit val getHakuResult: GetResult[Haku] = GetResult(r => Haku(
    oid = r.nextStringOption,
    tila = Julkaisutila.withName(r.nextString),
    nimi = extractKielistetty(r.nextStringOption),
    hakutapaKoodiUri = r.nextStringOption,
    hakukohteenLiittamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
    hakukohteenMuokkaamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
    alkamiskausiKoodiUri = r.nextStringOption,
    alkamisvuosi = r.nextStringOption,
    kohdejoukkoKoodiUri = r.nextStringOption,
    kohdejoukonTarkenneKoodiUri = r.nextStringOption,
    hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
    hakulomake = r.nextStringOption(),
    metadata = r.nextStringOption().map(read[HakuMetadata]),
    organisaatioOid = r.nextString,
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
    organisaatioOid = r.nextString,
    muokkaaja = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))
}

trait HakukohdeExctractors extends ExtractorBase {

  implicit val getHakukohdeResult: GetResult[Hakukohde] = GetResult(r => Hakukohde(
    oid = r.nextStringOption,
    toteutusOid = r.nextString,
    hakuOid = r.nextString,
    tila = Julkaisutila.withName(r.nextString),
    nimi = extractKielistetty(r.nextStringOption),
    alkamiskausiKoodiUri = r.nextStringOption,
    alkamisvuosi = r.nextStringOption,
    hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
    hakulomake = r.nextStringOption,
    aloituspaikat = r.nextIntOption,
    ensikertalaisenAloituspaikat = r.nextIntOption,
    pohjakoulutusvaatimusKoodiUri = r.nextStringOption,
    muuPohjakoulutusvaatimus = extractKielistetty(r.nextStringOption),
    toinenAsteOnkoKaksoistutkinto = r.nextBooleanOption,
    kaytetaanHaunAikataulua = r.nextBooleanOption,
    valintaperuste = r.nextStringOption.map(UUID.fromString),
    liitteetOnkoSamaToimitusaika = r.nextBooleanOption(),
    liitteetOnkoSamaToimitusosoite = r.nextBooleanOption(),
    liitteidenToimitusaika = r.nextTimestampOption().map(_.toLocalDateTime),
    liitteidenToimitustapa = r.nextStringOption().map(LiitteenToimitustapa.withName),
    liitteidenToimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoite]),
    muokkaaja = r.nextString,
    organisaatioOid = r.nextString,
    kielivalinta = extractKielivalinta(r.nextStringOption)
  ))

  implicit val getValintakoeResult: GetResult[Valintakoe] = GetResult(r => Valintakoe(
    id = r.nextStringOption.map(UUID.fromString),
    tyyppi = r.nextStringOption(),
    tilaisuudet = r.nextStringOption().map(read[List[Valintakoetilaisuus]]).getOrElse(List()),
  ))

  implicit val getLiiteResult: GetResult[Liite] = GetResult(r => Liite(
    id = r.nextStringOption.map(UUID.fromString),
    tyyppi = r.nextStringOption(),
    nimi = extractKielistetty(r.nextStringOption),
    kuvaus = extractKielistetty(r.nextStringOption),
    toimitusaika = r.nextTimestampOption().map(_.toLocalDateTime),
    toimitustapa = r.nextStringOption().map(LiitteenToimitustapa.withName),
    toimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoite]),
  ))
}

trait KeywordExtractors extends ExtractorBase