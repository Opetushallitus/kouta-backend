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
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomake: Option[String] = None,
                     aloituspaikat: Option[Int] = None,
                     ensikertalaisenAloituspaikat: Option[Int] = None,
                     pohjakoulutusvaatimusKoodiUri: Option[String] = None,
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
                     kielivalinta: Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     validateIfDefined[HakukohdeOid](oid, assertValid(_)),
     assertValid(toteutusOid),
     assertValid(hakuOid),
     validateIfDefined[String](alkamisvuosi, validateAlkamisvuosi(_)),
     validateIfDefined[String](alkamiskausiKoodiUri, assertMatch(_, KausiKoodiPattern)),
     validateHakuajat(hakuajat),
     validateIfDefined[String](pohjakoulutusvaatimusKoodiUri, assertMatch(_, PohjakoulutusvaatimusKoodiPattern)),
     validateIfDefined[Int](aloituspaikat, assertNotNegative(_, "aloituspaikat")),
     validateIfDefined[Int](ensikertalaisenAloituspaikat, assertNotNegative(_, "ensikertalaisenAloituspaikat")),
     validateIfTrue(tila == Julkaistu, () => and(
       validateIfDefined[Boolean](liitteetOnkoSamaToimitusaika, validateIfTrue(_, () => assertNotOptional(liitteidenToimitusaika, "liitteiden toimitusaika"))),
       validateIfDefined[Boolean](liitteetOnkoSamaToimitusosoite, validateIfTrue(_, () => assertNotOptional(liitteidenToimitusosoite, "liitteiden toimitusaika")))
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