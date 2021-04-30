package fi.oph.kouta.integration

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.mocks.KoutaIndexMock
import fi.oph.kouta.security.RoleEntity
import org.json4s.jackson.Serialization.read
import org.mockserver.model
import org.scalatest.BeforeAndAfterEach
import org.json4s.jackson.Serialization.write

class SearchSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture with SearchFixture with KoutaIndexMock with BeforeAndAfterEach  {


  override val roleEntities = RoleEntity.all
  override val DebugJson = false

  var koid1, koid2, koid3, koid4, koid5, koid6: String = _
  var toid1, toid2, toid3, toid4, toid5, toid6: String = _
  var hoid1, hoid2, hoid3: String = _
  var hkoid1, hkoid2, hkoid3, hkoid4, hkoid5: String = _
  var vpid1, vpid2, vpid3, vpid4, vpid5: String = _

  val params = Map("nimi" -> "Hassu", "page" -> "1")

  def barams(organisaatioOid: OrganisaatioOid): Map[String, String] = params + ("organisaatioOid" -> organisaatioOid.s)
  def mockParams(oids: List[String]): Map[String, String] = params + ("oids" -> oids.map(_.toString).sorted.mkString(","))
  def mockIdParams(ids: List[String]): Map[String, String] = params + ("ids" -> ids.map(_.toString).sorted.mkString(","))

  var mocks: Seq[model.HttpRequest] = Seq()

  def addMock(mockRequest: model.HttpRequest): Unit =
    mocks = mocks :+ mockRequest

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
    koid1 = put(koulutus.copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)), ophSession)
    koid2 = put(koulutus.copy(organisaatioOid = ParentOid, tarjoajat = List(ParentOid)), ophSession)
    koid3 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)), ophSession)
    koid4 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid), julkinen = true), ophSession)
    koid5 = put(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid, ChildOid), julkinen = true), ophSession)
    koid6 = put(koulutus.copy(organisaatioOid = OphOid, tarjoajat = List(LonelyOid), julkinen = true), ophSession)

    toid1 = put(toteutus.copy(koulutusOid = KoulutusOid(koid1), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
    toid2 = put(toteutus.copy(koulutusOid = KoulutusOid(koid2), organisaatioOid = ParentOid, tarjoajat = List(ParentOid)))
    toid3 = put(toteutus.copy(koulutusOid = KoulutusOid(koid3), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid4 = put(toteutus.copy(koulutusOid = KoulutusOid(koid4), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))
    toid5 = put(toteutus.copy(koulutusOid = KoulutusOid(koid5), organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)))
    toid6 = put(toteutus.copy(koulutusOid = KoulutusOid(koid6), organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)))

    hoid1 = put(haku.copy(organisaatioOid = ParentOid))
    hoid2 = put(haku.copy(organisaatioOid = LonelyOid))
    hoid3 = put(haku.copy(organisaatioOid = OphOid))

    hkoid1 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid1), hakuOid = HakuOid(hoid1), organisaatioOid = GrandChildOid))
    hkoid2 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid2), hakuOid = HakuOid(hoid1), organisaatioOid = ParentOid))
    hkoid3 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid2), organisaatioOid = LonelyOid))
    hkoid4 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid3), organisaatioOid = LonelyOid))
    hkoid5 = put(hakukohde.copy(toteutusOid = ToteutusOid(toid5), hakuOid = HakuOid(hoid3), organisaatioOid = GrandChildOid))

    val sorakuvausId = put(sorakuvaus)

    vpid1 = put(valintaperuste.copy(organisaatioOid = GrandChildOid, julkinen = false)).toString
    vpid2 = put(valintaperuste.copy(organisaatioOid = ParentOid, julkinen = false)).toString
    vpid3 = put(valintaperuste.copy(organisaatioOid = OphOid, julkinen = false)).toString
    vpid4 = put(valintaperuste.copy(organisaatioOid = LonelyOid, julkinen = false)).toString
    vpid5 = put(valintaperuste.copy(organisaatioOid = LonelyOid, julkinen = true)).toString
  }

  "Search koulutukset" should "search allowed koulutukset and allowed toteutus counts 1" in {
    addMock(mockKoulutusResponse(
      List(koid1, koid2, koid4, koid5, koid6),
      params,
      List(
        KoulutusResponseData(oid = koid1, organisaatiot = List(GrandChildOid.toString())),
        KoulutusResponseData(oid = koid2, organisaatiot = List(ParentOid.toString())),
        KoulutusResponseData(oid = koid4, organisaatiot = List(LonelyOid.toString())), // NOTE: tämä on ainoa jolle ei pitäisi löytyä
        KoulutusResponseData(oid = koid5, organisaatiot = List(LonelyOid.toString(), ChildOid.toString())),
        KoulutusResponseData(oid = koid6, organisaatiot = List(LonelyOid.toString())),
      )))

    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be (List(koid1, koid2, koid4, koid5, koid6))
      r.map(_.toteutusCount) should be (List(1, 1, 0, 1, 0))
    }
  }

  /* TODO: These should be converted (as in above test) to use mocked organisaatio lists
  it should "search allowed koulutukset and allowed toteutus counts 2" in {
    addMock(mockKoulutusResponse(List(koid3, koid4, koid5, koid6), params, List(koid3, koid4, koid5, koid6)))

    get(s"$SearchPath/koulutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be (List(koid6, koid5, koid4, koid3))
      r.map(_.toteutusCount) should be (List(1, 0, 1, 1))
    }
  }

  it should "search oph koulutukset and total toteutus counts for oph organisaatio" in {
    addMock(mockKoulutusResponse(List(koid4, koid5, koid6), params, List(koid4, koid5, koid6)))

    get(s"$SearchPath/koulutukset", barams(OphOid), Seq(sessionHeader(ophSession))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be (List(koid6, koid5, koid4))
      r.map(_.toteutusCount) should be (List(1, 1, 1))
    }
  }

  it should "return empty result if there are allowed koulutukset but nothing match kouta index search" in {
    addMock(mockKoulutusResponse(List(koid1, koid2, koid4, koid5, koid6), params, List()))

    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[KoulutusSearchResult](body)
      read[KoulutusSearchResult](body).result.size should be (0)
    }
  } */

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
    addMock(mockKoulutusResponse(List(koid3, koid4, koid5, koid6), params, List(), 500))

    get(s"$SearchPath/koulutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  /* TODO: update mockToteutusResponse
  "Search toteutukset" should "search allowed toteutukset and allowed hakukohde counts 1" in {
    addMock(mockToteutusResponse(List(toid1, toid2, toid5), params, List(toid1, toid2, toid5)))

    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      val r = read[ToteutusSearchResult](body).result
      r.map(_.oid.s) should be (List(toid5, toid2, toid1))
      r.map(_.hakukohdeCount) should be (List(1, 0, 1))
    }
  }

  it should "search allowed toteutukset and allowed toteutus counts 2" in {
    addMock(mockToteutusResponse(List(toid3, toid4, toid6), params, List(toid3, toid4, toid6)))

    get(s"$SearchPath/toteutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      val r = read[ToteutusSearchResult](body).result
      r.map(_.oid.s) should be (List(toid6, toid4, toid3))
      r.map(_.hakukohdeCount) should be (List(0, 0, 2))
    }
  }

  it should "return empty result if there are allowed toteutukset but nothing match kouta index search" in {
    addMock(mockToteutusResponse(List(toid1, toid2, toid5), params, List()))

    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ToteutusSearchResult](body)
      read[ToteutusSearchResult](body).result.size should be (0)
    }
  } */

  "Search toteutukset" should "return empty result if there are no allowed toteutukset" in {
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

  /*
  it should "return 500 if kouta index returns 500" in {
    addMock(mockToteutusResponse(List(toid1, toid5), params, List(), 500))

    get(s"$SearchPath/toteutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  } */

  /*
  "Search haut" should "search allowed haut and allowed hakukohde counts 1" in {
    addMock(mockHakuResponse(List(hoid1, hoid3), params, List(hoid1, hoid3)))

    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      val r = read[HakuSearchResult](body).result
      r.map(_.oid.s) should be (List(hoid3, hoid1))
      r.map(_.hakukohdeCount) should be (List(1, 1))
    }
  }

  it should "search allowed haut and allowed hakukohde counts 2" in {
    addMock(mockHakuResponse(List(hoid2, hoid3), params, List(hoid2, hoid3)))

    get(s"$SearchPath/haut", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      val r = read[HakuSearchResult](body).result
      r.map(_.oid.s) should be (List(hoid3, hoid2))
      r.map(_.hakukohdeCount) should be (List(1, 1))
    }
  }

  it should "return empty result if there are allowed haut but nothing match kouta index search" in {
    addMock(mockHakuResponse(List(hoid1, hoid3), params, List()))

    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakuSearchResult](body)
      read[HakuSearchResult](body).result.size should be (0)
    }
  } */

  ignore should "return empty result if there are no allowed haut" in { //TODO: koulutustyyppi haulle?
    get(s"$SearchPath/haut", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[HakuSearchResult](body).result.size should be (0)
    }
  }

  "Search haut" should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/haut", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  /*
  it should "return 500 if kouta index returns 500" in {
    addMock(mockHakuResponse(List(hoid1, hoid3), params, List(), 500))

    get(s"$SearchPath/haut", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }*/

  "Search hakukohteet" should "search allowed hakukohteet 1" in {
    addMock(mockHakukohdeResponse(List(hkoid1, hkoid5), params, List(hkoid1, hkoid5)))

    get(s"$SearchPath/hakukohteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[HakukohdeSearchResult](body)
      val r = read[HakukohdeSearchResult](body).result
      r.map(_.oid.s) should be (List(hkoid5, hkoid1))
    }
  }

  it should "search allowed hakukohteet 2" in {
    addMock(mockHakukohdeResponse(List(hkoid3, hkoid4), params, List(hkoid3, hkoid4)))

    get(s"$SearchPath/hakukohteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[HakukohdeSearchResult](body)
      val r = read[HakukohdeSearchResult](body).result
      r.map(_.oid.s) should be (List(hkoid4, hkoid3))
    }
  }

  it should "return empty result if there are allowed hakukohteet but nothing match kouta index search" in {
    addMock(mockHakukohdeResponse(List(hkoid1, hkoid5), params, List()))

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
    addMock(mockHakukohdeResponse(List(hkoid1, hkoid5), params, List(), 500))

    get(s"$SearchPath/hakukohteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  "Search valintaperusteet" should "search allowed valintaperusteet 1" in {
    addMock(mockValintaperusteResponse(List(vpid1, vpid2, vpid3, vpid5), params, List(vpid1, vpid2, vpid3, vpid5)))

    get(s"$SearchPath/valintaperusteet", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal (200)
      debugJson[ValintaperusteSearchResult](body)
      val r = read[ValintaperusteSearchResult](body).result
      r.map(_.id.toString) should be (List(vpid5, vpid3, vpid2, vpid1).sorted.reverse)
    }
  }

  it should "search allowed valintaperusteet 2" in {
    addMock(mockValintaperusteResponse(List(vpid3, vpid4, vpid5), params, List(vpid3, vpid4, vpid5)))

    get(s"$SearchPath/valintaperusteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (200)
      debugJson[ValintaperusteSearchResult](body)
      val r = read[ValintaperusteSearchResult](body).result
      r.map(_.id.toString) should be (List(vpid5, vpid4, vpid3).sorted.reverse)
    }
  }

  it should "return empty result if there are allowed valintaperusteet but nothing match kouta index search" in {
    addMock(mockValintaperusteResponse(List(vpid1, vpid2, vpid3, vpid5), params, List()))

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
    addMock(mockValintaperusteResponse(List(vpid3, vpid4, vpid5), params, List(), 500))

    get(s"$SearchPath/valintaperusteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }
}
