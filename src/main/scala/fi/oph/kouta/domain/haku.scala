package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

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
                hakulomakeAtaruId: Option[UUID] = None,
                hakulomakeKuvaus: Kielistetty = Map(),
                hakulomakeLinkki: Kielistetty = Map(),
                metadata: Option[HakuMetadata] = None,
                organisaatioOid: OrganisaatioOid,
                hakuajat: List[Ajanjakso] = List(),
                muokkaaja: UserOid,
                kielivalinta: Seq[Kieli] = Seq(),
                modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable /*with Indexable*/ {

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
       assertNotOptional(hakulomaketyyppi, "hakulomaketyyppi"),
       validateAtaruId(hakulomaketyyppi, hakulomakeAtaruId)
     ))
  )

  //override val indexType: String = IndexTypeHaku
  //override def getIndexIds: Seq[String] = oid.map(_.toString).map(Seq(_)).getOrElse(Seq())
}

case class HakuListItem(oid: HakuOid,
                        nimi: Kielistetty,
                        tila: Julkaisutila,
                        organisaatioOid: OrganisaatioOid,
                        muokkaaja: UserOid,
                        modified: LocalDateTime) extends OidListItem

case class HakuMetadata(yhteystieto: Option[Yhteystieto] = None,
                        tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq())
