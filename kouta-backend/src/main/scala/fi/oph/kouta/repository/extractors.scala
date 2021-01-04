package fi.oph.kouta.repository

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.util.TimeUtils.timeStampToModified
import org.json4s.jackson.Serialization.read
import slick.jdbc._

trait ExtractorBase extends KoutaJsonFormats {
  case class Tarjoaja(oid: GenericOid, tarjoajaOid: OrganisaatioOid)
  case class Hakuaika(oid: GenericOid, alkaa: LocalDateTime, paattyy: Option[LocalDateTime])

  implicit val getKoulutusOidResult: GetResult[KoulutusOid] = GetResult(r => KoulutusOid(r.nextString()))
  implicit val getToteutusOidResult: GetResult[ToteutusOid] = GetResult(r => ToteutusOid(r.nextString()))
  implicit val getHakukohdeOidResult: GetResult[HakukohdeOid] = GetResult(r => HakukohdeOid(r.nextString()))
  implicit val getHakuOidResult: GetResult[HakuOid] = GetResult(r => HakuOid(r.nextString()))
  implicit val getOrganisaatioOidResult: GetResult[OrganisaatioOid] = GetResult(r => OrganisaatioOid(r.nextString()))

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))
  implicit val getInstantResult: GetResult[Instant] = GetResult(r => r.nextTimestamp().toInstant)

  implicit val getTarjoajatResult: GetResult[Tarjoaja] = GetResult(r => new Tarjoaja(GenericOid(r.nextString()), OrganisaatioOid(r.nextString())))

  implicit val getHakuaikaResult: GetResult[Hakuaika] = GetResult(r => {
    new Hakuaika(GenericOid(r.nextString()), r.nextTimestamp().toLocalDateTime, r.nextTimestampOption().map(_.toLocalDateTime))
  })

  implicit val getValintakoeResult: GetResult[Valintakoe] = GetResult(r => Valintakoe(
    id = r.nextStringOption().map(UUID.fromString),
    tyyppiKoodiUri = r.nextStringOption(),
    nimi = extractKielistetty(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[ValintakoeMetadata]),
    tilaisuudet = r.nextStringOption().map(read[List[Valintakoetilaisuus]]).getOrElse(List())
  ))

  implicit val getKeywordResult: GetResult[Keyword] = GetResult(r => Keyword(Kieli.withName(r.nextString()), r.nextString()))

  implicit val getJulkaisutilaResult: GetResult[Julkaisutila] = GetResult(r => Julkaisutila.withName(r.nextString()))
  implicit val getJulkaisutilaOptionResult: GetResult[Option[Julkaisutila]] = GetResult(r => r.nextStringOption().map(Julkaisutila.withName))

  implicit val getKoulutustyyppiResult: GetResult[Koulutustyyppi] = GetResult(r => Koulutustyyppi.withName(r.nextString()))
  implicit val getKoulutustyyppiOptionResult: GetResult[Option[Koulutustyyppi]] = GetResult(r => r.nextStringOption().map(Koulutustyyppi.withName))

  implicit val getToteutusMetadataOptionResult: GetResult[Option[ToteutusMetadata]] = GetResult(r => r.nextStringOption().map(read[ToteutusMetadata]))

  def extractArray[U](o: Option[Object]): Seq[U] = o
    .map(_.asInstanceOf[org.postgresql.jdbc.PgArray])
    .map(_.getArray.asInstanceOf[Array[U]].toSeq)
    .getOrElse(Seq.empty[U])

  def extractKielistetty(json: Option[String]): Kielistetty = json.map(read[Map[Kieli, String]]).getOrElse(Map())
  def extractKielivalinta(json: Option[String]): Seq[Kieli] = json.map(read[Seq[Kieli]]).getOrElse(Seq())

  implicit val getUUIDResult: GetResult[UUID] = GetResult(r => {
    UUID.fromString(r.nextString())
  })
}

trait KoulutusExtractors extends ExtractorBase {
  implicit val getKoulutusResult: GetResult[Koulutus] = GetResult(r => Koulutus(
    oid = r.nextStringOption().map(KoulutusOid(_)),
    johtaaTutkintoon = r.nextBoolean(),
    koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
    koulutusKoodiUri = r.nextStringOption(),
    tila = Julkaisutila.withName(r.nextString()),
    tarjoajat = List(),
    nimi = extractKielistetty(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[KoulutusMetadata]),
    julkinen = r.nextBoolean(),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    teemakuva = r.nextStringOption(),
    ePerusteId = r.nextLongOption(),
    modified = Some(timeStampToModified(r.nextTimestamp()))))

  implicit val getKoulutusListItem: GetResult[KoulutusListItem] = GetResult(r => KoulutusListItem(
    oid = KoulutusOid(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    tarjoajat = List(),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait ToteutusExtractors extends ExtractorBase {
  implicit val getToteutusResult: GetResult[Toteutus] = GetResult(r => Toteutus(
    oid = r.nextStringOption().map(ToteutusOid(_)),
    koulutusOid = KoulutusOid(r.nextString()),
    tila = Julkaisutila.withName(r.nextString()),
    tarjoajat = List(),
    nimi = extractKielistetty(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[ToteutusMetadata]),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    teemakuva = r.nextStringOption(),
    sorakuvausId = r.nextStringOption().map(UUID.fromString),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getToteutusListItemResult: GetResult[ToteutusListItem] = GetResult(r => ToteutusListItem(
    oid = ToteutusOid(r.nextString()),
    koulutusOid = KoulutusOid(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    tarjoajat = List(),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait HakuExtractors extends ExtractorBase {
  implicit val getHakuResult: GetResult[Haku] = GetResult(r => Haku(
    oid = r.nextStringOption().map(HakuOid(_)),
    tila = Julkaisutila.withName(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    hakutapaKoodiUri = r.nextStringOption(),
    hakukohteenLiittamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
    hakukohteenMuokkaamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
    ajastettuJulkaisu = r.nextTimestampOption().map(_.toLocalDateTime),
    kohdejoukkoKoodiUri = r.nextStringOption(),
    kohdejoukonTarkenneKoodiUri = r.nextStringOption(),
    hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
    hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
    hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
    hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[HakuMetadata]),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    hakuajat = List(),
    muokkaaja = UserOid(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getHakuListItem: GetResult[HakuListItem] = GetResult(r => HakuListItem(
    oid = HakuOid(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait ValintaperusteExtractors extends ExtractorBase {
  implicit val getValintaperusteResult: GetResult[Valintaperuste] = GetResult(r => Valintaperuste(
    id = r.nextStringOption().map(UUID.fromString),
    tila = Julkaisutila.withName(r.nextString()),
    koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
    hakutapaKoodiUri = r.nextStringOption(),
    kohdejoukkoKoodiUri = r.nextStringOption(),
    kohdejoukonTarkenneKoodiUri = r.nextStringOption(),
    nimi = extractKielistetty(r.nextStringOption()),
    julkinen = r.nextBoolean(),
    metadata = r.nextStringOption().map(read[ValintaperusteMetadata]),
    sorakuvausId = r.nextStringOption().map(UUID.fromString),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getValintaperusteListItemResult: GetResult[ValintaperusteListItem] = GetResult(r => ValintaperusteListItem(
    id = UUID.fromString(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait SorakuvausExtractors extends ExtractorBase {
  implicit val getSorakuvausResult: GetResult[Sorakuvaus] = GetResult(r => Sorakuvaus(
    id = r.nextStringOption().map(UUID.fromString),
    tila = Julkaisutila.withName(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[SorakuvausMetadata]),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getSorakuvausListItemResult: GetResult[SorakuvausListItem] = GetResult(r => SorakuvausListItem(
    id = UUID.fromString(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait HakukohdeExctractors extends ExtractorBase {
  implicit val getHakukohdeResult: GetResult[Hakukohde] = GetResult(r => Hakukohde(
    oid = r.nextStringOption().map(HakukohdeOid(_)),
    toteutusOid = ToteutusOid(r.nextString()),
    hakuOid = HakuOid(r.nextString()),
    tila = Julkaisutila.withName(r.nextString()),
    nimi = extractKielistetty(r.nextStringOption()),
    alkamiskausiKoodiUri = r.nextStringOption(),
    alkamisvuosi = r.nextStringOption(),
    kaytetaanHaunAlkamiskautta = r.nextBooleanOption(),
    hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
    hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
    hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
    hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
    kaytetaanHaunHakulomaketta = r.nextBooleanOption(),
    jarjestyspaikkaOid = r.nextStringOption().map(OrganisaatioOid(_)),
    aloituspaikat = r.nextIntOption(),
    ensikertalaisenAloituspaikat = r.nextIntOption(),
    pohjakoulutusvaatimusKoodiUrit = extractArray[String](r.nextObjectOption()),
    pohjakoulutusvaatimusTarkenne = extractKielistetty(r.nextStringOption()),
    muuPohjakoulutusvaatimus = extractKielistetty(r.nextStringOption()),
    toinenAsteOnkoKaksoistutkinto = r.nextBooleanOption(),
    kaytetaanHaunAikataulua = r.nextBooleanOption(),
    valintaperusteId = r.nextStringOption().map(UUID.fromString),
    liitteetOnkoSamaToimitusaika = r.nextBooleanOption(),
    liitteetOnkoSamaToimitusosoite = r.nextBooleanOption(),
    liitteidenToimitusaika = r.nextTimestampOption().map(_.toLocalDateTime),
    liitteidenToimitustapa = r.nextStringOption().map(LiitteenToimitustapa.withName),
    liitteidenToimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoite]),
    metadata = r.nextStringOption().map(read[HakukohdeMetadata]),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getHakukohdeListItemResult: GetResult[HakukohdeListItem] = GetResult(r => HakukohdeListItem(
    oid = HakukohdeOid(r.nextString()),
    toteutusOid = ToteutusOid(r.nextString()),
    hakuOid = HakuOid(r.nextString()),
    valintaperusteId = r.nextStringOption().map(UUID.fromString),
    nimi = extractKielistetty(r.nextStringOption()),
    tila = Julkaisutila.withName(r.nextString()),
    jarjestyspaikkaOid = r.nextStringOption().map(OrganisaatioOid),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))

  implicit val getLiiteResult: GetResult[Liite] = GetResult(r => Liite(
    id = r.nextStringOption().map(UUID.fromString),
    tyyppiKoodiUri = r.nextStringOption(),
    nimi = extractKielistetty(r.nextStringOption()),
    kuvaus = extractKielistetty(r.nextStringOption()),
    toimitusaika = r.nextTimestampOption().map(_.toLocalDateTime),
    toimitustapa = r.nextStringOption().map(LiitteenToimitustapa.withName),
    toimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoite]),
  ))
}

trait OppilaitosExtractors extends ExtractorBase {
  implicit val getOppilaitosResult: GetResult[Oppilaitos] = GetResult(r => Oppilaitos(
    oid = OrganisaatioOid(r.nextString()),
    tila = Julkaisutila.withName(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[OppilaitosMetadata]),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    teemakuva = r.nextStringOption(),
    logo = r.nextStringOption(),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))
}

trait OppilaitoksenOsaExtractors extends ExtractorBase {
  implicit val getOppilaitoksenOsaResult: GetResult[OppilaitoksenOsa] = GetResult(r => OppilaitoksenOsa(
    oid = OrganisaatioOid(r.nextString()),
    oppilaitosOid = OrganisaatioOid(r.nextString()),
    tila = Julkaisutila.withName(r.nextString()),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    metadata = r.nextStringOption().map(read[OppilaitoksenOsaMetadata]),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    teemakuva = r.nextStringOption(),
    modified = Some(timeStampToModified(r.nextTimestamp()))
  ))

  implicit val getOppilaitoksenOsaListItemResult: GetResult[OppilaitoksenOsaListItem] = GetResult(r => OppilaitoksenOsaListItem(
    oid = OrganisaatioOid(r.nextString()),
    oppilaitosOid = OrganisaatioOid(r.nextString()),
    tila = Julkaisutila.withName(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    muokkaaja = UserOid(r.nextString()),
    modified = timeStampToModified(r.nextTimestamp())
  ))
}

trait HakutietoExtractors extends ExtractorBase {

  implicit val getHakutietoHakuResult: GetResult[(ToteutusOid, HakutietoHaku)] = GetResult(r =>
    ( ToteutusOid(r.nextString()), HakutietoHaku(
      hakuOid = HakuOid(r.nextString()),
      nimi = extractKielistetty(r.nextStringOption()),
      hakutapaKoodiUri = r.nextStringOption(),
      alkamiskausiKoodiUri = r.nextStringOption(),
      alkamisvuosi = r.nextStringOption(),
      hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
      hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
      hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
      hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
      organisaatioOid = OrganisaatioOid(r.nextString()),
      hakuajat = Seq(),
      muokkaaja = UserOid(r.nextString()),
      modified = Some(timeStampToModified(r.nextTimestamp())),
      hakukohteet = Seq())))

  implicit val getHakutietoHakukohdeResult: GetResult[(ToteutusOid, HakuOid, HakutietoHakukohde)] = GetResult(r =>
    ( ToteutusOid(r.nextString()), HakuOid(r.nextString()), HakutietoHakukohde(
      hakukohdeOid = HakukohdeOid(r.nextString()),
      nimi = extractKielistetty(r.nextStringOption()),
      valintaperusteId = r.nextStringOption().map(UUID.fromString),
      alkamiskausiKoodiUri = r.nextStringOption(),
      alkamisvuosi = r.nextStringOption(),
      kaytetaanHaunAlkamiskautta = r.nextBooleanOption(),
      jarjestyspaikkaOid = r.nextStringOption().map(OrganisaatioOid(_)),
      hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
      hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
      hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
      hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
      kaytetaanHaunHakulomaketta = r.nextBooleanOption(),
      aloituspaikat = r.nextIntOption(),
      ensikertalaisenAloituspaikat = r.nextIntOption(),
      kaytetaanHaunAikataulua = r.nextBooleanOption(),
      pohjakoulutusvaatimusKoodiUrit = extractArray[String](r.nextObjectOption()),
      pohjakoulutusvaatimusTarkenne = extractKielistetty(r.nextStringOption()),
      organisaatioOid = OrganisaatioOid(r.nextString()),
      hakuajat = List(),
      muokkaaja = UserOid(r.nextString()),
      modified = Some(timeStampToModified(r.nextTimestamp())))))
}

trait KeywordExtractors extends ExtractorBase
