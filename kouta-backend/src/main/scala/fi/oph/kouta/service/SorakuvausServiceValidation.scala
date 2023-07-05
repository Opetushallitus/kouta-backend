package fi.oph.kouta.service

import fi.oph.kouta.domain.{KoulutusKoodisto, KoulutusalaKoodisto, Sorakuvaus, SorakuvausMetadata, TilaFilter}
import fi.oph.kouta.repository.KoulutusDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations.{and, assertKoodistoQueryResult, assertNotDefined, assertNotOptional, assertTrue, integrityViolationMsg, invalidKoulutusAlaKoodiuri, invalidKoulutuskoodiuri, notModifiableMsg, validateIfDefined, validateIfJulkaistu, validateIfNonEmpty, validateIfTrueOrElse, validateKielistetty}
import fi.oph.kouta.validation.{IsValid, SorakuvausDiffResolver, ValidationContext}

object SorakuvausServiceValidation
    extends SorakuvausServiceValidation(KoodistoService, KoulutusDAO)
class SorakuvausServiceValidation(
    koodistoService: KoodistoService,
    koulutusDAO: KoulutusDAO
) extends ValidatingService[Sorakuvaus] {
  override def validateEntity(sk: Sorakuvaus, oldSk: Option[Sorakuvaus]): IsValid = {
    val sorakuvausDiffResolver = SorakuvausDiffResolver(sk, oldSk)
    val vCtx                   = ValidationContext(sk.tila, sk.kielivalinta, if (oldSk.isDefined) update else create)

    and(
      sk.validate(),
      validateIfTrueOrElse(
        vCtx.crudOperation == update,
        and(
          assertNotOptional(sk.id, "id"),
          assertTrue(
            sk.koulutustyyppi == oldSk.get.koulutustyyppi,
            "koulutustyyppi",
            notModifiableMsg("koulutustyyppiä", "sorakuvaukselle")
          )
        ),
        assertNotDefined(sk.id, "id")
      ),
      validateIfDefined[SorakuvausMetadata](
        sk.metadata,
        validateMetadata(_, sorakuvausDiffResolver, vCtx)
      ),
      validateIfJulkaistu(sk.tila, assertNotOptional(sk.metadata, "metadata"))
    )
  }

  private def validateMetadata(
      metadata: SorakuvausMetadata,
      sorakuvausDiffResolver: SorakuvausDiffResolver,
      vCtx: ValidationContext
  ): IsValid =
    and(
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateKielistetty(vCtx.kielivalinta, metadata.kuvaus, "metadata.kuvaus")
        )
      ),
      validateIfNonEmpty[String](
        sorakuvausDiffResolver.newKoulutusKoodiUrit(),
        "metadata.koulutusKoodiUrit",
        (koodiUri, path) =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(KoulutusKoodisto, _),
            path,
            vCtx,
            invalidKoulutuskoodiuri(koodiUri)
          )
      ),
      validateIfDefined[String](
        sorakuvausDiffResolver.newKoulutusalaKoodiUri(),
        koodiUri =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto, _),
            "metadata.koulutusalaKoodiUri",
            vCtx,
            invalidKoulutusAlaKoodiuri(koodiUri)
          )
      )
    )

  override def validateInternalDependenciesWhenDeletingEntity(sk: Sorakuvaus): IsValid = assertTrue(
    koulutusDAO.listBySorakuvausId(sk.id.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Sorakuvausta", "koulutuksia")
  )
}
