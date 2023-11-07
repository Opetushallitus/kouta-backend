package fi.oph.kouta.service

import fi.oph.kouta.client.{HakemusPalveluClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{HakuDiffResolver, IsValid, ValidationContext}

import java.util.UUID

object HakuServiceValidation
    extends HakuServiceValidation(KoodistoService, HakemusPalveluClient, HakukohdeDAO, OrganisaatioServiceImpl)
class HakuServiceValidation(
    koodistoService: KoodistoService,
    hakemusPalveluClient: HakemusPalveluClient,
    hakukohdeDAO: HakukohdeDAO,
    organisaatioService: OrganisaatioService
) extends ValidatingService[Haku] {
  private def isYhteisHaku(haku: Haku): Boolean =
    haku.hakutapaKoodiUri.map(_.toString).getOrElse("").startsWith("hakutapa_01")
  private def isJatkuvaHaku(haku: Haku): Boolean =
    haku.hakutapaKoodiUri.map(_.toString).getOrElse("").startsWith("hakutapa_03")
  private def isJoustavaHaku(haku: Haku): Boolean =
    haku.hakutapaKoodiUri.map(_.toString).getOrElse("").startsWith("hakutapa_04")

  override def validateEntity(haku: Haku, oldHaku: Option[Haku]): IsValid = {
    val hakuDiffResolver = HakuDiffResolver(haku, oldHaku)
    val vCtx             = ValidationContext(haku.tila, haku.kielivalinta, if (oldHaku.isDefined) update else create)

    and(
      haku.validate(),
      validateIfTrueOrElse(
        vCtx.crudOperation == update,
        assertNotOptional(haku.oid, "oid"),
        assertNotDefined(haku.oid, "oid")
      ),
      assertNotOptional(haku.hakutapaKoodiUri, "hakutapaKoodiUri"),
      assertNotOptional(haku.kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri"),
      validateIfDefined[String](
        hakuDiffResolver.newHakutapaKoodiUri(),
        koodiUri =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(HakutapaKoodisto, _),
            "hakutapaKoodiUri",
            vCtx,
            invalidHakutapaKoodiUri(koodiUri)
          )
      ),
      validateIfDefined[String](
        hakuDiffResolver.newKohdejoukkoKoodiUri(),
        koodiUri =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, _),
            "kohdejoukkoKoodiUri",
            vCtx,
            invalidHaunKohdejoukkoKoodiUri(koodiUri)
          )
      ),
      validateIfDefined[String](
        hakuDiffResolver.newKohdejoukonTarkenneKoodiUri(),
        koodiUri =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukonTarkenneKoodisto, _),
            "kohdejoukonTarkenneKoodiUri",
            vCtx,
            invalidHaunKohdejoukonTarkenneKoodiUri(koodiUri)
          )
      ),
      validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validate(vCtx, _)),
      validateIfDefined[HakuMetadata](
        haku.metadata,
        validateMetadata(_, hakuDiffResolver, vCtx)
      ),
      validateIfTrue(
        haku.hakulomaketyyppi.contains(Ataru),
        validateIfDefined[UUID](
          hakuDiffResolver.newAtaruId(),
          ataruId =>
            assertAtaruQueryResult(
              ataruId,
              hakemusPalveluClient,
              "hakulomakeAtaruId"
            )
        )
      ),
      validateIfTrue(
        haku.hakulomaketyyppi.contains(Ataru),
        validateIfDefined[UUID](
          hakuDiffResolver.newAtaruId(),
          ataruId =>
            assertAtaruFormAllowsOnlyYhteisHakuResult(
              ataruId,
              haku.hakutapaKoodiUri,
              hakemusPalveluClient,
              "hakulomakeAtaruId",
              invalidAtaruFormAllowsOnlyYhteishaku(ataruId)
            )
        )
      ),
      validateIfJulkaistu(
        vCtx.tila,
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
            isYhteisHaku(haku),
            assertNotOptional(haku.metadata.flatMap(_.koulutuksenAlkamiskausi), "metadata.koulutuksenAlkamiskausi")
          ),
          validateHakukohteenLiittajaOrganisaatiot(haku)
        )
      )
    )
  }

  private def validateHakukohteenLiittajaOrganisaatiot(haku: Haku): IsValid =
    assertFalse(
      haku.hakukohteenLiittajaOrganisaatiot.forall(
        Seq(
          "1.2.246.562.10.81934895871",
          "1.2.246.562.10.67603619189",
          "1.2.246.562.10.66603619189",
          "1.2.246.562.10.39218317368",
          "1.2.246.562.10.000000000777"
        ).contains
      ),
      "hakukohteenLiittajaOrganisaatiot",
      invalidHakukohteenLiittajaOrganisaatio(
        "organisaatioService.getAllChildOidsFlat(haku.organisaatioOid): " + organisaatioService
          .getAllChildOidsFlat(haku.organisaatioOid)
          .toString() + "\nhaku.hakukohteenLiittajaOrganisaatiot: " + haku.hakukohteenLiittajaOrganisaatiot.toString()
      )
    )

  private def validateMetadata(
      m: HakuMetadata,
      hakuDiffResolver: HakuDiffResolver,
      vCtx: ValidationContext
  ): IsValid = {
    and(
      validateIfNonEmpty[Yhteyshenkilo](
        m.yhteyshenkilot,
        "metadata.yhteyshenkilot",
        _.validate(vCtx, _)
      ),
      validateIfNonEmpty[Ajanjakso](
        m.tulevaisuudenAikataulu,
        "metadata.tulevaisuudenAikataulu",
        _.validate(vCtx, _)
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        m.koulutuksenAlkamiskausi,
        _.validate(
          "metadata.koulutuksenAlkamiskausi",
          hakuDiffResolver.koulutuksenAlkamiskausiWithNewValues(),
          vCtx,
          koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, _)
        )
      )
    )
  }

  override def validateInternalDependenciesWhenDeletingEntity(haku: Haku): IsValid = assertTrue(
    hakukohdeDAO.listByHakuOid(haku.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Hakua", "hakukohteita")
  )

  override def validateEntityOnJulkaisu(haku: Haku): IsValid = and(
    validateIfTrueOrElse(
      isJatkuvaHaku(haku) || isJoustavaHaku(haku),
      validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validateOnJulkaisuForJatkuvaOrJoustavaHaku(_)),
      validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validateOnJulkaisu(_))
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
