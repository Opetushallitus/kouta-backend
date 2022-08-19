package fi.oph.kouta.service

import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, isToisenAsteenYhteishaku}
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{
  IsValid,
  NoErrors,
  ammatillinenPerustutkintoKoulutustyyppi,
  lukioKoulutusKoodiUritAllowedForKaksoistutkinto
}

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
        hakukohde.tila,
        hakukohde.kielivalinta
      )

    errors match {
      case NoErrors => f(hakukohde)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  override def validateEntity(hk: Hakukohde, oldHk: Option[Hakukohde]): IsValid = {
    val tila         = hk.tila
    val kielivalinta = hk.kielivalinta
    val crudOperation = if (oldHk.isDefined) update else create

    and(
      hk.validate(),
      assertValid(hk.toteutusOid, "toteutusOid"),
      assertValid(hk.hakuOid, "hakuOid"),
      // Joko hakukohdeKoodiUri tai nimi täytyy olla, mutta ei molempia!
      assertTrue(hk.hakukohdeKoodiUri.nonEmpty != hk.nimi.nonEmpty, "nimi", oneNotBoth("nimi", "hakukohdeKoodiUri")),
      validateIfDefined[String](
        hk.hakukohdeKoodiUri,
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, HakukohdeKoodiPattern, "hakukohdeKoodiUri"),
            assertTrue(
              hakuKoodiClient.hakukohdeKoodiUriExists(koodiUri),
              "hakukohdeKoodiUri",
              invalidHakukohdeKooriuri(koodiUri)
            )
          )
      ),
      validateIfTrue(
        hk.nimi.nonEmpty,
        validateKielistetty(kielivalinta, hk.nimi, "nimi")
      ),
      assertFalse(
        hk.kaytetaanHaunAikataulua.getOrElse(false) && hk.hakuajat.nonEmpty,
        "hakuajat",
        noneOrOneNotBoth("kaytetaanHaunAikataulua", "hakuajat")
      ),
      validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validate(tila, kielivalinta, _)),
      validateIfNonEmpty[String](
        hk.pohjakoulutusvaatimusKoodiUrit,
        "pohjakoulutusvaatimusKoodiUrit",
        (koodiUri, path) =>
          validateIfSuccessful(
            assertMatch(koodiUri, PohjakoulutusvaatimusKoodiPattern, path),
            assertTrue(
              hakuKoodiClient.pohjakoulutusVaatimusKoodiUriExists(koodiUri),
              path,
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
        validateLiitteenToimitusosoite(_, "liitteidenToimitusosoite", tila, kielivalinta)
      ),
      validateIfNonEmpty[Liite](
        hk.liitteet,
        "liitteet",
        (liite, path) => validateLiite(liite, path, tila, kielivalinta)
      ),
      validateIfNonEmpty[Valintakoe](
        hk.valintakokeet,
        "valintakokeet",
        _.validate(
          tila,
          kielivalinta,
          _,
          hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
          hakuKoodiClient.postiosoitekoodiExists
        )
      ),
      validateIfDefined[HakukohdeMetadata](hk.metadata, validateMetadata(_, tila, kielivalinta)),
      assertFalse(
        hk.kaytetaanHaunHakulomaketta.getOrElse(false) && hk.hakulomaketyyppi.isDefined,
        "hakulomaketyyppi",
        noneOrOneNotBoth("kaytetaanHaunHakulomaketta", "hakulomaketyyppi")
      ),
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
          validateIfSuccessful(
            validateHakulomake(
              hk.hakulomaketyyppi,
              hk.hakulomakeAtaruId,
              hk.hakulomakeKuvaus,
              hk.hakulomakeLinkki,
              kielivalinta
            ),
            validateIfDefinedOrModified[UUID](
              hk.hakulomakeAtaruId,
              oldHk.map(_.hakulomakeAtaruId).getOrElse(None),
              ataruId =>
                assertTrue(
                  hakemusPalveluClient.isExistingAtaruId(ataruId),
                  "hakulomakeAtaruId",
                  unknownAtaruId(ataruId)
                )
            )
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

  private def validateLiite(liite: Liite, path: String, tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid =
    and(
      validateIfDefined[LiitteenToimitusosoite](
        liite.toimitusosoite,
        validateLiitteenToimitusosoite(_, s"$path.toimitusosoite", tila, kielivalinta)
      ),
      validateIfDefined[String](
        liite.tyyppiKoodiUri,
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, LiiteTyyppiKoodiPattern, s"$path.tyyppiKoodiUri"),
            assertTrue(
              hakuKoodiClient.liiteTyyppiKoodiUriExists(koodiUri),
              s"$path.tyyppiKoodiUri",
              invalidLiitetyyppiKooriuri(koodiUri)
            )
          )
      ),
      validateIfJulkaistu(
        tila,
        and(
          validateOptionalKielistetty(kielivalinta, liite.nimi, s"$path.nimi"),
          validateOptionalKielistetty(kielivalinta, liite.kuvaus, s"$path.kuvaus"),
          validateIfTrue(
            liite.toimitustapa.contains(MuuOsoite),
            assertNotOptional(liite.toimitusosoite, s"$path.toimitusosoite")
          )
        )
      )
    )

  private def validateLiitteenToimitusosoite(
      osoite: LiitteenToimitusosoite,
      path: String,
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli]
  ): IsValid = and(
    osoite.osoite.validate(tila, kielivalinta, path, hakuKoodiClient.postiosoitekoodiExists),
    validateIfDefined[String](osoite.sahkoposti, assertValidEmail(_, s"$path.sahkoposti")),
    validateIfDefined[String](osoite.verkkosivu, assertValidUrl(_, s"$path.verkkosivu"))
  )

  private def validateMetadata(
      m: HakukohdeMetadata,
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli]
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
          tila,
          kielivalinta,
          "metadata.koulutuksenAlkamiskausi",
          hakuKoodiClient.kausiKoodiUriExists
        )
      ),
      validateIfJulkaistu(
        tila,
        and(
          validateOptionalKielistetty(
            kielivalinta,
            m.valintakokeidenYleiskuvaus,
            "metadata.valintakokeidenYleiskuvaus"
          ),
          validateOptionalKielistetty(kielivalinta, m.kynnysehto, "metadata.kynnysehto"),
          assertNotOptional(m.aloituspaikat, "metadata.aloituspaikat"),
          validateIfDefined[Aloituspaikat](m.aloituspaikat, _.validate(tila, kielivalinta, "metadata.aloituspaikat")),
          // NOTE: hakukohteenLinja validoidaan pakolliseksi lukiotyyppisille
          validateIfDefined[HakukohteenLinja](
            m.hakukohteenLinja,
            l =>
              and(
                validateIfDefined[Double](
                  l.alinHyvaksyttyKeskiarvo,
                  assertNotNegative(_, "metadata.hakukohteenLinja.alinHyvaksyttyKeskiarvo")
                ),
                validateOptionalKielistetty(kielivalinta, l.lisatietoa, "metadata.hakukohteenLinja.lisatietoa"),
                validateIfNonEmpty[PainotettuOppiaine](
                  l.painotetutArvosanat,
                  "metadata.hakukohteenLinja.painotetutArvosanat",
                  (oppiaine, path) =>
                    and(
                      assertNotOptional(oppiaine.koodiUrit, s"$path.koodiUrit"),
                      validateIfDefined[OppiaineKoodiUrit](
                        oppiaine.koodiUrit,
                        uri =>
                          and(
                            assertNotOptional(uri.oppiaine, s"$path.oppiaine"),
                            validateIfDefined[String](
                              uri.oppiaine,
                              koodiUri =>
                                validateIfSuccessful(
                                  assertMatch(koodiUri, OppiaineKoodiPattern, s"$path.oppiaine"),
                                  assertTrue(
                                    hakuKoodiClient.oppiaineKoodiUriExists(koodiUri),
                                    s"$path.oppiaine",
                                    invalidOppiaineKoodiuri(koodiUri)
                                  )
                                )
                            ),
                            validateIfDefined[String](
                              uri.kieli,
                              koodiUri =>
                                validateIfSuccessful(
                                  assertMatch(koodiUri, KieliKoodiPattern, s"$path.kieli"),
                                  assertTrue(
                                    hakuKoodiClient.kieliKoodiUriExists(koodiUri),
                                    s"$path.kieli",
                                    invalidOppiaineKieliKoodiuri(koodiUri)
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
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli]
  ) = {
    val deps = hakukohdeDAO.getDependencyInformation(hakukohde)

    val haku = hakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    val hakuOid             = hakukohde.hakuOid.s
    val toteutusOid         = hakukohde.toteutusOid.s
    val koulutustyyppi      = deps.get(toteutusOid).flatMap(_._2)
    val haunJulkaisutila    = haku.map { haku => Some(haku.tila) }.getOrElse(None)
    val koulutuksetKoodiUri = deps.get(toteutusOid).flatMap(_._4).getOrElse(Seq())

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
                hakukohde.valintaperusteId,
                valintaperusteenValintakokeet,
                tila,
                kielivalinta
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
        hakukohde.toinenAsteOnkoKaksoistutkinto.contains(true),
        assertTrue(
          toinenAsteOnkoKaksoistutkintoAllowed(koulutustyyppi, deps.get(toteutusOid).flatMap(_._4).getOrElse(Seq())),
          "toinenAsteOnkoKaksoistutkinto",
          toinenAsteOnkoKaksoistutkintoNotAllowed
        )
      )
    )
  }

  private def validateValintaperusteenValintakokeidenLisatilaisuudet(
      tilaisuudet: Seq[ValintakokeenLisatilaisuudet],
      valintaperusteId: Option[UUID],
      valintaperusteenValintakokeet: Seq[UUID],
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli]
  ): IsValid =
    and(
      assertNotOptional(valintaperusteId, "valintaperusteId"),
      validateIfDefined[UUID](
        valintaperusteId,
        valintaperusteId =>
          validateIfNonEmpty[ValintakokeenLisatilaisuudet](
            tilaisuudet,
            "metadata.valintaperusteenValintakokeidenLisatilaisuudet",
            (kokeenTilaisuudet, path) =>
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
                validateIfNonEmpty[Valintakoetilaisuus](
                  kokeenTilaisuudet.tilaisuudet,
                  s"$path.tilaisuudet",
                  _.validate(tila, kielivalinta, _, hakuKoodiClient.postiosoitekoodiExists)
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

  private def toinenAsteOnkoKaksoistutkintoAllowed(
      koulutustyyppi: Option[Koulutustyyppi],
      koulutusKoodiUrit: Seq[String]
  ): Boolean =
    koulutustyyppi match {
      case Some(Amm) =>
        koulutusKoodiUrit.nonEmpty && koulutusKoodiUrit.forall(
          koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(Seq(ammatillinenPerustutkintoKoulutustyyppi), _)
        )
      case Some(Lk) =>
        koulutusKoodiUrit.nonEmpty && koulutusKoodiUrit.forall(
          koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUritAllowedForKaksoistutkinto, _)
        )
      case _ => false
    }

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
