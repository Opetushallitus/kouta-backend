package fi.oph.kouta.service

import fi.oph.kouta.domain.{AmmOsaamisala, AmmTutkinnonOsa, AmmatillinenOsaamisalaToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, Ataru, Haku, Hakukohde, Julkaisutila, Koulutustyyppi, Lk, ToteutusMetadata, VapaaSivistystyoMuu, VapaaSivistystyoMuuToteutusMetadata}
import fi.oph.kouta.util.MiscUtils.isToisenAsteenYhteishaku
import fi.oph.kouta.validation.Validations.{and, assertDependencyExists, assertInFuture, assertNotOptional, assertTrue, cannotLinkToHakukohde, tyyppiMismatch, validateDependency, validateIfDefined, validateIfJulkaistu, validateIfTrue}

import java.time.{Instant, LocalDateTime}
import java.util.UUID

object HakukohdeServiceValidation {

  def validate(hakukohde: Hakukohde, haku: Option[Haku], isOphPaakayttaja: Boolean, deps: Map[String, (Julkaisutila, Option[Koulutustyyppi], Option[ToteutusMetadata])], method: String) = {
    val hakuOid = hakukohde.hakuOid.s
    val toteutusOid = hakukohde.toteutusOid.s

    val koulutustyyppi = deps.get(toteutusOid).flatMap(_._2)

    and(
      validateDependency(hakukohde.tila, deps.get(toteutusOid).map(_._1), toteutusOid, "Toteutusta", "toteutusOid"),
      assertDependencyExists(deps.contains(hakuOid), hakuOid, "Hakua", "hakuOid"),
      validateIfDefined[UUID](hakukohde.valintaperusteId, valintaperusteId => and(
        validateDependency(hakukohde.tila, deps.get(valintaperusteId.toString).map(_._1), valintaperusteId, "Valintaperustetta", "valintaperusteId"),
        validateIfDefined[Koulutustyyppi](deps.get(valintaperusteId.toString).flatMap(_._2), valintaperusteTyyppi =>
          validateIfDefined[Koulutustyyppi](deps.get(toteutusOid).flatMap(_._2), toteutusTyyppi =>
            assertTrue(toteutusTyyppi == valintaperusteTyyppi, "valintaperusteId", tyyppiMismatch("Toteutuksen", toteutusOid, "valintaperusteen", valintaperusteId))
          )
        )
      )),
      validateIfTrue(
        !isOphPaakayttaja && method == "put",
        validateIfDefined[LocalDateTime](haku.flatMap(_.hakukohteenLiittamisenTakaraja), assertInFuture(_, "hakukohteenLiittamisenTakaraja")),
      ),
      validateIfTrue(
        !isOphPaakayttaja && method == "update",
        validateIfDefined[LocalDateTime](haku.flatMap(_.hakukohteenMuokkaamisenTakaraja), assertInFuture(_, "hakukohteenMuokkaamisenTakaraja"))
      ),
      validateIfDefined[ToteutusMetadata](deps.get(toteutusOid).flatMap(_._3), metadata => and(
        validateIfTrue(metadata.tyyppi == AmmTutkinnonOsa,
          assertTrue(metadata.asInstanceOf[AmmatillinenTutkinnonOsaToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == AmmOsaamisala,
          assertTrue(metadata.asInstanceOf[AmmatillinenOsaamisalaToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == VapaaSivistystyoMuu,
          assertTrue(metadata.asInstanceOf[VapaaSivistystyoMuuToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == Lk,
          assertNotOptional(hakukohde.metadata.get.hakukohteenLinja, "metadata.hakukohteenLinja"))
      )),
      validateIfJulkaistu(
        hakukohde.tila,
        validateIfTrue(isToisenAsteenYhteishaku(
          koulutustyyppi, haku.flatMap(_.hakutapaKoodiUri)
        ), assertNotOptional(hakukohde.valintaperusteId, "valintaperusteId")))
    )
  }
}
