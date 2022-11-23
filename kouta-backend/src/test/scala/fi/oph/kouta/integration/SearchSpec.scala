package fi.oph.kouta.integration

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.searchResults.{HakukohdeSearchResult, KoulutusSearchResult, ValintaperusteSearchResult}
import fi.oph.kouta.security.RoleEntity
import fi.oph.kouta.servlet.SearchParams
import org.json4s.jackson.Serialization.read
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach

import java.time.LocalDateTime
import java.util.UUID

class SearchSpec
    extends KoutaIntegrationSpec
    with AccessControlSpec
    with SearchFixture
    with BeforeAndAfterEach
    with MockitoSugar {

  override val roleEntities = RoleEntity.all
  override val DebugJson    = false

  var koulutus1, koulutus2, koulutus3, koulutus4, koulutus5, koulutus6: Koulutus = _
  var toteutus1, toteutus2, toteutus3, toteutus4, toteutus5, toteutus6: Toteutus = _
  var hoid1, hoid2, hoid3: String                                                = _
  var hkoid1, hkoid2, hkoid3, hkoid4, hkoid5: String                             = _
  var vpid1, vpid2, vpid3, vpid4, vpid5: String                                  = _

  var toteutusOidsByKoulutusOid: Map[KoulutusOid, List[Toteutus]] = Map()

  val params = Map("nimi" -> "Hassu", "page" -> "1", "koulutustyyppi" -> "amm")

  def barams(organisaatioOid: OrganisaatioOid): Map[String, String] = params + ("organisaatioOid" -> organisaatioOid.s)
  def mockParams(oids: List[String]): Map[String, String] =
    params + ("oids" -> oids.sorted.mkString(","))
  def mockIdParams(ids: List[String]): Map[String, String] =
    params + ("ids" -> ids.sorted.mkString(","))

  override def beforeAll(): Unit = {
    super.beforeAll()
    createTestData()
  }

  def addKoulutusMock(koulutus: Koulutus, session: UUID): Koulutus = {
    val oid = put(koulutus, session)
    koulutus.copy(oid = Some(KoulutusOid(oid)))
  }

  def addToteutusMock(toteutus: Toteutus): Toteutus = {
    val oid         = put(toteutus)
    val koulutusOid = toteutus.koulutusOid
    val t           = toteutus.copy(oid = Some(ToteutusOid(oid)))

    toteutusOidsByKoulutusOid += (koulutusOid -> (toteutusOidsByKoulutusOid.getOrElse(
      koulutusOid,
      List[Toteutus]()
    ) ++ List(t).distinct))
    t
  }

  def createTestData(): Unit = {
    koulutus1 =
      addKoulutusMock(koulutus.copy(organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid)), ophSession)
    koulutus2 = addKoulutusMock(koulutus.copy(organisaatioOid = ParentOid, tarjoajat = List(ParentOid)), ophSession)
    koulutus3 = addKoulutusMock(koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid)), ophSession)
    koulutus4 = addKoulutusMock(
      koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid), julkinen = true),
      ophSession
    )
    koulutus5 = addKoulutusMock(
      koulutus.copy(organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid, ChildOid), julkinen = true),
      ophSession
    )
    koulutus6 =
      addKoulutusMock(koulutus.copy(organisaatioOid = OphOid, tarjoajat = List(LonelyOid), julkinen = true), ophSession)

    toteutus1 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus1.oid.get, organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    )
    toteutus2 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus2.oid.get, organisaatioOid = ParentOid, tarjoajat = List(ParentOid))
    )

    toteutus3 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus3.oid.get, organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid))
    )

    toteutus4 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus4.oid.get, organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid))
    )

    toteutus5 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus5.oid.get, organisaatioOid = GrandChildOid, tarjoajat = List(GrandChildOid))
    )

    toteutus6 = addToteutusMock(
      toteutus.copy(koulutusOid = koulutus6.oid.get, organisaatioOid = LonelyOid, tarjoajat = List(LonelyOid))
    )

    /*
    hoid1 = put(haku.copy(organisaatioOid = ParentOid))
    hoid2 = put(haku.copy(organisaatioOid = LonelyOid))
    hoid3 = put(haku.copy(organisaatioOid = OphOid))

    hkoid1 = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toid1), hakuOid = HakuOid(hoid1), organisaatioOid = GrandChildOid))
    hkoid2 = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toid2), hakuOid = HakuOid(hoid1), organisaatioOid = ParentOid))
    hkoid3 = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid2), organisaatioOid = LonelyOid))
    hkoid4 = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toid3), hakuOid = HakuOid(hoid3), organisaatioOid = LonelyOid))
    hkoid5 = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toid5), hakuOid = HakuOid(hoid3), organisaatioOid = GrandChildOid))
    
    vpid1 = put(valintaperuste.copy(organisaatioOid = GrandChildOid, julkinen = false)).toString
    vpid2 = put(valintaperuste.copy(organisaatioOid = ParentOid, julkinen = false)).toString
    vpid3 = put(valintaperuste.copy(organisaatioOid = OphOid, julkinen = false)).toString
    vpid4 = put(valintaperuste.copy(organisaatioOid = LonelyOid, julkinen = false)).toString
    vpid5 = put(valintaperuste.copy(organisaatioOid = LonelyOid, julkinen = true)).toString
    */

  }

  "Koulutus search" should "search allowed koulutukset and allowed toteutus counts 1" in {
    // TODO: Tämä kannattaa ehkä muuttaa siten, että mockkaa elastic-kyselyn vastauksen.
    // Koulutukset ja toteutusten organisaatiot, jotka KoutaSearchClientin pitäisi palauttaa tämän testin kyselylle
    val koulutusJaOrganisaatiot = List(
      (koulutus1, List(GrandChildOid)),
      (koulutus2, List(ParentOid)),
      (koulutus4, List(LonelyOid)),
      (koulutus5, List(LonelyOid, ChildOid)),
      (koulutus6, List(LonelyOid))
    )
    when(
      mockKoutaSearchClient.searchKoulutukset(
        koulutusJaOrganisaatiot.map(_._1.oid.get),
        SearchParams(params)
      )
    ).thenAnswer(
      SearchResult[KoulutusSearchItemFromIndex](
        totalCount = koulutusJaOrganisaatiot.length,
        result = koulutusJaOrganisaatiot.map(kData => {
          val k             = kData._1
          val organisaatiot = kData._2
          KoulutusSearchItemFromIndex(
            oid = k.oid.get,
            nimi = k.nimi,
            koulutustyyppi = k.koulutustyyppi,
            organisaatio = IndexedOrganisaatio(
              oid = k.organisaatioOid,
              nimi = Map(Fi -> "fi"),
            ),
            muokkaaja = Muokkaaja(nimi = "Matti Muokkaaja", oid = k.muokkaaja),
            modified = k.modified.getOrElse(Modified(LocalDateTime.now())),
            tila = k.tila,
            toteutukset = toteutusOidsByKoulutusOid
              .getOrElse(k.oid.get, List())
              .map(t =>
                KoulutusSearchItemToteutus(
                  t.oid.get,
                  nimi = t.nimi,
                  organisaatio = IndexedOrganisaatio(
                    oid = t.organisaatioOid,
                    nimi = Map(Fi -> "fi"),
                  ),
                  modified = t.modified.getOrElse(Modified(LocalDateTime.now())),
                  tila = t.tila,
                  organisaatiot = organisaatiot.map(_.toString)
                )
              )
          )
        })
      )
    )

    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(ChildOid)))) {
      status should equal(200)
      debugJson[KoulutusSearchResult](body)
      val r = read[KoulutusSearchResult](body).result
      r.map(_.oid.s) should be(koulutusJaOrganisaatiot.map(_._1.oid.get.s))
      r.map(_.toteutusCount) should be(List(1, 1, 0, 1, 0))
    }
  }

  //TODO: These should be converted (as in above test) to use mocked organisaatio lists
  /*
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

  "Koulutus search" should "return empty result if there are no allowed koulutukset" in {
    get(s"$SearchPath/koulutukset", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal(200)
      read[KoulutusSearchResult](body).result.size should be(0)
    }
  }

  "Koulutus search" should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/koulutukset", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal(403)
    }
  }

  /*
  it should "return 500 if elasticsearch returns 500" in {
    addMock(mockKoulutusResponse(List(koid3, koid4, koid5, koid6), params, List(), 500))

    get(s"$SearchPath/koulutukset", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }

  // TODO: update mockToteutusResponse
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
  }
   */

  "Toteutus search" should "return empty result if there are no allowed toteutukset" in {
    get(s"$SearchPath/toteutukset", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal(200)
      read[ToteutusSearchResult](body).result.size should be(0)
    }
  }

  "Toteutus search" should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/toteutukset", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal(403)
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
  }
 */

  ignore should "return empty result if there are no allowed haut" in { //TODO: koulutustyyppi haulle?
    get(s"$SearchPath/haut", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[HakuSearchResult](body).result.size should be (0)
    }
  }

  "Haku search" should "return 403 if organisaatio is not allowed" in {
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

  /*
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
  */


/*  "Hakukohde search" should "return empty result if there are no allowed hakukohteet" in {
    get(s"$SearchPath/hakukohteet", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (200)
      read[HakukohdeSearchResult](body).result.size should be (0)
    }
  }*/

  "Hakukohde search" should "return 403 if organisaatio is not allowed" in {
    get(s"$SearchPath/hakukohteet", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
      status should equal (403)
    }
  }

  /*

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
  */

    "Valintaperuste search" should "return empty result if there are no allowed valintaperusteet"  in {
      get(s"$SearchPath/valintaperusteet", barams(YoOid), Seq(sessionHeader(readSessions(YoOid)))) {
        status should equal(200)
        read[ValintaperusteSearchResult](body).result.size should be(0)
      }
    }


    "Valintaperuste search" should "return 403 if organisaatio is not allowed" in {
      get(s"$SearchPath/valintaperusteet", barams(ChildOid), Seq(sessionHeader(readSessions(YoOid)))) {
        status should equal(403)
      }
    }
  /*

  it should "return 500 if kouta index returns 500" in {
    addMock(mockValintaperusteResponse(List(vpid3, vpid4, vpid5), params, List(), 500))

    get(s"$SearchPath/valintaperusteet", barams(LonelyOid), Seq(sessionHeader(crudSessions(LonelyOid)))) {
      status should equal (500)
    }
  }
   */

}
