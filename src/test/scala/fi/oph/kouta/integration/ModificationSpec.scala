package fi.oph.kouta.integration

import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.Instant.now
import java.util.UUID

import fi.oph.kouta.KonfoIndexingQueues
import fi.oph.kouta.TestData.inFuture
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.servlet.AnythingServlet
import org.json4s.jackson.Serialization.read


class ModificationSpec extends KoutaIntegrationSpec with KonfoIndexingQueues with EverythingFixture {

  val AnythingPath = "/anything"
  addServlet(new AnythingServlet(), AnythingPath)

  override def beforeAll() = {
    super.beforeAll()
    truncateDatabase()
    addDefaultSession()
    createTestData(15)
    updateTestData()
  }

  def nTimes[T](f: () => T, n: Int = 15) = (0 to n).map(i => f()).toList

  def iHakukohde(i: Int) = hakukohde(hakukohdeOids(i), toteutusOids(i), hakuOids(i), valintaperusteIds(i))

  var koulutusOids: List[String] = List()
  var toteutusOids: List[String] = List()
  var hakuOids: List[String] = List()
  var valintaperusteIds: List[UUID] = List()
  var hakukohdeOids: List[String] = List()

  def formatInstant(i:Instant) = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(i, ZoneId.of("GMT")))

  var timestampBeforeAllModifications = ""
  var timestampAfterInserts = ""
  var timestampAfterAllModifications = ""

  def createTestData(n: Int = 10) = {
    timestampBeforeAllModifications = formatInstant(now())
    Thread.sleep(1000)
    koulutusOids = nTimes(() => put(koulutus), n)
    toteutusOids = koulutusOids.map(oid => put(toteutus(oid)))
    hakuOids = nTimes(() => put(haku), n)
    valintaperusteIds = nTimes(() => put(valintaperuste), n)
    hakukohdeOids = toteutusOids.zipWithIndex.map { case (oid, i) => put(hakukohde(oid, hakuOids(i), valintaperusteIds(i))) }
    Thread.sleep(1000)
    timestampAfterInserts = formatInstant(now())
  }

  def updateTestData() = {
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
    Thread.sleep(1000)
    timestampAfterAllModifications = formatInstant(now())
  }

  def updateInKoulutusTable(i: Int) = update(koulutus(koulutusOids(i), Arkistoitu), timestampAfterInserts)
  def updateInKoulutuksenTarjoajatTable(i: Int) = update(koulutus(koulutusOids(i)).copy(tarjoajat = List("9.8.7.6.5").map(OrganisaatioOid)), timestampAfterInserts)
  def deleteInKoulutuksenTarjoajatTable(i: Int) = update(koulutus(koulutusOids(i)).copy(tarjoajat = List()), timestampAfterInserts)
  def updateInToteutusTable(i: Int) = update(toteutus(toteutusOids(i), koulutusOids(i), Arkistoitu), timestampAfterInserts)
  def updateInToteutuksenTarjoajatTable(i: Int) = update(toteutus(toteutusOids(i), koulutusOids(i)).copy(tarjoajat = List("9.8.7.6.5").map(OrganisaatioOid)), timestampAfterInserts)
  def deleteInToteutuksenTarjoajatTable(i: Int) = update(toteutus(toteutusOids(i), koulutusOids(i)).copy(tarjoajat = List()), timestampAfterInserts)
  def updateInHakuTable(i: Int) = update(haku(hakuOids(i), Arkistoitu), timestampAfterInserts)
  def updateInHakuHakuajatTable(i: Int) = update(haku(hakuOids(i)).copy(hakuajat = List(Ajanjakso(alkaa = inFuture(2000), paattyy = inFuture(5000)))), timestampAfterInserts)
  def updateInHakukohdeTable(i: Int) = update(iHakukohde(i).copy(tila = Arkistoitu), timestampAfterInserts)
  def updateInHakukohteenHakuajatTable(i: Int) = update(iHakukohde(i).copy(hakuajat = List(Ajanjakso(alkaa = inFuture(2000), paattyy = inFuture(5000)))), timestampAfterInserts)
  def updateInHakukohteenLiitteetTable(i: Int) = update(iHakukohde(i).copy(liitteet = List(Liite(tyyppi = Some(s"tyyppi$i")))), timestampAfterInserts)
  def updateInHakukohteenValintakokeetTable(i: Int) = update(iHakukohde(i).copy(valintakokeet = List(Valintakoe(tyyppi = Some(s"tyyppi$i")))), timestampAfterInserts)
  def updateInValintaperusteetTable(i: Int) = update(valintaperuste(valintaperusteIds(i), Arkistoitu), timestampAfterInserts)

  it should "return only modified oids 1" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterInserts, "UTF-8")

    get(s"$AnythingPath/modifiedSince/$lastModifiedEncoded") {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should contain theSameElementsAs (List(koulutusOids(0), koulutusOids(1), koulutusOids(2)).map(KoulutusOid))
      result.toteutukset should contain theSameElementsAs (List(toteutusOids(3), toteutusOids(4), toteutusOids(5)).map(ToteutusOid))
      result.haut should contain theSameElementsAs (List(hakuOids(6), hakuOids(7)).map(HakuOid))
      result.hakukohteet should contain theSameElementsAs (List(hakukohdeOids(8), hakukohdeOids(9), hakukohdeOids(10), hakukohdeOids(11)).map(HakukohdeOid))
      result.valintaperusteet should contain theSameElementsAs (List(valintaperusteIds(12)))
    }
  }

  it should "return only modified oids 2" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampBeforeAllModifications, "UTF-8")

    get(s"$AnythingPath/modifiedSince/$lastModifiedEncoded") {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should contain theSameElementsAs (koulutusOids).map(KoulutusOid)
      result.toteutukset should contain theSameElementsAs (toteutusOids).map(ToteutusOid)
      result.haut should contain theSameElementsAs (hakuOids).map(HakuOid)
      result.hakukohteet should contain theSameElementsAs (hakukohdeOids).map(HakukohdeOid)
      result.valintaperusteet should contain theSameElementsAs (valintaperusteIds)
    }
  }

  it should "return  only modified oids 3" in {

    val lastModifiedEncoded = URLEncoder.encode(timestampAfterAllModifications, "UTF-8")

    get(s"$AnythingPath/modifiedSince/$lastModifiedEncoded") {
      status should be(200)
      val result = read[ListEverything](body)
      result.koulutukset should be(empty)
      result.toteutukset should be(empty)
      result.haut should be(empty)
      result.hakukohteet should be(empty)
      result.valintaperusteet should be(empty)
    }
  }
}
