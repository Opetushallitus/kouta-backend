package fi.oph.kouta.integration

import java.net.URLEncoder
import java.time.Instant.now
import java.util.UUID

import fi.oph.kouta.TestData.inFuture
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.security.Role
import fi.oph.kouta.util.TimeUtils.renderHttpDate
import org.json4s.jackson.Serialization.read


class ModificationSpec extends IndexerFixture {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createTestData(15)
    updateTestData()
  }

  def iHakukohde(i: Int): Hakukohde = withValintaperusteenValintakokeet(hakukohde(hakukohdeOids(i), toteutusOids(i), hakuOids(i), valintaperusteIds(i)).copy(tila = Tallennettu))

  var koulutusOids: List[String] = List()
  var toteutusOids: List[String] = List()
  var hakuOids: List[String] = List()
  var valintaperusteIds: List[UUID] = List()
  var hakukohdeOids: List[String] = List()
  var sorakuvausIds: List[UUID] = List()

  var timestampBeforeAllModifications = ""
  var timestampAfterInserts = ""
  var timestampAfterAllModifications = ""

  def createTestData(n: Int = 10): Unit = {
    timestampBeforeAllModifications = renderHttpDate(now())
    Thread.sleep(1000)
    koulutusOids = List.fill(n)(put(koulutus.copy(tila = Tallennettu), ophSession))
    toteutusOids = koulutusOids.map(oid => put(toteutus(oid).copy(tila = Tallennettu)))
    hakuOids = List.fill(n)(put(haku))
    sorakuvausIds = List.fill(n)(put(sorakuvaus))
    valintaperusteIds = List.fill(n)(put(valintaperuste))
    hakukohdeOids = toteutusOids.zipWithIndex.map { case (oid, i) =>
      put(withValintaperusteenValintakokeet(hakukohde(oid, hakuOids(i), valintaperusteIds(i)).copy(tila = Tallennettu)))
    }
    Thread.sleep(1000)
    timestampAfterInserts = renderHttpDate(now())
  }

  def updateTestData(): Unit = {
    // Ilman alla olevaa sleeppiä yksi testi feilasi joissakin (nopeammissa?) ympäristöissä.
    // Syy oli se, että timestampAfterInserts osui samalle sekunnille alla olevien update-operaatioiden kanssa.
    Thread.sleep(1500)
    updateInKoulutusTable(0)
    updateInKoulutuksenTarjoajatTable(1)
    deleteInKoulutuksenTarjoajatTable(2)
    updateInToteutusTable(3)
    updateInToteutuksenTarjoajatTable(4)
    deleteInToteutuksenTarjoajatTable(5)
    updateInHakuTable(6)
    updateInHakuHakuajatTable(7)
    updateInHakukohdeTable(8)
    updateInHakukohteenHakuajatTable(9)
    updateInHakukohteenLiitteetTable(10)
    updateInHakukohteenValintakokeetTable(11)
    updateInValintaperusteetTable(12)
    updateInSorakuvauksetTable(13)
    Thread.sleep(1000)
    timestampAfterAllModifications = renderHttpDate(now())
  }

  def updateInKoulutusTable(i: Int): Unit = update(koulutus(koulutusOids(i), Julkaistu), timestampAfterInserts, ophSession, 200)
  def updateInKoulutuksenTarjoajatTable(i: Int): Unit = update(koulutus(koulutusOids(i), Tallennettu).copy(tarjoajat = List(LonelyOid)), timestampAfterInserts, ophSession, 200)
  def deleteInKoulutuksenTarjoajatTable(i: Int): Unit = update(koulutus(koulutusOids(i), Tallennettu).copy(tarjoajat = List()), timestampAfterInserts, ophSession, 200)
  def updateInToteutusTable(i: Int): Unit = update(toteutus(toteutusOids(i), koulutusOids(i), UUID.randomUUID().toString), timestampAfterInserts)
  def updateInToteutuksenTarjoajatTable(i: Int): Unit = update(toteutus(toteutusOids(i), koulutusOids(i), Tallennettu).copy(tarjoajat = List(LonelyOid)), timestampAfterInserts)
  def deleteInToteutuksenTarjoajatTable(i: Int): Unit = update(toteutus(toteutusOids(i), koulutusOids(i), Tallennettu).copy(tarjoajat = List()), timestampAfterInserts)
  def updateInHakuTable(i: Int): Unit = update(haku(hakuOids(i), Arkistoitu), timestampAfterInserts)
  def updateInHakuHakuajatTable(i: Int): Unit = update(haku(hakuOids(i)).copy(hakuajat = List(Ajanjakso(alkaa = inFuture(2000), paattyy = Some(inFuture(5000))))), timestampAfterInserts)
  def updateInHakukohdeTable(i: Int): Unit = update(iHakukohde(i).copy(externalId = Some(UUID.randomUUID().toString)), timestampAfterInserts)
  def updateInHakukohteenHakuajatTable(i: Int): Unit = update(iHakukohde(i).copy(hakuajat = List(Ajanjakso(alkaa = inFuture(2000), paattyy = Some(inFuture(5000))))), timestampAfterInserts)
  def updateInHakukohteenLiitteetTable(i: Int): Unit = update(iHakukohde(i).copy(liitteet = List(Liite(tyyppiKoodiUri = Some(s"liitetyypitamm_$i#1")))), timestampAfterInserts)
  def updateInHakukohteenValintakokeetTable(i: Int): Unit = update(iHakukohde(i).copy(valintakokeet = List(Valintakoe(tyyppiKoodiUri = Some(s"valintakokeentyyppi_$i#1")))), timestampAfterInserts)
  def updateInValintaperusteetTable(i: Int): Any = update(valintaperuste(valintaperusteIds(i), Arkistoitu), timestampAfterInserts)
  def updateInSorakuvauksetTable(i: Int): Unit = update(sorakuvaus(sorakuvausIds(i), Tallennettu), timestampAfterInserts)

  "Modified since" should "return 401 without a valid session" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterInserts, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded") {
      status should be(401)
    }
  }

  it should "deny access without indexer role" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterInserts, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded", headers = Seq(sessionHeader(addTestSession(Role.Koulutus.Crud, OphOid)))) {
      status should be(403)
    }
  }

  it should "deny access without root access to the indexer role" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterInserts, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded", headers = Seq(sessionHeader(addTestSession(Role.Indexer, ChildOid)))) {
      status should be(403)
    }
  }

  it should "return only modified oids 1" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterInserts, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded", headers = Seq(sessionHeader(indexerSession))) {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should contain theSameElementsAs List(koulutusOids.head, koulutusOids(1), koulutusOids(2)).map(KoulutusOid)
      result.toteutukset should contain theSameElementsAs List(toteutusOids(3), toteutusOids(4), toteutusOids(5)).map(ToteutusOid)
      result.haut should contain theSameElementsAs List(hakuOids(6), hakuOids(7)).map(HakuOid)
      result.hakukohteet should contain theSameElementsAs List(hakukohdeOids(8), hakukohdeOids(9), hakukohdeOids(10), hakukohdeOids(11)).map(HakukohdeOid)
      result.valintaperusteet should contain theSameElementsAs List(valintaperusteIds(12))
      result.sorakuvaukset should contain theSameElementsAs List(sorakuvausIds(13))
    }
  }

  it should "return only modified oids 2" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampBeforeAllModifications, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded", headers = Seq(sessionHeader(indexerSession))) {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should contain theSameElementsAs koulutusOids.map(KoulutusOid)
      result.toteutukset should contain theSameElementsAs toteutusOids.map(ToteutusOid)
      result.haut should contain theSameElementsAs hakuOids.map(HakuOid)
      result.hakukohteet should contain theSameElementsAs hakukohdeOids.map(HakukohdeOid)
      result.valintaperusteet should contain theSameElementsAs valintaperusteIds
      result.sorakuvaukset should contain theSameElementsAs sorakuvausIds
    }
  }

  it should "return only modified oids 3" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterAllModifications, "UTF-8")

    get(s"$IndexerPath/modifiedSince/$lastModifiedEncoded", headers = Seq(sessionHeader(indexerSession))) {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should be(empty)
      result.toteutukset should be(empty)
      result.haut should be(empty)
      result.hakukohteet should be(empty)
      result.valintaperusteet should be(empty)
      result.sorakuvaukset should be(empty)
    }
  }
}
