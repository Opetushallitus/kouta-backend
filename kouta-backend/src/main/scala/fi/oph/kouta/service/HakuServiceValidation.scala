package fi.oph.kouta.service

import fi.oph.kouta.domain.{Ajanjakso, Haku, HakuMetadata, Julkaisutila, Kieli, KoulutuksenAlkamiskausi, TilaFilter, Yhteyshenkilo}
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.Validations.{HakutapaKoodiPattern, KohdejoukkoKoodiPattern, KohdejoukonTarkenneKoodiPattern, and, assertMatch, assertNotOptional, assertTrue, assertValid, integrityViolationMsg, validateHakulomake, validateIfDefined, validateIfJulkaistu, validateIfNonEmpty, validateIfTrue}
import fi.oph.kouta.validation.{IsValid, NoErrors}

object HakuServiceValidation extends HakuServiceValidation(OrganisaatioServiceImpl, HakukohdeDAO)
class HakuServiceValidation(val organisaatioService: OrganisaatioService, hakukohdeDAO: HakukohdeDAO) extends ValidatingService[Haku] {
  override def validateEntity(haku: Haku, crudOperation: CrudOperation): IsValid = {
    val tila         = haku.tila
    val kielivalinta = haku.kielivalinta

    and(
      haku.validate(),
      assertNotOptional(haku.hakutapaKoodiUri, "hakutapaKoodiUri"),
      assertNotOptional(haku.kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri"),
      validateIfDefined[String](haku.hakutapaKoodiUri, assertMatch(_, HakutapaKoodiPattern, "hakutapaKoodiUri")),
      validateIfDefined[String](
        haku.kohdejoukkoKoodiUri,
        assertMatch(_, KohdejoukkoKoodiPattern, "kohdejoukkoKoodiUri")
      ),
      validateIfDefined[String](
        haku.kohdejoukonTarkenneKoodiUri,
        assertMatch(_, KohdejoukonTarkenneKoodiPattern, "kohdejoukonTarkenneKoodiUri")
      ),
      validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validate(tila, kielivalinta, _)),
      validateIfDefined[HakuMetadata](haku.metadata, validateMetadata(_, tila, kielivalinta)),
      validateIfJulkaistu(
        tila,
        and(
          assertNotOptional(haku.metadata, "metadata"),
          assertNotOptional(haku.hakulomaketyyppi, "hakulomaketyyppi"),
          validateHakulomake(
            haku.hakulomaketyyppi,
            haku.hakulomakeAtaruId,
            haku.hakulomakeKuvaus,
            haku.hakulomakeLinkki,
            haku.kielivalinta
          ),
          validateIfTrue(
            haku.hakutapaKoodiUri.contains("hakutapa_01#1"), //Yhteishaku
            assertNotOptional(haku.metadata.get.koulutuksenAlkamiskausi, "metadata.koulutuksenAlkamiskausi")
          )
        )
      )
    )
  }

  private def validateMetadata(m: HakuMetadata, tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateIfNonEmpty[Yhteyshenkilo](m.yhteyshenkilot, "path.yhteyshenkilot", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[Ajanjakso](
      m.tulevaisuudenAikataulu,
      "path.tulevaisuudenAikataulu",
      _.validate(tila, kielivalinta, _)
    ),
    validateIfDefined[KoulutuksenAlkamiskausi](
      m.koulutuksenAlkamiskausi,
      _.validate(tila, kielivalinta, "path.koulutuksenAlkamiskausi")
    )
  )

  override def validateInternalDependenciesWhenDeletingEntity(haku: Haku): IsValid = assertTrue(
    hakukohdeDAO.listByHakuOid(haku.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Hakua", "hakukohteita")
  )

  override def validateEntityOnJulkaisu(haku: Haku, crudOperation: CrudOperation): IsValid = and(
    validateIfTrue(
      !haku.hakutapaKoodiUri.contains("hakutapa_03#1"),
      and( // Not Jatkuva haku
        validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validateOnJulkaisu(_))
      )
    ),
    validateIfTrue(
      haku.hakutapaKoodiUri.contains("hakutapa_03#1"),
      and( // Jatkuva haku
        validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validateOnJulkaisuForJatkuvaHaku(_))
      )
    ),
    validateIfDefined[HakuMetadata](
      haku.metadata,
      m =>
        and(
          validateIfNonEmpty[Ajanjakso](
            m.tulevaisuudenAikataulu,
            "metadata.tulevaisuudenAikataulu",
            _.validateOnJulkaisu(_)
          ),
          validateIfDefined[KoulutuksenAlkamiskausi](
            m.koulutuksenAlkamiskausi,
            _.validateOnJulkaisu("metadata.koulutuksenAlkamiskausi")
          )
        )
    )
  )
}
