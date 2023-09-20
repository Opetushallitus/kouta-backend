package fi.oph.kouta.service

import fi.oph.kouta.client.{HakemusPalveluClient, HakukohdeInfo, KoodistoElement, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, isToisenAsteenYhteishaku, isYhteishakuHakutapa}
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

object HakukohdeServiceValidation
    extends HakukohdeServiceValidation(
      KoodistoService,
      HakemusPalveluClient,
      OrganisaatioServiceImpl,
      LokalisointiClient,
      HakukohdeDAO,
      HakuDAO
    )

class HakukohdeServiceValidation(
    koodistoService: KoodistoService,
    hakemusPalveluClient: HakemusPalveluClient,
    organisaatioService: OrganisaatioService,
    lokalisointiClient: LokalisointiClient,
    hakukohdeDAO: HakukohdeDAO,
    hakuDAO: HakuDAO
) extends ValidatingService[Hakukohde] {
  def withValidation[R](hakukohde: Hakukohde, oldHakukohde: Option[Hakukohde], authenticated: Authenticated)(
      f: Hakukohde => R
  ): R = {
    val haku = hakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    var errors = super.validateWithStateChangeValidationResults(
      hakukohde,
      oldHakukohde,
      validateHakukohdeStateChange(oldHakukohde, hakukohde, haku)
    )
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
      assertFalse(
        hk.kaytetaanHaunAikataulua.getOrElse(false) && hk.hakuajat.nonEmpty,
        "hakuajat",
        noneOrOneNotBoth("kaytetaanHaunAikataulua", "hakuajat")
      ),
      validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validate(vCtx, _)),
      validateIfNonEmpty[String](
        hakukohdeDiffResolver.newPohjakoulutusvaatimusKoodiUrit(),
        "pohjakoulutusvaatimusKoodiUrit",
        (koodiUri, path) =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, _),
            path,
            vCtx,
            invalidPohjakoulutusVaatimusKoodiuri(koodiUri)
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
            koodistoService.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, _),
            koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _))
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
              hakemusPalveluClient,
              "hakulomakeAtaruId"
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

  private def valintakoeTyyppiKoodiIsAllowed(valintakoeTyyppiKoodi : Option[String],
                                             koulutusKoodit : Seq[String],
                                             hakutapaKoodi : Option[String],
                                             haunkohdejoukkoKoodi : Option[String],
                                             osaamisalaKoodit: Seq[String]): Boolean = {
    koodistoService.getValintakokeenTyypit(koulutusKoodit, hakutapaKoodi, haunkohdejoukkoKoodi, osaamisalaKoodit) match {
      case Right(elements: Seq[KoodistoElement]) =>
        val koodiUrit: Seq[String] = elements.map(koodi => koodi.koodiUri + "#" + koodi.versio)
        valintakoeTyyppiKoodi.exists(valintakoe => koodiUrit.contains(valintakoe))
      case Left(_) => false
    }
  }

  private def isHakuaikaMenossa(hakuaika: Ajanjakso): Boolean = {
    val now = LocalDateTime.now();
    hakuaika.alkaa.isBefore(now) && !hakuaika.paattyy.exists(ha => ha.isBefore(now))
  }

  private def isHakuaikaMenossa(hakuajat: Seq[Ajanjakso]): Boolean = {
    hakuajat.exists(isHakuaikaMenossa)
  }

  private def isAllowedToRemoveArchived(newHakukohde: Hakukohde, haku: Option[Haku]): Boolean = {
    val hakukohdeInfo: Option[HakukohdeInfo] = newHakukohde.oid.map(oid => hakemusPalveluClient.getHakukohdeInfo(oid))
    val hakukohteessaEiOleHakijoita: Boolean = hakukohdeInfo.exists(_.applicationCount == 0)

    val hakukohteenHakuaikaEiOleMenossa: Boolean = !isHakuaikaMenossa(newHakukohde.hakuajat)
    val haunHakuaikaEiOleMenossa: Boolean = !isHakuaikaMenossa(haku.map(_.hakuajat).getOrElse(Seq()))
    val hakuaikaEiOleMenossa: Boolean = haunHakuaikaEiOleMenossa && hakukohteenHakuaikaEiOleMenossa
    hakuaikaEiOleMenossa && hakukohteessaEiOleHakijoita
  }

  private def getValidStatesForNewHakukohde(
      oldHakukohde: Option[Hakukohde],
      newHakukohde: Hakukohde,
      haku: Option[Haku]
  ): Seq[Julkaisutila] = {
    if (oldHakukohde.isEmpty) {
      Seq()
    } else {
      val validStates = validStateChanges.getOrElse(oldHakukohde.get.tila, Seq())
      validStates ++ (if (oldHakukohde.get.tila == Arkistoitu && isAllowedToRemoveArchived(newHakukohde, haku))
                        Seq(Poistettu)
                      else Seq())
    }
  }

  private def validateHakukohdeStateChange(
      oldHakukohde: Option[Hakukohde],
      newHakukohde: Hakukohde,
      haku: Option[Haku]
  ): IsValid = {
    val validStatesForNewHakukohde = getValidStatesForNewHakukohde(oldHakukohde, newHakukohde, haku)
    validateIfDefinedAndTrue(
      oldHakukohde.map(_.tila != newHakukohde.tila),
      validateIfTrue(
        validStatesForNewHakukohde.nonEmpty,
        assertTrue(
          validStatesForNewHakukohde.contains(newHakukohde.tila),
          "tila",
          illegalStateChange(newHakukohde.getEntityDescriptionAllative(), oldHakukohde.get.tila, newHakukohde.tila)
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
      validateSubEntityId(
        liite.id,
        s"$path.id",
        vCtx.crudOperation,
        existingIds,
        unknownLiiteId(uuidToString(liite.id))
      ),
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
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, _),
            s"$path.tyyppiKoodiUri",
            vCtx,
            invalidLiitetyyppiKoodiuri(koodiUri)
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
      koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
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
          koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, _)
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
            _.validate(vCtx, "metadata.aloituspaikat")
          )
        )
      ),
      validateIfNonEmpty(m.uudenOpiskelijanUrl, "metadata.uudenOpiskelijanUrl", assertValidUrl _)
    )

  override def validateInternalDependenciesWhenDeletingEntity(e: Hakukohde): IsValid = NoErrors

  private def validateHakukohteenLinjaAndNimi(
      hkLinja: HakukohteenLinja,
      nimi: Kielistetty,
      allowedLinjaUris: Seq[String],
      vCtx: ValidationContext,
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ): IsValid = {
    and(
      validateIfSuccessful(
        validateIfDefined[String](
          hkLinja.linja,
          linja =>
            assertTrue(
              allowedLinjaUris.contains(linja),
              "metadata.hakukohteenLinja.linja",
              invalidHakukohteenLinja(linja)
            )
        ),
        validateIfTrue(
          hakukohdeDiffResolver.newNimi().isDefined,
          validateIfTrueOrElse(
            hkLinja.linja.isDefined,
            koodistoService.getKaannokset(hkLinja.linja.get) match {
              case Left(_) => error("metadata.hakukohteenLinja.linja", koodistoServiceFailureMsg)
              case Right(kaannokset) =>
                assertNimiMatchExternal(
                  nimi,
                  kaannokset,
                  "nimi",
                  "hakukohteen linjalla"
                )
              case _ => error("metadata.hakukohteenLinja.linja", invalidHakukohteenLinja(hkLinja.linja.get))
            },
            Try[Kielistetty] {
              lokalisointiClient.getKaannoksetWithKeyFromCache("hakukohdelomake.lukionYleislinja")
            } match {
              case Success(yleisLinjaKaannos) =>
                assertNimiMatchExternal(nimi, yleisLinjaKaannos, "nimi", "toteutuksen yleislinjalla")
              case Failure(_) => error("nimi", lokalisointiServiceFailureMsg)
            }
          )
        )
      ),
      validateIfDefined[Double](
        hkLinja.alinHyvaksyttyKeskiarvo,
        assertNotNegative(_, "metadata.hakukohteenLinja.alinHyvaksyttyKeskiarvo")
      ),
      validateOptionalKielistetty(vCtx.kielivalinta, hkLinja.lisatietoa, "metadata.hakukohteenLinja.lisatietoa"),
      validateIfNonEmptySeq[PainotettuOppiaine](
        hkLinja.painotetutArvosanat,
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
                      assertKoodistoQueryResult(
                        oppiaineKoodiUri,
                        koodistoService.oppiaineArvoExists,
                        s"$path.oppiaine",
                        vCtx,
                        invalidOppiaineKoodiuri(oppiaineKoodiUri)
                      )
                  ),
                  validateIfDefined[String](
                    newOppiaine.flatMap(_.koodiUrit).flatMap(_.kieli),
                    kieliKoodiUri =>
                      assertKoodistoQueryResult(
                        kieliKoodiUri,
                        koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, _),
                        s"$path.kieli",
                        vCtx,
                        invalidOppiaineKieliKoodiuri(kieliKoodiUri)
                      )
                  )
                )
            ),
            assertNotOptional(oppiaine.painokerroin, s"$path.painokerroin"),
            validateIfDefined[Double](oppiaine.painokerroin, assertNotNegative(_, s"$path.painokerroin"))
          )
      )
    )
  }

  private def validateDependencyIntegrity(
      hakukohde: Hakukohde,
      isOphPaakayttaja: Boolean,
      crudOperation: CrudOperation,
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ) = {
    val dependencyInfo = hakukohdeDAO.getDependencyInformation(hakukohde)

    val haku = hakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    val hakuOid             = hakukohde.hakuOid.s
    val toteutusOid         = hakukohde.toteutusOid.s
    val koulutustyyppi      = dependencyInfo.map(_.toteutus.koulutustyyppi)
    val haunJulkaisutila    = haku.map(_.tila)
    val koulutuksetKoodiUri = dependencyInfo.map(_.toteutus.koulutusKoodiUrit).getOrElse(Seq())
    val tarjoajat           = dependencyInfo.map(_.toteutus.tarjoajat).getOrElse(Seq())
    val vCtx                = ValidationContext(hakukohde.tila, hakukohde.kielivalinta, crudOperation)
    val osaamisalaKoodit: Seq[String] = dependencyInfo
      .flatMap(_.toteutus.metadata)
      .filter(_.isInstanceOf[AmmatillinenToteutusMetadata])
      .map(metadata => (metadata.asInstanceOf[AmmatillinenToteutusMetadata]).osaamisalat)
      .map(osaamisalat => osaamisalat.map(_.koodiUri))
      .getOrElse(Seq.empty)
    val jarjestyspaikkaJarjestaaUrheilijanAmmKoulutusta =
      dependencyInfo.flatMap(di => di.jarjestyspaikka.flatMap(j => j.jarjestaaUrheilijanAmmKoulutusta))

    and(
      validateIfSuccessful(
        validateDependency(
          hakukohde.tila,
          dependencyInfo.map(_.toteutus.tila),
          toteutusOid,
          "Toteutusta",
          "toteutusOid"
        ),
        validateIfDefined[OrganisaatioOid](
          hakukohde.jarjestyspaikkaOid,
          jarjestysPaikkaOid =>
            assertTrue(
              tarjoajat
                .flatMap(org => organisaatioService.getAllChildOidsFlat(org))
                .distinct
                .contains(jarjestysPaikkaOid),
              "jarjestyspaikkaOid",
              invalidJarjestyspaikkaOid(jarjestysPaikkaOid, hakukohde.toteutusOid)
            )
        )
      ),
      validateIfDefinedAndTrue(
        hakukohde.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta),
        assertTrue(
          koulutustyyppi.contains(Amm),
          "metadata.jarjestaaUrheilijanAmmKoulutusta",
          invalidKoulutustyyppiForHakukohdeJarjestaaUrheilijanAmmKoulutusta(koulutustyyppi)
        )
      ),
      validateIfDefinedAndTrue(
        hakukohde.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta),
        assertTrue(
          jarjestyspaikkaJarjestaaUrheilijanAmmKoulutusta.contains(true) || !hakukohdeDiffResolver
            .jarjestaaUrheilijanAmmatillistakoulutustaChanged(),
          "metadata.jarjestaaUrheilijanAmmKoulutusta",
          invalidJarjestypaikkaForHakukohdeJarjestaaUrheilijanAmmKoulutusta(
            jarjestyspaikkaJarjestaaUrheilijanAmmKoulutusta.getOrElse(false)
          )
        )
      ),
      validateDependencyExistence(haunJulkaisutila, hakuOid, "Hakua", "hakuOid"),
      validateIfDefined[UUID](
        hakukohde.valintaperusteId,
        valintaperusteId =>
          and(
            validateDependency(
              hakukohde.tila,
              dependencyInfo.flatMap(_.valintaperuste).map(_.tila),
              valintaperusteId,
              "Valintaperustetta",
              "valintaperusteId"
            ),
            validateIfDefined[Koulutustyyppi](
              dependencyInfo.flatMap(_.valintaperuste).map(_.koulutustyyppi),
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
      validateToteutusmetadataDependentParameters(
        hakukohde,
        dependencyInfo,
        haku.flatMap(_.hakutapaKoodiUri),
        vCtx,
        hakukohdeDiffResolver
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
                if (hakukohde.valintaperusteId.isDefined) {
                  dependencyInfo.flatMap(_.valintaperuste).map(_.valintakoeIdt).getOrElse(Seq())
                } else Seq()
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
              koulutuksetKoodiUri.flatMap(assertKoulutuskoodiQueryResult(
                _,
                AmmatillisetKoulutuskooditAllowedForKaksoistutkinto,
                koodistoService,
                "toinenAsteOnkoKaksoistutkinto",
                vCtx,
                toinenAsteOnkoKaksoistutkintoNotAllowed,
                kaksoistutkintoValidationFailedDuetoKoodistoFailureMsg
              ))

            case Some(Lk) =>
              koulutuksetKoodiUri.flatMap(assertKoulutuskoodiQueryResult(
                _,
                LukioKoulutusKooditAllowedForKaksoistutkinto,
                koodistoService,
                "toinenAsteOnkoKaksoistutkinto",
                vCtx,
                toinenAsteOnkoKaksoistutkintoNotAllowed,
                kaksoistutkintoValidationFailedDuetoKoodistoFailureMsg
              ))

            case _ => error("toinenAsteOnkoKaksoistutkinto", toinenAsteOnkoKaksoistutkintoNotAllowed)
          }
        }
      ),
      validateIfJulkaistu(
        hakukohde.tila,
        validateIfFalse(hakukohde.valintakokeet.isEmpty, {
          hakukohde.valintakokeet.flatMap(valintakoe => {
            assertTrue(valintakoeTyyppiKoodiIsAllowed(
              valintakoe.tyyppiKoodiUri,
              koulutuksetKoodiUri,
              haku.flatMap(h => h.hakutapaKoodiUri),
              haku.flatMap(h => h.kohdejoukkoKoodiUri),
              osaamisalaKoodit),
              "valintakokeet", valintakoeIsNotFoundFromAllowedRelations(valintakoe.tyyppiKoodiUri.getOrElse("")))
          })
        })
      )
    )
  }

  private def validateToteutusmetadataDependentParameters(
      hk: Hakukohde,
      dependencyInfo: Option[HakukohdeDependencyInformation],
      hakutapaKoodiUri: Option[String],
      vCtx: ValidationContext,
      hakukohdeDiffResolver: HakukohdeDiffResolver
  ): IsValid = {
    val toteutusOid = hk.toteutusOid.s
    dependencyInfo.map(_.toteutus) match {
      case Some(toteutus) =>
        toteutus.metadata match {
          case Some(metadata) =>
            metadata match {
              case m: AmmatillinenToteutusMetadata if isYhteishakuHakutapa(hakutapaKoodiUri) =>
                val erityisOpetus = m.ammatillinenPerustutkintoErityisopetuksena.contains(true)
                val koodisto =
                  if (erityisOpetus) HakukohdeAmmErityisopetusKoodisto else HakukohdePoJalkYhteishakuKoodisto
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertEmptyKielistetty(hk.nimi, "nimi"),
                  validateIfSuccessful(
                    assertNotOptional[String](hk.hakukohdeKoodiUri, "hakukohdeKoodiUri"),
                    validateIfDefined[String](
                      hakukohdeDiffResolver.newHakukohdeKoodiUri(),
                      koodiUri =>
                        assertKoodistoQueryResult(
                          koodiUri,
                          koodistoService.koodiUriExistsInKoodisto(
                            if (erityisOpetus) HakukohdeAmmErityisopetusKoodisto
                            else HakukohdePoJalkYhteishakuKoodisto,
                            _
                          ),
                          "hakukohdeKoodiUri",
                          vCtx,
                          invalidHakukohdeKoodiuri(koodiUri, koodisto.toString)
                        )
                    )
                  )
                )
              case _: TuvaToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  validateIfSuccessful(
                    assertNimiNotHakukohdeKoodiuri(hk),
                    assertNimiMatchExternal(hk.nimi, toteutus.nimi, "nimi", "toteutuksella")
                  )
                )
              case m: AmmatillinenTutkinnonOsaToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk),
                  assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
                )
              case m: AmmatillinenOsaamisalaToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk),
                  assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
                )
              case m: VapaaSivistystyoMuuToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk),
                  assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
                )
              case m: AmmatillinenMuuToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk),
                  assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
                )
              case m: AikuistenPerusopetusToteutusMetadata =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk),
                  assertHakulomaketyyppiAtaru(m.hakulomaketyyppi, toteutusOid)
                )
              case m: LukioToteutusMetadata =>
                and(
                  assertNimiNotHakukohdeKoodiuri(hk),
                  validateIfTrueOrElse(
                    isDIAlukiokoulutus(toteutus.koulutusKoodiUrit) || isEBlukiokoulutus(toteutus.koulutusKoodiUrit),
                    and(
                      assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                      assertNimiMatchExternal(hk.nimi, toteutus.nimi, "nimi", "toteutuksella")
                    ),
                    validateIfSuccessful(
                      assertNotOptional[HakukohteenLinja](
                        hk.metadata.flatMap(_.hakukohteenLinja),
                        "metadata.hakukohteenLinja"
                      ),
                      validateIfDefined[HakukohteenLinja](
                        hk.metadata.flatMap(_.hakukohteenLinja),
                        linja =>
                          validateHakukohteenLinjaAndNimi(
                            linja,
                            hk.nimi,
                            m.erityisetKoulutustehtavat.map(_.koodiUri) ++ m.painotukset.map(_.koodiUri),
                            vCtx,
                            hakukohdeDiffResolver
                          )
                      )
                    )
                  )
                )
              case _ =>
                and(
                  assertNotDefined(hk.metadata.flatMap(_.hakukohteenLinja), "metadata.hakukohteenLinja"),
                  assertNimiNotHakukohdeKoodiuri(hk)
                )
            }
          case _ => NoErrors
        }
      case _ => NoErrors
    }
  }

  private def assertNimiNotHakukohdeKoodiuri(hakukohde: Hakukohde): IsValid =
    and(
      assertNotDefined[String](hakukohde.hakukohdeKoodiUri, "hakukohdeKoodiUri"),
      validateKielistetty(hakukohde.kielivalinta, hakukohde.nimi, "nimi")
    )

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
                      koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
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

  override def validateEntityOnJulkaisu(hk: Hakukohde): IsValid = {
    and(
      validateIfNonEmpty[Ajanjakso](hk.hakuajat, "hakuajat", _.validateOnJulkaisuForHakukohde(_)),
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
}
