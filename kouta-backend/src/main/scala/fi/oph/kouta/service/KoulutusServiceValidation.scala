package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.domain.{Amm, AmmOsaamisala, AmmTutkinnonOsa, Julkaisutila, Koulutus, Koulutustyyppi, Poistettu, TilaFilter}
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{and, assertTrue, integrityViolationMsg, tyyppiMismatch, validateDependency, validateIfDefined, validateIfTrue, valuesDontMatch}

import java.util.UUID

object KoulutusServiceValidation extends KoulutusServiceValidation(KoulutusKoodiClient)

class KoulutusServiceValidation(koulutusKoodiClient: KoulutusKoodiClient) extends ValidatingService[Koulutus] {
  override def withValidation[R](
      koulutus: Koulutus,
      oldKoulutus: Option[Koulutus]
  )(f: Koulutus => R): R = {
    super.withValidation(koulutus, oldKoulutus) { validatedKoulutus =>
      {
        validateSorakuvausIntegrity(validatedKoulutus)
        if (oldKoulutus.isDefined)
          validateToteutusIntegrityIfDeletingKoulutus(
            oldKoulutus.get.tila,
            validatedKoulutus.tila,
            validatedKoulutus.oid.get
          )

        //throwValidationErrors(validateKoodiuris(validatedKoulutus))
        f(validatedKoulutus)
      }
    }
  }

  private def validateSorakuvausIntegrity(
      koulutus: Koulutus
  ): Unit =
    throwValidationErrors(
      validateIfDefined[UUID](
        koulutus.sorakuvausId,
        sorakuvausId => {
          val (sorakuvausTila, sorakuvausTyyppi, koulutuskoodiUrit) =
            SorakuvausDAO.getTilaTyyppiAndKoulutusKoodit(sorakuvausId)
          and(
            validateDependency(koulutus.tila, sorakuvausTila, sorakuvausId, "Sorakuvausta", "sorakuvausId"),
            validateIfDefined[Koulutustyyppi](
              sorakuvausTyyppi,
              sorakuvausTyyppi =>
                // "Tutkinnon osa" ja Osaamisala koulutuksiin saa liittää myös SORA-kuvauksen, jonka koulutustyyppi on "ammatillinen"
                assertTrue(
                  sorakuvausTyyppi == koulutus.koulutustyyppi || (sorakuvausTyyppi == Amm && Seq(
                    AmmOsaamisala,
                    AmmTutkinnonOsa
                  ).contains(koulutus.koulutustyyppi)),
                  "koulutustyyppi",
                  tyyppiMismatch("sorakuvauksen", sorakuvausId)
                )
            ),
            validateIfDefined[Seq[String]](
              koulutuskoodiUrit,
              koulutuskoodiUrit => {
                validateIfTrue(
                  koulutuskoodiUrit.nonEmpty,
                  assertTrue(
                    koulutuskoodiUrit.intersect(koulutus.koulutuksetKoodiUri).nonEmpty,
                    "koulutuksetKoodiUri",
                    valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
                  )
                )
              }
            )
          )
        }
      )
    )

  private def validateToteutusIntegrityIfDeletingKoulutus(
      aiempiTila: Julkaisutila,
      tulevaTila: Julkaisutila,
      koulutusOid: KoulutusOid
  ) = {
    KoulutusServiceValidation.throwValidationErrors(
      validateIfTrue(
        tulevaTila == Poistettu && tulevaTila != aiempiTila,
        assertTrue(
          ToteutusDAO.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat()).isEmpty,
          "tila",
          integrityViolationMsg("Koulutusta", "toteutuksia")
        )
      )
    )
  }

  private def validateKoodiuris(koulutus: Koulutus): IsValid = {
    koulutus.koulutustyyppi match {
      case Amm =>
    }
    NoErrors
  }
}
