package fi.oph.kouta.integration

import cats.Show.Shown.mat
import fi.oph.kouta.TestData.AmmToteutuksenMetatieto

import java.time.LocalDateTime
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture._
import fi.oph.kouta.mocks.{KoodistoServiceMock, LokalisointiServiceMock, MockAuditLogger}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.{TestData, TestOids}
import org.json4s.jackson.JsonMethods

class ToteutusSpec extends KoutaIntegrationSpec
  with AccessControlSpec with KoulutusFixture with ToteutusFixture with SorakuvausFixture
  with KeywordFixture with UploadFixture with KoodistoServiceMock with LokalisointiServiceMock
  with HakukohdeFixture with HakuFixture {

  override val roleEntities = Seq(Role.Toteutus, Role.Koulutus)

  var koulutusOid: String = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockKoodistoResponse("lukiopainotukset", List("lukiopainotukset_1"))
    mockKoodistoResponse("lukiolinjaterityinenkoulutustehtava", List("lukiolinjaterityinenkoulutustehtava_1"))
    mockKoodiUriResponse("koulutus_201101", 12)
    mockLokalisointiResponse("yleiset.opintopistetta")
    mockLokalisointiResponse("toteutuslomake.lukionYleislinjaNimiOsa")
    koulutusOid = put(koulutus, ophSession)
  }

  "Get toteutus by oid" should "return 404 if toteutus not found" in {
    get(s"$ToteutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown toteutus oid")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$ToteutusPath/123") {
      status should equal (401)
    }
  }

  it should "allow a user of the toteutus organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(toteutus.organisaatioOid), toteutus(oid, koulutusOid))
  }

  it should "allow a user of the tarjoaja organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid).copy(tarjoajat = List(LonelyOid)))
    get(oid, crudSessions(LonelyOid), toteutus(oid, koulutusOid).copy(tarjoajat = List(LonelyOid)))
  }

  it should "deny a user without access to the toteutus organization" in {
    val oid = put(toteutus(koulutusOid))
    get(s"$ToteutusPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(ParentOid), toteutus(oid, koulutusOid))
  }

  it should "allow a user with only access to a descendant organization" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, crudSessions(GrandChildOid), toteutus(oid, koulutusOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(toteutus(koulutusOid))
    get(s"$ToteutusPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, indexerSession, toteutus(oid, koulutusOid))
  }

  it should "return error when trying to get deleted toteutus" in {
    val oid = put(toteutus(koulutusOid).copy(tila = Poistettu), ophSession)
    get(s"$ToteutusPath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted toteutus with myosPoistetut = true" in {
    val oid = put(toteutus(koulutusOid).copy(tila = Poistettu), ophSession)
    get(s"$ToteutusPath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create toteutus" should "store toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, toteutus(oid, koulutusOid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(toteutus(koulutusOid).copy(muokkaaja = UserOid("random")))
    get(oid, toteutus(oid, koulutusOid).copy(muokkaaja = testUser.oid))
  }

  it should "store korkeakoulutus toteutus" in {
    val koulutusOid = put(TestData.YoKoulutus, ophSession)
    val oid = put(TestData.JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid(koulutusOid)))
    get(oid, TestData.JulkaistuYoToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid)))
  }

  it should "add toteutuksen tarjoajan oppilaitos to koulutuksen tarjoajat if it's not there" in {
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List())
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role], Role.Koulutus.Read.asInstanceOf[Role]), GrandChildOid)
    val oid = put(newToteutus, session)
    get(oid, newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = userOidForTestSessionId(session)))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid)))
  }

  it should "fail to store toteutus if no permission to add toteutuksen tarjoajan oppilaitos to koulutuksen tarjoajat" in {
    val newKoulutus = koulutus.copy(organisaatioOid = LonelyOid, julkinen = false, tarjoajat = List(LonelyOid))
    val koulutusOid = put(newKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role], Role.Koulutus.Read.asInstanceOf[Role]), GrandChildOid)
    put(ToteutusPath, newToteutus, session, 403)
    get(koulutusOid, newKoulutus.copy(oid = Some(KoulutusOid(koulutusOid))))
  }

  it should "fail to store toteutus if no koulutus permission to add toteutuksen tarjoajan oppilaitos to koulutuksen tarjoajat" in {
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List())
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role]), GrandChildOid)
    put(ToteutusPath, newToteutus, session, 403)
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid))))
  }

  it should "fail to store toteutus if koulutus does not exist" in {
    put(ToteutusPath, toteutus, 400, "koulutusOid", nonExistent("Koulutusta", toteutus.koulutusOid))
  }

  it should "fail to store toteutus if toteutus tyyppi does not match koulutustyyppi of koulutus" in {
    val toteutus = TestData.JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    put(ToteutusPath, toteutus, 400, "metadata.tyyppi", tyyppiMismatch("koulutuksen", koulutusOid))
  }

  it should "fail to store julkaistu toteutus if the koulutus is not yet julkaistu" in {
    val koulutusOid = put(koulutus.copy(tila = Tallennettu), ophSession)
    put(ToteutusPath, toteutus(koulutusOid), 400, "tila", notYetJulkaistu("Koulutusta", koulutusOid))
  }

  it should "write create toteutus to audit log" in {
    MockAuditLogger.clean()
    val oid = put(toteutus(koulutusOid).withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "toteutus_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    put(ToteutusPath, bytes(toteutus(koulutusOid))) {
      status should equal (401)
    }
  }

  it should "validate new toteutus" in {
    put(ToteutusPath, bytes(toteutus(koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("katkarapu"), "tarjoajat[0]"))
    }
  }

  it should "allow a user of the toteutus organization to create the toteutus" in {
    put(toteutus(koulutusOid), crudSessions(toteutus.organisaatioOid))
  }

  it should "deny a user without access to the toteutus organization" in {
    put(ToteutusPath, toteutus(koulutusOid), crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the toteutus" in {
     put(toteutus(koulutusOid), crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(ToteutusPath, toteutus(koulutusOid), crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(ToteutusPath, toteutus(koulutusOid), readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(ToteutusPath, toteutus(koulutusOid), indexerSession, 403)
  }

  it should "copy a temporary image to a permanent location while creating the toteutus" in {
    saveLocalPng("temp/image.png")
    val oid = put(toteutus(koulutusOid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))

    get(oid, toteutus(oid, koulutusOid).withTeemakuva(Some(s"$PublicImageServer/toteutus-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"toteutus-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch an image that's not in the temporary location" in {
    val toteutusWithImage = toteutus(koulutusOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(toteutusWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, toteutusWithImage.copy(oid = Some(ToteutusOid(oid))))
  }

  it should "deny sorakuvaus id when not allowed" in {
    val sorakuvausId = put(sorakuvaus)
    val thisToteutus = toteutus(koulutusOid).copy(sorakuvausId = Some(sorakuvausId))
    put(ToteutusPath, bytes(thisToteutus), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(notMissingMsg(Some(sorakuvausId)), "sorakuvausId"))
    }
  }

  "Update toteutus" should "update toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    get(oid, toteutus(oid, koulutusOid, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(toteutus(koulutusOid), crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid).copy(muokkaaja = userOid))
    update(toteutus(oid, koulutusOid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, toteutus(oid, koulutusOid, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "not update toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    MockAuditLogger.clean()
    update(thisToteutus, lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(oid, thisToteutus)
  }

  it should "add toteutuksen tarjoajan oppilaitos to koulutuksen tarjoajat on update if it's not there" in {
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List())
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = GrandChildOid, tarjoajat = List())
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role], Role.Koulutus.Read.asInstanceOf[Role]), GrandChildOid)
    val oid = put(newToteutus, session)
    val createdToteutus = newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = userOidForTestSessionId(session))
    val lastModified = get(oid, createdToteutus)
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List()))
    update(createdToteutus.copy(tarjoajat = List(GrandChildOid)), lastModified)
    get(oid, createdToteutus.copy(tarjoajat = List(GrandChildOid)))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid)))
  }

  it should "remove toteutuksen tarjoajan oppilaitos from koulutuksen tarjoajat when removed on update" in {
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List(ChildOid))
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role], Role.Koulutus.Read.asInstanceOf[Role]), GrandChildOid)
    val oid = put(newToteutus, session)
    val createdToteutus = newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = userOidForTestSessionId(session))
    val lastModified = get(oid, createdToteutus)
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid)))
    update(createdToteutus.copy(tarjoajat = List()), lastModified)
    get(oid, createdToteutus.copy(tarjoajat = List()))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List()))
  }

  it should "prevent removal of toteutuksen tarjoajan oppilaitos from koulutuksen tarjoajat when oppilaitos still in use" in {
    val oppilaitosAlsoInOtherToteutus = OrganisaatioOid("1.2.246.562.10.39218317368")
    val tarjoajaOfOtherOppilaitos = OrganisaatioOid("1.2.246.562.10.74478323608")
    val tarjoajaInOtherTotetus = OrganisaatioOid("1.2.246.562.10.46789068684")
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List(ChildOid, oppilaitosAlsoInOtherToteutus))
    val koulutusOid = put(ophKoulutus, ophSession)
    val otherToteutus = toteutus(koulutusOid).copy(organisaatioOid = OphOid, tarjoajat = List(tarjoajaInOtherTotetus))
    put(otherToteutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = OphOid, tarjoajat = List(GrandChildOid, EvilGrandChildOid, tarjoajaOfOtherOppilaitos))
    val oid = put(newToteutus, ophSession)
    val createdToteutus = newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = OphUserOid)
    val lastModified = get(oid, createdToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(isMuokkaajaOphVirkailija = Some(true)))))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid, oppilaitosAlsoInOtherToteutus)))
    update(createdToteutus.copy(tarjoajat = List(GrandChildOid)), lastModified)
    get(oid, createdToteutus.copy(tarjoajat = List(GrandChildOid), muokkaaja = TestUserOid))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid, oppilaitosAlsoInOtherToteutus)))
  }

  it should "make all changes to toteutuksen tarjoajien oppilaitokset accordingly in koulutuksen tarjoajat when updated in toteutus" in {
    val untouchedOppilaitosOid = OrganisaatioOid("1.2.246.562.10.46312206843")
    val newTarjoaja = OrganisaatioOid("1.2.246.562.10.74478323608")
    val newTarjoaja2 = OrganisaatioOid("1.2.246.562.10.46789068684")
    val newOppilaitos = OrganisaatioOid("1.2.246.562.10.39218317368")
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List(ChildOid, untouchedOppilaitosOid))
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(organisaatioOid = OphOid, tarjoajat = List(GrandChildOid))
    val oid = put(newToteutus, ophSession)
    val createdToteutus = newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = OphUserOid)
    val lastModified = get(oid, createdToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(isMuokkaajaOphVirkailija = Some(true)))))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid, untouchedOppilaitosOid)))
    update(createdToteutus.copy(tarjoajat = List(newTarjoaja, newTarjoaja2)), lastModified)
    get(oid, createdToteutus.copy(tarjoajat = List(newTarjoaja, newTarjoaja2), muokkaaja = TestUserOid))
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(untouchedOppilaitosOid, newOppilaitos)))
  }

  it should "remove toteutuksen tarjoajan oppilaitos from koulutuksen tarjoajat when deleting toteutus" in {
    val ophKoulutus = koulutus.copy(organisaatioOid = OphOid, julkinen = true, tarjoajat = List(ChildOid))
    val koulutusOid = put(ophKoulutus, ophSession)
    val newToteutus = toteutus(koulutusOid).copy(tila = Tallennettu, organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    val session = addTestSession(Seq(Role.Toteutus.Crud.asInstanceOf[Role], Role.Koulutus.Read.asInstanceOf[Role]), GrandChildOid)
    val oid = put(newToteutus, session)
    val createdToteutus = newToteutus.copy(oid = Some(ToteutusOid(oid)), muokkaaja = userOidForTestSessionId(session))
    val lastModified = get(oid, createdToteutus)
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List(ChildOid)))
    update(createdToteutus.copy(tila = Poistettu), lastModified)
    get(koulutusOid, ophKoulutus.copy(oid = Some(KoulutusOid(koulutusOid)), tarjoajat = List()))
  }

  it should "fail to update if toteutus tyyppi does not match koulutustyyppi of koulutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid).copy(metadata = Some(TestData.YoToteutuksenMetatieto))
    update(ToteutusPath, thisToteutus, lastModified, 400, "metadata.tyyppi", tyyppiMismatch("koulutuksen", koulutusOid))
  }

  it should "fail to update julkaistu toteutus if the koulutus is not yet julkaistu" in {
    val koulutusOid = put(koulutus.copy(tila = Tallennettu), ophSession)
    val oid = put(toteutus(koulutusOid).copy(tila = Tallennettu))
    val lastModified = get(oid, toteutus(oid, koulutusOid).copy(tila = Tallennettu))
    update(ToteutusPath, toteutus(oid, koulutusOid), lastModified, 400, "tila", notYetJulkaistu("Koulutusta", koulutusOid))
  }

  it should "fail to update toteutus if koulutus does not exist" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val randomOid = TestOids.randomKoulutusOid.s
    update(ToteutusPath, toteutus(oid, randomOid), lastModified,400, "koulutusOid", nonExistent("Koulutusta", randomOid))
  }

  it should "write toteutus update to audit log" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    MockAuditLogger.clean()
    update(toteutus(oid, koulutusOid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "toteutus_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    post(ToteutusPath, bytes(thisToteutus), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the toteutus organization to update the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, expectUpdate = false, crudSessions(toteutus.organisaatioOid))
  }

  it should "deny a user without access to the toteutus organization" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, expectUpdate = false, crudSessions(ParentOid))
  }

  it should "allow a user with only access to a descendant organization" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, expectUpdate = false, crudSessions(GrandChildOid))
  }

  it should "allow a user of the tarjoaja organization to update the toteutus" in {
    val newKoulutusOid = put(koulutus.copy(julkinen = true), ophSession)
    val oid = put(toteutus(newKoulutusOid).copy(tarjoajat = List(LonelyOid)))
    val thisToteutus = toteutus(oid, newKoulutusOid).copy(tarjoajat = List(LonelyOid))
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, expectUpdate = false, crudSessions(LonelyOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, readSessions(toteutus.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    post(ToteutusPath, bytes(thisToteutus), headers = defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    post(ToteutusPath, bytes(thisToteutus), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update toteutuksen nimi, metadata ja tarjoajat" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    val uusiToteutus = thisToteutus.copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = Some(ammMetatieto.copy(kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv", En -> "description"))),
      tarjoajat = List(LonelyOid, OtherOid, AmmOid),
      _enrichedData = Some(ToteutusEnrichedData(esitysnimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"), muokkaajanNimi = Some("Testi Muokkaaja")))
    )
    update(uusiToteutus, lastModified, expectUpdate = true)
    get(oid, uusiToteutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    val uusiToteutus = thisToteutus.copy(tarjoajat = List())
    update(uusiToteutus, lastModified, expectUpdate = true)
    get(oid, uusiToteutus) should not equal lastModified
  }

  it should "store and update unfinished toteutus" in {
    val unfinishedToteutus = Toteutus(muokkaaja = TestUserOid, koulutusOid = KoulutusOid(koulutusOid),
      organisaatioOid = ChildOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "toteutus"))
    val oid = put(unfinishedToteutus)
    val lastModified = get(oid, unfinishedToteutus.copy(oid = Some(ToteutusOid(oid)),
      _enrichedData = Some(ToteutusEnrichedData(esitysnimi = Map(Fi -> "toteutus"), muokkaajanNimi = Some("Testi Muokkaaja")))))
    val newKoulutusOid = put(koulutus, ophSession)
    val newUnfinishedToteutus = unfinishedToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(newKoulutusOid),
      _enrichedData = Some(ToteutusEnrichedData(esitysnimi = Map(Fi -> "toteutus"), muokkaajanNimi = Some("Testi Muokkaaja"))))
    update(newUnfinishedToteutus, lastModified)
    get(oid, newUnfinishedToteutus)
  }

  it should "validate updated toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    post(ToteutusPath, bytes(toteutus(oid, koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("katkarapu"), "tarjoajat[0]"))
    }
  }

  def pastAlkamiskausi(alkamispvm: LocalDateTime, paattymispvm: LocalDateTime): Some[KoulutuksenAlkamiskausi] = {
    Some(KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
      koulutuksenAlkamispaivamaara = Some(alkamispvm),
      koulutuksenPaattymispaivamaara = Some(paattymispvm)))
  }

  it should "validate koulutuksen alkamisen dates only when adding a new julkaistu toteutus" in {
    val (past, morePast) = (TestData.inPast(5000), TestData.inPast(60000))

    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausi = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val thisToteutus = toteutus(koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Julkaistu)

    put(thisToteutus.copy(tila = Tallennettu))

    put(ToteutusPath, bytes(thisToteutus), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(List(
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(morePast)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(past)),
      )))
    }
  }

  it should "validate koulutuksen alkamisen dates when moving from other states to julkaistu" in {
    val (past, morePast) = (TestData.inPast(5000), TestData.inPast(60000))
    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausi = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val thisToteutus = toteutus(koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Tallennettu)

    val oid = put(thisToteutus)
    val thisToteutusWithOid = toteutus(oid, koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Tallennettu)

    val lastModified = get(oid, thisToteutusWithOid)

    post(ToteutusPath, bytes(thisToteutusWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(List(
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(morePast)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(past)),
      )))
    }

    update(thisToteutusWithOid.copy(tila = Poistettu), lastModified)
  }

  it should "not validate koulutuksen alkamisen dates when updating a julkaistu toteutus" in {
    val (past, morePast) = (TestData.inPast(5000), TestData.inPast(60000))
    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausi = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val inPastMetadata = ammMetatieto.copy(opetus = Some(inPastOpetus))

    val oid = put(toteutus(koulutusOid).copy(tila = Julkaistu))
    val thisToteutus = toteutus(oid, koulutusOid)

    val lastModified = get(oid, thisToteutus)

    update(thisToteutus.copy(metadata = Some(inPastMetadata)), lastModified)
  }

  it should "allow missing koulutuksen alkamiskausi" in {
    val (future, morefuture) = (TestData.inFuture(5000), TestData.inFuture(6000))

    val futureOpetus = opetus.copy(koulutuksenAlkamiskausi = pastAlkamiskausi(alkamispvm = future, paattymispvm = morefuture))
    val toteutusMetadata = Some(ammMetatieto.copy(opetus = Some(futureOpetus)))
    val toteutusOid = put(toteutus(koulutusOid).copy(metadata = toteutusMetadata, tila = Julkaistu))

    val copyOfJulkaistuToteutus = toteutus(toteutusOid, koulutusOid).copy(metadata = toteutusMetadata, tila = Julkaistu)
    val lastModified = get(toteutusOid, copyOfJulkaistuToteutus)

    val toteutusWithoutAlkamiskausi = copyOfJulkaistuToteutus.copy(metadata = Some(ammMetatieto.copy(opetus = Some(opetus.copy(koulutuksenAlkamiskausi = None)))))
    update(toteutusWithoutAlkamiskausi, lastModified)
  }

  it should "copy a temporary image to a permanent location while updating the toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))

    saveLocalPng("temp/image.png")
    val toteutusWithImage = toteutus(oid, koulutusOid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(toteutusWithImage, lastModified)
    get(oid, toteutusWithImage.withTeemakuva(Some(s"$PublicImageServer/toteutus-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"toteutus-teemakuva/$oid/image.png"))
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    val toteutusWithImage = toteutus(oid, koulutusOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(toteutusWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, toteutusWithImage.copy(oid = Some(ToteutusOid(oid))))
  }

  object ToteutusJsonMethods extends JsonMethods {
    def extractJsonString(s: String): Toteutus = {
      parse(s).extract[Toteutus]
    }
  }

  val incorrectJson: String = """{
    "oid": "1.2.246.562.17.00000000000000000067",
    "koulutusOid": "1.2.246.562.13.00000000000000000167",
    "tila": "tallennettu",
    "tarjoajat": [],
    "nimi": {
      "fi": "Metalliseppäalan osaamisala, pk (Taideteollisuusalan perustutkinto)"
    },
    "metadata": {
      "tyyppi": "amm",
      "kuvaus": {},
      "osaamisalat": [],
      "opetus": {
      "opetuskieliKoodiUrit": [
      "oppilaitoksenopetuskieli_1#1",
      "oppilaitoksenopetuskieli_2#1"
      ],
      "opetuskieletKuvaus": {},
      "opetusaikaKoodiUrit": [
      "opetusaikakk_1#1"
      ],
      "opetusaikaKuvaus": {},
      "opetustapaKoodiUrit": [],
      "opetustapaKuvaus": {},
      "maksullisuustyyppi": "maksuton",
      "maksullisuusKuvaus": {},
      "maksunMaara": {"a": "b"},
      "alkamisaikaKuvaus": {},
      "lisatiedot": [],
      "onkoStipendia": false,
      "stipendinKuvaus": {}
    },
      "asiasanat": [],
      "ammattinimikkeet": []
    },
    "muokkaaja": "1.2.246.562.24.87917166937",
    "organisaatioOid": "1.2.246.562.10.53642770753",
    "kielivalinta": [
    "fi",
    "sv",
    "en"
    ],
    "modified": "2019-10-29T15:21:23"
  }"""

  it should "fail to extract toteutus from JSON of incorrect form" in {
    an [org.json4s.MappingException] shouldBe thrownBy(ToteutusJsonMethods.extractJsonString(incorrectJson))
  }

  "When tutkintoon johtamaton, toteutus servlet" should "create, get and update ammatillinen osaamisala toteutus" in {
    val sorakuvausId = put(sorakuvaus)
    val ammOaKoulutusOid = put(TestData.AmmOsaamisalaKoulutus.copy(tila = Julkaistu))
    val ammOaToteutus = TestData.AmmOsaamisalaToteutus.copy(koulutusOid = KoulutusOid(ammOaKoulutusOid), sorakuvausId = Some(sorakuvausId), tila = Tallennettu)
    val oid = put(ammOaToteutus)
    val lastModified = get(oid, ammOaToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(ammOaToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammOaToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update ammatillinen tutkinnon osa toteutus" in {
    val sorakuvausId = put(sorakuvaus)
    val ammToKoulutusOid = put(TestData.AmmTutkinnonOsaKoulutus.copy(tila = Julkaistu))
    val ammToToteutus = TestData.AmmTutkinnonOsaToteutus.copy(koulutusOid = KoulutusOid(ammToKoulutusOid), sorakuvausId = Some(sorakuvausId), tila = Tallennettu)
    val oid = put(ammToToteutus)
    val lastModified = get(oid, ammToToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(ammToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update muu ammatillinen toteutus" in {
    val ammMuuKoulutusOid = put(TestData.AmmMuuKoulutus.copy(tila = Julkaistu), ophSession)
    val ammMuuToteutus = TestData.AmmMuuToteutus.copy(koulutusOid = KoulutusOid(ammMuuKoulutusOid), tila = Tallennettu)
    val oid = put(ammMuuToteutus)
    val lastModified = get(oid, ammMuuToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(ammMuuToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, ammMuuToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu))
  }

  it should "create, get and update lukio toteutus" in {
    val _enrichedData = Some(ToteutusEnrichedData(esitysnimi = Map(
      Fi -> s"""toteutuslomake.lukionYleislinjaNimiOsa fi, 40 yleiset.opintopistetta fi
        |lukiopainotukset_1 fi, 40 yleiset.opintopistetta fi
        |lukiolinjaterityinenkoulutustehtava_1 fi, 40 yleiset.opintopistetta fi""".stripMargin,
      Sv -> s"""toteutuslomake.lukionYleislinjaNimiOsa sv, 40 yleiset.opintopistetta sv
        |lukiopainotukset_1 sv, 40 yleiset.opintopistetta sv
        |lukiolinjaterityinenkoulutustehtava_1 sv, 40 yleiset.opintopistetta sv""".stripMargin,
      ),
      muokkaajanNimi = Some("Testi Muokkaaja")))
    val lukioToKoulutusOid = put(TestData.LukioKoulutus.copy(tila = Julkaistu), ophSession)
    val lukioToToteutus = TestData.LukioToteutus.copy(koulutusOid = KoulutusOid(lukioToKoulutusOid), tila = Tallennettu, _enrichedData = _enrichedData)
    val oid = put(lukioToToteutus)
    val lastModified = get(oid, lukioToToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(lukioToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, lukioToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu,
      _enrichedData = _enrichedData
      ))
  }

  it should "create, get and update tuva toteutus" in {
    val tuvaToKoulutusOid = put(TestData.TuvaKoulutus.copy(tila = Julkaistu), ophSession)
    val tuvaToToteutus = TestData.TuvaToteutus.copy(koulutusOid = KoulutusOid(tuvaToKoulutusOid), tila = Tallennettu)
    val oid = put(tuvaToToteutus)
    val lastModified = get(oid, tuvaToToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(tuvaToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, tuvaToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu))
  }

  it should "pass legal state changes" in {
    val koulutusOid = put(koulutus, ophSession)
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu), ophSession)
    var toteutusBase = toteutus(koulutusOid)
    toteutusBase = toteutusBase.copy(oid = Some(ToteutusOid(toteutusOid)), muokkaaja = OphUserOid,
      metadata = Some(toteutusBase.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true))))
    var lastModified = get(toteutusOid, toteutusBase.copy(tila = Tallennettu))
    update(toteutusBase.copy(tila = Julkaistu), lastModified, true, ophSession)
    lastModified = get(toteutusOid, toteutusBase.copy(tila = Julkaistu))
    update(toteutusBase.copy(tila = Arkistoitu), lastModified, true, ophSession)
    lastModified = get(toteutusOid, toteutusBase.copy(tila = Arkistoitu))
    update(toteutusBase.copy(tila = Julkaistu), lastModified, true, ophSession)
    lastModified = get(toteutusOid, toteutusBase.copy(tila = Julkaistu))
    update(toteutusBase.copy(tila = Tallennettu), lastModified, true, ophSession)
    lastModified = get(toteutusOid, toteutusBase.copy(tila = Tallennettu))
    update(toteutusBase.copy(tila = Poistettu), lastModified, true, ophSession)

    val arkistoituOid = Some(ToteutusOid(put(toteutus(koulutusOid).copy(tila = Arkistoitu), ophSession)))
    lastModified = get(arkistoituOid.get.s, toteutus(koulutusOid).copy(oid = arkistoituOid, tila = Arkistoitu, muokkaaja = OphUserOid,
      metadata = Some(toteutusBase.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true)))))
    update(toteutus(koulutusOid).copy(oid = arkistoituOid, tila = Julkaistu), lastModified, true, ophSession)
    get(arkistoituOid.get.s, toteutus(koulutusOid).copy(oid = arkistoituOid, tila = Julkaistu, muokkaaja = OphUserOid,
      metadata = Some(toteutusBase.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true)))))
  }

  it should "fail illegal state changes" in {
    val koulutusOid = put(koulutus, ophSession)

    val tallennettuOid = Some(ToteutusOid(put(toteutus(koulutusOid).copy(tila = Tallennettu), ophSession)))
    val julkaistuOid = Some(ToteutusOid(put(toteutus(koulutusOid).copy(tila = Julkaistu), ophSession)))
    val arkistoituOid = Some(ToteutusOid(put(toteutus(koulutusOid).copy(tila = Arkistoitu), ophSession)))
    var toteutusBase = toteutus(koulutusOid)
    toteutusBase = toteutusBase.copy(muokkaaja = OphUserOid, metadata = Some(toteutusBase.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true))))

    var lastModified = get(tallennettuOid.get.s, toteutusBase.copy(oid = tallennettuOid, tila = Tallennettu))
    update(ToteutusPath, toteutusBase.copy(oid = tallennettuOid, tila = Arkistoitu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("toteutukselle", Tallennettu, Arkistoitu))))
    lastModified = get(julkaistuOid.get.s, toteutusBase.copy(oid = julkaistuOid, tila = Julkaistu))
    update(ToteutusPath, toteutusBase.copy(oid = julkaistuOid, tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("toteutukselle", Julkaistu, Poistettu))))
    lastModified = get(arkistoituOid.get.s, toteutusBase.copy(oid = arkistoituOid, tila = Arkistoitu))
    update(ToteutusPath, toteutusBase.copy(oid = arkistoituOid, tila = Tallennettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("toteutukselle", Arkistoitu, Tallennettu))))
    update(ToteutusPath, toteutusBase.copy(oid = arkistoituOid, tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("toteutukselle", Arkistoitu, Poistettu))))
  }

  private def createToteutusWithHakukohteet(markAllHakukohteetDeleted: Boolean) = {
    val koulutusOid = put(koulutus.copy(tila = Tallennettu), ophSession)
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu), ophSession)
    val hakuOid = put(haku.copy(tila = Tallennettu), ophSession)
    put(hakukohde(toteutusOid, hakuOid).copy(tila = Poistettu), ophSession)
    put(hakukohde(toteutusOid, hakuOid).copy(tila = if (markAllHakukohteetDeleted) Poistettu else Tallennettu), ophSession)
    var toteutusBase = toteutus(koulutusOid).copy(oid = Some(ToteutusOid(toteutusOid)))
    val toteutusLastModified = get(toteutusOid, toteutusBase.copy(tila = Tallennettu, muokkaaja = OphUserOid, metadata = Some(toteutusBase.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(isMuokkaajaOphVirkailija = Some(true)))))
    (toteutusOid, koulutusOid, toteutusLastModified)
  }

  it should "pass deletion when related hakukohteet deleted" in {
    val (toteutusOid: String, koulutusOid: String, lastModified: String) = createToteutusWithHakukohteet(true)
    update(toteutus(koulutusOid).copy(oid = Some(ToteutusOid(toteutusOid)), tila = Poistettu), lastModified, true, ophSession)
  }

  it should "fail deletion when all related hakukohteet not deleted" in {
    val (toteutusOid: String, koulutusOid: String, lastModified: String) = createToteutusWithHakukohteet(false)
    update(ToteutusPath, toteutus(koulutusOid).copy(oid = Some(ToteutusOid(toteutusOid)), tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", integrityViolationMsg("Toteutusta", "hakukohteita"))))
  }

  "Copy toteutukset" should "make a copy of a julkaistu toteutus and store it as tallennettu" in {
    val julkaistuToteutusOid = put(toteutus(koulutusOid))
    val toteutukset = List(julkaistuToteutusOid)
    val copyResults = put(toteutukset)
    val copyOid = copyResults.head.created.toteutusOid
    get(copyOid.get.toString, toteutus(koulutusOid).copy(oid = Some(copyOid.get), tila = Tallennettu))
  }

  it should "copy two julkaistu toteutus and store them as tallennettu" in {
    val julkaistuToteutusOid1 = put(toteutus(koulutusOid))
    val julkaistuToteutusOid2 = put(toteutus(koulutusOid))
    val toteutukset = List(julkaistuToteutusOid1, julkaistuToteutusOid2)
    val copyResults = put(toteutukset)
    for (result <- copyResults) {
      val copyOid = result.created.toteutusOid.get
      get(copyOid.toString, toteutus(koulutusOid).copy(oid = Some(copyOid), tila = Tallennettu))
    }
  }

  it should "return 404 Not Found error if trying to copy a toteutus that does not exist" in {
    put(ToteutusCopyPath, bytes(List("1.2.246.562.17.123")), defaultHeaders) {
      withClue(body) {
        status should equal(404)
      }
    }
  }

  it should "create, get and update aikuisten perusopetus -toteutus" in {
    val aiPeToKoulutusOid = put(TestData.AikuistenPerusopetusKoulutus.copy(tila = Julkaistu), ophSession)
    val aiPeToToteutus = TestData.AikuistenPerusopetusToteutus.copy(koulutusOid = KoulutusOid(aiPeToKoulutusOid), tila = Tallennettu)
    val oid = put(aiPeToToteutus)
    val lastModified = get(oid, aiPeToToteutus.copy(oid = Some(ToteutusOid(oid))))
    update(aiPeToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu), lastModified)
    get(oid, aiPeToToteutus.copy(oid = Some(ToteutusOid(oid)), tila = Julkaistu))
  }
}
