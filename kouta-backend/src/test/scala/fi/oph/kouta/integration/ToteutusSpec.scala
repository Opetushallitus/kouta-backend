package fi.oph.kouta.integration

import java.time.LocalDateTime

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture._
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.{TestData, TestOids}
import org.json4s.jackson.JsonMethods

class ToteutusSpec extends KoutaIntegrationSpec
  with AccessControlSpec with KoulutusFixture with ToteutusFixture with SorakuvausFixture
  with KeywordFixture with UploadFixture {


  override val roleEntities = Seq(Role.Toteutus, Role.Koulutus)

  var koulutusOid: String = _

  override def beforeAll(): Unit = {
    super.beforeAll()
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
    put(ToteutusPath, toteutus, 400, "metadata.tyyppi", s"Tyyppi ei vastaa koulutuksen ($koulutusOid) tyyppiä")
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
    update(toteutus(oid, koulutusOid, Arkistoitu).copy(modified = Some(LocalDateTime.parse("1000-01-01T12:00:00"))), lastModified)
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
      tarjoajat = List(LonelyOid, OtherOid, AmmOid))
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
    val unfinishedToteutus = Toteutus(muokkaaja = TestUserOid, koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = ChildOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "toteutus"))
    val oid = put(unfinishedToteutus)
    val lastModified = get(oid, unfinishedToteutus.copy(oid = Some(ToteutusOid(oid))))
    val newKoulutusOid = put(koulutus, ophSession)
    val newUnfinishedToteutus = unfinishedToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(newKoulutusOid))
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

    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausiUUSI = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val thisToteutus = toteutus(koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Julkaistu)

    put(thisToteutus.copy(tila = Tallennettu))

    put(ToteutusPath, bytes(thisToteutus), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(List(
        ValidationError("metadata.opetus.koulutuksenAlkamiskausiUUSI.koulutuksenAlkamispaivamaara", pastDateMsg(morePast)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausiUUSI.koulutuksenPaattymispaivamaara", pastDateMsg(past)),
      )))
    }
  }

  it should "validate koulutuksen alkamisen dates when moving from other states to julkaistu" in {
    val (past, morePast) = (TestData.inPast(5000), TestData.inPast(60000))
    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausiUUSI = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val thisToteutus = toteutus(koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Tallennettu)

    val oid = put(thisToteutus)
    val thisToteutusWithOid = toteutus(oid, koulutusOid).copy(metadata = Some(ammMetatieto.copy(opetus = Some(inPastOpetus))), tila = Tallennettu)

    val lastModified = get(oid, thisToteutusWithOid)

    post(ToteutusPath, bytes(thisToteutusWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(List(
        ValidationError("metadata.opetus.koulutuksenAlkamiskausiUUSI.koulutuksenAlkamispaivamaara", pastDateMsg(morePast)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausiUUSI.koulutuksenPaattymispaivamaara", pastDateMsg(past)),
      )))
    }

    update(thisToteutusWithOid.copy(tila = Arkistoitu), lastModified)
  }

  it should "not validate koulutuksen alkamisen dates when updating a julkaistu toteutus" in {
    val (past, morePast) = (TestData.inPast(5000), TestData.inPast(60000))
    val inPastOpetus = opetus.copy(koulutuksenAlkamiskausiUUSI = pastAlkamiskausi(alkamispvm = morePast, paattymispvm = past))
    val inPastMetadata = ammMetatieto.copy(opetus = Some(inPastOpetus))

    val oid = put(toteutus(koulutusOid).copy(tila = Julkaistu))
    val thisToteutus = toteutus(oid, koulutusOid)

    val lastModified = get(oid, thisToteutus)

    update(thisToteutus.copy(metadata = Some(inPastMetadata)), lastModified)
  }

  it should "allow missing koulutuksen alkamiskausi" in {
    val (future, morefuture) = (TestData.inFuture(5000), TestData.inFuture(6000))

    val futureOpetus = opetus.copy(koulutuksenAlkamiskausiUUSI = pastAlkamiskausi(alkamispvm = future, paattymispvm = morefuture))
    val toteutusMetadata = Some(ammMetatieto.copy(opetus = Some(futureOpetus)))
    val toteutusOid = put(toteutus(koulutusOid).copy(metadata = toteutusMetadata, tila = Julkaistu))

    val copyOfJulkaistuToteutus = toteutus(toteutusOid, koulutusOid).copy(metadata = toteutusMetadata, tila = Julkaistu)
    val lastModified = get(toteutusOid, copyOfJulkaistuToteutus)

    val toteutusWithoutAlkamiskausi = copyOfJulkaistuToteutus.copy(metadata = Some(ammMetatieto.copy(opetus = Some(opetus.copy(koulutuksenAlkamiskausiUUSI = None)))))
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
      "onkoMaksullinen": false,
      "maksullisuusKuvaus": {},
      "maksunMaara": {"a": "b"},
      "alkamisaikaKuvaus": {},
      "lisatiedot": [],
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
    "modified": "2019-10-29T15:21"
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
}
