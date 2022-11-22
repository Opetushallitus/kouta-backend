package fi.oph.kouta.service

import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain.{
  NimettyLinkki,
  OppilaitoksenOsa,
  OppilaitoksenOsaMetadata,
  Oppilaitos,
  OppilaitosMetadata,
  Osoite,
  TietoaOpiskelusta,
  Yhteystieto
}
import fi.oph.kouta.repository.OppilaitosDAO
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations.{
  and,
  assertKoodistoQueryResult,
  assertNotEmpty,
  assertNotNegative,
  assertNotOptional,
  assertValid,
  assertValidEmail,
  assertValidUrl,
  invalidTietoaOpiskelustaOtsikkoKoodiUri,
  validateDependency,
  validateIfDefined,
  validateIfJulkaistu,
  validateIfNonEmpty,
  validateIfNonEmptySeq,
  validateIfSuccessful,
  validateKielistetty,
  validateOptionalKielistetty
}
import fi.oph.kouta.validation.{IsValid, NoErrors, OppilaitosOrOsaDiffResolver, ValidationContext}

object OppilaitosServiceValidation extends OppilaitosServiceValidation(HakuKoodiClient)

class OppilaitosServiceValidation(hakuKoodiClient: HakuKoodiClient) extends ValidatingService[Oppilaitos] {
  override def validateEntity(ol: Oppilaitos, oldOl: Option[Oppilaitos]): IsValid = {
    val vCtx                                                            = ValidationContext(ol.tila, ol.kielivalinta, if (oldOl.isDefined) update else create)
    val oppilaitosDiffResolver: OppilaitosOrOsaDiffResolver[Oppilaitos] = OppilaitosOrOsaDiffResolver(ol, oldOl)

    and(
      assertValid(ol.oid, "oid"),
      assertValid(ol.organisaatioOid, "organisaatioOid"),
      validateIfDefined[OppilaitosMetadata](ol.metadata, validateMetadata(_, vCtx, oppilaitosDiffResolver)),
      validateIfDefined[String](ol.teemakuva, assertValidUrl(_, "teemakuva")),
      validateIfDefined[String](ol.logo, assertValidUrl(_, "logo")),
      assertNotEmpty(ol.kielivalinta, "kielivalinta")
    )
  }

  private def validateMetadata(
      m: OppilaitosMetadata,
      vCtx: ValidationContext,
      oppilaitosDiffResolver: OppilaitosOrOsaDiffResolver[Oppilaitos]
  ): IsValid = and(
    validateIfNonEmptySeq[TietoaOpiskelusta](
      m.tietoaOpiskelusta,
      oppilaitosDiffResolver.newTietoaOpiskelusta(),
      "metadata.tietoaOpiskelusta",
      (tietoa, newTietoa, path) => validateTietoaOpiskelusta(tietoa, newTietoa, path, vCtx)
    ),
    validateIfDefined[NimettyLinkki](m.wwwSivu, _.validate(vCtx, "metadata.wwwSivu")),
    validateIfDefined[Yhteystieto](
      m.hakijapalveluidenYhteystiedot,
      _.validate(
        "metadata.hakijapalveluidenYhteystiedot",
        oppilaitosDiffResolver.newHakijapalveluidenYhteystiedot(),
        vCtx,
        hakuKoodiClient.postiosoitekoodiExists
      )
    ),
    validateIfDefined[Int](m.opiskelijoita, assertNotNegative(_, "metadata.opiskelijoita")),
    validateIfDefined[Int](m.korkeakouluja, assertNotNegative(_, "metadata.korkeakouluja")),
    validateIfDefined[Int](m.tiedekuntia, assertNotNegative(_, "metadata.tiedekuntia")),
    validateIfDefined[Int](m.kampuksia, assertNotNegative(_, "metadata.kampuksia")),
    validateIfDefined[Int](m.yksikoita, assertNotNegative(_, "metadata.yksikoita")),
    validateIfDefined[Int](m.toimipisteita, assertNotNegative(_, "metadata.toimipisteita")),
    validateIfDefined[Int](m.akatemioita, assertNotNegative(_, "metadata.akatemioita")),
    validateIfJulkaistu(
      vCtx.tila,
      and(
        validateOptionalKielistetty(vCtx.kielivalinta, m.esittely, "metadata.esittely"),
        assertNotOptional(m.wwwSivu, "metadata.wwwSivu")
      )
    )
  )

  private def validateTietoaOpiskelusta(
      tietoaOpiskelusta: TietoaOpiskelusta,
      newTietoaOpiskelusta: Option[TietoaOpiskelusta],
      path: String,
      vCtx: ValidationContext
  ): IsValid = and(
    validateIfDefined[TietoaOpiskelusta](
      newTietoaOpiskelusta,
      newValues => {
        val koodiUri = newValues.otsikkoKoodiUri
        assertKoodistoQueryResult(
          koodiUri,
          hakuKoodiClient.tietoaOpiskelustaOtsikkoKoodiUriExists,
          s"$path.otsikkoKoodiUri",
          vCtx,
          invalidTietoaOpiskelustaOtsikkoKoodiUri(koodiUri)
        )
      }
    ),
    validateIfJulkaistu(vCtx.tila, validateKielistetty(vCtx.kielivalinta, tietoaOpiskelusta.teksti, s"$path.teksti"))
  )

  override def validateInternalDependenciesWhenDeletingEntity(e: Oppilaitos): IsValid = NoErrors
}

object OppilaitoksenOsaServiceValidation extends OppilaitoksenOsaServiceValidation(HakuKoodiClient, OppilaitosDAO)

class OppilaitoksenOsaServiceValidation(hakuKoodiClient: HakuKoodiClient, oppilaitosDAO: OppilaitosDAO)
    extends ValidatingService[OppilaitoksenOsa] {
  override def validateEntity(osa: OppilaitoksenOsa, oldOsa: Option[OppilaitoksenOsa]): IsValid = {
    val vCtx                                                        = ValidationContext(osa.tila, osa.kielivalinta, if (oldOsa.isDefined) update else create)
    val diffResolver: OppilaitosOrOsaDiffResolver[OppilaitoksenOsa] = OppilaitosOrOsaDiffResolver(osa, oldOsa)

    and(
      assertValid(osa.oid, "oid"),
      validateIfSuccessful(assertValid(osa.oppilaitosOid, "oppilaitosOid"), validateOppilaitosIntegrity(osa)),
      assertValid(osa.organisaatioOid, "organisaatioOid"),
      validateIfDefined[OppilaitoksenOsaMetadata](osa.metadata, validateMetadata(_, vCtx, diffResolver)),
      validateIfDefined[String](osa.teemakuva, assertValidUrl(_, "teemakuva")),
      assertNotEmpty(osa.kielivalinta, "kielivalinta")
    )
  }

  private def validateMetadata(
      m: OppilaitoksenOsaMetadata,
      vCtx: ValidationContext,
      diffResolver: OppilaitosOrOsaDiffResolver[OppilaitoksenOsa]
  ): IsValid = and(
    validateIfDefined[NimettyLinkki](m.wwwSivu, _.validate(vCtx, "metadata.wwwSivu")),
    validateIfDefined[Yhteystieto](
      m.hakijapalveluidenYhteystiedot,
      _.validate(
        "metadata.hakijapalveluidenYhteystiedot",
        diffResolver.newHakijapalveluidenYhteystiedot(),
        vCtx,
        hakuKoodiClient.postiosoitekoodiExists
      )
    ),
    validateIfDefined[Int](m.opiskelijoita, assertNotNegative(_, "metadata.opiskelijoita")),
    validateIfJulkaistu(
      vCtx.tila,
      and(
        validateOptionalKielistetty(vCtx.kielivalinta, m.kampus, "metadata.kampus"),
        validateOptionalKielistetty(vCtx.kielivalinta, m.esittely, "metadata.esittely"),
        assertNotOptional(m.wwwSivu, "metadata.wwwSivu")
      )
    )
  )

  private def validateOppilaitosIntegrity(oppilaitoksenOsa: OppilaitoksenOsa): IsValid = {
    validateDependency(
      oppilaitoksenOsa.tila,
      oppilaitosDAO.getTila(oppilaitoksenOsa.oppilaitosOid),
      oppilaitoksenOsa.oppilaitosOid,
      "Oppilaitosta",
      "oppilaitosOid"
    )
  }

  override def validateInternalDependenciesWhenDeletingEntity(e: OppilaitoksenOsa): IsValid = NoErrors
}
