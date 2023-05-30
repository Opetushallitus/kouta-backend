package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids.{ChildOid, GrandChildOid, OphOid, OtherOid, ParentOid, UnknownOid}
import fi.oph.kouta.client.{CachedKoodistoClient, HakemusPalveluClient, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket}
import fi.oph.kouta.service.{HakukohdeServiceValidation, KoutaValidationException, OrganisaatioService}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{ErrorMessage, ValidationError}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}
import fi.oph.kouta.mocks.{KoodistoServiceMock, TestKoodistoElement}

import java.net.InetAddress
import java.time.ZonedDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

class HakukohdeServiceValidationSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val koodistoClient  = mock[CachedKoodistoClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]
  val organisaatioService  = mock[OrganisaatioService]
  val lokalisointiClient   = mock[LokalisointiClient]
  val hakukohdeDao         = mock[HakukohdeDAO]
  val hakuDao              = mock[HakuDAO]

  val authenticatedPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set("APP_KOUTA", "APP_KOUTA_OPHPAAKAYTTAJA", s"APP_KOUTA_OPHPAAKAYTTAJA_${OphOid}").map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )
  val authenticatedNonPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set("APP_KOUTA", "APP_KOUTA_HAKUKOHDE_READ", s"APP_KOUTA_HAKUKOHDE_READ_${ChildOid}").map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )

  val ataruId                       = UUID.randomUUID()
  val valintaperusteId              = UUID.randomUUID()
  val valintaperusteId2             = UUID.randomUUID()
  val valintaperusteenValintakoeId1 = UUID.randomUUID()
  val valintaperusteenValintakoeId2 = UUID.randomUUID()

  val haku = JulkaistuHaku.copy(hakulomaketyyppi = Some(MuuHakulomake))
  val valintakokeenLisatilaisuus = JulkaistuHakukohde.metadata.get.valintaperusteenValintakokeidenLisatilaisuudet.head
    .copy(id = Some(valintaperusteenValintakoeId1))
  val max: Hakukohde = JulkaistuHakukohde.copy(
    valintaperusteId = Some(valintaperusteId),
    hakulomaketyyppi = Some(Ataru),
    hakulomakeAtaruId = Some(ataruId),
    hakulomakeKuvaus = Map(),
    hakulomakeLinkki = Map(),
    metadata = Some(
      JulkaistuHakukohde.metadata.get
        .copy(valintaperusteenValintakokeidenLisatilaisuudet = List(valintakokeenLisatilaisuus))
    )
  )
  val min: Hakukohde                 = MinHakukohde
  val maxMetadata: HakukohdeMetadata = max.metadata.get
  val maxWithIds = max.copy(
    oid = Some(HakukohdeOid("1.2.246.562.20.0000000001")),
    valintakokeet = max.valintakokeet.map(_.copy(id = Some(UUID.randomUUID()))),
    liitteet = max.liitteet.map(_.copy(id = Some(UUID.randomUUID())))
  )
  val yhteisHakuHakuOid = HakuOid("1.2.246.562.29.321")

  val validator = new HakukohdeServiceValidation(
    koodistoClient,
    hakemusPalveluClient,
    organisaatioService,
    lokalisointiClient,
    hakukohdeDao,
    hakuDao
  )

  val dependencies = HakukohdeDependencyInformation(
    HakukohdeToteutusDependencyInfo(
      ToteutusOid("1.2.246.562.17.123"),
      Julkaistu,
      Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
      Amm,
      Some(AmmToteutuksenMetatieto),
      Seq("koulutus_371101#1"),
      Seq(OtherOid, ParentOid)
    ),
    Some(
      HakukohdeValintaperusteDependencyInfo(
        valintaperusteId,
        Julkaistu,
        Amm,
        Seq(valintaperusteenValintakoeId1, valintaperusteenValintakoeId2)
      )
    ),
    Some(HakukohdeJarjestyspaikkaDependencyInfo(
      oid = OrganisaatioOid("DUMMY"),
      jarjestaaUrheilijanAmmKoulutusta = Some(true)
    ))
  )

  val lukioDependencies = HakukohdeDependencyInformation(
    HakukohdeToteutusDependencyInfo(
      ToteutusOid("1.2.246.562.17.123"),
      Julkaistu,
      Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
      Lk,
      Some(LukioToteutuksenMetatieto),
      Seq("koulutus_301101#1"),
      Seq(OtherOid, ParentOid)
    ),
    Some(
      HakukohdeValintaperusteDependencyInfo(
        valintaperusteId,
        Julkaistu,
        Lk,
        Seq(valintaperusteenValintakoeId1, valintaperusteenValintakoeId2)
      )
    ),
    None
  )

  def initMockSeq(hakukohde: Hakukohde): Hakukohde = {
    when(hakukohdeDao.getDependencyInformation(hakukohde)).thenAnswer(Some(dependencies))
    hakukohde
  }

  private def initMockDepsForKoulutustyyppi(
      hakukohde: Hakukohde,
      toteutusMetadata: ToteutusMetadata,
      koulutusKoodiUrit: Seq[String] = Seq()
  ): Unit =
    when(hakukohdeDao.getDependencyInformation(hakukohde)).thenAnswer(
      Some(
        HakukohdeDependencyInformation(
          HakukohdeToteutusDependencyInfo(
            hakukohde.toteutusOid,
            Julkaistu,
            Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
            toteutusMetadata.tyyppi,
            Some(toteutusMetadata),
            koulutusKoodiUrit,
            Seq(OtherOid, ParentOid)
          ),
          Some(
            HakukohdeValintaperusteDependencyInfo(
              valintaperusteId,
              Julkaistu,
              toteutusMetadata.tyyppi,
              Seq(valintaperusteenValintakoeId1, valintaperusteenValintakoeId2)
            )
          ),
          None
        )
      )
    )

  private def initMockDepsWithToteutusOidAndTila(hakukohde: Hakukohde, toteutusOid: String, tila: Julkaisutila): Unit =
    when(hakukohdeDao.getDependencyInformation(hakukohde)).thenAnswer(
      Some(
        dependencies.copy(toteutus =
          HakukohdeToteutusDependencyInfo(
            ToteutusOid(toteutusOid),
            tila,
            Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
            Amm,
            Some(AmmToteutuksenMetatieto),
            Seq("koulutus_371101#1"),
            Seq(OtherOid, ParentOid)
          )
        )
      )
    )

  private def initMockDepsWithValintaperusteParams(
      hakukohde: Hakukohde,
      valintaperusteId: UUID,
      tila: Julkaisutila,
      koulutustyyppi: Koulutustyyppi = Amm
  ): Unit =
    when(hakukohdeDao.getDependencyInformation(hakukohde)).thenAnswer(
      Some(
        dependencies.copy(valintaperuste =
          Some(
            HakukohdeValintaperusteDependencyInfo(
              valintaperusteId,
              tila,
              koulutustyyppi,
              Seq(valintaperusteenValintakoeId1, valintaperusteenValintakoeId2)
            )
          )
        )
      )
    )

  val vainSuomeksi         = Map(Fi -> "vain suomeksi", Sv -> "")
  val fullKielistetty      = Map(Fi -> "suomeksi", Sv -> "på svenska")
  val kielistettyWoSvenska = invalidKielistetty(Seq(Sv))

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(koodistoClient.koodiUriExistsInKoodisto(HakukohdePoJalkYhteishakuKoodisto, "hakukohteetperusopetuksenjalkeinenyhteishaku_01#1"))
      .thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(HakukohdeAmmErityisopetusKoodisto, "hakukohteeterammatillinenerityisopetus_01#1"))
      .thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_pk#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_yo#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_1#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_2#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_04230#2")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    when(hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId)).thenAnswer(itemFound)

    when(koodistoClient.oppiaineArvoExists("painotettavatoppiaineetlukiossa_b3pt")).thenAnswer(itemFound)
    when(koodistoClient.oppiaineArvoExists("painotettavatoppiaineetlukiossa_b1lt")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_FI")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_SV")).thenAnswer(itemFound)

    when(lokalisointiClient.getKaannoksetWithKeyFromCache("hakukohdelomake.lukionYleislinja"))
      .thenAnswer(Map(Fi -> "Lukion yleislinja", Sv -> "Gymnasium allmän linje", En -> "Yleislinja"))
    when(koodistoClient.getKoodistoElementVersionOrLatestFromCache("lukiopainotukset_1#1"))
      .thenAnswer(Right(TestKoodistoElement("lukiopainotukset_1", 1, Map(Fi -> "painotus", Sv -> "painotus sv"))))
    when(koodistoClient.getKoodistoElementVersionOrLatestFromCache("lukiolinjaterityinenkoulutustehtava_1#1")).thenAnswer(
      Right(
        TestKoodistoElement("lukiolinjaterityinenkoulutustehtava_1", 1, Map(Fi -> "erityistehtävä", Sv -> "erityistehtävä sv"))
      )
    )
    when(koodistoClient.getKoodistoElementVersionOrLatestFromCache("failure")).thenAnswer(Left(new RuntimeException()))

    when(hakukohdeDao.getDependencyInformation(max)).thenAnswer(Some(dependencies))
    when(hakukohdeDao.getDependencyInformation(min)).thenAnswer(Some(dependencies.copy(valintaperuste = None)))
    when(hakuDao.get(HakuOid("1.2.246.562.29.123"), TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Some((haku, ZonedDateTime.now().toInstant)))
    when(hakuDao.get(yhteisHakuHakuOid, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Some((haku.copy(hakutapaKoodiUri = Some("hakutapa_01#1")), ZonedDateTime.now().toInstant)))

    when(
      koodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(
        AmmatillisetKoulutuskooditAllowedForKaksoistutkinto.koulutusTyypit,
        "koulutus_371101#1"
      )
    ).thenAnswer(itemFound)
    when(
      koodistoClient.koulutusKoodiUriExists(
        LukioKoulutusKooditAllowedForKaksoistutkinto.koulutusKoodiUrit,
        "koulutus_301101#1"
      )
    ).thenAnswer(itemFound)
    when(organisaatioService.getAllChildOidsFlat(ParentOid)).thenAnswer(Seq(ParentOid, ChildOid, GrandChildOid))
    when(organisaatioService.getAllChildOidsFlat(OtherOid)).thenAnswer(Seq(OtherOid))
  }

  def passesValidation(
      hk: Hakukohde,
      oldHk: Option[Hakukohde] = None,
      authenticated: Authenticated = authenticatedNonPaakayttaja
  ): Unit = validator.withValidation(hk, oldHk, authenticated)(hk => hk)

  def failsValidation(
      hk: Hakukohde,
      path: String,
      message: ErrorMessage
  ): Assertion =
    failsValidation(hk, Seq(ValidationError(path, message)), authenticatedNonPaakayttaja)

  def failsValidation(
      hk: Hakukohde,
      expected: Seq[ValidationError],
      authenticated: Authenticated = authenticatedNonPaakayttaja
  ): Assertion =
    Try(validator.withValidation(hk, None, authenticated)(hk => hk)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  def failsModifyValidation(hk: Hakukohde, oldHk: Hakukohde, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(hk, Some(oldHk), authenticatedNonPaakayttaja)(hk => hk)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  "Hakukohde validation" should "succeed when new valid hakukohde" in {
    passesValidation(max)
  }

  it should "succeed when new incomplete luonnos" in {
    passesValidation(min)
  }

  it should "succeed when new amm-hakukohde with jarjestaaUrheilijanAmmKoulutusta when orgganisaatio is allowed" in {
    passesValidation(
      initMockSeq(
        max.copy(
          metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(true)))
        )
      )
    )
  }

  it should "succeed when new lk-hakukohde with jarjestaaUrheilijanAmmKoulutusta" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "painotus", Sv -> "painotus sv"),
      metadata = Some(
        maxMetadata.copy(
          hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = Some("lukiopainotukset_1#1"))),
          jarjestaaUrheilijanAmmKoulutusta = Some(true)
        )
      )
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    failsValidation(
      lkHakukohde,
      Seq(
        ValidationError(
          "metadata.jarjestaaUrheilijanAmmKoulutusta",
          ErrorMessage(
            "Hakukohde saa järjestää ammatillista urheiljan koulutusta vain jos koulutuksen koulutustyyppi on Amm. Hakukohteen koulutustyyppi on lk",
            "invalidKoulutustyyppiForHakukohdeJarjestaaUrheilijanAmmKoulutusta"
          )
        ),
        ValidationError(
          "metadata.jarjestaaUrheilijanAmmKoulutusta",
          ErrorMessage(
            "Hakukohde saa järjestää ammatillista urheiljan koulutusta vain jos järjestyspaikka saa järjestää ammatillista urheiljan koulutusta. Järjestyspaikan jarjestaaUrheilijanAmmKoulutusta on false",
            "invalidJarjestypaikkaForHakukohdeJarjestaaUrheilijanAmmKoulutusta"
          )
        )
      )
    )
  }

  it should "succeed when new amm-hakukohde for yhteishaku with correct hakukohdeKoodiUri" in {
    passesValidation(
      initMockSeq(
        max.copy(
          hakuOid = yhteisHakuHakuOid,
          nimi = Map(),
          hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_01#1")
        )
      )
    )
  }

  it should "succeed when new amm-yhteishakukohde with ammatillinenPerustutkintoErityisopetuksena with correct hakukohdeKoodiUri" in {
    val hk = max.copy(
      hakuOid = yhteisHakuHakuOid,
      nimi = Map(),
      hakukohdeKoodiUri = Some("hakukohteeterammatillinenerityisopetus_01#1")
    )
    initMockDepsForKoulutustyyppi(
      hk,
      AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = Some(true))
    )
    passesValidation(hk)
  }

  it should "succeed when new tuva-hakukohde with nimi matching toteutus" in {
    val hk = max.copy(nimi = Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"))
    initMockDepsForKoulutustyyppi(hk, TuvaToteutuksenMetatieto)
    passesValidation(hk)
  }

  it should "succeed when hakuajat taken from haku, defined in hakukohde itself, or not defined at all for luonnos" in {
    passesValidation(initMockSeq(max.copy(hakuajat = Seq(), kaytetaanHaunAikataulua = Some(true))))
    passesValidation(
      initMockSeq(
        max.copy(
          hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
          kaytetaanHaunAikataulua = Some(false)
        )
      )
    )
    passesValidation(initMockSeq(max.copy(tila = Tallennettu, hakuajat = Seq(), kaytetaanHaunAikataulua = None)))
  }

  it should "succeed when new hakukohde with common liitteiden toimitusosoite" in {
    passesValidation(
      initMockSeq(
        max.copy(
          liitteetOnkoSamaToimitusosoite = Some(true),
          liitteidenToimitustapa = Some(MuuOsoite),
          liitteidenToimitusosoite = Some(LiitteenToimitusosoite1)
        )
      )
    )
  }

  it should "succeed when common liitteiden toimitusaika and toimitusosoite used and no liitteet" in {
    passesValidation(
      initMockSeq(
        max.copy(
          liitteetOnkoSamaToimitusaika = Some(true),
          liitteetOnkoSamaToimitusosoite = Some(true),
          liitteidenToimitustapa = Some(MuuOsoite),
          liitteidenToimitusaika = None,
          liitteidenToimitusosoite = None,
          liitteet = Seq()
        )
      )
    )
  }

  it should "succeed when kaytetaanHaunAlkamiskautta used" in {
    passesValidation(
      initMockSeq(
        max.copy(metadata =
          Some(maxMetadata.copy(kaytetaanHaunAlkamiskautta = Some(true), koulutuksenAlkamiskausi = None))
        )
      )
    )
  }

  it should "succeed when lukio-hakukohde with hakukohteenlinja (yleislinja)" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "Lukion yleislinja", Sv -> "Gymnasium allmän linje"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja)))
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    passesValidation(lkHakukohde)
  }

  it should "succeed when lukio-hakukohde with hakukohteenLinja from painotukset" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "painotus", Sv -> "painotus sv"),
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = Some("lukiopainotukset_1#1"))))
      )
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    passesValidation(lkHakukohde)
  }

  it should "succeed when lukio-hakukohde with hakukohteenLinja from erityisetKoulutustehtavat" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "erityistehtävä", Sv -> "erityistehtävä sv"),
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja =
          Some(LukioHakukohteenLinja.copy(linja = Some("lukiolinjaterityinenkoulutustehtava_1#1")))
        )
      )
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    passesValidation(lkHakukohde)
  }

  it should "succeed when lukio-hakukohde of DIAKoulutus or EBKoulutus without hakukohteenlinja" in {
    val lkHakukohde = max.copy(nimi = Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"))
    initMockDepsForKoulutustyyppi(lkHakukohde, LukioToteutuksenMetatieto, Seq("koulutus_301104#1"))
    passesValidation(lkHakukohde)
  }

  it should "succeed for Oph-virkailija even though hakukohteen liittämisen takaraja has expired" in {
    when(hakuDao.get(HakuOid("1.2.246.562.29.1111111111"), TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Some((haku.copy(hakukohteenLiittamisenTakaraja = Some(inPast(100))), ZonedDateTime.now().toInstant)))
    passesValidation(
      initMockSeq(max.copy(hakuOid = HakuOid("1.2.246.562.29.1111111111"))),
      None,
      authenticatedPaakayttaja
    )
  }

  it should "succeed for Oph-virkailija even though hakukohteen moukkaamisen takaraja has expired" in {
    when(hakuDao.get(HakuOid("1.2.246.562.29.1111111111"), TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Some((haku.copy(hakukohteenMuokkaamisenTakaraja = Some(inPast(100))), ZonedDateTime.now().toInstant)))
    passesValidation(
      initMockSeq(maxWithIds),
      Some(maxWithIds),
      authenticatedPaakayttaja
    )
  }

  it should "succeed when kaksoistutkinto for Amm koulutus" in {
    passesValidation(initMockSeq(max.copy(toinenAsteOnkoKaksoistutkinto = Some(true))))
  }

  it should "succeed when kaksoistutkinto for Lukio koulutus" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "Lukion yleislinja", Sv -> "Gymnasium allmän linje"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja))),
      toinenAsteOnkoKaksoistutkinto = Some(true)
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    passesValidation(lkHakukohde)
  }

  it should "succeed when hakukohdeKoodiUri not changed in modify operation, eventhough hakukohdeKoodiUri unknown" in {
    passesValidation(
      initMockSeq(
        maxWithIds.copy(
          hakuOid = yhteisHakuHakuOid,
          nimi = Map(),
          hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_XX")
        )
      ),
      Some(
        maxWithIds.copy(
          hakuOid = yhteisHakuHakuOid,
          nimi = Map(),
          hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_XX")
        )
      )
    )
  }

  it should "succeed when pohjakoulutusKoodiUrit not changed in modify operation, eventhough koodiUrit unknown" in {
    val urit = Seq("pohjakoulutusvaatimuskouta_XX#1", "pohjakoulutusvaatimuskouta_YY#1")
    passesValidation(
      initMockSeq(
        maxWithIds.copy(pohjakoulutusvaatimusKoodiUrit = urit)
      ),
      Some(maxWithIds.copy(pohjakoulutusvaatimusKoodiUrit = urit))
    )
  }

  it should "succeed when liitteidentoimitusosoite not changed in modify operation, eventhough postinumeroKoodiUri unknown" in {
    val osoite = Some(LiitteenToimitusosoite(osoite = Osoite(postinumeroKoodiUri = Some("posti_12345#1"))))
    passesValidation(
      initMockSeq(
        maxWithIds.copy(
          tila = Tallennettu,
          liitteidenToimitustapa = Some(MuuOsoite),
          liitteetOnkoSamaToimitusosoite = Some(true),
          liitteidenToimitusosoite = osoite
        )
      ),
      Some(maxWithIds.copy(liitteidenToimitusosoite = osoite))
    )
  }

  it should "succeed when liitteet not changed in modify operation, eventhough unknown liitetyyppi" in {
    val liitteet = List(Liite1.copy(tyyppiKoodiUri = Some("liitetyypitamm_9#1")))
    passesValidation(
      initMockSeq(
        maxWithIds.copy(liitteet = liitteet)
      ),
      Some(maxWithIds.copy(liitteet = liitteet))
    )
  }

  it should "succeed when valintakokeet not changed in modify operation, eventhough unknown valintakoetyyppi" in {
    val valintakokeet = List(Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_9#1")))
    passesValidation(
      initMockSeq(
        maxWithIds.copy(valintakokeet = valintakokeet)
      ),
      Some(maxWithIds.copy(valintakokeet = valintakokeet))
    )
  }

  it should "succeed when ataruId not changed in modify operation, eventhough ataruId is unknown" in {
    val unknownAtaruId = Some(UUID.randomUUID())
    passesValidation(
      initMockSeq(maxWithIds.copy(hakulomakeAtaruId = unknownAtaruId)),
      Some(maxWithIds.copy(hakulomakeAtaruId = unknownAtaruId))
    )
  }

  it should "succeed when KoulutuksenAlkamiskausi not changed in modify operation, eventhough kausiKoodiUri is unknown" in {
    val metadata = maxMetadata.copy(koulutuksenAlkamiskausi =
      Some(KoulutuksenAlkamiskausi(koulutuksenAlkamiskausiKoodiUri = Some("kausi_xx#2")))
    )
    passesValidation(
      initMockSeq(maxWithIds.copy(tila = Tallennettu, metadata = Some(metadata))),
      Some(maxWithIds.copy(metadata = Some(metadata)))
    )
  }

  it should "succeed when PainotetutArvosanat not changed in modify operation, eventhough oppiaineKoodiUri and kieli unknown" in {
    val hk = maxWithIds.copy(metadata =
      Some(
        maxMetadata.copy(hakukohteenLinja =
          Some(
            LukioHakukohteenLinja.copy(painotetutArvosanat =
              Seq(
                PainotettuOppiaine(
                  Some(
                    OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa_xxxx"), kieli = Some("kieli_xx"))
                  ),
                  Some(1.0)
                )
              )
            )
          )
        )
      )
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto)
    passesValidation(
      hk,
      Some(hk)
    )
  }

  it should "succeed when ValintaperusteenValintakokeidenLisatilaisuudet not changed in modify operation, eventhough unknown osoiteKoodiUri" in {
    val metadata = maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet =
      Seq(
        ValintakokeenLisatilaisuudet(
          id = Some(valintaperusteenValintakoeId1),
          tilaisuudet = Seq(Valintakoetilaisuus(osoite = Some(Osoite(postinumeroKoodiUri = Some("posti_12345#1")))))
        )
      )
    )
    passesValidation(
      initMockSeq(maxWithIds.copy(tila = Tallennettu, metadata = Some(metadata))),
      Some(maxWithIds.copy(metadata = Some(metadata)))
    )
  }

  it should "succeed when ToinenAsteOnkoKaksoistutkinto not changed in modify operation, eventhough illegal koulutus-koodiurit" in {
    val hk = maxWithIds.copy(
      nimi = Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
      toteutusOid = ToteutusOid("1.2.246.562.17.456"),
      toinenAsteOnkoKaksoistutkinto = Some(true)
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto, Seq("koulutus_301104#1"))
    passesValidation(hk, Some(hk))
  }

  it should "succeed when lukiokohde nimi not changed in modify operation, eventhough illegal name" in {
    val lkHakukohde = maxWithIds.copy(
      nimi = Map(Fi -> "Nimi fi", Sv -> "Nimi sv"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja)))
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    passesValidation(lkHakukohde, Some(lkHakukohde))
  }

  it should "fail when invalid perustiedot" in {
    failsValidation(
      max.copy(oid = Some(HakukohdeOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(HakukohdeOid("1.2.3"))))
      )
    )
    failsValidation(min.copy(nimi = Map(), kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
    failsValidation(min.copy(hakuOid = HakuOid("2.3.4")), "hakuOid", validationMsg("2.3.4"))
    failsValidation(min.copy(toteutusOid = ToteutusOid("3.4.5")), "toteutusOid", validationMsg("3.4.5"))
  }

  it should "fail when oid not given for modified hakukohde" in {
    Try(
      validator.withValidation(maxWithIds.copy(oid = None), Some(maxWithIds), authenticatedNonPaakayttaja)(hk => hk)
    ) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs Seq(ValidationError("oid", missingMsg))
      case _ => fail("Expecting validation failure, but it succeeded")
    }
  }

  it should "fail when hakuajat taken from haku, but still defined in hakukohde as well" in {
    failsValidation(
      max.copy(
        hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        kaytetaanHaunAikataulua = Some(true)
      ),
      "hakuajat",
      noneOrOneNotBoth("kaytetaanHaunAikataulua", "hakuajat")
    )
  }

  it should "fail if invalid hakuajat" in {
    val ajanJakso = Ajanjakso(alkaa = now(), paattyy = Some(inPast()))
    failsValidation(min.copy(hakuajat = List(ajanJakso)), "hakuajat[0]", invalidAjanjaksoMsg(ajanJakso))
  }

  it should "fail when invalid pohjakoulutusKoodiUrit" in {
    failsValidation(
      min.copy(pohjakoulutusvaatimusKoodiUrit = Seq("puppu", "pohjakoulutusvaatimuskouta_XX#1")),
      Seq(
        ValidationError("pohjakoulutusvaatimusKoodiUrit[0]", invalidPohjakoulutusVaatimusKoodiuri("puppu")),
        ValidationError(
          "pohjakoulutusvaatimusKoodiUrit[1]",
          invalidPohjakoulutusVaatimusKoodiuri("pohjakoulutusvaatimuskouta_XX#1")
        )
      )
    )
  }

  it should "fail when common liitteidenToimitusaika defined but not used" in {
    failsValidation(
      min.copy(liitteetOnkoSamaToimitusaika = Some(false), liitteidenToimitusaika = Some(inFuture())),
      "liitteidenToimitusaika",
      notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusaika")
    )
  }

  it should "fail when common liitteidenToimitusaika used but not defined" in {
    failsValidation(
      max.copy(liitteetOnkoSamaToimitusaika = Some(true), liitteidenToimitusaika = None),
      "liitteidenToimitusaika",
      missingMsg
    )
  }

  it should "fail when common liitteidenToimitustapa defined but not used" in {
    failsValidation(
      min.copy(liitteetOnkoSamaToimitusosoite = Some(false), liitteidenToimitustapa = Some(MuuOsoite)),
      "liitteidenToimitustapa",
      notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusosoite")
    )
  }

  it should "fail when common liitteidenToimitusosoite defined but not used" in {
    failsValidation(
      min.copy(
        liitteetOnkoSamaToimitusosoite = Some(false),
        liitteidenToimitusosoite = Some(LiitteenToimitusosoite1)
      ),
      "liitteidenToimitusosoite",
      notEmptyAlthoughBooleanFalseMsg("liitteetOnkoSamaToimitusosoite")
    )
  }

  it should "fail when common liitteidenToimitusosoite used but liitteidenToimitustapa not defined" in {
    failsValidation(
      max.copy(liitteetOnkoSamaToimitusosoite = Some(true), liitteidenToimitustapa = None),
      "liitteidenToimitustapa",
      missingMsg
    )
  }

  it should "fail when common liitteidenToimitusosoite used but not defined" in {
    failsValidation(
      max.copy(
        liitteetOnkoSamaToimitusosoite = Some(true),
        liitteidenToimitustapa = Some(MuuOsoite),
        liitteidenToimitusosoite = None
      ),
      "liitteidenToimitusosoite",
      missingMsg
    )
  }

  it should "fail when common liitteidenToimitusosoite given but toimitustapa not 'Muu osoite'" in {
    failsValidation(
      min.copy(
        liitteetOnkoSamaToimitusosoite = Some(true),
        liitteidenToimitustapa = Some(Lomake),
        liitteidenToimitusosoite = Some(LiitteenToimitusosoite1)
      ),
      "liitteidenToimitusosoite",
      notAllowedDueTo("liitteiden toimitustavaksi ei ole valittu 'muu osoite'")
    )
  }

  it should "fail when common liitteidenToimitusosoite was invalid" in {
    failsValidation(
      max.copy(
        liitteetOnkoSamaToimitusosoite = Some(true),
        liitteidenToimitustapa = Some(MuuOsoite),
        liitteidenToimitusosoite = Some(
          LiitteenToimitusosoite(
            osoite = Osoite(osoite = vainSuomeksi, postinumeroKoodiUri = None),
            sahkoposti = Some("puppu"),
            verkkosivu = Some("huttu")
          )
        )
      ),
      Seq(
        ValidationError("liitteidenToimitusosoite.osoite", kielistettyWoSvenska),
        ValidationError("liitteidenToimitusosoite.postinumeroKoodiUri", missingMsg),
        ValidationError("liitteidenToimitusosoite.sahkoposti", invalidEmail("puppu")),
        ValidationError("liitteidenToimitusosoite.verkkosivu", invalidUrl("huttu"))
      )
    )
  }

  it should "fail when hakulomake taken from haku, but still defined in hakukohde as well" in {
    failsValidation(
      max.copy(
        hakulomaketyyppi = Some(Ataru),
        kaytetaanHaunHakulomaketta = Some(true)
      ),
      "hakulomaketyyppi",
      noneOrOneNotBoth("kaytetaanHaunHakulomaketta", "hakulomaketyyppi")
    )
  }

  it should "fail when unknown ataruId" in {
    val unknownId = UUID.randomUUID()
    failsValidation(max.copy(hakulomakeAtaruId = Some(unknownId)), "hakulomakeAtaruId", unknownAtaruId(unknownId))
  }

  it should "fail when ataruId modified to unknown value" in {
    val originalId = UUID.randomUUID()
    val unknownId  = UUID.randomUUID()
    failsModifyValidation(
      maxWithIds.copy(hakulomakeAtaruId = Some(unknownId)),
      maxWithIds.copy(hakulomakeAtaruId = Some(originalId)),
      Seq(ValidationError("hakulomakeAtaruId", unknownAtaruId(unknownId)))
    )
  }

  it should "fail when values missing from julkaistu hakukohde" in {
    failsValidation(
      max.copy(
        jarjestyspaikkaOid = None,
        liitteetOnkoSamaToimitusaika = Some(true),
        liitteidenToimitusaika = None,
        liitteetOnkoSamaToimitusosoite = Some(true),
        liitteidenToimitustapa = Some(MuuOsoite),
        liitteidenToimitusosoite = None,
        pohjakoulutusvaatimusKoodiUrit = Seq(),
        pohjakoulutusvaatimusTarkenne = vainSuomeksi,
        muuPohjakoulutusvaatimus = vainSuomeksi,
        kaytetaanHaunAikataulua = None,
        kaytetaanHaunHakulomaketta = None
      ),
      Seq(
        ValidationError("jarjestyspaikkaOid", missingMsg),
        ValidationError("liitteidenToimitusaika", missingMsg),
        ValidationError("liitteidenToimitusosoite", missingMsg),
        ValidationError("pohjakoulutusvaatimusKoodiUrit", missingMsg),
        ValidationError("pohjakoulutusvaatimusTarkenne", kielistettyWoSvenska),
        ValidationError("muuPohjakoulutusvaatimus", kielistettyWoSvenska),
        ValidationError("kaytetaanHaunAikataulua", missingMsg),
        ValidationError("kaytetaanHaunHakulomaketta", missingMsg)
      )
    )
    failsValidation(
      max.copy(
        liitteetOnkoSamaToimitusosoite = Some(true),
        liitteidenToimitustapa = None,
        kaytetaanHaunAikataulua = Some(false),
        hakuajat = Seq(),
        kaytetaanHaunHakulomaketta = Some(false)
      ),
      Seq(
        ValidationError("liitteidenToimitustapa", missingMsg),
        ValidationError("hakuajat", missingMsg)
      )
    )
  }

  it should "fail if hakulomaketyyppi missing from julkaistu hakukohde while other hakulomake values given" in {
    failsValidation(
      max.copy(hakulomaketyyppi = None),
      Seq(
        ValidationError("hakulomakeAtaruId", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        ValidationError("hakulomaketyyppi", missingMsg)
      )
    )
  }

  "Liite validation" should "fail if id given for liite in new hakukohde" in {
    val liiteId = Some(UUID.randomUUID())
    failsValidation(
      max.copy(liitteet = List(Liite2.copy(id = liiteId))),
      "liitteet[0].id",
      notMissingMsg(liiteId)
    )
  }

  it should "fail if unknown id given for liite in modified hakukohde" in {
    val liiteId = Some(UUID.randomUUID())
    failsModifyValidation(
      max.copy(oid = Some(HakukohdeOid("1.2.246.562.20.0000000001")), liitteet = List(Liite2.copy(id = liiteId))),
      max.copy(liitteet = List(Liite2.copy(id = Some(UUID.randomUUID())))),
      Seq(
        ValidationError("liitteet[0].id", unknownLiiteId(liiteId.get.toString))
      )
    )
  }

  it should "fail if invalid toimitusosoite" in {
    failsValidation(
      max.copy(liitteet =
        List(Liite2.copy(toimitusosoite = Some(LiitteenToimitusosoite(osoite = Osoite1.copy(osoite = Map())))))
      ),
      "liitteet[0].toimitusosoite.osoite",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  it should "fail if invalid tyyppiKoodiUri" in {
    failsValidation(
      max.copy(liitteet =
        List(Liite1.copy(tyyppiKoodiUri = Some("puppu")), Liite2.copy(tyyppiKoodiUri = Some("liitetyypitamm_99#1")))
      ),
      Seq(
        ValidationError("liitteet[0].tyyppiKoodiUri", invalidLiitetyyppiKoodiuri("puppu")),
        ValidationError("liitteet[1].tyyppiKoodiUri", invalidLiitetyyppiKoodiuri("liitetyypitamm_99#1"))
      )
    )
  }

  it should "fail if values missing from julkaistu hakukohde" in {
    failsValidation(
      max.copy(liitteet =
        List(
          Liite1.copy(toimitustapa = Some(MuuOsoite)),
          Liite2.copy(nimi = Map(Fi -> "Vain suomeksi", Sv -> ""), kuvaus = Map(Fi -> "", Sv -> "Bara svenska"))
        )
      ),
      Seq(
        ValidationError("liitteet[0].toimitusosoite", missingMsg),
        ValidationError("liitteet[1].nimi", invalidKielistetty(Seq(Sv))),
        ValidationError("liitteet[1].kuvaus", invalidKielistetty(Seq(Fi)))
      )
    )
  }

  "Valintakoe validation" should "fail if invalid tyyppiKoodiUri" in {
    failsValidation(
      max.copy(valintakokeet = List(Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_99#1")))),
      "valintakokeet[0].tyyppiKoodiUri",
      invalidValintakoeTyyppiKoodiuri("valintakokeentyyppi_99#1")
    )
  }

  "Metadata validation" should "fail when kaytetaanHaunAlkamiskautta used but koulutuksenAlkamiskausi defined as well" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(kaytetaanHaunAlkamiskautta = Some(true)))),
      "metadata.koulutuksenAlkamiskausi",
      oneNotBoth("kaytetaanHaunAlkamiskautta", "koulutuksenAlkamiskausi")
    )
  }

  it should "fail when invalid koulutuksenAlkamiskausi" in {
    failsValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(koulutuksenAlkamiskausi =
            Some(
              KoulutuksenAlkamiskausi(
                alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
                koulutuksenAlkamispaivamaara = Some(inFuture(2000)),
                koulutuksenPaattymispaivamaara = Some(inFuture(1000))
              )
            )
          )
        )
      ),
      "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara",
      InvalidKoulutuspaivamaarat
    )
  }

  it should "fail when invalid aloituspaikat" in {
    failsValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(aloituspaikat =
            Some(Aloituspaikat(lukumaara = Some(10), ensikertalaisille = Some(-5), kuvaus = Map()))
          )
        )
      ),
      "metadata.aloituspaikat.ensikertalaisille",
      notNegativeMsg
    )
  }

  it should "fail if invalid uudenOpiskelijanUrl" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(uudenOpiskelijanUrl = Map(Fi -> "puppu", Sv -> "huttu")))),
      Seq(
        ValidationError("metadata.uudenOpiskelijanUrl.fi", invalidUrl("puppu")),
        ValidationError("metadata.uudenOpiskelijanUrl.sv", invalidUrl("huttu"))
      )
    )
  }

  it should "fail if values missing from julkaistu hakukohde" in {
    failsValidation(
      max.copy(metadata =
        Some(
          HakukohdeMetadata(
            valintakokeidenYleiskuvaus = vainSuomeksi,
            kynnysehto = vainSuomeksi,
            koulutuksenAlkamiskausi = None,
            kaytetaanHaunAlkamiskautta = Some(false),
            aloituspaikat = None,
            isMuokkaajaOphVirkailija = Some(false)
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutuksenAlkamiskausi",
          oneNotBoth("kaytetaanHaunAlkamiskautta", "koulutuksenAlkamiskausi")
        ),
        ValidationError("metadata.valintakokeidenYleiskuvaus", kielistettyWoSvenska),
        ValidationError("metadata.kynnysehto", kielistettyWoSvenska),
        ValidationError("metadata.aloituspaikat", missingMsg)
      )
    )
  }

  private def initMockSeqForHaku(haku: Haku, hakuOid: String): Unit = {
    when(hakuDao.get(HakuOid(hakuOid), TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Some(haku, ZonedDateTime.now().toInstant))
  }

  "Dependency validation" should "fail when toteutus not existing" in {
    val hk = max.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.456"),
      valintaperusteId = None,
      metadata = Some(maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )
    when(hakukohdeDao.getDependencyInformation(hk)).thenAnswer(None)
    failsValidation(hk, "toteutusOid", nonExistent("Toteutusta", hk.toteutusOid))
  }

  it should "fail when toteutus not julkaistu" in {
    val hk = max.copy(toteutusOid = ToteutusOid("1.2.246.562.17.456"))
    initMockDepsWithToteutusOidAndTila(hk, "1.2.246.562.17.456", Tallennettu)
    failsValidation(hk, "tila", notYetJulkaistu("Toteutusta", hk.toteutusOid))
  }

  it should "fail when toteutus poistettu" in {
    val hk = max.copy(toteutusOid = ToteutusOid("1.2.246.562.17.456"))
    initMockDepsWithToteutusOidAndTila(hk, "1.2.246.562.17.456", Poistettu)
    failsValidation(hk, "toteutusOid", nonExistent("Toteutusta", hk.toteutusOid))
  }

  it should "fail when jarjestyspaikkaOID not exist in tarjoaja-list" in {
    val hk = max.copy(jarjestyspaikkaOid = Some(UnknownOid))
    failsValidation(initMockSeq(hk), "jarjestyspaikkaOid", invalidJarjestyspaikkaOid(UnknownOid, hk.toteutusOid))
  }

  it should "fail when haku not existing" in {
    val hk = max.copy(hakuOid = HakuOid("1.2.246.562.29.456"))
    when(hakuDao.get(HakuOid("1.2.246.562.29.456"), TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(None)
    failsValidation(initMockSeq(hk), "hakuOid", nonExistent("Hakua", hk.hakuOid))
  }

  it should "fail when haku poistettu" in {
    val hk = max.copy(hakuOid = HakuOid("1.2.246.562.29.456"))
    initMockSeqForHaku(haku.copy(tila = Poistettu), "1.2.246.562.29.456")
    failsValidation(initMockSeq(hk), "hakuOid", nonExistent("Hakua", hk.hakuOid))
  }

  it should "fail when valintaperuste not existing" in {
    val hk = max.copy(
      valintaperusteId = Some(valintaperusteId2),
      metadata = Some(maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )
    when(hakukohdeDao.getDependencyInformation(hk)).thenAnswer(Some(dependencies.copy(valintaperuste = None)))
    failsValidation(hk, "valintaperusteId", nonExistent("Valintaperustetta", valintaperusteId2))
  }

  it should "fail when valintaperuste not julkaistu" in {
    val hk = max.copy(
      valintaperusteId = Some(valintaperusteId2),
      metadata = Some(maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )
    initMockDepsWithValintaperusteParams(hk, valintaperusteId2, Tallennettu)
    failsValidation(hk, "tila", notYetJulkaistu("Valintaperustetta", hk.valintaperusteId.get))
  }

  it should "fail when valintaperuste poistettu" in {
    val hk = max.copy(
      valintaperusteId = Some(valintaperusteId2),
      metadata = Some(maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )
    initMockDepsWithValintaperusteParams(hk, valintaperusteId2, Poistettu)
    failsValidation(hk, "valintaperusteId", nonExistent("Valintaperustetta", hk.valintaperusteId.get))
  }

  it should "fail when toteutus and valintaperuste have different koulutustyyppi" in {
    val hk = max.copy(
      valintaperusteId = Some(valintaperusteId2),
      metadata = Some(maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq()))
    )
    initMockDepsWithValintaperusteParams(hk, valintaperusteId2, Julkaistu, Lk)
    failsValidation(
      hk,
      "valintaperusteId",
      tyyppiMismatch("Toteutuksen", hk.toteutusOid, "valintaperusteen", valintaperusteId2)
    )
  }

  it should "fail for oppilaitosvirkailija when hakukohteen liittämisen takaraja has expired" in {
    val hk       = max.copy(hakuOid = HakuOid("1.2.246.562.29.456"))
    val takaraja = inPast(100)
    initMockSeqForHaku(haku.copy(hakukohteenLiittamisenTakaraja = Some(takaraja)), "1.2.246.562.29.456")
    failsValidation(initMockSeq(hk), "hakukohteenLiittamisenTakaraja", pastDateMsg(takaraja))
  }

  it should "fail for oppilaitosvirkailija when hakukohteen muokkaamisen takaraja has expired" in {
    val hk       = maxWithIds.copy(hakuOid = HakuOid("1.2.246.562.29.456"))
    val takaraja = inPast(100)
    initMockSeqForHaku(
      haku.copy(hakukohteenLiittamisenTakaraja = Some(inPast(200)), hakukohteenMuokkaamisenTakaraja = Some(takaraja)),
      "1.2.246.562.29.456"
    )
    failsModifyValidation(
      initMockSeq(hk),
      hk,
      Seq(ValidationError("hakukohteenMuokkaamisenTakaraja", pastDateMsg(takaraja)))
    )
  }

  it should "fail if nimi given for amm-yhteishakukohde" in {
    failsValidation(
      initMockSeq(
        max.copy(
          hakuOid = yhteisHakuHakuOid,
          hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_01#1")
        )
      ),
      "nimi",
      notEmptyMsg
    )
  }

  it should "fail if hakukohdeKoodiUri not given for amm-yhteishakukohde" in {
    failsValidation(
      initMockSeq(
        max.copy(
          hakuOid = yhteisHakuHakuOid,
          nimi = Map(),
          hakukohdeKoodiUri = None
        )
      ),
      "hakukohdeKoodiUri",
      missingMsg
    )
  }

  it should "fail when invalid hakukohdeKoodiUri for amm-yhteishakukohde with ammatillinenPerustutkintoErityisopetuksena " in {
    val hk = max.copy(
      hakuOid = yhteisHakuHakuOid,
      nimi = Map(),
      hakukohdeKoodiUri = Some("hakukohteeterammatillinenerityisopetus_66#1")
    )
    initMockDepsForKoulutustyyppi(
      hk,
      AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = Some(true))
    )
    failsValidation(
      hk,
      "hakukohdeKoodiUri",
      invalidHakukohdeKoodiuri("hakukohteeterammatillinenerityisopetus_66#1", HakukohdeAmmErityisopetusKoodisto.toString)
    )
  }

  it should "fail when invalid hakukohdeKoodiUri for amm-yhteishakukohde" in {
    failsValidation(
      initMockSeq(
        max.copy(
          hakuOid = yhteisHakuHakuOid,
          nimi = Map(),
          hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_66#1")
        )
      ),
      "hakukohdeKoodiUri",
      invalidHakukohdeKoodiuri("hakukohteetperusopetuksenjalkeinenyhteishaku_66#1", HakukohdePoJalkYhteishakuKoodisto.toString)
    )
  }

  it should "fail when new tuva-hakukohde with nimi not matching toteutus" in {
    val hk = max.copy(nimi = Map(Fi -> "pupppu fi", Sv -> "puppu sv"))
    initMockDepsForKoulutustyyppi(hk, TuvaToteutuksenMetatieto)
    failsValidation(
      hk,
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("Toteutus fi", "toteutuksella")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("Toteutus sv", "toteutuksella"))
      )
    )
  }

  private def assertFailuresForHakukohdeKoodiUriInsteadOfNimi(
      toteutusMetadata: ToteutusMetadata,
      hkLinja: Option[HakukohteenLinja] = None
  ): Assertion = {
    val hk = max.copy(
      nimi = Map(),
      hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_01#1"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = hkLinja))
    )
    initMockDepsForKoulutustyyppi(hk, toteutusMetadata)
    failsValidation(
      hk,
      Seq(
        ValidationError("nimi", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("hakukohdeKoodiUri", notMissingMsg(Some("hakukohteetperusopetuksenjalkeinenyhteishaku_01#1")))
      )
    )
  }

  it should "fail when hakukohdeKoodiUri given instead of nimi" in {
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(TuvaToteutuksenMetatieto)
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(AmmTutkinnonOsaToteutusMetadataHakemuspalvelu)
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(AmmOsaamisalaToteutusMetadataHakemuspalvelu)
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(
      VapaaSivistystyoMuuToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru))
    )
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(AmmMuuToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru)))
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(
      AikuistenPerusopetusToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru))
    )
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(
      AikuistenPerusopetusToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru))
    )
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(LukioToteutuksenMetatieto, Some(LukioHakukohteenLinja))
    assertFailuresForHakukohdeKoodiUriInsteadOfNimi(YoToteutuksenMetatieto)
  }

  private def assertFailureForLomaketyyppiNotAtaru(toteutusMetadata: ToteutusMetadata): Assertion = {
    val hk = max.copy(toteutusOid = ToteutusOid("1.2.246.562.17.456"))
    initMockDepsForKoulutustyyppi(hk, toteutusMetadata)
    failsValidation(hk, "toteutusOid", cannotLinkToHakukohde(hk.toteutusOid.s))
  }

  it should "fail when hakulomaketyyppi not Ataru for AmmatillinenTutkinnonOsa-toteutus" in {
    assertFailureForLomaketyyppiNotAtaru(AmmTutkinnonOsaToteutus.metadata.get)
  }

  it should "fail when hakulomaketyyppi not Ataru for AmmatillinenOsaamisala-toteutus" in {
    assertFailureForLomaketyyppiNotAtaru(AmmOsaamisalaToteutus.metadata.get)
  }

  it should "fail when hakulomaketyyppi not Ataru for AmmMuu-toteutus" in {
    assertFailureForLomaketyyppiNotAtaru(AmmMuuToteutusMetatieto)
  }

  it should "fail when hakulomaketyyppi not Ataru for VapaaSivistystyoMuu-toteutus" in {
    assertFailureForLomaketyyppiNotAtaru(VapaaSivistystyoMuuToteutusMetatieto)
  }

  it should "fail when hakulomaketyyppi not Ataru for AikuistenPerusopetus-toteutus" in {
    assertFailureForLomaketyyppiNotAtaru(AikuistenPerusopetusToteutusMetatieto)
  }

  it should "fail when valintaperusteId not defined for toisen asteen yhteishaku" in {
    val hk = max.copy(
      hakuOid = yhteisHakuHakuOid,
      nimi = Map(Fi -> "Lukion yleislinja", Sv -> "Gymnasium allmän linje"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja))),
      valintaperusteId = None
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto)
    failsValidation(hk, "valintaperusteId", missingMsg)
  }

  it should "fail when hakukohde specific hakulomake used but hakulomaketyyppi of haku not 'Muu'" in {
    val hk = max.copy(
      hakuOid = HakuOid("1.2.246.562.29.456"),
      kaytetaanHaunHakulomaketta = Some(false)
    )
    initMockSeqForHaku(
      haku.copy(hakulomaketyyppi = Some(Ataru)),
      "1.2.246.562.29.456"
    )
    failsValidation(initMockSeq(hk), "hakuOid", illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi)
  }

  it should "fail when kaksoistutkinto selected but not allowed" in {
    val hk = max.copy(
      nimi = Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
      toteutusOid = ToteutusOid("1.2.246.562.17.456"),
      toinenAsteOnkoKaksoistutkinto = Some(true)
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto, Seq("koulutus_301104#1"))
    failsValidation(hk, "toinenAsteOnkoKaksoistutkinto", toinenAsteOnkoKaksoistutkintoNotAllowed)
  }

  "Lukio-hakukohde validation" should "fail when hakukohteenLinja not defined for Lukio-toteutus" in {
    val hk = max.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.456"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = None))
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto, Seq("koulutus_301101#1"))
    failsValidation(hk, "metadata.hakukohteenLinja", missingMsg)
  }

  private def assertFailuresForHkLinjaForNonLukio(
      toteutusMetadata: ToteutusMetadata,
      nimi: Kielistetty = Map(Fi -> "nimi", Sv -> "nimi sv"),
      hakukohdeKoodiUri: Option[String] = None
  ): Assertion = {
    val hk = max.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.456"),
      hakuOid = yhteisHakuHakuOid,
      nimi = nimi,
      hakukohdeKoodiUri = hakukohdeKoodiUri,
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja)))
    )
    initMockDepsForKoulutustyyppi(hk, toteutusMetadata)
    failsValidation(hk, "metadata.hakukohteenLinja", notMissingMsg(Some(LukioHakukohteenLinja)))
  }

  it should "fail when hakukohteenLinja defined for other than Lukio-toteutus" in {
    assertFailuresForHkLinjaForNonLukio(
      AmmToteutuksenMetatieto,
      Map(),
      Some("hakukohteetperusopetuksenjalkeinenyhteishaku_01#1")
    )
    assertFailuresForHkLinjaForNonLukio(
      TuvaToteutuksenMetatieto,
      Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv")
    )
    assertFailuresForHkLinjaForNonLukio(AmmTutkinnonOsaToteutusMetadataHakemuspalvelu)
    assertFailuresForHkLinjaForNonLukio(AmmOsaamisalaToteutusMetadataHakemuspalvelu)
    assertFailuresForHkLinjaForNonLukio(AmmMuuToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru)))
    assertFailuresForHkLinjaForNonLukio(VapaaSivistystyoMuuToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru)))
    assertFailuresForHkLinjaForNonLukio(AmmOsaamisalaToteutusMetadataHakemuspalvelu)
    assertFailuresForHkLinjaForNonLukio(AikuistenPerusopetusToteutusMetatieto.copy(hakulomaketyyppi = Some(Ataru)))
    assertFailuresForHkLinjaForNonLukio(YoToteutuksenMetatieto)
  }

  it should "fail when hakukohteenLinja defined for lukio DIA- or EB -koulutus" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "Toteutus fi", Sv -> "Toteutus sv"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja)))
    )
    initMockDepsForKoulutustyyppi(lkHakukohde, LukioToteutuksenMetatieto, Seq("koulutus_301104#1"))
    failsValidation(lkHakukohde, "metadata.hakukohteenLinja", notMissingMsg(Some(LukioHakukohteenLinja)))
  }

  it should "fail when invalid name for lukio DIA- or EB -koulutus" in {
    val lkHakukohde = max.copy(nimi = Map(Fi -> "nimi fi", Sv -> "nimi sv"))
    initMockDepsForKoulutustyyppi(lkHakukohde, LukioToteutuksenMetatieto, Seq("koulutus_301104#1"))
    failsValidation(
      lkHakukohde,
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("Toteutus fi", "toteutuksella")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("Toteutus sv", "toteutuksella"))
      )
    )
  }

  it should "fail when invalid name for lukio-hakukohde with yleislinja" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
      metadata = Some(maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja)))
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    failsValidation(
      lkHakukohde,
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("Lukion yleislinja", "toteutuksen yleislinjalla")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("Gymnasium allmän linje", "toteutuksen yleislinjalla"))
      )
    )
  }

  it should "fail when invalid name for lukio-hakukohde with lukiopainotukset" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = Some("lukiopainotukset_1#1"))))
      )
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    failsValidation(
      lkHakukohde,
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("painotus", "hakukohteen linjalla")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("painotus sv", "hakukohteen linjalla"))
      )
    )
  }

  it should "fail when invalid name for lukio-hakukohde with erityisetKoulutustehtavat" in {
    val lkHakukohde = max.copy(
      nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja =
          Some(LukioHakukohteenLinja.copy(linja = Some("lukiolinjaterityinenkoulutustehtava_1#1")))
        )
      )
    )
    when(hakukohdeDao.getDependencyInformation(lkHakukohde)).thenAnswer(Some(lukioDependencies))
    failsValidation(
      lkHakukohde,
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("erityistehtävä", "hakukohteen linjalla")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("erityistehtävä sv", "hakukohteen linjalla"))
      )
    )
  }

  it should "fail when koodisto-service query failed when checking hakukohde name" in {
    val lkHakukohde = max.copy(
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = Some("failure"))))
      )
    )
    initMockDepsForKoulutustyyppi(
      lkHakukohde,
      LukioToteutuksenMetatieto.copy(painotukset = List(LukiolinjaTieto(koodiUri = "failure", Map())))
    )
    failsValidation(lkHakukohde, "metadata.hakukohteenLinja.linja", koodistoServiceFailureMsg)
  }

  it should "fail when invalid HakukohteenLinja" in {
    val hk = max.copy(
      metadata = Some(
        maxMetadata.copy(hakukohteenLinja =
          Some(
            HakukohteenLinja(
              linja = Some("höttö"),
              alinHyvaksyttyKeskiarvo = Some(-8.2),
              lisatietoa = vainSuomeksi,
              painotetutArvosanat = Seq(
                PainotettuOppiaine(None, None),
                PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(None, None)), Some(-2.8)),
                PainotettuOppiaine(
                  Some(OppiaineKoodiUrit(Some("puppu"), Some("huttu"))),
                  Some(2.5)
                ),
                PainotettuOppiaine(
                  Some(OppiaineKoodiUrit(Some("painotettavatoppiaineetlukiossa_XX"), Some("kieli_XX"))),
                  Some(2.8)
                )
              )
            )
          )
        )
      ),
      nimi = Map(Fi -> "Lukio", Sv -> "Gymnasium")
    )
    initMockDepsForKoulutustyyppi(hk, LukioToteutuksenMetatieto)
    failsValidation(
      hk,
      Seq(
        ValidationError("metadata.hakukohteenLinja.linja", invalidHakukohteenLinja("höttö")),
        ValidationError("metadata.hakukohteenLinja.alinHyvaksyttyKeskiarvo", notNegativeMsg),
        ValidationError("metadata.hakukohteenLinja.lisatietoa", kielistettyWoSvenska),
        ValidationError("metadata.hakukohteenLinja.painotetutArvosanat[0].koodiUrit", missingMsg),
        ValidationError("metadata.hakukohteenLinja.painotetutArvosanat[0].painokerroin", missingMsg),
        ValidationError("metadata.hakukohteenLinja.painotetutArvosanat[1].oppiaine", missingMsg),
        ValidationError("metadata.hakukohteenLinja.painotetutArvosanat[1].painokerroin", notNegativeMsg),
        ValidationError("metadata.hakukohteenLinja.painotetutArvosanat[2].oppiaine", invalidOppiaineKoodiuri("puppu")),
        ValidationError(
          "metadata.hakukohteenLinja.painotetutArvosanat[2].kieli",
          invalidOppiaineKieliKoodiuri("huttu")
        ),
        ValidationError(
          "metadata.hakukohteenLinja.painotetutArvosanat[3].oppiaine",
          invalidOppiaineKoodiuri("painotettavatoppiaineetlukiossa_XX")
        ),
        ValidationError(
          "metadata.hakukohteenLinja.painotetutArvosanat[3].kieli",
          invalidOppiaineKieliKoodiuri("kieli_XX")
        )
      )
    )
  }

  "ValintaperusteenValintakokeidenLisatilaisuudet validation" should "fail when valintaperusteId not defined" in {
    failsValidation(initMockSeq(max.copy(valintaperusteId = None)), "valintaperusteId", missingMsg)
  }

  it should "fail when koetilaisuusId not defined" in {
    failsValidation(
      initMockSeq(
        max.copy(metadata =
          Some(
            maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = List(ValintakokeenLisatilaisuudet1))
          )
        )
      ),
      "metadata.valintaperusteenValintakokeidenLisatilaisuudet[0].id",
      missingMsg
    )
  }

  it should "fail when unknown koetilaisuusId" in {
    val unknownId = UUID.randomUUID()
    failsValidation(
      initMockSeq(
        max.copy(metadata =
          Some(
            maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet =
              List(ValintakokeenLisatilaisuudet1.copy(id = Some(unknownId)))
            )
          )
        )
      ),
      "metadata.valintaperusteenValintakokeidenLisatilaisuudet[0].id",
      unknownValintaperusteenValintakoeIdForHakukohde(valintaperusteId, unknownId)
    )
  }

  it should "fail when invalid ajanjakso for tilaisuus" in {
    val ajanjakso = Ajanjakso(inFuture(1000), Some(inFuture(500)))
    failsValidation(
      initMockSeq(
        max.copy(metadata =
          Some(
            maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet =
              List(
                ValintakokeenLisatilaisuudet1.copy(
                  id = Some(valintaperusteenValintakoeId1),
                  tilaisuudet = List(ValintakokeenLisatilaisuudet1.tilaisuudet.head.copy(aika = Some(ajanjakso)))
                )
              )
            )
          )
        )
      ),
      "metadata.valintaperusteenValintakokeidenLisatilaisuudet[0].tilaisuudet[0].aika",
      invalidAjanjaksoMsg(ajanjakso)
    )
  }

  "Validate on julkaisu" should "fail if hakuajat not in future" in {
    failsValidation(
      initMockSeq(max.copy(hakuajat = Seq(Ajanjakso(inFuture(1000), None), inPastJakso))),
      Seq(
        ValidationError("hakuajat[0].paattyy", missingMsg),
        ValidationError("hakuajat[1].paattyy", pastDateMsg(inPastAikaleima))
      )
    )
  }

  it should "fail if liitteiden yhteinen toimitusaika not in future" in {
    failsValidation(
      initMockSeq(max.copy(liitteidenToimitusaika = Some(inPastAikaleima))),
      "liitteidenToimitusaika",
      pastDateMsg(inPastAikaleima)
    )
  }

  it should "fail if toimitusaika for an individual liite not in future" in {
    failsValidation(
      initMockSeq(max.copy(liitteet = List(Liite1.copy(toimitusaika = Some(inPastAikaleima))))),
      "liitteet[0].toimitusaika",
      pastDateMsg(inPastAikaleima)
    )
  }

  it should "fail if ajanjakso for valintakoetilaisuus not in future" in {
    failsValidation(
      initMockSeq(
        max.copy(valintakokeet = inPastValintakokeet)
      ),
      "valintakokeet[0].tilaisuudet[0].aika.paattyy",
      pastDateMsg(inPastAikaleima)
    )
  }

  it should "fail if koulutuksenAlkamiskausi not in future" in {
    val metadata = Some(maxMetadata.copy(koulutuksenAlkamiskausi = Some(inPastKoulutuksenAlkamiskausi)))
    failsValidation(
      initMockSeq(max.copy(metadata = metadata)),
      "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi",
      pastDateMsg(inPastKoulutuksenAlkamiskausi.koulutuksenAlkamisvuosi.get)
    )
  }

  it should "fail if valintakokeen lisatilaisuus not in future" in {
    val metadata = Some(
      maxMetadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = inPastLisaTilaisuudet(None))
    )
    failsValidation(
      initMockSeq(max.copy(metadata = metadata)),
      "metadata.valintaperusteenValintakokeidenLisatilaisuudet[0].tilaisuudet[0].aika.paattyy",
      pastDateMsg(inPastAikaleima)
    )
  }

  it should "succeed when state changes from arkistoitu to julkaistu, eventhough timestamps not in future" in {
    val hakukohde = maxWithIds.copy(
      hakuajat = Seq(Ajanjakso(inFuture(1000), None), inPastJakso),
      liitteidenToimitusaika = Some(inPast()),
      valintakokeet = inPastValintakokeet,
      metadata = Some(
        maxMetadata.copy(
          koulutuksenAlkamiskausi = Some(inPastKoulutuksenAlkamiskausi),
          valintaperusteenValintakokeidenLisatilaisuudet = inPastLisaTilaisuudet(Some(valintaperusteenValintakoeId1))
        )
      )
    )
    passesValidation(initMockSeq(hakukohde), Some(maxWithIds.copy(tila = Arkistoitu)))
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(initMockSeq(maxWithIds), Some(maxWithIds.copy(tila = Tallennettu)))
  }
  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(initMockSeq(maxWithIds.copy(tila = Arkistoitu)), Some(maxWithIds))
  }
  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(initMockSeq(maxWithIds), Some(maxWithIds.copy(tila = Arkistoitu)))
  }
  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(initMockSeq(maxWithIds.copy(tila = Tallennettu)), Some(maxWithIds))
  }
  it should "succeed from tallennettu to poistettu" in {
    passesValidation(initMockSeq(maxWithIds.copy(tila = Poistettu)), Some(maxWithIds.copy(tila = Tallennettu)))
  }

  def failStageChangeValidation(newTila: Julkaisutila, oldTila: Julkaisutila): Assertion =
    Try(
      validator.withValidation(maxWithIds.copy(tila = newTila), Some(maxWithIds.copy(tila = oldTila)))(e => e)
    ) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs Seq(
          ValidationError("tila", illegalStateChange("hakukohteelle", oldTila, newTila))
        )
      case _ => fail("Expecting illegalStateChange, but it succeeded")
    }

  it should "fail from tallennettu to arkistoitu" in {
    failStageChangeValidation(Arkistoitu, Tallennettu)
  }

  it should "fail from arkistoitu to tallennettu" in {
    failStageChangeValidation(Tallennettu, Arkistoitu)
  }

  it should "fail from julkaistu to poistettu" in {
    failStageChangeValidation(Poistettu, Julkaistu)
  }

  it should "fail from arkistoitu to poistettu" in {
    failStageChangeValidation(Poistettu, Arkistoitu)
  }

  it should "fail from poistettu to tallennettu" in {
    failStageChangeValidation(Tallennettu, Poistettu)
  }
}
