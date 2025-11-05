package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID
import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.{AmmValintaperusteMetadata, MinYoValintaperuste}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.{Fi, Sv, _}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.integration.fixture.ValintaperusteFixture
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._
import org.json4s.jackson.Serialization

class ValintaperusteSpec extends KoutaIntegrationSpec with ValintaperusteFixture {

  override val roleEntities: Seq[RoleEntity] = Seq(Role.Valintaperuste)

  def ophValintaperuste: Valintaperuste = valintaperuste.copy(organisaatioOid = OphOid)

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  "Get valintaperuste by id" should "return 404 if valintaperuste not found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}", headers = defaultHeaders) {
      status should equal(404)
      body should include("Unknown valintaperuste id")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the valintaperuste organization to read the valintaperuste" in {
    val id = put(valintaperuste)
    get(id, crudSessions(valintaperuste.organisaatioOid), valintaperuste(id))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the valintaperuste" in {
    val id = put(valintaperuste)
    get(id, crudSessions(ParentOid), valintaperuste(id))
  }

  it should "allow a user with only access to a descendant organization" in {
    val id = put(valintaperuste)
    get(id, crudSessions(GrandChildOid), valintaperuste(id))
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", otherRoleSession, 403)
  }

  it should "allow the user of proper koulutustyyppi to read julkinen valintaperuste" in {
    val id = put(valintaperuste.copy(julkinen = true))
    get(id, readSessions(AmmOid), valintaperuste.copy(julkinen = true, id = Some(id)))
  }

  it should "deny the user of wrong koulutustyyppi to read julkinen valintaperuste" in {
    val id = put(valintaperuste.copy(julkinen = true))
    get(s"$ValintaperustePath/$id", readSessions(YoOid), 403)
  }

  it should "allow indexer access" in {
    val id = put(valintaperuste)
    get(id, indexerSession, valintaperuste(id))
  }

  it should "return error when trying to get deleted valintaperuste" in {
    val oid = put(valintaperuste.copy(tila = Poistettu))
    get(s"$ValintaperustePath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted valintaperuste with myosPoistetut = true" in {
    val oid = put(valintaperuste.copy(tila = Poistettu))
    get(s"$ValintaperustePath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create valintaperuste" should "store valintaperuste" in {
    val id = put(valintaperuste)
    get(id, valintaperuste(id))
  }

  it should "read muokkaaja from the session" in {
    val id = put(valintaperuste.copy(muokkaaja = UserOid("random")))
    get(id, valintaperuste(id).copy(muokkaaja = testUser.oid))
  }

  it should "write create valintaperuste in audit log" in {
    MockAuditLogger.clean()
    val id = put(valintaperuste.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(id.toString, "valintaperuste_create")
    MockAuditLogger.find("1000-01-01") should not be defined
    get(id, valintaperuste(id))
  }

  it should "store korkeakoulutus valintaperuste" in {
    val id = put(TestData.YoValintaperuste)
    get(id, TestData.YoValintaperuste.copy(id = Some(id)))
  }

  it should "return 401 if no session is found" in {
    put(s"$ValintaperustePath", bytes(valintaperuste)) {
      status should equal(401)
    }
  }

  it should "allow a user of the valintaperuste organization and koulutustyyppi to create valintaperuste" in {
    put(valintaperuste, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    put(ValintaperustePath, valintaperuste, crudSessions(LonelyOid), 403)
  }

  it should "deny a user with wrong koulutustyyppi" in {
    put(ValintaperustePath, valintaperuste.copy(organisaatioOid = YoOid), crudSessions(YoOid), 403)
  }

  it should "allow a user of an ancestor organization to create valintaperuste" in {
    put(valintaperuste, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(ValintaperustePath, valintaperuste, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(ValintaperustePath, valintaperuste, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(ValintaperustePath, valintaperuste, indexerSession, 403)
  }

  it should "validate new valintaperuste" in {
    put(ValintaperustePath, bytes(valintaperuste.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  "Update valintaperuste" should "update valintaperuste" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val id           = put(valintaperuste, crudSessions(ChildOid))
    val userOid      = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(id, valintaperuste(id).copy(muokkaaja = userOid))
    update(getIds(valintaperuste(id, Arkistoitu).copy(muokkaaja = userOid)), lastModified)
    get(id, valintaperuste(id, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "write valintaperuste update to audit log" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean()
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", id.toString, "valintaperuste_update") shouldBe defined
  }

  it should "write valintaperuste update to audit log without truncating long values" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean(true)
    val part = "Jukolan talo, eteläisessä Hämeessä, seisoo erään mäen pohjoisella rinteellä, liki Toukolan kylää."
    val longString = (1 to 4000 / part.length).map(_=>part).mkString(" ")
    val hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> longString)
    val updated = valintaperuste(id).copy(metadata = Some(AmmValintaperusteMetadata.copy(hakukelpoisuus = hakukelpoisuus)))

    update(getIds(updated), lastModified)

    val logRow = MockAuditLogger.find("metadata.hakukelpoisuus.sv")
    logRow shouldBe defined
    logRow.get should include(longString)
  }

  it should "not update valintaperuste" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean()
    update(tallennettuValintaperuste(id), lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(id, valintaperuste(id)) should equal(lastModified)
  }

  it should "not update muokkaaja of valintaperuste if there are no changes" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(
      tallennettuValintaperuste(id),
      lastModified,
      expectUpdate = false,
      crudSessions(valintaperuste.organisaatioOid)
    )
    MockAuditLogger.logs should not be empty // audit log is updated anyway
    get(id, valintaperuste(id)) should equal(lastModified)
    get(s"$ValintaperustePath/$id", headers = defaultHeaders) {
      status should equal(200)
      val valintaperusteResult: Valintaperuste = Serialization.read[Valintaperuste](body)
      valintaperusteResult.muokkaaja.shouldEqual(TestUserOid)
    }
  }

  it should "return 401 if no session is found" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal(401)
    }
  }

  it should "allow a user of the valintaperuste organization to update the valintaperuste" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(
      tallennettuValintaperuste(id),
      lastModified,
      expectUpdate = false,
      crudSessions(valintaperuste.organisaatioOid)
    )
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to update valintaperuste" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, expectUpdate = false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, readSessions(valintaperuste.organisaatioOid))
  }

  it should "deny indexer access" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val id = put(valintaperuste)
    post(ValintaperustePath, bytes(valintaperuste(id)), defaultHeaders) {
      status should equal(400)
      body should include(KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    post(ValintaperustePath, bytes(valintaperuste(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal(409)
    }
  }

  it should "update valintakokeet" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    val uusiValintaperuste = valintaperuste(id).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv"),
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2")))
    )
    update(getIds(uusiValintaperuste), lastModified, expectUpdate = true)
    get(id, uusiValintaperuste)
  }

  it should "put, update and delete valintakokeet correctly" in {
    val valintaperusteWithValintakokeet = valintaperuste.copy(valintakokeet =
      Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
    )
    val id            = put(valintaperusteWithValintakokeet)
    val lastModified  = get(id, getIds(valintaperusteWithValintakokeet.copy(id = Some(id))))
    val newValintakoe = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updateValintakoe =
      getIds(valintaperuste(id)).valintakokeet.head.copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    update(valintaperuste(id).copy(valintakokeet = Seq(newValintakoe, updateValintakoe)), lastModified)
    get(id, getIds(valintaperuste(id).copy(valintakokeet = Seq(newValintakoe, updateValintakoe))))
  }

  it should "update muokkaaja of valintaperuste on put, update and delete for valintakokeet" in {
    val valintakokeet =
      Seq(TestData.Valintakoe1)
    val valintaperusteWithValintakokeet = valintaperuste.copy(valintakokeet = valintakokeet)
    val id                              = put(valintaperusteWithValintakokeet)
    assert(readValintaperusteMuokkaaja(id.toString) == TestUserOid.toString)
    val valintaperusteetWithId = valintaperusteWithValintakokeet.copy(id = Some(id))
    val valintakokeetWithIds = getIds(valintaperusteetWithId).valintakokeet
    var lastModified       = get(id, valintaperusteetWithId)

    // päivitetään valintakoe
    val updatedValintakoe = valintakokeetWithIds.head.copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    var updatedValintaperusteetWithId = valintaperusteetWithId.copy(valintakokeet = Seq(updatedValintakoe))
    update(updatedValintaperusteetWithId, lastModified, expectUpdate = true, crudSessions(valintaperuste.organisaatioOid))
    var updatedMuokkaaja = readValintaperusteMuokkaaja(id.toString)
    assert(updatedMuokkaaja === userOidForTestSessionId(crudSessions(valintaperuste.organisaatioOid)).toString)

    get(s"$ValintaperustePath/$id", headers = defaultHeaders) {
      status should equal(200)
      val valintaperuste: Valintaperuste = Serialization.read[Valintaperuste](body)
      valintaperuste.valintakokeet.size.shouldEqual(1)
      valintaperuste.muokkaaja.shouldEqual(userOidForTestSessionId(crudSessions(valintaperuste.organisaatioOid)))
    }

    // lisätään valintakoe
    lastModified       = get(id, updatedValintaperusteetWithId.copy(muokkaaja = UserOid(updatedMuokkaaja)))
    val newValintakoe      = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updatedValintakokeetWithIds = getIds(valintaperusteetWithId).valintakokeet
    updatedValintaperusteetWithId = valintaperusteetWithId.copy(valintakokeet = updatedValintakokeetWithIds :+ newValintakoe)
    update(
      updatedValintaperusteetWithId,
      lastModified,
      expectUpdate = true
    )
    updatedMuokkaaja = readValintaperusteMuokkaaja(id.toString)
    assert(updatedMuokkaaja === TestUserOid.toString)

    get(s"$ValintaperustePath/$id", headers = defaultHeaders) {
      status should equal(200)
      val valintaperuste: Valintaperuste = Serialization.read[Valintaperuste](body)
      valintaperuste.valintakokeet.size.shouldEqual(2)
      valintaperuste.muokkaaja.shouldEqual(TestUserOid)
    }
    // poistetaan valintakoe
    lastModified = getLastModified(id)
    update(valintaperusteetWithId.copy(valintakokeet = updatedValintakokeetWithIds), lastModified, expectUpdate = true, crudSessions(valintaperuste.organisaatioOid))
    updatedMuokkaaja = readValintaperusteMuokkaaja(id.toString)
    assert(updatedMuokkaaja == userOidForTestSessionId(crudSessions(valintaperuste.organisaatioOid)).toString)

    get(s"$ValintaperustePath/$id", headers = defaultHeaders) {
      status should equal(200)

      val valintaperuste: Valintaperuste = Serialization.read[Valintaperuste](body)
      valintaperuste.valintakokeet.size.shouldEqual(1)
      valintaperuste.muokkaaja.shouldEqual(userOidForTestSessionId(crudSessions(valintaperuste.organisaatioOid)))
    }

    // poistetaan kaikki valintakokeet
    lastModified       = get(id, updatedValintaperusteetWithId.copy(valintakokeet = updatedValintakokeetWithIds, muokkaaja = UserOid(updatedMuokkaaja)))
    update(valintaperusteetWithId.copy(valintakokeet = List()), lastModified, expectUpdate = true)
    assert(readValintaperusteMuokkaaja(id.toString) == TestUserOid.toString)

    get(s"$ValintaperustePath/$id", headers = defaultHeaders) {
      status should equal(200)

      val valintaperuste: Valintaperuste = Serialization.read[Valintaperuste](body)
      valintaperuste.valintakokeet shouldBe empty
      valintaperuste.muokkaaja.shouldEqual(TestUserOid)
    }
  }

  it should "delete all valintakokeet and read last modified from history" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    val uusiValintaperuste = getIds(valintaperuste(id).copy(valintakokeet = List()))
    update(uusiValintaperuste, lastModified, expectUpdate = true)
    get(id, uusiValintaperuste) should not equal lastModified
  }

  it should "add right amount of rows to history tables" in {
    resetTableHistory("valintaperusteet")
    resetTableHistory("valintaperusteiden_valintakokeet")

    val id                 = put(valintaperuste)
    val lastModified       = get(id, valintaperuste(id))
    val uusiValintaperuste = getIds(valintaperuste(id).copy(valintakokeet = List()))
    update(uusiValintaperuste, lastModified, expectUpdate = true)

    assert(getTableHistorySize("valintaperusteet") == 1)
    assert(getTableHistorySize("valintaperusteiden_valintakokeet") == 1)
  }

  it should "store and update unfinished valintaperuste" in {
    val unfinishedValintaperuste    = MinYoValintaperuste
    val id                          = put(unfinishedValintaperuste)
    val lastModified                = get(id, unfinishedValintaperuste.copy(id = Some(id)))
    val newUnfinishedValintaperuste = unfinishedValintaperuste.copy(id = Some(id), organisaatioOid = LonelyOid)
    update(newUnfinishedValintaperuste, lastModified)
    get(id, newUnfinishedValintaperuste)
  }

  it should "validate updated valintaperuste" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(
      ValintaperustePath,
      bytes(valintaperuste(id).copy(organisaatioOid = OrganisaatioOid("saippua"))),
      headersIfUnmodifiedSince(lastModified)
    ) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "allow oph user to update from julkaistu to tallennettu" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id).copy(tila = Tallennettu), lastModified, expectUpdate = true, ophSession)
    get(
      id,
      valintaperuste(id).copy(
        tila = Tallennettu,
        muokkaaja = OphUserOid,
        metadata = Some(TestData.AmmValintaperusteMetadata.copy(isMuokkaajaOphVirkailija = Some(true)))
      )
    )
  }

  it should "not allow non oph user to update from julkaistu to tallennettu" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id).copy(tila = Tallennettu), lastModified, 403, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "allow organisaatioOid change if user had rights to new organisaatio" in {
    val id           = put(valintaperuste.copy(organisaatioOid = HkiYoOid))
    val lastModified = get(id, valintaperuste(id).copy(organisaatioOid = HkiYoOid))
    update(valintaperuste(id).copy(organisaatioOid = YoOid), lastModified, expectUpdate = true, yliopistotSession)
  }

  it should "fail organisaatioOid change if user doesn't have rights to new organisaatio" in {
    val id           = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id).copy(organisaatioOid = YoOid), lastModified, 403, crudSessions(YoOid))
  }

}
