package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{HakuOid, Oid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

case class HakuMetadata(yhteystieto: Option[Yhteystieto] = None,
                        tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq())

case class Haku(oid: Option[HakuOid] = None,
                tila: Julkaisutila = Tallennettu,
                nimi: Kielistetty = Map(),
                hakutapaKoodiUri: Option[String] = None,
                hakukohteenLiittamisenTakaraja: Option[LocalDateTime] = None,
                hakukohteenMuokkaamisenTakaraja: Option[LocalDateTime] = None,
                ajastettuJulkaisu: Option[LocalDateTime] = None,
                alkamiskausiKoodiUri: Option[String] = None,
                alkamisvuosi: Option[String] = None,
                kohdejoukkoKoodiUri: Option[String] = None,
                kohdejoukonTarkenneKoodiUri: Option[String] = None,
                hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                hakulomake: Kielistetty = Map(),
                metadata: Option[HakuMetadata] = None,
                organisaatioOid: OrganisaatioOid,
                hakuajat: List[Ajanjakso] = List(),
                muokkaaja: UserOid,
                kielivalinta: Seq[Kieli] = Seq(),
                modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and (
     super.validate(),
     validateIfDefined[HakuOid](oid, assertValid(_)),
     validateIfDefined[String](hakutapaKoodiUri, assertMatch(_, HakutapaKoodiPattern)),
     validateIfDefined[String](kohdejoukkoKoodiUri, assertMatch(_, KohdejoukkoKoodiPattern)),
     validateIfDefined[String](kohdejoukonTarkenneKoodiUri, assertMatch(_, KohdejoukonTarkenneKoodiPattern)),
     validateIfDefined[String](alkamisvuosi, validateAlkamisvuosi(_)),
     validateIfDefined[String](alkamiskausiKoodiUri, assertMatch(_, KausiKoodiPattern)),
     validateHakuajat(hakuajat),
     validateIfTrue(tila == Julkaistu, () => and (
       assertNotOptional(hakutapaKoodiUri, "hakutapaKoodiUri"),
       assertNotOptional(kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri"),
       assertNotOptional(hakulomaketyyppi, "hakulomaketyyppi")
     ))
  )
}

case class HakuListItem(oid: HakuOid,
                        nimi: Kielistetty,
                        tila: Julkaisutila,
                        organisaatioOid: OrganisaatioOid,
                        muokkaaja: UserOid,
                        modified: LocalDateTime) extends OidListItem
