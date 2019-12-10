package fi.oph.kouta.integration

import fi.oph.kouta.KoutaIndexMock
import fi.oph.kouta.client.{KoulutusResult, KoutaClientSpec}
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.security.RoleEntity
import org.json4s.jackson.Serialization.read
import org.mockserver.model
import org.scalatest.BeforeAndAfterEach

class SearchSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture with SearchFixture with KoutaIndexMock with BeforeAndAfterEach  {

  override val roleEntities = RoleEntity.all
  override val DebugJson = true

  var koid1, koid2, koid3, koid4, koid5: String = null
  var toid1, toid2, toid3, toid4, toid5: String = null

  val params = Map("nimi" -> "Hassu", "page" -> "1")

  def barams(organisaatioOid: OrganisaatioOid): Map[String, String] = params + ("organisaatioOid" -> organisaatioOid.s)
  def mockParams(oids: List[String]): Map[String, String] = params + ("oids" -> oids.mkString(","))

  var mocks: Seq[model.HttpRequest] = Seq()

  def addMock(mockRequest: model.HttpRequest) =
    mocks = mocks ++ Seq(mockRequest)

  override def beforeAll(): Unit = {
    super.beforeAll()
    createTestData()
  }

  override def afterEach {
    super.afterEach()
    mocks.foreach(clearMock)
    mocks = Seq()
  }

  def createTestData(): Unit = {
    koid1 = put(koulutus.copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
    koid2 = put(koulutus.copy(organisaatioOid = ParentOid, tarjoajat = List(ParentOid)))
    koid3 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    koid4 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid), julkinen = true))
    koid5 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid, ChildOid), julkinen = true))

    toid1 = put(toteutus.copy(koulutusOid = KoulutusOid(koid1), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
    toid2 = put(toteutus.copy(koulutusOid = KoulutusOid(koid2), organisaatioOid = ParentOid, tarjoajat = List(ParentOid)))
    toid3 = put(toteutus.copy(koulutusOid = KoulutusOid(koid3), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid4 = put(toteutus.copy(koulutusOid = KoulutusOid(koid4), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid5 = put(toteutus.copy(koulutusOid = KoulutusOid(koid5), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
  }

  "Search koulutukset" should "search allowed koulutukset and allowed toteutus counts 1" in {
    addMock(mockKoulutusResponse(mockParams(List(koid1, koid2, koid4, koid5)), List(koid1, koid2, koid4, koid5)))

    get(s"$SearchPath/koulutus", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusResult](body)
      val r = read[KoulutusResult](body).result
      r.map(_.oid.s) should be (List(koid5, koid4, koid2, koid1))
      r.map(_.toteutukset) should be (List(1, 0, 0, 1))
    }
  }

  it should "search allowed koulutukset and allowed toteutus counts 2" in {
    addMock(mockKoulutusResponse(mockParams(List(koid3, koid4, koid5)), List(koid3, koid4, koid5)))

    get(s"$SearchPath/koulutus", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[KoulutusResult](body)
      val r = read[KoulutusResult](body).result
      r.map(_.oid.s) should be (List(koid5, koid4, koid3))
      r.map(_.toteutukset) should be (List(0, 1, 1))
    }
  }

  it should "return empty result if there are allowed koulutukset but nothing match kouta index search" in {
    addMock(mockKoulutusResponse(mockParams(List(koid1, koid2, koid4, koid5)), List()))

    get(s"$SearchPath/koulutus", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusResult](body)
      read[KoulutusResult](body).result.size should be (0)
    }
  }

  it should "return empty result if there are no allowed koulutukset" in {
    get(s"$SearchPath/koulutus", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[KoulutusResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/koulutus", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockKoulutusResponse(mockParams(List(koid3, koid4, koid5)), List(koid3, koid4, koid5), 500))

    get(s"$SearchPath/koulutus", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }
}