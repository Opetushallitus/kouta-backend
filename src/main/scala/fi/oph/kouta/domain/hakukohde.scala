package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.{IsValid, Validatable}

case class Hakukohde(oid: Option[HakukohdeOid] = None,
                     toteutusOid: ToteutusOid,
                     hakuOid: HakuOid,
                     tila: Julkaisutila = Tallennettu,
                     nimi: Kielistetty = Map(),
                     alkamiskausiKoodiUri: Option[String] = None,
                     alkamisvuosi: Option[String] = None,
                     kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomakeAtaruId: Option[UUID] = None,
                     hakulomakeKuvaus: Kielistetty = Map(),
                     hakulomakeLinkki: Kielistetty = Map(),
                     kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                     aloituspaikat: Option[Int] = None,
                     minAloituspaikat: Option[Int] = None,
                     maxAloituspaikat: Option[Int] = None,
                     ensikertalaisenAloituspaikat: Option[Int] = None,
                     minEnsikertalaisenAloituspaikat: Option[Int] = None,
                     maxEnsikertalaisenAloituspaikat: Option[Int] = None,
                     pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
                     muuPohjakoulutusvaatimus: Kielistetty = Map(),
                     toinenAsteOnkoKaksoistutkinto: Option[Boolean] = None,
                     kaytetaanHaunAikataulua: Option[Boolean] = None,
                     valintaperusteId: Option[UUID] = None,
                     liitteetOnkoSamaToimitusaika: Option[Boolean] = None,
                     liitteetOnkoSamaToimitusosoite: Option[Boolean] = None,
                     liitteidenToimitusaika: Option[LocalDateTime] = None,
                     liitteidenToimitustapa: Option[LiitteenToimitustapa] = None,
                     liitteidenToimitusosoite: Option[LiitteenToimitusosoite] = None,
                     liitteet: List[Liite] = List(),
                     valintakokeet: List[Valintakoe] = List(),
                     hakuajat: List[Ajanjakso] = List(),
                     muokkaaja: UserOid,
                     organisaatioOid: OrganisaatioOid,
                     kielivalinta: Seq[Kieli] = Seq(),
                     modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     validateIfDefined[HakukohdeOid](oid, assertValid(_)),
     assertValid(toteutusOid),
     assertValid(hakuOid),
     validateIfDefined[String](alkamisvuosi, validateAlkamisvuosi(_)),
     validateIfDefined[String](alkamiskausiKoodiUri, assertMatch(_, KausiKoodiPattern)),
     validateHakuajat(hakuajat),
     validateIfNonEmpty[String](pohjakoulutusvaatimusKoodiUrit, assertMatch(_, PohjakoulutusvaatimusKoodiPattern)),
     validateIfDefined[Int](aloituspaikat, assertNotNegative(_, "aloituspaikat")),
     validateIfDefined[Int](ensikertalaisenAloituspaikat, assertNotNegative(_, "ensikertalaisenAloituspaikat")),
     validateIfTrue(tila == Julkaistu, () => and(
       validateIfDefined[Boolean](liitteetOnkoSamaToimitusaika, validateIfTrue(_, () => assertNotOptional(liitteidenToimitusaika, "liitteiden toimitusaika"))),
       validateIfDefined[Boolean](liitteetOnkoSamaToimitusosoite, validateIfTrue(_, () => assertNotOptional(liitteidenToimitusosoite, "liitteiden toimitusaika"))),
       validateAtaruId(hakulomaketyyppi, hakulomakeAtaruId)
    ))
  )
}

case class Valintakoe(id: Option[UUID] = None,
                      tyyppi: Option[String] = None,
                      tilaisuudet: List[Valintakoetilaisuus] = List())

case class Valintakoetilaisuus(osoite: Option[Osoite],
                               aika: Option[Ajanjakso] = None,
                               lisatietoja: Kielistetty = Map())

case class Liite(id: Option[UUID] = None,
                 tyyppi: Option[String],
                 nimi: Kielistetty = Map(),
                 kuvaus: Kielistetty = Map(),
                 toimitusaika: Option[LocalDateTime] = None,
                 toimitustapa: Option[LiitteenToimitustapa] = None,
                 toimitusosoite: Option[LiitteenToimitusosoite] = None)

case class LiitteenToimitusosoite(osoite: Osoite,
                                  sahkoposti: Option[String] = None)

case class HakukohdeListItem(oid: HakukohdeOid,
                             toteutusOid: ToteutusOid,
                             hakuOid: HakuOid,
                             valintaperusteId: Option[UUID],
                             nimi: Kielistetty,
                             tila: Julkaisutila,
                             organisaatioOid: OrganisaatioOid,
                             muokkaaja: UserOid,
                             modified: LocalDateTime) extends OidListItem
