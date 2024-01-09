package fi.oph.kouta.repository

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.raportointi._
import fi.oph.kouta.util.TimeUtils.timeStampToModified
import org.json4s.jackson.Serialization.read
import slick.jdbc.GetResult

import java.util.UUID

trait RaportointiExtractors extends ExtractorBase {
  implicit val getKoulutusResult: GetResult[Koulutus] = GetResult(r => Koulutus(
    oid = r.nextStringOption().map(KoulutusOid),
    externalId = r.nextStringOption(),
    johtaaTutkintoon = r.nextBoolean(),
    koulutustyyppi = Koulutustyyppi.withName(r.nextString()),
    koulutuksetKoodiUri = extractArray[String](r.nextObjectOption()),
    tila = Julkaisutila.withName(r.nextString()),
    tarjoajat = extractArray[String](r.nextObjectOption()).map(oid => OrganisaatioOid(oid)).toList,
    nimi = extractKielistetty(r.nextStringOption()),
    sorakuvausId = r.nextStringOption().map(UUID.fromString),
    metadata = r.nextStringOption().map(read[KoulutusMetadata]),
    julkinen = r.nextBoolean(),
    muokkaaja = UserOid(r.nextString()),
    organisaatioOid = OrganisaatioOid(r.nextString()),
    esikatselu = r.nextBoolean(),
    kielivalinta = extractKielivalinta(r.nextStringOption()),
    teemakuva = r.nextStringOption(),
    ePerusteId = r.nextLongOption(),
    modified = Some(timeStampToModified(r.nextTimestamp()))))

  implicit val getToteutusResult: GetResult[Toteutus] = GetResult(r =>
    Toteutus(
      oid = r.nextStringOption().map(ToteutusOid),
      externalId = r.nextStringOption(),
      koulutusOid = KoulutusOid(r.nextString()),
      tila = Julkaisutila.withName(r.nextString()),
      tarjoajat = extractArray[String](r.nextObjectOption()).map(oid => OrganisaatioOid(oid)).toList,
      nimi = extractKielistetty(r.nextStringOption()),
      metadata = r.nextStringOption().map(read[ToteutusMetadata]),
      muokkaaja = UserOid(r.nextString()),
      esikatselu = r.nextBoolean(),
      organisaatioOid = OrganisaatioOid(r.nextString()),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      teemakuva = r.nextStringOption(),
      sorakuvausId = r.nextStringOption().map(UUID.fromString),
      modified = Some(timeStampToModified(r.nextTimestamp()))
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
      esikatselu = r.nextBoolean(),
      metadata = r.nextStringOption().map(read[HakukohdeMetadataRaporttiItem]),
      muokkaaja = UserOid(r.nextString()),
      organisaatioOid = OrganisaatioOid(r.nextString()),
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
      metadata = r.nextStringOption().map(read[ValintakoeMetadata]),
      tilaisuudet = r.nextStringOption().map(read[List[Valintakoetilaisuus]]).getOrElse(List()),
      muokkaaja = r.nextString()
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
      julkinen = r.nextBoolean(),
      esikatselu = r.nextBoolean(),
      metadata = r.nextStringOption().map(read[ValintaperusteMetadataRaporttiItem]),
      organisaatioOid = OrganisaatioOid(r.nextString()),
      muokkaaja = UserOid(r.nextString()),
      kielivalinta = extractKielivalinta(r.nextStringOption()),
      modified = Some(timeStampToModified(r.nextTimestamp()))
    )
  )


}
