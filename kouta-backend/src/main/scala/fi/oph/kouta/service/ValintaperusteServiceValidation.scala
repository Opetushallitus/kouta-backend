package fi.oph.kouta.service

import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidationContext, ValintaperusteDiffResolver}

object ValintaperusteServiceValidation
    extends ValintaperusteServiceValidation(HakuKoodiClient, HakukohdeDAO)

class ValintaperusteServiceValidation(
    hakuKoodiClient: HakuKoodiClient,
    hakukohdeDAO: HakukohdeDAO
) extends ValidatingService[Valintaperuste] {
  override def validateEntity(vp: Valintaperuste, oldVp: Option[Valintaperuste]): IsValid = {
    val vCtx                  = ValidationContext(vp.tila, vp.kielivalinta, if (oldVp.isDefined) update else create)
    val vpDiffResolver        = ValintaperusteDiffResolver(vp, oldVp)
    val existingValintakoeIds = oldVp.map(_.valintakokeet).getOrElse(Seq()).filter(_.id.isDefined).map(_.id.get)

    and(
      vp.validate(),
      validateIfTrueOrElse(
        vCtx.crudOperation == update,
        and(
          assertNotOptional(vp.id, "id"),
          assertTrue(
            vp.koulutustyyppi == oldVp.get.koulutustyyppi,
            "koulutustyyppi",
            notModifiableMsg("koulutustyyppiä", "valintaperusteelle")
          )
        ),
        assertNotDefined(vp.id, "id")
      ),
      validateIfDefined[String](
        vpDiffResolver.newHakutapaKoodiUri(),
        koodiUri =>
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.hakutapaKoodiUriExists,
              "hakutapaKoodiUri",
              vCtx,
              invalidHakutapaKoodiUri(koodiUri)
          )
      ),
      validateIfDefined[String](
        vpDiffResolver.newKohdejoukkoKoodiUri(),
        koodiUri =>
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.haunkohdejoukkoKoodiUriExists,
              "kohdejoukkoKoodiUri",
              vCtx,
              invalidHaunKohdejoukkoKoodiUri(koodiUri)
          )
      ),
      validateIfNonEmptySeq[Valintakoe](
        vp.valintakokeet,
        vpDiffResolver.newValintakokeet(),
        "valintakokeet",
        (valintakoe, newValintakoe, path) =>
          valintakoe.validate(
            path,
            newValintakoe,
            vCtx,
            existingValintakoeIds,
            hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
            hakuKoodiClient.postiosoitekoodiExists
          )
      ),
      validateIfDefined[ValintaperusteMetadata](vp.metadata, validateMetadata(vp.koulutustyyppi, _, vCtx, vpDiffResolver)),
      validateIfJulkaistu(
        vp.tila,
        and(
          assertNotOptional(vp.hakutapaKoodiUri, "hakutapaKoodiUri"),
          assertNotOptional(vp.kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri")
        )
      )
    )
  }

  private def validateMetadata(
      tyyppi: Koulutustyyppi,
      m: ValintaperusteMetadata,
      vCtx: ValidationContext,
      vpDiffResolver: ValintaperusteDiffResolver
  ): IsValid = and(
    assertTrue(m.tyyppi == tyyppi, s"metadata.tyyppi", InvalidMetadataTyyppi),
    validateIfNonEmptySeq[Valintatapa](
      m.valintatavat,
      vpDiffResolver.newValintatavat(),
      "metadata.valintatavat",
      (valintatapa, newValintatapa, path) =>
        valintatapa.validate(path, newValintatapa, vCtx, hakuKoodiClient.valintatapaKoodiUriExists)
    ),
    validateIfNonEmpty[Sisalto](m.sisalto, "metadata.sisalto", _.validate(vCtx, _)),
    validateIfJulkaistu(
      vCtx.tila,
      and(
        validateOptionalKielistetty(vCtx.kielivalinta, m.kuvaus, "metadata.kuvaus"),
        validateOptionalKielistetty(vCtx.kielivalinta, m.hakukelpoisuus, "metadata.hakukelpoisuus"),
        validateOptionalKielistetty(vCtx.kielivalinta, m.lisatiedot, "metadata.lisatiedot"),
        validateOptionalKielistetty(
          vCtx.kielivalinta,
          m.valintakokeidenYleiskuvaus,
          "metadata.valintakokeidenYleiskuvaus"
        )
      )
    )
  )

  override def validateEntityOnJulkaisu(vp: Valintaperuste): IsValid =
    validateIfNonEmpty[Valintakoe](vp.valintakokeet, "valintakokeet", _.validateOnJulkaisu(_))

  override def validateInternalDependenciesWhenDeletingEntity(vp: Valintaperuste): IsValid =
    assertTrue(
      hakukohdeDAO.listByValintaperusteId(vp.id.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
      "tila",
      integrityViolationMsg("Valintaperustetta", "hakukohteita")
    )

}
