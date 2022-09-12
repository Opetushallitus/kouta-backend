package fi.oph.kouta.service

import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, isToisenAsteenYhteishaku}
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._

import java.time.LocalDateTime
import java.util.UUID

object HakukohdeServiceValidation
    extends HakukohdeServiceValidation(
      OrganisaatioServiceImpl,
      HakuKoodiClient,
      KoulutusKoodiClient,
      HakemusPalveluClient,
      HakukohdeDAO,
      HakuDAO
    )

class HakukohdeServiceValidation(
    val organisaatioService: OrganisaatioService,
    hakuKoodiClient: HakuKoodiClient,
    koulutusKoodiClient: KoulutusKoodiClient,
    hakemusPalveluClient: HakemusPalveluClient,
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
        if (oldHakukohde.isDefined) update else create,
        HakukohdeDiffResolver(hakukohde, oldHakukohde)
      )

    errors match {
      case NoErrors => f(hakukohde)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  override def validateEntity(hk: Hakukohde, oldHk: Option[Hakukohde]): IsValid = {
    val vCtx                  = ValidationContext(hk.tila, hk.kielivalinta, if (oldHk.isDefined) update else create)
    val hakukohdeDiffResolver = HakukohdeDiffResolver(hk, oldHk)
    val existingValintakoeIds = oldHk.map(_.valintakokeet).getOrElse(Seq()).filter(_.id.isDefined).map(_.id.get)
    val existingLiiteIds      = oldHk.map(_.liitteet).getOrElse(Seq()).filter(_.id.isDefined).map(_.id.get)

    and(
      hk.validate(),
      validateIfTrueOrElse(
        vCtx.crudOperation == update,
        assertNotOptional(hk.oid, "oid"),
        assertNotDefined(hk.oid, "oid")
      ),
      assertValid(hk.toteutusOid, "toteutusOid"),
      assertValid(hk.hakuOid, "hakuOid"),
      // Joko hakukohdeKoodiUri tai nimi täytyy olla, mutta ei molempia!
      assertTrue(hk.hakukohdeKoodiUri.nonEmpty != hk.nimi.nonEmpty, "nimi", oneNotBoth("nimi", "hakukohdeKoodiUri")),
      validateIfDefined[String](
        hakukohdeDiffResolver.newHakukohdeKoodiUri(),
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, HakukohdeKoodiPattern, "hakukohdeKoodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.hakukohdeKoodiUriExists,
              "hakukohdeKoodiUri",
              vCtx,
              invalidHakukohdeKooriuri(koodiUri)
            )
          )
      ),
      validateIfTrue(
        hk.nimi.nonEmpty,
        validateKielistetty(vCtx.kielivalinta, hk.nimi, "nimi")
      ),
      assertFalse(
        hk.kaytetaanHaunAikataulua.getOrElse(false) && hk.hakuajat.nonEmpty,
        "hakuajat",
        noneOrOneNotBoth("kaytetaanHaunAikataulua", "hakuajat")
      ),
      validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validate(vCtx.tila, vCtx.kielivalinta, _)),
      validateIfNonEmpty[String](
        hakukohdeDiffResolver.newPohjakoulutusvaatimusKoodiUrit(),
        "pohjakoulutusvaatimusKoodiUrit",
        (koodiUri, path) =>
          validateIfSuccessful(
            assertMatch(koodiUri, PohjakoulutusvaatimusKoodiPattern, path),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.pohjakoulutusVaatimusKoodiUriExists,
              path,
              vCtx,
              invalidPohjakoulutusVaatimusKooriuri(koodiUri)
            )
          )
      ),
      assertFalse(
        !hk.liitteetOnkoSamaToimitusaika.getOrElse(false) && hk.liitteidenToimitusaika.nonEmpty,
        "liitteidenToimitusaika",
        notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusaika")
      ),
      assertFalse(
        !hk.liitteetOnkoSamaToimitusosoite.getOrElse(false) && hk.liitteidenToimitustapa.nonEmpty,
        "liitteidenToimitustapa",
        notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusosoite")
      ),
      assertFalse(
        !hk.liitteetOnkoSamaToimitusosoite.getOrElse(false) && hk.liitteidenToimitusosoite.nonEmpty,
        "liitteidenToimitusosoite",
        notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusosoite")
      ),
      assertFalse(
        hk.liitteetOnkoSamaToimitusosoite.getOrElse(false) && !hk.liitteidenToimitustapa.contains(
          MuuOsoite
        ) && hk.liitteidenToimitusosoite.nonEmpty,
        "liitteidenToimitusosoite",
        notAllowedDueTo("liitteiden toimitustavaksi ei ole valittu 'muu osoite'")
      ),
      validateIfDefined[LiitteenToimitusosoite](
        hk.liitteidenToimitusosoite,
        validateLiitteenToimitusosoite(
          _,
          hakukohdeDiffResolver.liitteidenToimitusosoiteWithNewValues(),
          "liitteidenToimitusosoite",
          vCtx
        )
      ),
      validateIfNonEmptySeq[Liite](
        hk.liitteet,
        hakukohdeDiffResolver.newLiitteet(),
        "liitteet",
        (liite, newLiite, path) =>
          validateLiite(
            liite,
            newLiite,
            path,
            vCtx,
            existingLiiteIds,
            hakukohdeDiffResolver
          )
      ),
      validateIfNonEmptySeq[Valintakoe](
        hk.valintakokeet,
        hakukohdeDiffResolver.newValintakokeet(),
        "valintakokeet",
        (valintakoe, newValintakoe, path) =>
          valintakoe.validate(
            path,
            newValintakoe,
            vCtx,
            existingValintakoeIds,
            hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
            hakuKoodiClient.postiosoitekoodiExists
          )
      ),
      validateIfDefined[HakukohdeMetadata](
        hk.metadata,
        validateMetadata(_, vCtx, hakukohdeDiffResolver)
      ),
      assertFalse(
        hk.kaytetaanHaunHakulomaketta.getOrElse(false) && hk.hakulomaketyyppi.isDefined,
        "hakulomaketyyppi",
        noneOrOneNotBoth("kaytetaanHaunHakulomaketta", "hakulomaketyyppi")
      ),
      validateIfTrue(
        hk.hakulomaketyyppi.contains(Ataru),
        validateIfDefined[UUID](
          hakukohdeDiffResolver.newAtaruId(),
          ataruId =>
            assertAtaruQueryResult(
              ataruId,
              hakemusPalveluClient.isExistingAtaruIdFromCache,
              "hakulomakeAtaruId",
              unknownAtaruId(ataruId)
            )
        )
      ),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          assertNotOptional(hk.jarjestyspaikkaOid, "jarjestyspaikkaOid"),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusaika.contains(true) && hk.liitteet.nonEmpty,
            assertNotOptional(hk.liitteidenToimitusaika, "liitteidenToimitusaika")
          ),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusosoite.contains(true) && hk.liitteet.nonEmpty,
            assertNotOptional(hk.liitteidenToimitustapa, "liitteidenToimitustapa")
          ),
          validateIfTrue(
            hk.liitteetOnkoSamaToimitusosoite.contains(true) && hk.liitteet.nonEmpty
              && hk.liitteidenToimitustapa.contains(MuuOsoite),
            assertNotOptional(hk.liitteidenToimitusosoite, "liitteidenToimitusosoite")
          ),
          validateHakulomake(
            hk.hakulomaketyyppi,
            hk.hakulomakeAtaruId,
            hk.hakulomakeKuvaus,
            hk.hakulomakeLinkki,
            vCtx.kielivalinta
          ),
          assertNotEmpty(hk.pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit"),
          validateOptionalKielistetty(
            vCtx.kielivalinta,
            hk.pohjakoulutusvaatimusTarkenne,
            "pohjakoulutusvaatimusTarkenne"
          ),
          validateOptionalKielistetty(vCtx.kielivalinta, hk.muuPohjakoulutusvaatimus, "muuPohjakoulutusvaatimus"),
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

  private def validateLiite(
      liite: Liite,
      newLiite: Option[Liite],
      path: String,
      vCtx: ValidationContext,
      existingIds: Seq[UUID],
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ): IsValid =
    and(
      validateSubEntityId(liite.id, s"$path.id", vCtx.crudOperation, existingIds, unknownLiiteId(uuidToString(liite.id))),
      validateIfDefined[LiitteenToimitusosoite](
        liite.toimitusosoite,
        validateLiitteenToimitusosoite(
          _,
          hakukohdeDiffResolver.liitteenOsoiteWithNewValues(newLiite),
          s"$path.toimitusosoite",
          vCtx
        )
      ),
      validateIfDefined[String](
        newLiite.flatMap(_.tyyppiKoodiUri),
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, LiiteTyyppiKoodiPattern, s"$path.tyyppiKoodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              hakuKoodiClient.liiteTyyppiKoodiUriExists,
              s"$path.tyyppiKoodiUri",
              vCtx,
              invalidLiitetyyppiKooriuri(koodiUri)
            )
          )
      ),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateOptionalKielistetty(vCtx.kielivalinta, liite.nimi, s"$path.nimi"),
          validateOptionalKielistetty(vCtx.kielivalinta, liite.kuvaus, s"$path.kuvaus"),
          validateIfTrue(
            liite.toimitustapa.contains(MuuOsoite),
            assertNotOptional(liite.toimitusosoite, s"$path.toimitusosoite")
          )
        )
      )
    )

  private def validateLiitteenToimitusosoite(
      osoite: LiitteenToimitusosoite,
      osoiteWithNewValues: Option[Osoite],
      path: String,
      validationContext: ValidationContext
  ): IsValid = and(
    osoite.osoite.validate(
      path,
      osoiteWithNewValues,
      validationContext,
      hakuKoodiClient.postiosoitekoodiExists
    ),
    validateIfDefined[String](osoite.sahkoposti, assertValidEmail(_, s"$path.sahkoposti")),
    validateIfDefined[String](osoite.verkkosivu, assertValidUrl(_, s"$path.verkkosivu"))
  )

  private def validateMetadata(
      m: HakukohdeMetadata,
      vCtx: ValidationContext,
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ): IsValid =
    and(
      assertNotOptional(m.kaytetaanHaunAlkamiskautta, "metadata.kaytetaanHaunAlkamiskautta"),
      assertTrue(
        m.kaytetaanHaunAlkamiskautta.getOrElse(false) != m.koulutuksenAlkamiskausi.isDefined,
        "metadata.koulutuksenAlkamiskausi",
        oneNotBoth("kaytetaanHaunAlkamiskautta", "koulutuksenAlkamiskausi")
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        m.koulutuksenAlkamiskausi,
        _.validate(
          "metadata.koulutuksenAlkamiskausi",
          hakukohdeDiffResolver.koulutuksenAlkamiskausiWithNewValues(),
          vCtx,
          hakuKoodiClient.kausiKoodiUriExists
        )
      ),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateOptionalKielistetty(
            vCtx.kielivalinta,
            m.valintakokeidenYleiskuvaus,
            "metadata.valintakokeidenYleiskuvaus"
          ),
          validateOptionalKielistetty(vCtx.kielivalinta, m.kynnysehto, "metadata.kynnysehto"),
          assertNotOptional(m.aloituspaikat, "metadata.aloituspaikat"),
          validateIfDefined[Aloituspaikat](
            m.aloituspaikat,
            _.validate(vCtx.tila, vCtx.kielivalinta, "metadata.aloituspaikat")
          ),
          // NOTE: hakukohteenLinja validoidaan pakolliseksi lukiotyyppisille
          validateIfDefined[HakukohteenLinja](
            m.hakukohteenLinja,
            l =>
              and(
                validateIfDefined[Double](
                  l.alinHyvaksyttyKeskiarvo,
                  assertNotNegative(_, "metadata.hakukohteenLinja.alinHyvaksyttyKeskiarvo")
                ),
                validateOptionalKielistetty(vCtx.kielivalinta, l.lisatietoa, "metadata.hakukohteenLinja.lisatietoa"),
                validateIfNonEmptySeq[PainotettuOppiaine](
                  l.painotetutArvosanat,
                  hakukohdeDiffResolver.newPainotetutArvosanat(),
                  "metadata.hakukohteenLinja.painotetutArvosanat",
                  (oppiaine, newOppiaine, path) =>
                    and(
                      assertNotOptional(oppiaine.koodiUrit, s"$path.koodiUrit"),
                      validateIfDefined[OppiaineKoodiUrit](
                        oppiaine.koodiUrit,
                        uri =>
                          and(
                            assertNotOptional(uri.oppiaine, s"$path.oppiaine"),
                            validateIfDefined[String](
                              newOppiaine.flatMap(_.koodiUrit).flatMap(_.oppiaine),
                              oppiaineKoodiUri =>
                                validateIfSuccessful(
                                  assertMatch(oppiaineKoodiUri, OppiaineKoodiPattern, s"$path.oppiaine"),
                                  assertKoodistoQueryResult(
                                    oppiaineKoodiUri,
                                    hakuKoodiClient.oppiaineKoodiUriExists,
                                    s"$path.oppiaine",
                                    vCtx,
                                    invalidOppiaineKoodiuri(oppiaineKoodiUri)
                                  )
                                )
                            ),
                            validateIfDefined[String](
                              newOppiaine.flatMap(_.koodiUrit).flatMap(_.kieli),
                              kieliKoodiUri =>
                                validateIfSuccessful(
                                  assertMatch(kieliKoodiUri, KieliKoodiPattern, s"$path.kieli"),
                                  assertKoodistoQueryResult(
                                    kieliKoodiUri,
                                    hakuKoodiClient.kieliKoodiUriExists,
                                    s"$path.kieli",
                                    vCtx,
                                    invalidOppiaineKieliKoodiuri(kieliKoodiUri)
                                  )
                                )
                            )
                          )
                      ),
                      assertNotOptional(oppiaine.painokerroin, s"$path.painokerroin"),
                      validateIfDefined[Double](oppiaine.painokerroin, assertNotNegative(_, s"$path.painokerroin"))
                    )
                )
              )
          )
        )
      ),
      validateIfNonEmpty(m.uudenOpiskelijanUrl, "metadata.uudenOpiskelijanUrl", assertValidUrl _)
    )

  override def validateInternalDependenciesWhenDeletingEntity(e: Hakukohde): IsValid = NoErrors

  private def validateDependencyIntegrity(
      hakukohde: Hakukohde,
      isOphPaakayttaja: Boolean,
      crudOperation: CrudOperation,
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ) = {
    val deps = hakukohdeDAO.getDependencyInformation(hakukohde)

    val haku = hakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    val hakuOid             = hakukohde.hakuOid.s
    val toteutusOid         = hakukohde.toteutusOid.s
    val koulutustyyppi      = deps.get(toteutusOid).flatMap(_._2)
    val haunJulkaisutila    = haku.map { haku => Some(haku.tila) }.getOrElse(None)
    val koulutuksetKoodiUri = deps.get(toteutusOid).flatMap(_._4).getOrElse(Seq())
    val vCtx                = ValidationContext(hakukohde.tila, hakukohde.kielivalinta, crudOperation)

    and(
      validateDependency(hakukohde.tila, deps.get(toteutusOid).map(_._1), toteutusOid, "Toteutusta", "toteutusOid"),
      validateDependencyExistence(haunJulkaisutila, hakuOid, "Hakua", "hakuOid"),
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
                  koulutustyyppi,
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
            case _: LukioToteutusMetadata =>
              validateIfFalse(
                isDIAlukiokoulutus(koulutuksetKoodiUri) || isEBlukiokoulutus(koulutuksetKoodiUri),
                assertTrue(
                  hakukohde.metadata.isDefined && hakukohde.metadata.get.hakukohteenLinja.isDefined,
                  "metadata.hakukohteenLinja",
                  missingMsg
                )
              )
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
      ),
      validateIfDefined[HakukohdeMetadata](
        hakukohde.metadata,
        metadata =>
          validateIfTrue(
            metadata.valintaperusteenValintakokeidenLisatilaisuudet.nonEmpty, {
              val valintaperusteenValintakokeet: Seq[UUID] =
                if (hakukohde.valintaperusteId.isDefined)
                  deps.get(hakukohde.valintaperusteId.get.toString).flatMap(_._5).getOrElse(Seq())
                else Seq()
              validateValintaperusteenValintakokeidenLisatilaisuudet(
                metadata.valintaperusteenValintakokeidenLisatilaisuudet,
                hakukohdeDiffResolver.newValintaperusteenValintakokeidenLisatilaisuudet(),
                hakukohde.valintaperusteId,
                valintaperusteenValintakokeet,
                vCtx
              )
            }
          )
      ),
      validateIfTrue(
        hakukohde.kaytetaanHaunHakulomaketta.contains(false),
        validateIfDefined[Haku](
          haku,
          haku =>
            assertTrue(
              haku.hakulomaketyyppi.contains[Hakulomaketyyppi](MuuHakulomake),
              "hakuOid",
              illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi
            )
        )
      ),
      validateIfTrue(
        hakukohdeDiffResolver.toinenAsteOnkoKaksoistutkintoNewlyActivated(), {
          koulutustyyppi match {
            case Some(Amm) =>
              koulutuksetKoodiUri
                .map(
                  assertKoulutustyyppiQueryResult(
                    _,
                    Seq(ammatillinenPerustutkintoKoulutustyyppi),
                    koulutusKoodiClient,
                    "toinenAsteOnkoKaksoistutkinto",
                    vCtx,
                    toinenAsteOnkoKaksoistutkintoNotAllowed,
                    kaksoistutkintoValidationFailedDuetoKoodistoFailureMsg
                  )
                )
                .flatten

            case Some(Lk) =>
              koulutuksetKoodiUri
                .map(
                  assertKoulutuskoodiQueryResult(
                    _,
                    lukioKoulutusKoodiUritAllowedForKaksoistutkinto,
                    koulutusKoodiClient,
                    "toinenAsteOnkoKaksoistutkinto",
                    vCtx,
                    toinenAsteOnkoKaksoistutkintoNotAllowed,
                    kaksoistutkintoValidationFailedDuetoKoodistoFailureMsg
                  )
                )
                .flatten

            case _ => error("toinenAsteOnkoKaksoistutkinto", toinenAsteOnkoKaksoistutkintoNotAllowed)
          }
        }
      )
    )
  }

  private def validateValintaperusteenValintakokeidenLisatilaisuudet(
      tilaisuudet: Seq[ValintakokeenLisatilaisuudet],
      newTilaisuudet: Seq[ValintakokeenLisatilaisuudet],
      valintaperusteId: Option[UUID],
      valintaperusteenValintakokeet: Seq[UUID],
      vCtx: ValidationContext
  ): IsValid =
    and(
      assertNotOptional(valintaperusteId, "valintaperusteId"),
      validateIfDefined[UUID](
        valintaperusteId,
        valintaperusteId =>
          validateIfNonEmptySeq[ValintakokeenLisatilaisuudet](
            tilaisuudet,
            newTilaisuudet,
            "metadata.valintaperusteenValintakokeidenLisatilaisuudet",
            (kokeenTilaisuudet, newKokeenTilaisuudet, path) =>
              and(
                assertNotOptional(kokeenTilaisuudet.id, s"$path.id"),
                validateIfDefined[UUID](
                  kokeenTilaisuudet.id,
                  id =>
                    assertTrue(
                      valintaperusteenValintakokeet.contains(id),
                      s"$path.id",
                      unknownValintaperusteenValintakoeIdForHakukohde(valintaperusteId, id)
                    )
                ),
                validateIfNonEmptySeq[Valintakoetilaisuus](
                  kokeenTilaisuudet.tilaisuudet,
                  newKokeenTilaisuudet.map(_.tilaisuudet).getOrElse(Seq()),
                  s"$path.tilaisuudet",
                  (tilaisuus, newTilaisuus, path) =>
                    tilaisuus.validate(
                      path,
                      newTilaisuus,
                      vCtx,
                      hakuKoodiClient.postiosoitekoodiExists
                    )
                )
              )
          )
      )
    )

  private def assertHakulomaketyyppiAtaru(hakulomaketyyppi: Option[Hakulomaketyyppi], toteutusOid: String): IsValid =
    assertTrue(
      hakulomaketyyppi.exists(_ == Ataru),
      "toteutusOid",
      cannotLinkToHakukohde(toteutusOid)
    )

  override def validateEntityOnJulkaisu(hk: Hakukohde): IsValid = and(
    validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validateOnJulkaisu(_)),
    validateIfDefined[LocalDateTime](hk.liitteidenToimitusaika, assertInFuture(_, "liitteidenToimitusaika")),
    validateIfNonEmpty[Liite](
      hk.liitteet,
      "liitteet",
      (liite, path) => validateIfDefined[LocalDateTime](liite.toimitusaika, assertInFuture(_, s"$path.toimitusaika"))
    ),
    validateIfNonEmpty[Valintakoe](hk.valintakokeet, "valintakokeet", _.validateOnJulkaisu(_)),
    validateIfDefined[HakukohdeMetadata](
      hk.metadata,
      metadata =>
        and(
          validateIfDefined[KoulutuksenAlkamiskausi](
            metadata.koulutuksenAlkamiskausi,
            _.validateOnJulkaisu("metadata.koulutuksenAlkamiskausi")
          ),
          validateIfNonEmpty[ValintakokeenLisatilaisuudet](
            metadata.valintaperusteenValintakokeidenLisatilaisuudet,
            "metadata.valintaperusteenValintakokeidenLisatilaisuudet",
            _.validateOnJulkaisu(_)
          )
        )
    )
  )
}
