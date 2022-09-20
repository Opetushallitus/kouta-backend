package fi.oph.kouta.service

import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{HakuDiffResolver, IsValid, NoErrors, ValidationContext}
import fi.vm.sade.utils.slf4j.Logging

import java.util.UUID

object HakuServiceValidation
    extends HakuServiceValidation(OrganisaatioServiceImpl, HakuKoodiClient, HakemusPalveluClient, HakukohdeDAO)
class HakuServiceValidation(
    val organisaatioService: OrganisaatioService,
    hakuKoodiClient: HakuKoodiClient,
    hakemusPalveluClient: HakemusPalveluClient,
    hakukohdeDAO: HakukohdeDAO
) extends ValidatingService[Haku]
    with Logging {
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
          validateIfSuccessful(
            assertMatch(koodiUri, HakutapaKoodiPattern, "hakutapaKoodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.hakutapaKoodiUriExists,
              "hakutapaKoodiUri",
              vCtx,
              invalidHakutapaKoodiUri(koodiUri)
            )
          )
      ),
      validateIfDefined[String](
        hakuDiffResolver.newKohdejoukkoKoodiUri(),
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, KohdejoukkoKoodiPattern, "kohdejoukkoKoodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.haunkohdejoukkoKoodiUriExists,
              "kohdejoukkoKoodiUri",
              vCtx,
              invalidHaunKohdejoukkoKoodiUri(koodiUri)
            )
          )
      ),
      validateIfDefined[String](
        hakuDiffResolver.newKohdejoukonTarkenneKoodiUri(),
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, KohdejoukonTarkenneKoodiPattern, "kohdejoukonTarkenneKoodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.haunkohdejoukonTarkenneKoodiUriExists,
              "kohdejoukonTarkenneKoodiUri",
              vCtx,
              invalidHaunKohdejoukonTarkenneKoodiUri(koodiUri)
            )
          )
      ),
      validateIfNonEmpty[Ajanjakso](haku.hakuajat, "hakuajat", _.validate(vCtx.tila, vCtx.kielivalinta, _)),
      validateIfDefined[HakuMetadata](
        haku.metadata,
        validateMetadata(_, hakuDiffResolver, vCtx)
      ), {
        val ataruIdValid = validateIfTrue(
          haku.hakulomaketyyppi.contains(Ataru),
          validateIfDefined[UUID](
            haku.hakulomakeAtaruId,
            ataruId =>
              assertAtaruQueryResult(
                ataruId,
                hakemusPalveluClient.isExistingAtaruId,
                "hakulomakeAtaruId",
                unknownAtaruId(ataruId)
              )
          )
        )
        logger.info("Would evaluate ataruId validation as " + ataruIdValid)
        NoErrors
      },
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
          )
        )
      )
    )
  }

  private def validateMetadata(
      m: HakuMetadata,
      hakuDiffResolver: HakuDiffResolver,
      vCtx: ValidationContext
  ): IsValid = {
    and(
      validateIfNonEmpty[Yhteyshenkilo](
        m.yhteyshenkilot,
        "metadata.yhteyshenkilot",
        _.validate(vCtx.tila, vCtx.kielivalinta, _)
      ),
      validateIfNonEmpty[Ajanjakso](
        m.tulevaisuudenAikataulu,
        "metadata.tulevaisuudenAikataulu",
        _.validate(vCtx.tila, vCtx.kielivalinta, _)
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        m.koulutuksenAlkamiskausi,
        _.validate(
          "metadata.koulutuksenAlkamiskausi",
          hakuDiffResolver.koulutuksenAlkamiskausiWithNewValues(),
          vCtx,
          hakuKoodiClient.kausiKoodiUriExists
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
