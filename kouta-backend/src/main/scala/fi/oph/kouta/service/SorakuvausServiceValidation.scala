package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausMetadata, TilaFilter}
import fi.oph.kouta.repository.KoulutusDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations.{
  KoulutusKoodiPattern,
  KoulutusalaKoodiPattern,
  and,
  assertKoodistoQueryResult,
  assertMatch,
  assertNotDefined,
  assertNotOptional,
  assertTrue,
  integrityViolationMsg,
  invalidKoulutusAlaKoodiuri,
  invalidKoulutuskoodiuri,
  notModifiableMsg,
  validateIfDefined,
  validateIfJulkaistu,
  validateIfNonEmpty,
  validateIfTrue,
  validateIfTrueOrElse,
  validateKielistetty
}
import fi.oph.kouta.validation.{IsValid, NoErrors, SorakuvausDiffResolver, ValidationContext}

object SorakuvausServiceValidation
    extends SorakuvausServiceValidation(OrganisaatioServiceImpl, KoulutusKoodiClient, KoulutusDAO)
class SorakuvausServiceValidation(
    val organisaatioService: OrganisaatioService,
    koulutusKoodiClient: KoulutusKoodiClient,
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
            koulutusKoodiClient.koulutusKoodiUriExists,
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
            koulutusKoodiClient.koulutusalaKoodiUriExists,
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
