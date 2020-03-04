package fi.oph.kouta.integration

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.mocks.KoutaIndexMock
import fi.oph.kouta.security.RoleEntity
import org.json4s.jackson.Serialization.read
import org.mockserver.model
import org.scalatest.BeforeAndAfterEach

class SearchSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture with SearchFixture with KoutaIndexMock with BeforeAndAfterEach  {

  override val roleEntities = RoleEntity.all
  override val DebugJson = false

  var koid1, koid2, koid3, koid4, koid5, koid6: String = _
  var toid1, toid2, toid3, toid4, toid5: String = _
  var hoid1, hoid2, hoid3: String = _
  var hkoid1, hkoid2, hkoid3, hkoid4, hkoid5: String = _
  var vpid1, vpid2, vpid3, vpid4, vpid5: String = _

  val params = Map("nimi" -> "Hassu", "page" -> "1")

  def barams(organisaatioOid: OrganisaatioOid): Map[String, String] = params + ("organisaatioOid" -> organisaatioOid.s)
  def mockParams(oids: List[String]): Map[String, String] = params + ("oids" -> oids.map(_.toString).sorted.mkString(","))
  def mockIdParams(ids: List[String]): Map[String, String] = params + ("ids" -> ids.map(_.toString).sorted.mkString(","))

  var mocks: Seq[model.HttpRequest] = Seq()

  def addMock(mockRequest: model.HttpRequest): Unit =
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
    koid6 = put(koulutus.copy(organisaatioOid = OphOid))

    toid1 = put(toteutus.copy(koulutusOid = KoulutusOid(koid1), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
    toid2 = put(toteutus.copy(koulutusOid = KoulutusOid(koid2), organisaatioOid = ParentOid, tarjoajat = List(ParentOid)))
    toid3 = put(toteutus.copy(koulutusOid = KoulutusOid(koid3), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid4 = put(toteutus.copy(koulutusOid = KoulutusOid(koid4), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid5 = put(toteutus.copy(koulutusOid = KoulutusOid(koid5), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))

    hoid1 = put(haku.copy(organisaatioOid = ParentOid))
    hoid2 = put(haku.copy(organisaatioOid = LonelyOid))
    hoid3 = put(haku.copy(organisaatioOid = OphOid))

    hkoid1 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid1), hakuOid = HakuOid(hoid1), organisaatioOid = GrandChildOid))
    hkoid2 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid2), hakuOid = HakuOid(hoid1), organisaatioOid = ParentOid))
    hkoid3 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid2), organisaatioOid = LonelyOid))
    hkoid4 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid3), organisaatioOid = LonelyOid))
    hkoid5 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid5), hakuOid = HakuOid(hoid3), organisaatioOid = GrandChildOid))

    val sorakuvausId = put(sorakuvaus)

    vpid1 = put(valintaperuste(sorakuvausId).copy(organisaatioOid = GrandChildOid, julkinen = false)).toString
    vpid2 = put(valintaperuste(sorakuvausId).copy(organisaatioOid = ParentOid, julkinen = false)).toString
    vpid3 = put(valintaperuste(sorakuvausId).copy(organisaatioOid = OphOid, julkinen = false)).toString
    vpid4 = put(valintaperuste(sorakuvausId).copy(organisaatioOid = LonelyOid, julkinen = false)).toString
    vpid5 = put(valintaperuste(sorakuvausId).copy(organisaatioOid = LonelyOid, julkinen = true)).toString
  }

  "Search koulutukset" should "search allowed koulutukset and allowed toteutus counts 1" in {
    addMock(mockKoulutusResponse(mockParams(List(koid1, koid2, koid4, koid5, koid6)), List(koid1, koid2, koid4, koid5, koid6)))

    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be (List(koid6, koid5, koid4, koid2, koid1))
      r.map(_.toteutukset) should be (List(0, 1, 0, 1, 1))
    }
  }

  it should "search allowed koulutukset and allowed toteutus counts 2" in {
    addMock(mockKoulutusResponse(mockParams(List(koid3, koid4, koid5, koid6)), List(koid3, koid4, koid5, koid6)))

    get(s"$SearchPath/koulutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be (List(koid6, koid5, koid4, koid3))
      r.map(_.toteutukset) should be (List(0, 0, 1, 1))
    }
  }

  it should "return empty result if there are allowed koulutukset but nothing match kouta index search" in {
    addMock(mockKoulutusResponse(mockParams(List(koid1, koid2, koid4, koid5, koid6)), List()))

    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      read[KoulutusSearchResult](body).result.size should be (0)
    }
  }

  it should "return empty result if there are no allowed koulutukset" in {
    get(s"$SearchPath/koulutukset", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[KoulutusSearchResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockKoulutusResponse(mockParams(List(koid3, koid4, koid5, koid6)), List(), 500))

    get(s"$SearchPath/koulutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  "Search toteutukset" should "search allowed toteutukset and allowed hakukohde counts 1" in {
    addMock(mockToteutusResponse(mockParams(List(toid1, toid2, toid5)), List(toid1, toid2, toid5)))

    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      val r = read[ToteutusSearchResult](body).result
      r.map(_.oid.s) should be (List(toid5, toid2, toid1))
      r.map(_.hakukohteet) should be (List(1, 0, 1))
    }
  }

  it should "search allowed toteutukset and allowed toteutus counts 2" in {
    addMock(mockToteutusResponse(mockParams(List(toid3, toid4)), List(toid3, toid4)))

    get(s"$SearchPath/toteutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      val r = read[ToteutusSearchResult](body).result
      r.map(_.oid.s) should be (List(toid4, toid3))
      r.map(_.hakukohteet) should be (List(0, 2))
    }
  }

  it should "return empty result if there are allowed toteutukset but nothing match kouta index search" in {
    addMock(mockToteutusResponse(mockParams(List(toid1, toid2, toid5)), List()))

    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      read[ToteutusSearchResult](body).result.size should be (0)
    }
  }

  it should "return empty result if there are no allowed toteutukset" in {
    get(s"$SearchPath/toteutukset", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[ToteutusSearchResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockToteutusResponse(mockParams(List(toid1, toid5)), List(), 500))

    get(s"$SearchPath/toteutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  "Search haut" should "search allowed haut and allowed hakukohde counts 1" in {
    addMock(mockHakuResponse(mockParams(List(hoid1, hoid3)), List(hoid1, hoid3)))

    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      val r = read[HakuSearchResult](body).result
      r.map(_.oid.s) should be (List(hoid3, hoid1))
      r.map(_.hakukohteet) should be (List(1, 1))
    }
  }

  it should "search allowed haut and allowed hakukohde counts 2" in {
    addMock(mockHakuResponse(mockParams(List(hoid2, hoid3)), List(hoid2, hoid3)))

    get(s"$SearchPath/haut", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      val r = read[HakuSearchResult](body).result
      r.map(_.oid.s) should be (List(hoid3, hoid2))
      r.map(_.hakukohteet) should be (List(1, 1))
    }
  }

  it should "return empty result if there are allowed haut but nothing match kouta index search" in {
    addMock(mockHakuResponse(mockParams(List(hoid1, hoid3)), List()))

    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      read[HakuSearchResult](body).result.size should be (0)
    }
  }

  ignore should "return empty result if there are no allowed haut" in { //TODO: koulutustyyppi haulle?
    get(s"$SearchPath/haut", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[HakuSearchResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockHakuResponse(mockParams(List(hoid1, hoid3)), List(), 500))

    get(s"$SearchPath/haut", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  "Search hakukohteet" should "search allowed hakukohteet 1" in {
    addMock(mockHakukohdeResponse(mockParams(List(hkoid1, hkoid5)), List(hkoid1, hkoid5)))

    get(s"$SearchPath/hakukohteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakukohdeSearchResult](body)
      val r = read[HakukohdeSearchResult](body).result
      r.map(_.oid.s) should be (List(hkoid5, hkoid1))
    }
  }

  it should "search allowed hakukohteet 2" in {
    addMock(mockHakukohdeResponse(mockParams(List(hkoid3, hkoid4)), List(hkoid3, hkoid4)))

    get(s"$SearchPath/hakukohteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[HakukohdeSearchResult](body)
      val r = read[HakukohdeSearchResult](body).result
      r.map(_.oid.s) should be (List(hkoid4, hkoid3))
    }
  }

  it should "return empty result if there are allowed hakukohteet but nothing match kouta index search" in {
    addMock(mockHakukohdeResponse(mockParams(List(hkoid1, hkoid5)), List()))

    get(s"$SearchPath/hakukohteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakukohdeSearchResult](body)
      read[HakukohdeSearchResult](body).result.size should be (0)
    }
  }

  it should "return empty result if there are no allowed hakukohteet" in {
    get(s"$SearchPath/hakukohteet", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[HakukohdeSearchResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/hakukohteet", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockHakukohdeResponse(mockParams(List(hkoid1, hkoid5)), List(), 500))

    get(s"$SearchPath/hakukohteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  "Search valintaperusteet" should "search allowed valintaperusteet 1" in {
    addMock(mockValintaperusteResponse(mockIdParams(List(vpid1, vpid2, vpid3, vpid5)), List(vpid1, vpid2, vpid3, vpid5)))

    get(s"$SearchPath/valintaperusteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ValintaperusteSearchResult](body)
      val r = read[ValintaperusteSearchResult](body).result
      r.map(_.id.toString) should be (List(vpid5, vpid3, vpid2, vpid1).sorted.reverse)
    }
  }

  it should "search allowed valintaperusteet 2" in {
    addMock(mockValintaperusteResponse(mockIdParams(List(vpid3, vpid4, vpid5)), List(vpid3, vpid4, vpid5)))

    get(s"$SearchPath/valintaperusteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[ValintaperusteSearchResult](body)
      val r = read[ValintaperusteSearchResult](body).result
      r.map(_.id.toString) should be (List(vpid5, vpid4, vpid3).sorted.reverse)
    }
  }

  it should "return empty result if there are allowed valintaperusteet but nothing match kouta index search" in {
    addMock(mockValintaperusteResponse(mockIdParams(List(vpid1, vpid2, vpid3, vpid5)), List()))

    get(s"$SearchPath/valintaperusteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ValintaperusteSearchResult](body)
      read[HakukohdeSearchResult](body).result.size should be (0)
    }
  }

  it should "return empty result if there are no allowed valintaperusteet" in {
    get(s"$SearchPath/valintaperusteet", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[ValintaperusteSearchResult](body).result.size should be (0)
    }
  }

  it should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/valintaperusteet", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  it should "return 500 if kouta index returns 500" in {
    addMock(mockValintaperusteResponse(mockIdParams(List(vpid3, vpid4, vpid5)), List(), 500))

    get(s"$SearchPath/valintaperusteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }
}
