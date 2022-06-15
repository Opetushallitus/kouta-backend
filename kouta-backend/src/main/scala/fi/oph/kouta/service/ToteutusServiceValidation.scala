package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain.{
  AmmatillinenToteutusMetadata,
  Kielivalikoima,
  Koulutustyyppi,
  Lk,
  LukioToteutusMetadata,
  Opetus,
  TilaFilter,
  Toteutus,
  ToteutusMetadata
}
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.validation.Validations.{
  and,
  assertTrue,
  integrityViolationMsg,
  invalidKieliKoodiUri,
  invalidLukioDiplomiKoodiUri,
  invalidLukioLinjaKoodiUri,
  invalidOpetusAikaKoodiUri,
  invalidOpetusKieliKoodiUri,
  invalidOpetusLisatietoOtsikkoKoodiuri,
  invalidOpetusTapaKoodiUri,
  invalidOsaamisalaKoodiUri,
  tyyppiMismatch,
  validateDependency,
  validateIfDefined,
  validateIfTrue,
  validateKielistetty
}
import fi.oph.kouta.validation.IsValid

object ToteutusServiceValidation
    extends ToteutusServiceValidation(
      KoulutusKoodiClient,
      OrganisaatioServiceImpl,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO
    )

class ToteutusServiceValidation(
    val koulutusKoodiClient: KoulutusKoodiClient,
    val organisaatioService: OrganisaatioService,
    koulutusDAO: KoulutusDAO,
    hakukohdeDAO: HakukohdeDAO,
    val sorakuvausDAO: SorakuvausDAO
) extends KoulutusToteutusValidatingService[Toteutus] {
  override def validateParameterFormatAndExistence(toteutus: Toteutus): IsValid = toteutus.validate()
  override def validateParameterFormatAndExistenceOnJulkaisu(toteutus: Toteutus): IsValid =
    toteutus.validateOnJulkaisu()

  override def validateDependenciesToExternalServices(toteutus: Toteutus): IsValid = {
    val commonErrors = and(
      validateTarjoajat(toteutus.tarjoajat),
      validateIfDefined[ToteutusMetadata](
        toteutus.metadata,
        metadata =>
          and(
            validateSorakuvausIntegrity(toteutus.sorakuvausId, toteutus.tila, metadata.tyyppi, "metadata.tyyppi"),
            validateIfDefined[Opetus](metadata.opetus, opetus => validateOpetus(opetus))
          )
      ),
      validateKoulutusIntegrity(toteutus)
    )

    val koulutustyyppiSpecificErrors = toteutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case ammMetadata: AmmatillinenToteutusMetadata =>
            assertTrue(
              koulutusKoodiClient.osaamisalaKoodiUriExist(ammMetadata.osaamisalat.map(_.koodiUri)),
              "metadata.osaamisalat.koodiUri",
              invalidOsaamisalaKoodiUri
            )
          case lkMetadata: LukioToteutusMetadata => validateLukioMetadata(lkMetadata)
        }
    }

    Seq(commonErrors, koulutustyyppiSpecificErrors).flatten
  }

  private def validateOpetus(opetus: Opetus): IsValid =
    and(
      assertTrue(
        koulutusKoodiClient.opetusKieliKoodiUritExist(opetus.opetuskieliKoodiUrit),
        "metadata.opetus.opetuskieliKoodiUrit",
        invalidOpetusKieliKoodiUri
      ),
      assertTrue(
        koulutusKoodiClient.opetusAikaKoodiUritExist(opetus.opetusaikaKoodiUrit),
        "metadata.opetus.opetusaikaKoodiUrit",
        invalidOpetusAikaKoodiUri
      ),
      assertTrue(
        koulutusKoodiClient.opetusTapaKoodiUritExist(opetus.opetustapaKoodiUrit),
        "metadata.opetus.opetustapaKoodiUrit",
        invalidOpetusTapaKoodiUri
      ),
      assertTrue(
        koulutusKoodiClient.lisatiedotOtsikkoKoodiUritExist(opetus.lisatiedot.map(_.otsikkoKoodiUri)),
        "metadata.opetus.lisatiedot.otsikkoKoodiUri",
        invalidOpetusLisatietoOtsikkoKoodiuri
      )
    )

  private def validateLukioMetadata(lkMetadata: LukioToteutusMetadata): IsValid =
    and(
      assertTrue(
        koulutusKoodiClient.lukioPainotusKoodiUritExist(lkMetadata.painotukset.map(_.koodiUri)),
        "metadata.painotukset.koodiUri",
        invalidLukioLinjaKoodiUri("painotukset")
      ),
      assertTrue(
        koulutusKoodiClient.lukioErityinenKoulutustehtavaKoodiUritExist(
          lkMetadata.erityisetKoulutustehtavat.map(_.koodiUri)
        ),
        "metadata.erityisetKoulutustehtavat.koodiUri",
        invalidLukioLinjaKoodiUri("erityisetKoulutustehtavat")
      ),
      assertTrue(
        koulutusKoodiClient.lukioDiplomiKoodiUritExist(lkMetadata.diplomit.map(_.koodiUri)),
        "metadata.diplomit.koodiUri",
        invalidLukioDiplomiKoodiUri
      ),
      validateIfDefined[Kielivalikoima](
        lkMetadata.kielivalikoima,
        kielivalikoima =>
          and(
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.A1Kielet),
              "metadata.kielivalikoima.A1Kielet",
              invalidKieliKoodiUri("A1Kielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.A2Kielet),
              "metadata.kielivalikoima.A2Kielet",
              invalidKieliKoodiUri("A2Kielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.B1Kielet),
              "metadata.kielivalikoima.B1Kielet",
              invalidKieliKoodiUri("B1Kielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.B2Kielet),
              "metadata.kielivalikoima.B2Kielet",
              invalidKieliKoodiUri("B2Kielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.B3Kielet),
              "metadata.kielivalikoima.B3Kielet",
              invalidKieliKoodiUri("B3Kielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.aidinkielet),
              "metadata.kielivalikoima.aidinkielet",
              invalidKieliKoodiUri("aidinkielet")
            ),
            assertTrue(
              koulutusKoodiClient.kieliKoodiUritExist(kielivalikoima.muutKielet),
              "metadata.kielivalikoima.muutKielet",
              invalidKieliKoodiUri("muutKielet")
            )
          )
      )
    )

  private def validateKoulutusIntegrity(toteutus: Toteutus): IsValid = {
    val (koulutusTila, koulutusTyyppi) = koulutusDAO.getTilaAndTyyppi(toteutus.koulutusOid)
    and(
      validateDependency(toteutus.tila, koulutusTila, toteutus.koulutusOid, "Koulutusta", "koulutusOid"),
      validateIfDefined[Koulutustyyppi](
        koulutusTyyppi,
        koulutusTyyppi =>
          and(
            validateIfTrue(koulutusTyyppi != Lk, validateKielistetty(toteutus.kielivalinta, toteutus.nimi, "nimi")),
            validateIfDefined[ToteutusMetadata](
              toteutus.metadata,
              toteutusMetadata =>
                assertTrue(
                  koulutusTyyppi == toteutusMetadata.tyyppi,
                  "metadata.tyyppi",
                  tyyppiMismatch("koulutuksen", toteutus.koulutusOid)
                )
            )
          )
      )
    )
  }

  override def validateInternalDependenciesWhenDeletingEntity(toteutus: Toteutus): IsValid = assertTrue(
    hakukohdeDAO.listByToteutusOid(toteutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Toteutusta", "hakukohteita")
  )
}
