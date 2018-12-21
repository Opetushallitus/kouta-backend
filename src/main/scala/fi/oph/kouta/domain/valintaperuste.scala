package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

case class ValintaperusteMetadata()

case class Valintaperuste(id: Option[UUID] = None,
                          tila: Julkaisutila = Tallennettu,
                          hakutapaKoodiUri: Option[String] = None,
                          kohdejoukkoKoodiUri: Option[String] = None,
                          kohdejoukonTarkenneKoodiUri: Option[String] = None,
                          nimi: Kielistetty = Map(),
                          onkoJulkinen: Boolean = false,
                          metadata: Option[ValintaperusteMetadata] = None,
                          organisaatioOid: OrganisaatioOid,
                          muokkaaja: UserOid,
                          kielivalinta: Seq[Kieli] = Seq()) extends PerustiedotWithId with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     validateIfDefined[String](hakutapaKoodiUri, assertMatch(_, HakutapaKoodiPattern)),
     validateIfDefined[String](kohdejoukkoKoodiUri, assertMatch(_, KohdejoukkoKoodiPattern)),
     validateIfDefined[String](kohdejoukonTarkenneKoodiUri, assertMatch(_, KohdejoukonTarkenneKoodiPattern)),
     validateIfTrue(Julkaistu == tila, () => and(
       assertNotOptional(hakutapaKoodiUri, "hakutapaKoodiUri"),
       assertNotOptional(kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri"),
       assertNotOptional(kohdejoukonTarkenneKoodiUri, "kohdejoukonTarkenneKoodiUri"),
     ))
  )
}