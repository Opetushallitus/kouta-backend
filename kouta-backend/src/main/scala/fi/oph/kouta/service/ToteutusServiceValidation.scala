package fi.oph.kouta.service

import fi.oph.kouta.domain.{Koulutustyyppi, Lk, TilaFilter, Toteutus, ToteutusMetadata}
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.validation.Validations.{and, assertTrue, integrityViolationMsg, tyyppiMismatch, validateDependency, validateIfDefined, validateIfTrue, validateKielistetty}
import fi.oph.kouta.validation.{IsValid, NoErrors, Validations}

object ToteutusServiceValidation extends ToteutusServiceValidation(OrganisaatioServiceImpl, KoulutusDAO, HakukohdeDAO, SorakuvausDAO)

class ToteutusServiceValidation(
   protected val organisaatioService: OrganisaatioService,
   koulutusDAO: KoulutusDAO,
   hakukohdeDAO: HakukohdeDAO,
   protected val sorakuvausDAO: SorakuvausDAO
) extends ValidatingService[Toteutus] {
  override def validateParameterFormatAndExistence(toteutus: Toteutus): IsValid = toteutus.validate()
  override def validateParameterFormatAndExistenceOnJulkaisu(toteutus: Toteutus): IsValid = toteutus.validateOnJulkaisu()

  override def validateDependenciesToExternalServices(toteutus: Toteutus): IsValid = {
    val commonErrors = and(
      validateTarjoajat(toteutus.tarjoajat),
      validateIfDefined[ToteutusMetadata](toteutus.metadata, metadata =>
        validateSorakuvausIntegrity(toteutus.sorakuvausId, toteutus.tila, metadata.tyyppi, "metadata.tyyppi"))

    )

    Seq(commonErrors).flatten
  }

  private def validateKoulutusIntegrity(toteutus: Toteutus): Unit = {
    val (koulutusTila, koulutusTyyppi) = koulutusDAO.getTilaAndTyyppi(toteutus.koulutusOid)

    and(
      validateDependency(toteutus.tila, koulutusTila, toteutus.koulutusOid, "Koulutusta", "koulutusOid"),
      validateIfDefined[Koulutustyyppi](koulutusTyyppi, koulutusTyyppi => and(
        validateIfTrue(koulutusTyyppi != Lk, validateKielistetty(toteutus.kielivalinta, toteutus.nimi, "nimi")),
        validateIfDefined[ToteutusMetadata](toteutus.metadata, toteutusMetadata =>
          assertTrue(koulutusTyyppi == toteutusMetadata.tyyppi, "metadata.tyyppi", tyyppiMismatch("koulutuksen", toteutus.koulutusOid))
        ))
      )
    )
  }
  override def validateInternalDependenciesWhenDeletingEntity(toteutus: Toteutus): IsValid = assertTrue(
    hakukohdeDAO.listByToteutusOid(toteutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Toteutusta", "hakukohteita"))
}
