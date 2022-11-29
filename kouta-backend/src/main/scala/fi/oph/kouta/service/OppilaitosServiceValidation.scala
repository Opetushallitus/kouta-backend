package fi.oph.kouta.service

import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain.{NimettyLinkki, OppilaitoksenOsa, OppilaitoksenOsaMetadata, Oppilaitos, OppilaitosMetadata, Osoite, TietoaOpiskelusta, Yhteystieto}
import fi.oph.kouta.repository.OppilaitosDAO
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations.{and, assertKoodistoQueryResult, assertNotEmpty, assertNotNegative, assertNotOptional, assertTrue, assertValid, assertValidEmail, assertValidUrl, invalidTietoaOpiskelustaOtsikkoKoodiUri, validateDependency, validateIfDefined, validateIfDefinedOrModified, validateIfJulkaistu, validateIfNonEmpty, validateIfNonEmptySeq, validateIfSuccessful, validateKielistetty, validateOptionalKielistetty}
import fi.oph.kouta.validation.{ErrorMessage, IsValid, NoErrors, OppilaitosOrOsaDiffResolver, ValidationContext}

object OppilaitosServiceValidation extends OppilaitosServiceValidation(HakuKoodiClient)

class OppilaitosServiceValidation(hakuKoodiClient: HakuKoodiClient) extends ValidatingService[Oppilaitos] {

  def withValidation[R](oppilaitos: Oppilaitos, oldOppilaitos: Option[Oppilaitos], authenticated: Authenticated)(
    f: Oppilaitos => R
  ): R = {
    val vCtx  = ValidationContext(oppilaitos.tila, oppilaitos.kielivalinta, if (oldOppilaitos.isDefined) update else create)
    val isOphPaakayttaja = authenticated.session.roles.contains(Role.Paakayttaja)
    val oppilaitosDiffResolver = OppilaitosOrOsaDiffResolver(oppilaitos, oldOppilaitos)
    var errors = super.validate(oppilaitos, oldOppilaitos)
    if (errors.isEmpty)
      errors = validateIfDefined[OppilaitosMetadata](
        oppilaitos.metadata,
        validateMetadata(_, isOphPaakayttaja, vCtx, oppilaitosDiffResolver))

    errors match {
      case NoErrors => f(oppilaitos)
      case errors => throw KoutaValidationException(errors)
    }
  }

  override def validateEntity(ol: Oppilaitos, oldOl: Option[Oppilaitos]): IsValid = {
    and(
      assertValid(ol.oid, "oid"),
      assertValid(ol.organisaatioOid, "organisaatioOid"),
      validateIfDefined[String](ol.teemakuva, assertValidUrl(_, "teemakuva")),
      validateIfDefined[String](ol.logo, assertValidUrl(_, "logo")),
      assertNotEmpty(ol.kielivalinta, "kielivalinta")
    )
  }

  private def validateMetadata(
      m: OppilaitosMetadata,
      isOphPaakayttaja: Boolean,
      vCtx: ValidationContext,
      oppilaitosDiffResolver: OppilaitosOrOsaDiffResolver[Oppilaitos]
  ): IsValid = and(
    validateIfNonEmptySeq[TietoaOpiskelusta](
      m.tietoaOpiskelusta,
      oppilaitosDiffResolver.newTietoaOpiskelusta(),
      "metadata.tietoaOpiskelusta",
      (tietoa, newTietoa, path) => validateTietoaOpiskelusta(tietoa, newTietoa, path, vCtx)
    ),
    assertTrue(
      !oppilaitosDiffResolver.jarjestaaUrheilijanAmmatillistakoulutustaChanged() || isOphPaakayttaja,
      "metadata.jarjestaaUrheilijanAmmKoulutusta",
      ErrorMessage(
        msg = "Vain OPH:n pääkäyttäjä voi muuttaa tiedon järjestääkö oppilaitos urheilijan ammatillista koulutusta",
        id = "invalidRightsForChangingJarjestaaUrheilijanAmmatillistaKoulutusta"
      )
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

  def withValidation[R](osa: OppilaitoksenOsa, oldOsa: Option[OppilaitoksenOsa], authenticated: Authenticated)(
    f: OppilaitoksenOsa => R
  ): R = {
    val vCtx = ValidationContext(osa.tila, osa.kielivalinta, if (oldOsa.isDefined) update else create)
    val isOphPaakayttaja = authenticated.session.roles.contains(Role.Paakayttaja)
    val oppilaitosDiffResolver = OppilaitosOrOsaDiffResolver(osa, oldOsa)
    var errors = super.validate(osa, oldOsa)
    if (errors.isEmpty)
      errors = validateIfDefined[OppilaitoksenOsaMetadata](
        osa.metadata,
        validateMetadata(_, isOphPaakayttaja, vCtx, oppilaitosDiffResolver))

    errors match {
      case NoErrors => f(osa)
      case errors => throw KoutaValidationException(errors)
    }
  }
  override def validateEntity(osa: OppilaitoksenOsa, oldOsa: Option[OppilaitoksenOsa]): IsValid = {
    and(
      assertValid(osa.oid, "oid"),
      validateIfSuccessful(assertValid(osa.oppilaitosOid, "oppilaitosOid"), validateOppilaitosIntegrity(osa)),
      assertValid(osa.organisaatioOid, "organisaatioOid"),
      validateIfDefined[String](osa.teemakuva, assertValidUrl(_, "teemakuva")),
      assertNotEmpty(osa.kielivalinta, "kielivalinta")
    )
  }

  private def validateMetadata(
      m: OppilaitoksenOsaMetadata,
      isOphPaakayttaja: Boolean,
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
    assertTrue(
      !diffResolver.jarjestaaUrheilijanAmmatillistakoulutustaChanged() || isOphPaakayttaja,
      "metadata.jarjestaaUrheilijanAmmKoulutusta",
      ErrorMessage(
        msg = "Vain OPH:n pääkäyttäjä voi muuttaa tiedon järjestääkö oppilaitoksen osa urheilijan ammatillista koulutusta",
        id = "invalidRightsForChangingJarjestaaUrheilijanAmmatillistaKoulutusta"
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
