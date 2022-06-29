package fi.oph.kouta.service

import fi.oph.kouta.domain.{AikuistenPerusopetusToteutusMetadata, AmmatillinenMuuToteutusMetadata, AmmatillinenOsaamisalaToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, Ataru, Hakukohde, Hakulomaketyyppi, Koulutustyyppi, LukioToteutusMetadata, TilaFilter, ToteutusMetadata, VapaaSivistystyoMuuToteutusMetadata}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, isToisenAsteenYhteishaku}
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{and, assertInFuture, assertNotOptional, assertTrue, cannotLinkToHakukohde, tyyppiMismatch, validateDependency, validateDependencyExistence, validateIfDefined, validateIfFalse, validateIfJulkaistu, validateIfTrue, validateIfTrueOrElse}

import java.time.{Instant, LocalDateTime}
import java.util.UUID

class HakukohdeServiceValidation(
    val organisaatioService: OrganisaatioService,
    hakukohdeDAO: HakukohdeDAO,
    hakuDAO: HakuDAO
) extends ValidatingService[Hakukohde] {

  def withValidation[R](hakukohde: Hakukohde, oldHakukohde: Option[Hakukohde], authenticated: Authenticated)(
      f: Hakukohde => R
  ): R = {
    var errors = super.validate(hakukohde, oldHakukohde)
    if (errors.isEmpty)
      errors = validateDependencyIntegrity(
        hakukohde,
        authenticated.session.roles.contains(Role.Paakayttaja),
        if (oldHakukohde.isDefined) update else create
      )

    errors match {
      case NoErrors => f(hakukohde)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  override def validateEntity(e: Hakukohde): IsValid = {
    e.validate()
  }

  override def validateInternalDependenciesWhenDeletingEntity(e: Hakukohde): IsValid = {
    NoErrors
  }

  private def validateDependencyIntegrity(
      hakukohde: Hakukohde,
      isOphPaakayttaja: Boolean,
      crudOperation: CrudOperation
  ) = {
    val deps = hakukohdeDAO.getDependencyInformation(hakukohde)
    val haku = hakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    val hakuOid        = hakukohde.hakuOid.s
    val toteutusOid    = hakukohde.toteutusOid.s
    val koulutustyyppi = deps.get(toteutusOid).flatMap(_._2)
    val koulutuksetKoodiUri = deps.get(toteutusOid) match {
      case None => Seq()
      case deps => deps.get._4
    }

    and(
      validateDependency(hakukohde.tila, deps.get(toteutusOid).map(_._1), toteutusOid, "Toteutusta", "toteutusOid"),
      validateDependencyExistence(deps.get(hakuOid).map(_._1), hakuOid, "Hakua", "hakuOid"),
      validateIfDefined[UUID](
        hakukohde.valintaperusteId,
        valintaperusteId =>
          and(
            validateDependency(
              hakukohde.tila,
              deps.get(valintaperusteId.toString).map(_._1),
              valintaperusteId,
              "Valintaperustetta",
              "valintaperusteId"
            ),
            validateIfDefined[Koulutustyyppi](
              deps.get(valintaperusteId.toString).flatMap(_._2),
              valintaperusteTyyppi =>
                validateIfDefined[Koulutustyyppi](
                  deps.get(toteutusOid).flatMap(_._2),
                  toteutusTyyppi =>
                    assertTrue(
                      toteutusTyyppi == valintaperusteTyyppi,
                      "valintaperusteId",
                      tyyppiMismatch("Toteutuksen", toteutusOid, "valintaperusteen", valintaperusteId)
                    )
                )
            )
          )
      ),
      validateIfFalse(isOphPaakayttaja,
        validateIfTrueOrElse(
          crudOperation == create,
          validateIfDefined[LocalDateTime](
            haku.flatMap(_.hakukohteenLiittamisenTakaraja),
            assertInFuture(_, "hakukohteenLiittamisenTakaraja")
          ),
          validateIfDefined[LocalDateTime](
            haku.flatMap(_.hakukohteenMuokkaamisenTakaraja),
            assertInFuture(_, "hakukohteenMuokkaamisenTakaraja")
          )
        )
      ),
      validateIfDefined[ToteutusMetadata](
        deps.get(toteutusOid).flatMap(_._3),
        metadata =>
          metadata match {
            case m: AmmatillinenTutkinnonOsaToteutusMetadata =>
              assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
            case m: AmmatillinenOsaamisalaToteutusMetadata =>
              assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
            case m: VapaaSivistystyoMuuToteutusMetadata  => assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
            case m: AmmatillinenMuuToteutusMetadata      => assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
            case m: AikuistenPerusopetusToteutusMetadata => assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
            case m: LukioToteutusMetadata =>
              validateIfFalse(isDIAlukiokoulutus(koulutuksetKoodiUri) || isEBlukiokoulutus(koulutuksetKoodiUri),
              assertNotOptional(hakukohde.metadata.get.hakukohteenLinja, "metadata.hakukohteenLinja"))
            case _ => NoErrors
          }
      ),
      validateIfJulkaistu(
        hakukohde.tila,
        validateIfTrue(
          isToisenAsteenYhteishaku(
            koulutustyyppi,
            haku.flatMap(_.hakutapaKoodiUri)
          ),
          assertNotOptional(hakukohde.valintaperusteId, "valintaperusteId")
        )
      )
    )
  }

  private def assertHakulomaketyyppiAtaru(hakulomaketyyppi: Option[Hakulomaketyyppi], toteutusOid: String): IsValid =
    assertTrue(
      hakulomaketyyppi.exists(_ == Ataru),
      "toteutusOid",
      cannotLinkToHakukohde(toteutusOid)
    )

  override def validateEntityOnJulkaisu(hakukohde: Hakukohde): IsValid = {
    hakukohde.validateOnJulkaisu()
  }
}
