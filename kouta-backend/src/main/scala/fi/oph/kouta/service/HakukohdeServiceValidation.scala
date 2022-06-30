package fi.oph.kouta.service

import fi.oph.kouta.domain.{
  AikuistenPerusopetusToteutusMetadata,
  Ajanjakso,
  AmmatillinenMuuToteutusMetadata,
  AmmatillinenOsaamisalaToteutusMetadata,
  AmmatillinenTutkinnonOsaToteutusMetadata,
  Ataru,
  Hakukohde,
  HakukohdeMetadata,
  Hakulomaketyyppi,
  Julkaisutila,
  Kieli,
  Koulutustyyppi,
  Liite,
  LiitteenToimitusosoite,
  LukioToteutusMetadata,
  MuuOsoite,
  TilaFilter,
  ToteutusMetadata,
  Valintakoe,
  VapaaSivistystyoMuuToteutusMetadata
}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, isToisenAsteenYhteishaku}
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{
  HakukohdeKoodiPattern,
  PohjakoulutusvaatimusKoodiPattern,
  and,
  assertInFuture,
  assertMatch,
  assertNotEmpty,
  assertNotOptional,
  assertTrue,
  assertValid,
  cannotLinkToHakukohde,
  oneNotBoth,
  tyyppiMismatch,
  validateDependency,
  validateDependencyExistence,
  validateHakulomake,
  validateIfDefined,
  validateIfFalse,
  validateIfJulkaistu,
  validateIfNonEmpty,
  validateIfTrue,
  validateIfTrueOrElse,
  validateKielistetty,
  validateOptionalKielistetty
}

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

  override def validateEntity(hk: Hakukohde): IsValid = {
    val tila         = hk.tila
    val kielivalinta = hk.kielivalinta

    and(
      hk.validate(),
      assertValid(hk.toteutusOid, "toteutusOid"),
      assertValid(hk.hakuOid, "hakuOid"),
      assertValid(hk.organisaatioOid, "organisaatioOid"),
      // Joko hakukohdeKoodiUri tai nimi täytyy olla, mutta ei molempia!
      assertTrue(hk.hakukohdeKoodiUri.nonEmpty != hk.nimi.nonEmpty, "nimi", oneNotBoth("nimi", "hakukohdeKoodiUri")),
      validateIfDefined[String](
        hk.hakukohdeKoodiUri,
        assertMatch(_, HakukohdeKoodiPattern, "hakukohdeKoodiUri")
      ),
      validateIfTrue(
        hk.nimi.nonEmpty,
        validateKielistetty(kielivalinta, hk.nimi, "nimi")
      ),
      validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validate(tila, kielivalinta, _)),
      validateIfNonEmpty[String](
        hk.pohjakoulutusvaatimusKoodiUrit,
        "pohjakoulutusvaatimusKoodiUrit",
        assertMatch(_, PohjakoulutusvaatimusKoodiPattern, _)
      ),
      validateIfDefined[LiitteenToimitusosoite](
        hk.liitteidenToimitusosoite,
        _.validate(tila, kielivalinta, "liitteidenToimitusosoite")
      ),
      validateIfNonEmpty[Liite](hk.liitteet, "liitteet", _.validate(tila, kielivalinta, _)),
      validateIfNonEmpty[Valintakoe](hk.valintakokeet, "valintakokeet", _.validate(tila, kielivalinta, _)),
      validateIfDefined[HakukohdeMetadata](hk.metadata, validateMetadata(_, tila, kielivalinta)),
      validateIfJulkaistu(
        tila,
        and(
          assertNotOptional(hk.jarjestyspaikkaOid, "jarjestyspaikkaOid"),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusaika.contains(true),
            assertNotOptional(hk.liitteidenToimitusaika, "liitteidenToimitusaika")
          ),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusosoite.contains(true),
            assertNotOptional(hk.liitteidenToimitustapa, "liitteidenToimitustapa")
          ),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusosoite.contains(true) && hk.liitteidenToimitustapa.contains(MuuOsoite),
            assertNotOptional(hk.liitteidenToimitusosoite, "liitteidenToimitusosoite")
          ),
          validateHakulomake(
            hk.hakulomaketyyppi,
            hk.hakulomakeAtaruId,
            hk.hakulomakeKuvaus,
            hk.hakulomakeLinkki,
            kielivalinta
          ),
          assertNotEmpty(hk.pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit"),
          validateOptionalKielistetty(kielivalinta, hk.pohjakoulutusvaatimusTarkenne, "pohjakoulutusvaatimusTarkenne"),
          validateOptionalKielistetty(kielivalinta, hk.muuPohjakoulutusvaatimus, "muuPohjakoulutusvaatimus"),
          assertNotOptional(hk.kaytetaanHaunAikataulua, "kaytetaanHaunAikataulua"),
          assertNotOptional(hk.kaytetaanHaunHakulomaketta, "kaytetaanHaunHakulomaketta"),
          validateIfTrue(hk.kaytetaanHaunAikataulua.contains(false), assertNotEmpty(hk.hakuajat, "hakuajat")),
          validateIfTrue(
            hk.kaytetaanHaunHakulomaketta.contains(false),
            assertNotOptional(hk.hakulomaketyyppi, "hakulomaketyyppi")
          )
        )
      )
    )
  }

  private def validateMetadata(metadata: HakukohdeMetadata, tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid =
    metadata.validate(tila, kielivalinta, "metadata")

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
      validateIfFalse(
        isOphPaakayttaja,
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
