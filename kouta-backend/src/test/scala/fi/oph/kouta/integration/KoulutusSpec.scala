package fi.oph.kouta.integration

import java.time.{Duration, Instant, LocalDateTime, ZoneId}

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{KoulutusFixture, MockS3Client, ToteutusFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.Validations._
import org.json4s.jackson.Serialization.read

import scala.util.Success

class KoulutusSpec extends KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture with ToteutusFixture with UploadFixture {

  override val roleEntities = Seq(Role.Koulutus)

  val ophKoulutus = koulutus.copy(tila = Julkaistu, organisaatioOid = OphOid, tarjoajat = List(), julkinen = true)


  "Get koulutus by oid" should "return 404 if koulutus not found" in {
    get(s"$KoulutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "return 401 without a session" in {
    get(s"$KoulutusPath/123", headers = Map.empty) {
      status should equal (401)
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

  it should "allow the user of proper koulutustyyppi to read julkinen koulutus created by oph" in {
    val oid = put(ophKoulutus, ophSession)
    get(oid, readSessions(AmmOid), ophKoulutus.copy(Some(KoulutusOid(oid))))
  }

  it should "deny the user of wrong koulutustyyppi to read julkinen koulutus created by oph" in {
    val oid = put(ophKoulutus, ophSession)
    get(s"$KoulutusPath/$oid", readSessions(YoOid), 403)
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
    val sessionId = crudSessions(koulutus.organisaatioOid)
    val oid = put(koulutus.copy(julkinen = true), ophSession)
    get(oid, readSessions(AmmOid), koulutus(oid).copy(julkinen = true, muokkaaja = OphUserOid))
  }

  it should "deny an authenticated user of a different oppilaitostyyppi to access public koulutus" in {
    val oid = put(koulutus.copy(julkinen = true), ophSession)
    get(s"$KoulutusPath/$oid", readSessions(YoOid), 403)
  }

  "Create koulutus" should "store koulutus" in {
    val oid = put(koulutus, ophSession)
    get(oid, koulutus(oid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(koulutus.copy(muokkaaja = UserOid("random")), ophSession)
    get(oid, koulutus(oid).copy(muokkaaja = OphUserOid))
  }

  it should "allow oph to create julkaistu koulutus without tarjoajat" in {
    val oid = put(ophKoulutus, ophSession)
    get(oid, ophKoulutus.copy(Some(KoulutusOid(oid)), muokkaaja = OphUserOid))
  }

  it should "store korkeakoulutus koulutus" in {
    val oid = put(TestData.YoKoulutus, ophSession)
    get(oid, TestData.YoKoulutus.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "validate new koulutus" in {
    put(KoulutusPath, bytes(koulutus.copy(koulutusKoodiUri = None)), customHeaders(ophSession)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(missingMsg, "koulutusKoodiUri"))
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

  it should "deny access if the user only has rights to a descendant of the koulutus organization" in {
    put(KoulutusPath, koulutus, crudSessions(EvilChildOid), 403)
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
    val oid = put(koulutusWithImage, ophSession)
    MockS3Client.storage shouldBe empty
    get(oid, koulutusWithImage.copy(oid = Some(KoulutusOid(oid))))
  }

  "Update koulutus" should "update koulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid, Arkistoitu), lastModified, ophSession, 200)
    get(oid, koulutus(oid, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(koulutus, ophSession)
    val userOid = OphUserOid
    val lastModified = get(oid, koulutus(oid).copy(muokkaaja = userOid))
    update(koulutus(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified, ophSession, 200)
    get(oid, koulutus(oid, Arkistoitu).copy(muokkaaja = OphUserOid))
  }

  it should "write koulutus update to audit log" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    MockAuditLogger.clean()
    update(koulutus(oid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified, expectUpdate = true, ophSession)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "koulutus_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 without a session" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow put and update access with oph credentials" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, expectUpdate = true, ophSession)
  }

  it should "deny access if the user is missing rights to the koulutus organization" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, crudSessions(LonelyOid), 403)
  }

  it should "deny the user of wrong koulutustyyppi to add tarjoaja to julkinen koulutus created by oph" in {
    val oid = put(ophKoulutus, ophSession)
    val koulutusWithOid = ophKoulutus.copy(Some(KoulutusOid(oid)), muokkaaja = OphUserOid)
    val lastModified = get(oid, koulutusWithOid)
    val koulutusWithNewTarjoaja = koulutusWithOid.copy(tarjoajat = List(YoOid))
    update(koulutusWithNewTarjoaja, lastModified, crudSessions(YoOid), 403)
  }

  it should "deny access if the user doesn't have update rights" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, readSessions(ChildOid), 403)
  }

  it should "deny access for the indexer" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    update(muokkaus(koulutus(oid)), lastModified, indexerSession, 403)
  }

  it should "allow access if the user has OPH CRUD rights when tarjoaja being removed for AmmKoulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = koulutus.tarjoajat diff Seq(EvilCousin))

    update(updatedKoulutus, lastModified, expectUpdate = true, ophSession)
  }

  it should "not allow access if the user doesn't have rights to a tarjoaja organization being removed for AmmKoulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = koulutus.tarjoajat diff Seq(EvilCousin))

    update(updatedKoulutus, lastModified, crudSessions(ChildOid), 403)
  }

  it should "not allow access if the user doesn't have rights to a tarjoaja organization being added for AmmKoulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = EvilChildOid :: koulutus.tarjoajat)

    update(updatedKoulutus, lastModified, crudSessions(ChildOid), 403)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(koulutus, ophSession)
    post(KoulutusPath, bytes(koulutus(oid)), defaultHeaders) {
      status should equal (400)
      body should equal (errorBody(s"Otsake ${KoutaServlet.IfUnmodifiedSinceHeader} on pakollinen."))
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    update(koulutus(oid, Arkistoitu), lastModified, true, ophSession)
    post(KoulutusPath, bytes(koulutus(oid)), headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))) {
      status should equal (409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid = put(koulutus, ophSession)
    val metadata = koulutus.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata]
    val lastModified = get(oid, koulutus(oid))
    val uusiKoulutus = koulutus(oid).copy(
      kielivalinta = Seq(Fi, Sv, En),
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      tarjoajat = List(LonelyOid, OtherOid, AmmOid),
      metadata = Some(metadata.copy(
        lisatiedot = metadata.lisatiedot.map(_.copy(teksti = Map(Fi -> "lisatiedot", Sv -> "Lisatiedot sv", En -> "Lisatiedot en"))),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv", En -> "kuvaus en")
      )))
    update(uusiKoulutus, lastModified, true, ophSession)
    get(oid, uusiKoulutus)
  }

  it should "update the modified time of the koulutus even when just tarjoajat is updated" in {
    val oid = put(koulutus, ophSession)

    setModifiedToPast(oid, "10 minutes") should be(Success(()))
    val lastModified = get(oid, koulutus(oid))
    val lastModifiedInstant = TimeUtils.parseHttpDate(lastModified)
    Duration.between(lastModifiedInstant, Instant.now).compareTo(Duration.ofMinutes(5)) should equal(1)

    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List(LonelyOid, OtherOid, AmmOid))
    update(uusiKoulutus, lastModified, expectUpdate = true, ophSession)

    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus = read[Koulutus](body)
      koulutus.modified.isDefined should be(true)
      val modifiedInstant = koulutus.modified.get.atZone(ZoneId.of("Europe/Helsinki")).toInstant

      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(5)) should equal(1)
      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(15)) should equal(-1)
    }
  }

  it should "delete some tarjoajat and read last modified from history" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List(GrandChildOid, EvilGrandChildOid))
    update(uusiKoulutus, lastModified, true, ophSession)
    get(oid, uusiKoulutus) should not equal lastModified
  }

  it should "store and update unfinished koulutus" in {
    val unfinishedKoulutus = Koulutus(koulutustyyppi = Amm, johtaaTutkintoon = true, muokkaaja = OphUserOid, organisaatioOid = ChildOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "koulutus"))
    val oid = put(unfinishedKoulutus, ophSession)
    val lastModified = get(oid, unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid))))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid)), johtaaTutkintoon = false)
    update(newUnfinishedKoulutus, lastModified, expectUpdate = true, ophSession)
    get(oid, newUnfinishedKoulutus)
  }

  it should "validate updated koulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid).copy(koulutusKoodiUri = None)), headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(missingMsg, "koulutusKoodiUri"))
    }
  }

  it should "copy a temporary image to a permanent location while updating the koulutus" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))

    saveLocalPng("temp/image.png")
    val koulutusWithImage = koulutus(oid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(koulutusWithImage, lastModified, expectUpdate = true, ophSession)
    get(oid, koulutusWithImage.withTeemakuva(Some(s"$PublicImageServer/koulutus-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"koulutus-teemakuva/$oid/image.png"))
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(koulutus, ophSession)
    val lastModified = get(oid, koulutus(oid))
    val koulutusWithImage = koulutus(oid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(koulutusWithImage, lastModified, expectUpdate = true, ophSession)

    MockS3Client.storage shouldBe empty
    get(oid, koulutusWithImage.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "create, get and update ammatillinen osaamisala koulutus" in {
    val ammOaKoulutus = TestData.AmmOsaamisalaKoulutus.copy(tila = Tallennettu)
    val oid = put(ammOaKoulutus)
    val lastModified = get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update ammatillinen tutkinnon osa koulutus" in {
    val ammOaKoulutus = TestData.AmmTutkinnonOsaKoulutus.copy(tila = Tallennettu)
    val oid = put(ammOaKoulutus)
    val lastModified = get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid))))
    update(ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammOaKoulutus.copy(oid = Some(KoulutusOid(oid)), tila = Julkaistu))
  }
}
