package fi.oph.kouta.repository

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.raportointi.{OppilaitoksenOsa, _}
import fi.oph.kouta.util.TimeUtils.timeStampToModified
import org.json4s.jackson.Serialization.read
import slick.jdbc.GetResult

import java.util.UUID

trait RaportointiExtractors extends ExtractorBase {
  implicit val getKoulutusRaporttiItemResult: GetResult[KoulutusRaporttiItem] = GetResult(r =>
    KoulutusRaporttiItem(
      oid = KoulutusOid(r.nextString()),
      externalId = r.nextStringOption(),
      johtaaTutkintoon = r.nextBooleanOption(),
      koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
      koulutuksetKoodiUri = extractArray[String](r.nextObjectOption()),
      tila = Julkaisutila.withName(r.nextString()),
      tarjoajat = extractArray[String](r.nextObjectOption()).map(oid => OrganisaatioOid(oid)).toList,
      nimi = extractKielistetty(r.nextStringOption()),
      sorakuvausId = r.nextStringOption().map(UUID.fromString),
      metadata = r.nextStringOption().map(read[KoulutusMetadataRaporttiItem]),
      julkinen = r.nextBooleanOption(),
      muokkaaja = UserOid(r.nextString()),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      esikatselu = r.nextBooleanOption(),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      teemakuva = r.nextStringOption(),
      ePerusteId = r.nextLongOption(),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  )

  implicit val getKoulutusEnrichmentDataResult: GetResult[KoulutusEnrichmentData] = GetResult(r =>
    KoulutusEnrichmentData(
      oid = KoulutusOid(r.nextString()),
      koulutuksetKoodiUri = extractArray[String](r.nextObjectOption()),
      metadata = r.nextStringOption().map(read[KoulutusMetadata])
    )
  )

  implicit val getToteutusRaporttiItemResult: GetResult[ToteutusRaporttiItem] = GetResult(r =>
    ToteutusRaporttiItem(
      oid = ToteutusOid(r.nextString()),
      externalId = r.nextStringOption(),
      koulutusOid = KoulutusOid(r.nextString()),
      tila = Julkaisutila.withName(r.nextString()),
      tarjoajat = extractArray[String](r.nextObjectOption()).map(oid => OrganisaatioOid(oid)).toList,
      nimi = extractKielistetty(r.nextStringOption()),
      metadata = r.nextStringOption().map(read[ToteutusMetadataRaporttiItem]),
      muokkaaja = r.nextStringOption().map(UserOid),
      esikatselu = r.nextBooleanOption(),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      teemakuva = r.nextStringOption(),
      sorakuvausId = r.nextStringOption().map(UUID.fromString),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  )

  implicit val getHakukohdeRaporttiItemResult: GetResult[HakukohdeRaporttiItem] = GetResult(r =>
    HakukohdeRaporttiItem(
      oid = HakukohdeOid(r.nextString()),
      externalId = r.nextStringOption(),
      toteutusOid = ToteutusOid(r.nextString()),
      hakuOid = HakuOid(r.nextString()),
      tila = Julkaisutila.withName(r.nextString()),
      nimi = extractKielistetty(r.nextStringOption()),
      hakukohdeKoodiUri = r.nextStringOption(),
      hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
      hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
      hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
      hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
      kaytetaanHaunHakulomaketta = r.nextBooleanOption(),
      jarjestyspaikkaOid = r.nextStringOption().map(OrganisaatioOid),
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
      liitteidenToimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoiteRaporttiItem]),
      esikatselu = r.nextBooleanOption(),
      metadata = r.nextStringOption().map(read[HakukohdeMetadataRaporttiItem]),
      muokkaaja = UserOid(r.nextString()),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      modified = Some(timeStampToModified(r.nextTimestamp()))
    )
  )

  implicit val getHakukohdeLiiteRaporttiItemResult: GetResult[HakukohdeLiiteRaporttiItem] = GetResult(r =>
    HakukohdeLiiteRaporttiItem(
      id = UUID.fromString(r.nextString()),
      hakukohdeOid = HakukohdeOid(r.nextString()),
      tyyppiKoodiUri = r.nextStringOption(),
      nimi = extractKielistetty(r.nextStringOption()),
      kuvaus = extractKielistetty(r.nextStringOption()),
      toimitusaika = r.nextTimestampOption().map(_.toLocalDateTime),
      toimitustapa = r.nextStringOption().map(LiitteenToimitustapa.withName),
      toimitusosoite = r.nextStringOption().map(read[LiitteenToimitusosoiteRaporttiItem])
    )
  )

  implicit val getValintakoeRaporttiItemResult: GetResult[ValintakoeRaporttiItem] = GetResult(r =>
    ValintakoeRaporttiItem(
      id = UUID.fromString(r.nextString()),
      parentOidOrUUID = r.nextString(),
      tyyppiKoodiUri = r.nextStringOption(),
      nimi = extractKielistetty(r.nextStringOption()),
      metadata = r.nextStringOption().map(read[ValintakoeMetadataRaporttiItem]),
      tilaisuudet = r.nextStringOption().map(read[List[ValintakoetilaisuusRaporttiItem]]).getOrElse(List()),
      muokkaaja = r.nextStringOption()
    )
  )

  implicit val getHakuRaporttiItemResult: GetResult[HakuRaporttiItem] = GetResult(r =>
    HakuRaporttiItem(
      oid = HakuOid(r.nextString()),
      externalId = r.nextStringOption(),
      tila = Julkaisutila.withName(r.nextString()),
      nimi = extractKielistetty(r.nextStringOption()),
      hakutapaKoodiUri = r.nextStringOption(),
      hakukohteenLiittamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
      hakukohteenMuokkaamisenTakaraja = r.nextTimestampOption().map(_.toLocalDateTime),
      hakukohteenLiittajaOrganisaatiot = read[Seq[OrganisaatioOid]](r.nextString()),
      ajastettuJulkaisu = r.nextTimestampOption().map(_.toLocalDateTime),
      ajastettuHaunJaHakukohteidenArkistointi = r.nextTimestampOption().map(_.toLocalDateTime),
      ajastettuHaunJaHakukohteidenArkistointiAjettu = r.nextTimestampOption().map(_.toLocalDateTime),
      kohdejoukkoKoodiUri = r.nextStringOption(),
      kohdejoukonTarkenneKoodiUri = r.nextStringOption(),
      hakulomaketyyppi = r.nextStringOption().map(Hakulomaketyyppi.withName),
      hakulomakeAtaruId = r.nextStringOption().map(UUID.fromString),
      hakulomakeKuvaus = extractKielistetty(r.nextStringOption()),
      hakulomakeLinkki = extractKielistetty(r.nextStringOption()),
      metadata = r.nextStringOption().map(read[HakuMetadataRaporttiItem]),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      hakuajat = List(),
      muokkaaja = UserOid(r.nextString()),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  )

  implicit val getValintaperusteRaporttiItemResult: GetResult[ValintaperusteRaporttiItem] = GetResult(r =>
    ValintaperusteRaporttiItem(
      id = UUID.fromString(r.nextString()),
      externalId = r.nextStringOption(),
      tila = Julkaisutila.withName(r.nextString()),
      koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
      hakutapaKoodiUri = r.nextStringOption(),
      kohdejoukkoKoodiUri = r.nextStringOption(),
      nimi = extractKielistetty(r.nextStringOption()),
      julkinen = r.nextBooleanOption(),
      esikatselu = r.nextBooleanOption(),
      metadata = r.nextStringOption().map(read[ValintaperusteMetadataRaporttiItem]),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      muokkaaja = UserOid(r.nextString()),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      modified = Some(timeStampToModified(r.nextTimestamp()))
    )
  )

  implicit val getSorakuvausRaporttiItemResult: GetResult[SorakuvausRaporttiItem] = GetResult(r =>
    SorakuvausRaporttiItem(
      id = UUID.fromString(r.nextString()),
      externalId = r.nextStringOption(),
      tila = Julkaisutila.withName(r.nextString()),
      nimi = extractKielistetty(r.nextStringOption()),
      koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      metadata = r.nextStringOption().map(read[SorakuvausMetadataRaporttiItem]),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      muokkaaja = UserOid(r.nextString()),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  )

  private def oppilaitosOrOsaMetadata(
      organisaatiotyyppi: Organisaatiotyyppi,
      metadataStr: String
  ): OppilaitosOrOsaMetadataRaporttiItem =
    if (organisaatiotyyppi == OppilaitoksenOsa) read[OppilaitoksenOsaMetadataRaporttiItem](metadataStr)
    else read[OppilaitosMetadataRaporttiItem](metadataStr)

  implicit val getOppilaitosOrOsaRaporttiItemResult: GetResult[OppilaitosOrOsaRaporttiItem] = GetResult(r => {
    val tyyppi = Organisaatiotyyppi.withName(r.rs.getString("tyyppi"))
    OppilaitosOrOsaRaporttiItem(
      oid = OrganisaatioOid(r.nextString()),
      parentOppilaitosOid = r.skip.nextStringOption().map(OrganisaatioOid),
      tila = Julkaisutila.withName(r.nextString()),
      esikatselu = r.nextBooleanOption(),
      metadata = r.nextStringOption().map(str => oppilaitosOrOsaMetadata(tyyppi, str)),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      organisaatioOid = r.nextStringOption().map(OrganisaatioOid),
      muokkaaja = UserOid(r.nextString()),
      teemakuva = r.nextStringOption(),
      logo = r.nextStringOption(),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  })

  implicit val getPistetietoRaporttiItemResult: GetResult[PistetietoRaporttiItem] = GetResult(r =>
    PistetietoRaporttiItem(
      tarjoaja = r.nextStringOption().map(OrganisaatioOid),
      hakukohdekoodi = r.nextStringOption(),
      pisteet = r.nextDoubleOption(),
      vuosi = r.nextStringOption(),
      valintatapajonoOid = r.nextStringOption(),
      hakukohdeOid = r.nextStringOption().map(HakukohdeOid),
      hakuOid = r.nextStringOption().map(HakuOid),
      valintatapajonoTyyppi = r.nextStringOption(),
      aloituspaikat = r.nextIntOption(),
      ensisijaisestiHakeneet = r.nextIntOption(),
      modified = r.nextTimestampOption().map(timeStampToModified)
    )
  )
}
