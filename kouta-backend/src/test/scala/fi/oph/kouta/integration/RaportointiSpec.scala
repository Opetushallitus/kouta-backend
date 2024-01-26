package fi.oph.kouta.integration

import fi.oph.kouta.TestOids.{AmmOid, EvilChildOid, EvilGrandChildOid, GrandChildOid, LukioOid, YoOid}
import fi.oph.kouta.integration.fixture.RaportointiFixture

import java.util.UUID

class RaportointiSpec extends KoutaIntegrationSpec with RaportointiFixture {
  var sorakuvausId1, sorakuvausId2, sorakuvausId3: UUID                        = _
  var valintaperusteId1, valintaperusteId2, valintaperusteId3: UUID            = _
  var koulutusOid1, koulutusOid2, koulutusOid3: String                         = _
  var toteutusOid1, toteutusOid2, toteutusOid3: String                         = _
  var hakuOid1, hakuOid2, hakuOid3: String                                     = _
  var hakukohdeOid1, hakukohdeOid2, hakukohdeOid3: String                      = _
  val oppilaitosOid1                                                           = YoOid
  val oppilaitosOid2                                                           = AmmOid
  val oppilaitosOid3                                                           = LukioOid
  val oppilaitoksenOsaOid1                                                     = EvilChildOid
  val oppilaitoksenOsaOid2                                                     = GrandChildOid
  val oppilaitoksenOsaOid3                                                     = EvilGrandChildOid

  override def beforeAll(): Unit = {
    super.beforeAll()

    sorakuvausId1 = put(sorakuvaus, ophSession)
    sorakuvausId2 = put(sorakuvaus, ophSession)
    sorakuvausId3 = put(sorakuvaus, ophSession)
    koulutusOid1 = put(koulutus.copy(sorakuvausId = Some(sorakuvausId1)), ophSession)
    koulutusOid2 = put(koulutus.copy(sorakuvausId = Some(sorakuvausId2)), ophSession)
    koulutusOid3 = put(koulutus.copy(sorakuvausId = Some(sorakuvausId3)), ophSession)
    toteutusOid1 = put(toteutus(koulutusOid1), ophSession)
    toteutusOid2 = put(toteutus(koulutusOid2), ophSession)
    toteutusOid3 = put(toteutus(koulutusOid3), ophSession)
    hakuOid1 = put(haku, ophSession);
    hakuOid2 = put(haku, ophSession);
    hakuOid3 = put(haku, ophSession);
    valintaperusteId1 = put(valintaperuste, ophSession)
    valintaperusteId2 = put(valintaperuste, ophSession)
    valintaperusteId3 = put(valintaperuste, ophSession)
    hakukohdeOid1 =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid1, hakuOid1, valintaperusteId1)), ophSession)
    hakukohdeOid2 =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid2, hakuOid2, valintaperusteId2)), ophSession)
    hakukohdeOid3 =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid3, hakuOid3, valintaperusteId3)), ophSession)
    put(oppilaitos(oppilaitosOid1.s), ophSession)
    put(oppilaitos(oppilaitosOid2.s), ophSession)
    put(oppilaitos(oppilaitosOid3.s), ophSession)
    put(oppilaitoksenOsa(oppilaitoksenOsaOid1.s, oppilaitosOid1.s), ophSession)
    put(oppilaitoksenOsa(oppilaitoksenOsaOid2.s, oppilaitosOid2.s), ophSession)
    put(oppilaitoksenOsa(oppilaitoksenOsaOid3.s, oppilaitosOid3.s), ophSession)
    storeAsiasanat()
    storeAmmattinimikkeet()
  }

  "Save koulutukset with given timerange" should "save koulutukset as requested" in {
    get("koulutukset", dayBefore, dayAfter, 200)
    verifyContents(Seq(koulutusOid1, koulutusOid2, koulutusOid3), "oid")
  }

  "Save toteutukset with given timerange" should "save toteutukset as requested" in {
    get("toteutukset", None, dayAfter, 200)
    verifyContents(Seq(toteutusOid1, toteutusOid2, toteutusOid3), "oid")
  }

  "Save hakukohteet with given timerange" should "save hakukohteet as requested" in {
    get("hakukohteet", dayBefore, dayAfter, 200)
    verifyContents(Seq(hakukohdeOid1, hakukohdeOid2, hakukohdeOid3), "oid")
  }

  "Save haut with given timerange" should "save haut as requested" in {
    get("haut", None, dayAfter, 200)
    verifyContents(Seq(hakuOid1, hakuOid2, hakuOid3), "oid")
  }

  "Save sorakuvaukset with given timerange" should "save sorakuvaukset as requested" in {
    get("sorakuvaukset", dayBefore, dayAfter, 200)
    verifyContents(Seq(sorakuvausId1.toString, sorakuvausId2.toString, sorakuvausId3.toString), "id")
  }

  "Save valintaperusteet with given timerange" should "save valintaperusteet as requested" in {
    get("valintaperusteet", None, dayAfter, 200)
    verifyContents(Seq(valintaperusteId1.toString, valintaperusteId2.toString, valintaperusteId3.toString), "id")
  }

  "Save oppilaitokset with given timerange" should "save oppilaitokset as requested" in {
    get("oppilaitokset", dayBefore, dayAfter, 200)
    verifyContents(Seq(oppilaitosOid1.s, oppilaitosOid2.s, oppilaitosOid3.s), "oid")
  }

  "Save oppilaitoksenosat with given timerange" should "save oppilaitoksenosat as requested" in {
    get("oppilaitoksenosat", None, dayAfter, 200)
    verifyContents(Seq(oppilaitoksenOsaOid1.s, oppilaitoksenOsaOid2.s, oppilaitoksenOsaOid3.s), "oid")
  }

  "Save ammattinimikkeet" should "save ammattinimikkeet as requested" in {
    get("ammattinimikkeet", None, None, 200)
    verifyKeywordContents(ammattinimikkeet)
  }

  "Save asiasanat" should "save asiasanat as requested" in {
    get("asiasanat", None, None, 200)
    verifyKeywordContents(asiasanat)
  }

  "Save entities without timerange" should "save entities ok" in {
    get("koulutukset", None, None, 200)
    verifyContents(Seq(koulutusOid1, koulutusOid2, koulutusOid3), "oid")
  }

  "Save entities with invalid time definition" should "return error" in {
    get(s"$RaportointiPath/koulutukset?startTime=puppua", headers = Seq(sessionHeader(raportointiSession))) {
      status should equal(400)
    }
  }

  "Save entities with future situated starttime" should "return error" in {
    get("koulutukset", dayAfter, None, 400)
  }

  "Save entities with illegal timerange" should "return error" in {
    get("koulutukset", dayBefore, twoDaysBefore, 400)
  }
}
