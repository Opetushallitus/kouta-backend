package fi.oph.kouta.integration

import fi.oph.kouta.ServiceMocks._
import fi.oph.kouta.domain._

class ListSpec extends KoutaIntegrationSpec with EverythingFixture {

  val oid1 = "1.2.246.562.10.594252633210"
  val oid2 = "1.2.246.562.10.81934895871"
  val oid3 = "1.2.246.562.10.67603619189"
  val oid4 = "1.2.246.562.10.99999999999"
  val oid5 = "1.2.246.562.10.99999999998"
  val oid6 = "1.2.246.562.10.99999999997"

  var k1:OidListItem = null
  var k2:OidListItem = null
  var k3:OidListItem = null
  var k4:OidListItem = null
  var k5:OidListItem = null

  override def beforeAll() = {
    super.beforeAll()
    mockOrganisaatioServiceFromResource(oid1)
    mockOrganisaatioServiceFromResource(oid2)
    mockOrganisaatioServiceFromResource(oid3)
    mockOrganisaatioService(oid4, singleOidOrganisaatioResponse(oid4))
    mockOrganisaatioService(oid5, EmptyOrganisaatioResponse)
    createTestData()
  }

  override def afterAll() = {
    super.afterAll()
    stopServiceMocks()
    truncateDatabase()
  }

  def r(oid:String, e:Perustiedot) = new OidListItem(oid, e.nimi, e.tila, e.organisaatioOid)

  def create(e:Koulutus) = r(put(e), e)
  def create(e:Toteutus) = r(put(e), e)

  def createTestData() = {
    k1 = create(koulutus.copy(julkinen = false, organisaatioOid = oid1, tila = Julkaistu))
    k2 = create(koulutus.copy(julkinen = false, organisaatioOid = oid2, tila = Arkistoitu))
    k3 = create(koulutus.copy(julkinen = false, organisaatioOid = oid3, tila = Tallennettu))
    k4 = create(koulutus.copy(julkinen = false, organisaatioOid = oid4, tila = Julkaistu))
    k5 = create(koulutus.copy(julkinen = true, organisaatioOid = oid4, tila = Julkaistu))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    list(KoulutusPath, Map("organisaatioOid" -> oid2), List(k1, k2, k3, k5))
  }
  it should "list all koulutukset for authorized organizations 2" in {
    list(KoulutusPath, Map("organisaatioOid" -> oid4), List(k4, k5))
  }
  it should "return forbidden if oid is unknown" in {
    list(KoulutusPath, Map("organisaatioOid" -> oid5), 403)
  }
  it should "return 404 if oid not given" in {
    list(KoulutusPath, Map[String,String](), 404)
  }
}