package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture._
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.util.TimeUtils.{instantToModified, modifiedToInstant, renderHttpDate}
import org.json4s.jackson.Serialization.read

import java.time.{Duration, Instant, LocalDateTime, ZoneId}
import java.util.UUID
import scala.util.Success

class KoulutusSpec
    extends KoutaIntegrationSpec
    with AccessControlSpec
    with KoulutusFixture
    with ToteutusFixture
    with SorakuvausFixture
    with UploadFixture {

  override val roleEntities: Seq[RoleEntity] = Seq(Role.Koulutus)

  val ophKoulutus: Koulutus =
    koulutus.copy(tila = Julkaistu, organisaatioOid = OphOid, tarjoajat = List(), julkinen = true)

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockKoodiUriVersionFailure("koulutus_111111", 11)
    mockTutkinnonOsatFailure(111111)
    mockOsaamisalaKoodiUritFailure(111111)
  }

  "Get koulutus by oid" should "return 404 if koulutus not found" in {
    get(s"$KoulutusPath/123", headers = defaultHeaders) {
      status should equal(404)
      body should include("Unknown koulutus oid")
    }
  }

  it should "return 401 without a session" in {
    get(s"$KoulutusPath/123", headers = Map.empty) {
      status should equal(401)
    }
  }

  it should "allow the user of the koulutus organization to read the koulutus" in {
    val oid = put(koulutus, ophSession)
    get(oid, crudSessions(ChildOid), koulutus(oid))
  }

  it should "deny an authenticated user without organization access to access unpublished koulutus" in {
    val oid = put(koulutus, ophSession)
    get(s"$KoulutusPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow the user of a tarjoaja organization without access to the koulutus organization to read the koulutus" in {
    val oid = put(koulutus.copy(tarjoajat = List(LonelyOid)), ophSession)
    get(oid, crudSessions(LonelyOid), koulutus(oid).copy(tarjoajat = List(LonelyOid)))
  }

  it should "allow the user of a parent organization to read the koulutus" in {
    val oid = put(koulutus, ophSession)
    get(oid, crudSessions(ParentOid), koulutus(oid))
  }

  it should "allow the user of a child organization to read the koulutus" in {
    val oid = put(koulutus, ophSession)
    get(oid, crudSessions(GrandChildOid), koulutus(oid))
  }

  it should "allow the user of proper koulutustyyppi to read julkinen koulutus" in {
    val oid = put(ophKoulutus, ophSession)
    get(oid, readSessions(AmmOid), ophKoulutus.copy(Some(KoulutusOid(oid))))
  }

  it should "deny the user of a wrong role to read the koulutus" in {
    val oid = put(koulutus, ophSession)
    get(s"$KoulutusPath/$oid", otherRoleSession, 403)
  }

  it should "allow the indexer to read any koulutus" in {
    val oid = put(koulutus, ophSession)
    get(s"$KoulutusPath/$oid", indexerSession, 200)
  }

  it should "allow a user of other organization but similar oppilaitostyyppi to access public koulutus" in {
    val oid = put(koulutus.copy(julkinen = true), ophSession)
    get(oid, readSessions(AmmOid), koulutus(oid).copy(julkinen = true, muokkaaja = OphUserOid))
  }

  it should "deny an authenticated user of a different oppilaitostyyppi to access public koulutus" in {
    val oid = put(koulutus.copy(julkinen = true), ophSession)
    get(s"$KoulutusPath/$oid", readSessions(YoOid), 403)
  }

  it should "return error when trying to get deleted koulutus" in {
    val oid = put(koulutus.copy(tila = Poistettu), ophSession)
    get(s"$KoulutusPath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted koulutus with myosPoistetut = true" in {
    val oid = put(koulutus.copy(tila = Poistettu), ophSession)
    get(s"$KoulutusPath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create koulutus" should "store koulutus" in {
    val oid = put(koulutus, ophSession)
    get(oid, koulutus(oid))
  }

  it should "read muokkaaja from the session" in {
    val oid      = put(koulutus.copy(muokkaaja = UserOid("random")), ophSession)
    val metadata = koulutus.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    get(
      oid,
      koulutus(oid).copy(muokkaaja = OphUserOid, metadata = Some(metadata.copy(isMuokkaajaOphVirkailija = Some(true))))
    )
  }

  it should "allow oph to create julkaistu koulutus without tarjoajat" in {
    val oid = put(ophKoulutus, ophSession)
    get(oid, ophKoulutus.copy(Some(KoulutusOid(oid)), muokkaaja = OphUserOid))
  }

  it should "allow non-oph user having rights for the koulutustyyppi to create koulutus" in {
    val createdKoulutus = YoKoulutus.copy(
      organisaatioOid = YoOid,
      tarjoajat = List(),
      muokkaaja = userOidForTestSessionId(crudSessions(YoOid)),
      metadata = Some(
        YoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata].copy(isMuokkaajaOphVirkailija = Some(false))
      )
    )
    val oid = put(createdKoulutus, crudSessions(YoOid))
    get(oid, createdKoulutus.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "store korkeakoulutus koulutus" in {
    val oid = put(YoKoulutus, ophSession)
    get(oid, YoKoulutus.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "store lukio koulutus" in {
    val oid = put(TestData.LukioKoulutus, ophSession)
    val metadata = TestData.LukiokoulutuksenMetatieto.copy(koulutusalaKoodiUrit =
      Seq("kansallinenkoulutusluokitus2016koulutusalataso1_00#1")
    )
    get(oid, TestData.LukioKoulutus.copy(oid = Some(KoulutusOid(oid)), metadata = Some(metadata)))
  }

  it should "validate new julkaistu koulutus" in {
    put(KoulutusPath, bytes(koulutus.copy(koulutuksetKoodiUri = Seq())), customHeaders(ophSession)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(missingMsg, "koulutuksetKoodiUri"))
    }
  }

  it should "write create koulutus to audit log" in {
    MockAuditLogger.clean()
    val oid = put(koulutus.withModified(LocalDateTime.parse("1000-01-01T12:00:00")), ophSession)
    MockAuditLogger.find(oid, "koulutus_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 without a session" in {
    put(KoulutusPath, bytes(koulutus), Seq(jsonHeader)) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "deny access if the user is missing rights to the koulutus organization" in {
    put(KoulutusPath, koulutus.copy(tarjoajat = List.empty), crudSessions(EvilChildOid), 403)
  }

  it should "deny access if the user is missing rights to tarjoaja organizations" in {
    put(KoulutusPath, YoKoulutus.copy(organisaatioOid = YoOid, tarjoajat = List(HkiYoOid)), crudSessions(YoOid), 403)
  }

  it should "allow access for adding any tarjoaja of correct oppilaitos-tyyppi for avoin korkeakoulutus" in {
    put(
      KkOpintokokonaisuusKoulutus.copy(
        metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true))),
        organisaatioOid = YoOid,
        tarjoajat = List(HkiYoOid, LutYoOid, KuopionKansalaisopistoOid)
      ),
      crudSessions(YoOid)
    )
  }

  it should "deny access if the user only has rights to a descendant of the koulutus organization" in {
    put(KoulutusPath, koulutus, crudSessions(EvilChildOid), 403)
  }

  it should "deny access if user has not rights for the koulutustyyppi to create koulutus" in {
    val createdKoulutus = YoKoulutus.copy(
      organisaatioOid = AmmOid,
      tarjoajat = List()
    )
    put(KoulutusPath, createdKoulutus, crudSessions(AmmOid), 403)
  }

  it should "fail if the user doesn't have the right role" in {
    put(KoulutusPath, koulutus, otherRoleSession, 403)
  }

  it should "deny access for the indexer" in {
    put(KoulutusPath, koulutus, indexerSession, 403)
  }

  it should "copy a temporary image to a permanent location while creating the koulutus" in {
    saveLocalPng("temp/image.png")
    val oid = put(koulutus.withTeemakuva(Some(s"$PublicImageServer/temp/image.png")), ophSession)

    get(oid, koulutus(oid).withTeemakuva(Some(s"$PublicImageServer/koulutus-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"koulutus-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch an image that's not in the temporary location" in {
    val koulutusWithImage = koulutus.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid               = put(koulutusWithImage, ophSession)
    MockS3Client.storage shouldBe empty
    get(oid, koulutusWithImage.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "fail to store koulutus if sorakuvaus doesn't exist" in {
    val sorakuvausId = UUID.randomUUID()
    put(KoulutusPath, koulutus.copy(sorakuvausId = Some(sorakuvausId)), ophSession, 400)
  }

  "Update koulutus" should "update koulutus" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid, Arkistoitu), lastModified, ophSession, 200)
    get(oid, koulutus(oid, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid          = put(koulutus, ophSession)
    val userOid      = OphUserOid
    val lastModified = get(oid, koulutus(oid).copy(muokkaaja = userOid))
    update(koulutus(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified, ophSession, 200)
    get(oid, koulutus(oid, Arkistoitu).copy(muokkaaja = OphUserOid))
  }

  it should "write koulutus update to audit log" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    MockAuditLogger.clean()
    update(
      koulutus(oid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")),
      lastModified,
      expectUpdate = true,
      ophSession
    )
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "koulutus_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 without a session" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal(401)
    }
  }

  it should "allow update access with oph credentials" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, expectUpdate = true, ophSession)
  }

  it should "deny access if the user is missing rights to the koulutus organization" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, crudSessions(LonelyOid), 403)
  }

  it should "deny the user of wrong koulutustyyppi to add tarjoaja to julkinen koulutus" in {
    val oid                     = put(ophKoulutus, ophSession)
    val koulutusWithOid         = ophKoulutus.copy(Some(KoulutusOid(oid)), muokkaaja = OphUserOid)
    val lastModified            = get(oid, koulutusWithOid)
    val koulutusWithNewTarjoaja = koulutusWithOid.copy(tarjoajat = List(YoOid))
    update(koulutusWithNewTarjoaja, lastModified, crudSessions(YoOid), 403)
  }

  it should "deny access if the user doesn't have update rights" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, readSessions(ChildOid), 403)
  }

  it should "deny access for the indexer" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, indexerSession, 403)
  }

  it should "allow access if the user has OPH CRUD rights when tarjoaja being removed for AmmKoulutus" in {
    val oid             = put(koulutus, ophSession)
    val lastModified    = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = koulutus.tarjoajat diff Seq(EvilCousin))

    update(updatedKoulutus, lastModified, expectUpdate = true, ophSession)
  }

  it should "allow access when removing tarjoaja organization from own koulutus, even though user doesn't have rights to the tarjoaja organization" in {
    var theKoulutus = yoKoulutus.copy(organisaatioOid = YoOid)
    val oid         = put(theKoulutus, ophSession)
    theKoulutus = theKoulutus.copy(oid = Some(KoulutusOid(oid)))
    val lastModified    = get(oid, theKoulutus)
    val updatedKoulutus = theKoulutus.copy(tarjoajat = List(YoOid))

    update(updatedKoulutus, lastModified, expectUpdate = true, crudSessions(YoOid))
  }

  it should "allow access if user had rights to all tarjoaja organizations being added to own koulutus" in {
    var theKoulutus = yoKoulutus.copy(organisaatioOid = YoOid, tarjoajat = List(YoOid))
    val oid         = put(theKoulutus, ophSession)
    theKoulutus = theKoulutus.copy(oid = Some(KoulutusOid(oid)))
    val lastModified    = get(oid, theKoulutus)
    val updatedKoulutus = theKoulutus.copy(tarjoajat = HkiYoOid :: theKoulutus.tarjoajat)

    update(updatedKoulutus, lastModified, expectUpdate = true, yliopistotSession)
  }

  it should "deny access if the user doesn't have rights to a tarjoaja organization being added for AmmKoulutus" in {
    val oid             = put(koulutus, ophSession)
    val lastModified    = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = EvilChildOid :: koulutus.tarjoajat)

    update(updatedKoulutus, lastModified, crudSessions(ChildOid), 403)
  }

  it should "allow access for adding any tarjoaja of correct oppilaitos-tyyppi for avoin korkeakoulutus" in {
    var theKoulutus = KkOpintokokonaisuusKoulutus.copy(
      metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true))),
      organisaatioOid = YoOid,
      tarjoajat = List(YoOid)
    )
    val oid = put(theKoulutus)
    theKoulutus = theKoulutus.copy(oid = Some(KoulutusOid(oid)))
    val lastModified = get(oid, theKoulutus)
    val updatedKoulutus =
      theKoulutus.copy(tarjoajat = HkiYoOid :: LutYoOid :: KuopionKansalaisopistoOid :: theKoulutus.tarjoajat)
    update(updatedKoulutus, lastModified, expectUpdate = true, crudSessions(YoOid))
  }

  it should "allow organisaatioOid change if user had rights to new organisaatio" in {
    var koulutus = yoKoulutus.copy(organisaatioOid = YoOid, tarjoajat = List(YoOid))
    val oid      = put(koulutus, ophSession)
    koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
    val lastModified = get(oid, koulutus)
    update(koulutus.copy(organisaatioOid = HkiYoOid), lastModified, expectUpdate = true, yliopistotSession)
  }

  it should "fail organisaatioOid change if user doesn't have rights to new organisaatio" in {
    var koulutus = yoKoulutus.copy(organisaatioOid = YoOid, tarjoajat = List(YoOid))
    val oid      = put(koulutus, ophSession)
    koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
    val lastModified = get(oid, koulutus)
    update(koulutus.copy(organisaatioOid = ChildOid), lastModified, crudSessions(YoOid), 403)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(koulutus, ophSession)
    post(KoulutusPath, bytes(koulutus(oid)), defaultHeaders) {
      status should equal(400)
      body should equal(errorBody(s"Otsake ${KoutaServlet.IfUnmodifiedSinceHeader} on pakollinen."))
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    update(koulutus(oid, Arkistoitu), lastModified, expectUpdate = true, ophSession)
    post(KoulutusPath, bytes(koulutus(oid)), headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))) {
      status should equal(409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid             = put(yoKoulutus.copy(tarjoajat = List()), ophSession)
    val createdKoulutus = yoKoulutus.copy(oid = Some(KoulutusOid(oid)), tarjoajat = List())
    val metadata        = createdKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata]
    val lastModified    = get(oid, createdKoulutus)
    val uusiKoulutus = createdKoulutus.copy(
      kielivalinta = Seq(Fi, Sv, En),
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      tarjoajat = List(YoOid, HkiYoOid),
      metadata = Some(
        metadata.copy(
          lisatiedot = metadata.lisatiedot.map(
            _.copy(teksti = Map(Fi -> "lisatiedot", Sv -> "Lisatiedot sv", En -> "Lisatiedot en"))
          ),
          kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv", En -> "kuvaus en")
        )
      )
    )
    update(uusiKoulutus, lastModified, expectUpdate = true, ophSession)
    get(oid, uusiKoulutus)
  }

  it should "update the modified time of the koulutus even when just tarjoajat is updated" in {
    val oid = put(koulutus, ophSession)

    setEntityModifiedToPast(
      oid,
      "10 minutes",
      "koulutukset",
      List("koulutusten_tarjoajat"),
      "oid",
      "koulutus_oid"
    ) should equal(Success(()))
    val lastModified        = get(oid, koulutus(oid))
    val lastModifiedInstant = TimeUtils.parseHttpDate(lastModified)
    Duration.between(lastModifiedInstant, Instant.now).compareTo(Duration.ofMinutes(9)) should equal(1)

    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List(LonelyOid, EvilChildOid, AmmOid))
    update(uusiKoulutus, lastModified, expectUpdate = true, ophSession)

    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus = read[Koulutus](body)
      koulutus.modified.isDefined should be(true)
      val modifiedInstant = modifiedToInstant(koulutus.modified.get)

      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(9)) should equal(1)
      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(11)) should equal(-1)
    }
  }

  it should "update muokkaaja of the koulutus when tarjoaja is added" in {
    logger.info(s"luodaan")
    var muokattavaKoulutus = KkOpintokokonaisuusKoulutus.copy(
      metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true))),
      organisaatioOid = YoOid,
      tarjoajat = List(YoOid)
    )
    logger.info(s"tarjoajia " + muokattavaKoulutus.tarjoajat.size)
    val oid = put(muokattavaKoulutus)
    muokattavaKoulutus = muokattavaKoulutus.copy(oid = Some(KoulutusOid(oid)))
    // haetaan kannasta asti
    assert(getKoulutusMuokkaaja(muokattavaKoulutus) == TestUserOid.toString)
    logger.info(s"haetaan ")
    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus  = read[Koulutus](body)
      val muokkaaja = koulutus.muokkaaja
      logger.info(s"muokkaaja apista: " + muokkaaja)
      muokkaaja.shouldEqual(TestUserOid)

    }
    val lastModified = get(oid, muokattavaKoulutus)
    val updatedKoulutus =
      muokattavaKoulutus.copy(tarjoajat = HkiYoOid :: muokattavaKoulutus.tarjoajat)
    logger.info(s"tarjoajia " + updatedKoulutus.tarjoajat.size)
    logger.info(s"päivitetään ")
    update(updatedKoulutus, lastModified, expectUpdate = true, ophSession)
    logger.info(s"haetaan ")
    assert(getKoulutusMuokkaaja(muokattavaKoulutus) == OphUserOid.toString)
    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus  = read[Koulutus](body)
      val muokkaaja = koulutus.muokkaaja
      logger.info(s"muokkaaja apista2: ", muokkaaja)
      muokkaaja.shouldEqual(OphUserOid)
    }
  }

  it should "update muokkaaja of the koulutus when tarjoaja is deleted" in {
    logger.info(s"luodaan")
    var muokattavaKoulutus = KkOpintokokonaisuusKoulutus.copy(
      metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true))),
      organisaatioOid = YoOid,
      tarjoajat = List(YoOid, HkiYoOid)
    )
    logger.info(s"tarjoajia " + muokattavaKoulutus.tarjoajat.size)
    val oid = put(muokattavaKoulutus)
    muokattavaKoulutus = muokattavaKoulutus.copy(oid = Some(KoulutusOid(oid)))
    // haetaan kannasta asti
    assert(getKoulutusMuokkaaja(muokattavaKoulutus) == TestUserOid.toString)
    logger.info(s"haetaan ")
    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus  = read[Koulutus](body)
      val muokkaaja = koulutus.muokkaaja
      logger.info(s"muokkaaja apista: " + muokkaaja)
      muokkaaja.shouldEqual(TestUserOid)

    }
    val lastModified = get(oid, muokattavaKoulutus)
    val updatedKoulutus =
      muokattavaKoulutus.copy(tarjoajat = List(HkiYoOid))
    logger.info(s"tarjoajia " + updatedKoulutus.tarjoajat.size)
    logger.info(s"päivitetään ")
    update(updatedKoulutus, lastModified, expectUpdate = true, ophSession)
    logger.info(s"haetaan ")
    assert(getKoulutusMuokkaaja(muokattavaKoulutus) == OphUserOid.toString)
    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus  = read[Koulutus](body)
      val muokkaaja = koulutus.muokkaaja
      logger.info(s"muokkaaja apista2: ", muokkaaja)
      muokkaaja.shouldEqual(OphUserOid)
    }
  }

  it should "delete some tarjoajat and read last modified from history" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List(GrandChildOid, EvilGrandChildOid))
    update(uusiKoulutus, lastModified, expectUpdate = true, ophSession)
    get(oid, uusiKoulutus) should not equal lastModified
  }

  it should "set last_modified right for old koulutukset after database migration" in {
    db.clean()
    db.migrate("106")
    addTestSessions()
    addDefaultSession()

    val (koulutus1, koulutus1Timestamp) = insertKoulutus(koulutus)
    val oid1                            = koulutus1.oid.get.toString

    val (koulutus2, _)              = insertKoulutus(koulutus)
    val oid2                        = koulutus2.oid.get.toString
    val koulutus2NewTarjoajat       = koulutus2.tarjoajat ++ Seq(YoOid)
    val koulutus2tarjoajatTimestamp = updateKoulutusTarjoajat(koulutus2.copy(tarjoajat = koulutus2NewTarjoajat))

    val (koulutus3, _)              = insertKoulutus(koulutus)
    val oid3                        = koulutus3.oid.get.toString
    val koulutus3tarjoajatTimestamp = updateKoulutusTarjoajat(koulutus3.copy(tarjoajat = List()))

    db.migrate("107")

    get(oid1, koulutus1.copy(modified = Some(instantToModified(koulutus1Timestamp))))
    get(
      oid2,
      koulutus2.copy(tarjoajat = koulutus2NewTarjoajat, modified = Some(instantToModified(koulutus2tarjoajatTimestamp)))
    )
    get(oid3, koulutus3.copy(tarjoajat = List(), modified = Some(instantToModified(koulutus3tarjoajatTimestamp))))

    db.clean()
    db.migrate()
    addTestSessions()
    addDefaultSession()
  }

  it should "add right amount of rows to history tables" in {
    resetTableHistory("koulutukset")
    resetTableHistory("koulutusten_tarjoajat")
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus.copy(oid = Some(KoulutusOid(oid))))

    val koulutus1 = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = Tallennettu, tarjoajat = List())
    update(koulutus1, lastModified, expectUpdate = true, ophSession)

    assert(getKoulutusHistorySize(koulutus1) == 1)
    // Poistetaan kolme tarjoajaa, koulutus-oidille kolme riviä tarjoajat-historiaan. Kaikilla kolmella rivillä sama aikaleima.
    assert(getKoulutusTarjoajatHistorySize(koulutus1) == 3)

    val lastModified2 = get(oid, koulutus1)
    val koulutus2     = koulutus1.copy(tila = Julkaistu, tarjoajat = koulutus.tarjoajat)
    update(koulutus2, lastModified2, expectUpdate = true, ophSession)

    assert(getKoulutusHistorySize(koulutus2) == 2)
    // Tarjoajien lisääminen ei lisää rivejä historiaan
    assert(getKoulutusTarjoajatHistorySize(koulutus2) == 3)
  }

  it should "store and update unfinished koulutus" in {
    val unfinishedKoulutus = Koulutus(
      koulutustyyppi = Amm,
      johtaaTutkintoon = true,
      muokkaaja = OphUserOid,
      organisaatioOid = ChildOid,
      modified = None,
      kielivalinta = Seq(Fi),
      nimi = Map(Fi -> "koulutus"),
      _enrichedData = Some(
        KoulutusEnrichedData(
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      )
    )
    val oid                   = put(unfinishedKoulutus, ophSession)
    val lastModified          = get(oid, unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid))))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid)), johtaaTutkintoon = false)
    update(newUnfinishedKoulutus, lastModified, expectUpdate = true, ophSession)
    get(oid, newUnfinishedKoulutus)
  }

  it should "validate updated koulutus" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    post(
      KoulutusPath,
      bytes(koulutus(oid).copy(koulutuksetKoodiUri = Seq())),
      headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))
    ) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(missingMsg, "koulutuksetKoodiUri"))
    }
  }

  it should "copy a temporary image to a permanent location while updating the koulutus" in {
    val oid          = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))

    saveLocalPng("temp/image.png")
    val koulutusWithImage = koulutus(oid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(koulutusWithImage, lastModified, expectUpdate = true, ophSession)
    get(oid, koulutusWithImage.withTeemakuva(Some(s"$PublicImageServer/koulutus-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"koulutus-teemakuva/$oid/image.png"))
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid               = put(koulutus, ophSession)
    val lastModified      = get(oid, koulutus(oid))
    val koulutusWithImage = koulutus(oid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(koulutusWithImage, lastModified, expectUpdate = true, ophSession)

    MockS3Client.storage shouldBe empty
    get(oid, koulutusWithImage.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "create, get and update ammatillinen osaamisala koulutus" in {
    val ammOaKoulutus = TestData.AmmOsaamisalaKoulutus.copy(tila = Tallennettu)
    val oid           = put(ammOaKoulutus)
    val lastModified  = get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "set nimi of ammatillinen osaamisala koulutus by osaamisala if nimi not given for koulutus" in {
    val ammOaKoulutus = AmmOsaamisalaKoulutus.copy(nimi = Map())
    val oid           = put(ammOaKoulutus)
    get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), nimi = Map(Fi -> "nimi", Sv -> "nimi sv")))
  }

  it should "create, get and update ammatillinen tutkinnon osa koulutus" in {
    val ammOaKoulutus = TestData.AmmTutkinnonOsaKoulutus.copy(tila = Tallennettu)
    val oid           = put(ammOaKoulutus)
    val lastModified  = get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "set nimi of ammatillinen tutkinnon osa koulutus by tutkinnonosa if nimi not given for koulutus" in {
    val ammToKoulutus = AmmTutkinnonOsaKoulutus.copy(nimi = Map())
    val oid           = put(ammToKoulutus)
    get(oid, ammToKoulutus.copy(oid = Some(KoulutusOid(oid)), nimi = Map(Fi -> "nimi", Sv -> "nimi sv")))
  }

  it should "create, get and update muu ammatillinen koulutus" in {
    val muuAmmKoulutus = AmmMuuKoulutus.copy(tila = Tallennettu)
    val oid            = put(muuAmmKoulutus)
    val lastModified   = get(oid, muuAmmKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(muuAmmKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, muuAmmKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update aikuisten perusopetus -koulutus" in {
    val aiPeKoulutus = AikuistenPerusopetusKoulutus.copy(tila = Tallennettu)
    val oid          = put(aiPeKoulutus, ophSession)
    val lastModified = get(oid, aiPeKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(aiPeKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified, true, ophSession)
    get(oid, aiPeKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "set koulutuksetKoodiUri of aikuisten perusopetus koulutus automatically if not given" in {
    val aiPeKoulutus = AikuistenPerusopetusKoulutus.copy(koulutuksetKoodiUri = Seq())
    val oid          = put(aiPeKoulutus, ophSession)
    get(oid, aiPeKoulutus.copy(oid = Some(KoulutusOid(oid)), koulutuksetKoodiUri = Seq("koulutus_201101#12")))
  }

  it should "set koulutuksetKoodiUri of taiteen perusopetus koulutus automatically if not given" in {
    val tpoKoulutus = TaiteenPerusopetusKoulutus.copy(koulutuksetKoodiUri = Seq())
    val oid         = put(tpoKoulutus)
    get(oid, tpoKoulutus.copy(oid = Some(KoulutusOid(oid)), koulutuksetKoodiUri = Seq("koulutus_999907#1")))
  }

  it should "create, get and update kk-opintojakso -koulutus" in {
    val kkOpintojaksoKoulutus = TestData.KkOpintojaksoKoulutus.copy(tila = Tallennettu)
    val oid                   = put(kkOpintojaksoKoulutus)
    val lastModified          = get(oid, kkOpintojaksoKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(kkOpintojaksoKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, kkOpintojaksoKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update kk-opintokokonaisuus-koulutus" in {
    val kkOpintokokonaisuusKoulutus = TestData.KkOpintokokonaisuusKoulutus.copy(tila = Tallennettu)
    val oid                         = put(kkOpintokokonaisuusKoulutus)
    val lastModified                = get(oid, kkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(kkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, kkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "set nimi of ammatillinen koulutus nimi by koulutusKoodiUri if nimi not given for koulutus" in {
    val ammKoulutus = koulutus.copy(nimi = Map())
    val oid         = put(ammKoulutus, ophSession)
    get(oid, ammKoulutus.copy(oid = Some(KoulutusOid(oid)), nimi = Map(Fi -> "nimi", Sv -> "nimi sv")))
  }

  it should "set opintojen laajuus of Yo -koulutus automatically if not given" in {
    val yoKoulutus = YoKoulutus.copy(metadata = Some(YliopistoKoulutusMetadata(opintojenLaajuusNumero = Some(10))))
    val oid        = put(yoKoulutus)
    val expectedMetadata = Some(
      YliopistoKoulutusMetadata(
        opintojenLaajuusNumero = Some(10),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
    get(oid, yoKoulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid, metadata = expectedMetadata))
  }

  it should "set opintojen laajuus of Amk -koulutus automatically if not given" in {
    val amkKoulutus =
      AmkKoulutus.copy(metadata = Some(AmmattikorkeakouluKoulutusMetadata(opintojenLaajuusNumero = Some(10))))
    val oid = put(amkKoulutus)
    val expectedMetadata = Some(
      AmmattikorkeakouluKoulutusMetadata(
        opintojenLaajuusNumero = Some(10),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
    get(oid, amkKoulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid, metadata = expectedMetadata))
  }

  it should "set opintojen laajuus and koulutusala of AmmOpe -koulutus automatically if not given" in {
    val ammOpeKoulutus = AmmOpettajaKoulutus.copy(metadata = Some(AmmOpeErityisopeJaOpoKoulutusMetadata()))
    val oid            = put(ammOpeKoulutus)
    val expectedMetadata = Some(
      AmmOpeErityisopeJaOpoKoulutusMetadata(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        opintojenLaajuusNumero = Some(60),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_01#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
    get(oid, ammOpeKoulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid, metadata = expectedMetadata))
  }

  it should "set opintojen laajuus and koulutusala of opettajien pedagogiset koulutukset automatically if not given" in {
    val yoOpeKoulutus = YoOpettajaKoulutus.copy(metadata = Some(OpePedagOpinnotKoulutusMetadata()))
    val oid           = put(yoOpeKoulutus)
    val expectedMetadata = Some(
      OpePedagOpinnotKoulutusMetadata(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        opintojenLaajuusNumero = Some(60),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_01#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
    get(oid, yoOpeKoulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid, metadata = expectedMetadata))
  }

  it should "set koulutusala and opintojen laajuus of lukio-koulutus automatically if not given" in {
    val lkKoulutus = LukioKoulutus.copy(metadata =
      Some(LukiokoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None, koulutusalaKoodiUrit = Seq()))
    )
    val oid              = put(lkKoulutus, ophSession)
    val expectedKoulutus = lkKoulutus.copy(oid = Some(KoulutusOid(oid)), metadata = Some(LukiokoulutuksenMetatieto))
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of Tuva-koulutus automatically if not given" in {
    val tuva             = TuvaKoulutus.copy(metadata = Some(TuvaKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None)))
    val oid              = put(tuva, ophSession)
    val expectedKoulutus = tuva.copy(oid = Some(KoulutusOid(oid)), metadata = Some(TuvaKoulutuksenMetatieto))
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of Telma-koulutus automatically if not given" in {
    val telma =
      TelmaKoulutus.copy(metadata = Some(TelmaKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None)))
    val oid              = put(telma, ophSession)
    val expectedKoulutus = telma.copy(oid = Some(KoulutusOid(oid)), metadata = Some(TelmaKoulutuksenMetatieto))
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of vapaa sivistystyö opistovuosi automatically if not given" in {
    val vst = VapaaSivistystyoOpistovuosiKoulutus.copy(metadata =
      Some(VapaaSivistystyoOpistovuosiKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None))
    )
    val oid = put(vst, ophSession)
    val expectedKoulutus =
      vst.copy(oid = Some(KoulutusOid(oid)), metadata = Some(VapaaSivistystyoOpistovuosiKoulutuksenMetatieto))
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of KK-opintojakso-koulutus automatically if not given" in {
    val kkOpintojakso =
      KkOpintojaksoKoulutus.copy(metadata =
        Some(
          KkOpintojaksoKoulutusMetadata(
            opintojenLaajuusNumeroMin = Some(10),
            opintojenLaajuusNumeroMax = Some(10),
            kuvaus = defaultKuvaus
          )
        )
      )
    val oid = put(kkOpintojakso)
    val expectedKoulutus = kkOpintojakso.copy(
      oid = Some(KoulutusOid(oid)),
      metadata = Some(
        KkOpintojaksoKoulutusMetadata(
          kuvaus = defaultKuvaus,
          opintojenLaajuusNumeroMin = Some(10),
          opintojenLaajuusNumeroMax = Some(10),
          opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
          isMuokkaajaOphVirkailija = Some(false)
        )
      )
    )
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of KK-opintokokonaisuus-koulutus automatically if not given" in {
    val kkOpintokokonaisuus =
      KkOpintokokonaisuusKoulutus.copy(metadata =
        Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None))
      )
    val oid = put(kkOpintokokonaisuus)
    val expectedKoulutus = kkOpintokokonaisuus.copy(
      oid = Some(KoulutusOid(oid)),
      metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto)
    )
    get(oid, expectedKoulutus)
  }

  it should "set opintojen laajuus of erikoistumiskoulutus-koulutus automatically if not given" in {
    val erikoistumisKoulutus =
      ErikoistumisKoulutus.copy(metadata =
        Some(ErikoistumisKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None))
      )
    val oid = put(erikoistumisKoulutus)
    val expectedKoulutus = erikoistumisKoulutus.copy(
      oid = Some(KoulutusOid(oid)),
      metadata = Some(ErikoistumisKoulutuksenMetatieto)
    )
    get(oid, expectedKoulutus)
  }

  it should "set koulutusala of erikoislääkäri-koulutus automatically if not given" in {
    val elKoulutus =
      ErikoislaakariKoulutus.copy(metadata = Some(ErikoislaakariKoulutusMetadata(kuvaus = defaultKuvaus)))
    val oid = put(elKoulutus)
    val expectedMetadata = Some(
      ErikoislaakariKoulutusMetadata(
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso2_091#1"),
        kuvaus = defaultKuvaus,
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
    get(oid, elKoulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid, metadata = expectedMetadata))
  }

  it should "fail to auto-populate parameter values if KoodistoService failure when fetching koodiuri-version" in {
    val koulutusCausingEerror = koulutus.copy(nimi = Map(), koulutuksetKoodiUri = Seq("koulutus_111111#11"))
    put(KoulutusPath, koulutusCausingEerror, ophSession, 500)
  }

  it should "fail to auto-populate parameter values if EPerusteService failure when fetching tutkinnonosat" in {
    mockTutkinnonOsatFailure(111111)
    val koulutusCausingEerror = AmmTutkinnonOsaKoulutus.copy(
      nimi = Map(),
      metadata = Some(
        AmmatillinenTutkinnonOsaKoulutusMetadata(tutkinnonOsat =
          Seq(TutkinnonOsa(Some(111111), None, Some(1), Some(1)))
        )
      )
    )
    put(KoulutusPath, koulutusCausingEerror, ophSession, 500)
  }

  it should "fail to auto-populate parameter values if EPerusteService failure when fetching osaamisalat" in {
    val koulutusCausingEerror = AmmOsaamisalaKoulutus.copy(
      nimi = Map(),
      ePerusteId = Some(111111)
    )
    put(KoulutusPath, koulutusCausingEerror, ophSession, 500)
  }

  it should "fail to update koulutus if sorakuvaus doesn't exist" in {
    val (koulutusOid: String, lastModified: String) = createKoulutusWithSorakuvaus

    val nonExistentSorakuvausId = UUID.randomUUID()
    update(
      KoulutusPath,
      koulutus(koulutusOid).copy(sorakuvausId = Some(nonExistentSorakuvausId)),
      ophSession,
      lastModified,
      400,
      List(ValidationError("sorakuvausId", nonExistent("Sorakuvausta", nonExistentSorakuvausId)))
    )
  }

  it should "allow oph user to update from julkaistu to tallennettu" in {
    val oid = put(yoKoulutus)
    val lastModified = get(
      oid,
      yoKoulutus.copy(
        oid = Some(KoulutusOid(oid)),
        muokkaaja = TestUserOid,
        metadata = Some(
          yoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata].copy(isMuokkaajaOphVirkailija = Some(false))
        )
      )
    )
    val updatedKoulutus = yoKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Tallennettu)
    update(updatedKoulutus, lastModified, expectUpdate = true, ophSession)
    get(
      oid,
      updatedKoulutus.copy(
        muokkaaja = OphUserOid,
        metadata = Some(
          yoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true))
        )
      )
    )
  }

  it should "not allow non oph user to update from julkaistu to tallennettu" in {
    val oid = put(yoKoulutus)
    val lastModified = get(
      oid,
      yoKoulutus.copy(
        oid = Some(KoulutusOid(oid)),
        muokkaaja = TestUserOid,
        metadata = Some(
          yoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata].copy(isMuokkaajaOphVirkailija = Some(false))
        )
      )
    )
    val updatedKoulutus = yoKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Tallennettu)
    update(updatedKoulutus, lastModified, crudSessions(koulutus.organisaatioOid), 403)
  }

  private def createKoulutusWithSorakuvaus = {
    val sorakuvausId         = put(sorakuvaus)
    val koulutusOid          = put(koulutus.copy(sorakuvausId = Some(sorakuvausId)), ophSession)
    val koulutusLastModified = get(koulutusOid, koulutus(koulutusOid).copy(sorakuvausId = Some(sorakuvausId)))
    (koulutusOid, koulutusLastModified)
  }

  it should "fail to update julkaistu koulutus if sorakuvaus is not yet julkaistu" in {
    val (koulutusOid: String, lastModified: String) = createKoulutusWithSorakuvaus

    val tallennettuSorakuvausId = put(sorakuvaus.copy(tila = Tallennettu))
    update(
      KoulutusPath,
      koulutus(koulutusOid).copy(sorakuvausId = Some(tallennettuSorakuvausId)),
      ophSession,
      lastModified,
      400,
      List(ValidationError("tila", notYetJulkaistu("Sorakuvausta", tallennettuSorakuvausId)))
    )
  }

  it should "return koulutustyyppi2opistotyyppi mappings" in {
    get(s"$KoulutusPath/listOppilaitostyypitByKoulutustyypit", headers = Seq(sessionHeader(ophSession))) {
      withClue(body) {
        status should equal(200)
      }
    }
  }
}
