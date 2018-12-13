package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.ServiceMocks._
import fi.oph.kouta.domain._

class ListSpec extends KoutaIntegrationSpec with EverythingFixture {

  val oid1 = "1.2.246.562.10.594252633210"
  val oid2 = "1.2.246.562.10.81934895871"
  val oid3 = "1.2.246.562.10.67603619189"
  val oid4 = "1.2.246.562.10.99999999999"
  val oid5 = "1.2.246.562.10.99999999998"
  val oid6 = "1.2.246.562.10.99999999997"

  var k1, k2, k3, k4, k5:OidListItem = null
  var t1, t2, t3, t4:OidListItem = null
  var h1, h2, h3, h4:OidListItem = null
  var v1, v2, v3, v4:IdListItem = null

  override def beforeAll() = {
    super.beforeAll()
    List(oid1, oid2, oid3).foreach(mockOrganisaatioServiceFromResource(_))
    mockOrganisaatioService(oid4, singleOidOrganisaatioResponse(oid4))
    mockOrganisaatioService(oid5, EmptyOrganisaatioResponse)
    createTestData()
  }

  override def afterAll() = {
    super.afterAll()
    stopServiceMocks()
    truncateDatabase()
  }

  def createTestData() = {
    def let[T,R](v:T)(f:(T)=>R) = f(v)

    def k = (j:Boolean, oid:String, t:Julkaisutila) => koulutus.copy(julkinen = j, organisaatioOid = oid, tila = t)
    def t = (koid:String, oid:String, t:Julkaisutila) => toteutus.copy(koulutusOid = koid, organisaatioOid = oid, tila = t)
    def h = (oid:String, t:Julkaisutila) => haku.copy(organisaatioOid = oid, tila = t)
    def v = (oid:String, t:Julkaisutila) => valintaperuste.copy(organisaatioOid = oid, tila = t)

    def r(oid:String, e:Perustiedot) = new OidListItem(oid, e.nimi, e.tila, e.organisaatioOid)
    def idr(id:UUID, e:Perustiedot) = new IdListItem(id, e.nimi, e.tila, e.organisaatioOid)

    def createK(j:Boolean, oid:String, t:Julkaisutila) = let(k(j, oid, t)) { (k:Koulutus) => r(put(k), k) }
    def createT(koid:String, oid:String, tila:Julkaisutila) = let(t(koid, oid, tila)) { (t:Toteutus) => r(put(t), t)}
    def createH(oid:String, t:Julkaisutila) = let(h(oid, t)) { (h:Haku) => r(put(h), h)}
    def createV(oid:String, t:Julkaisutila) = let(v(oid, t)) { (v:Valintaperuste) => idr(put(v), v)}

    k1 = createK(false, oid1, Julkaistu)
    k2 = createK(false, oid2, Arkistoitu)
    k3 = createK(false, oid3, Tallennettu)
    k4 = createK(false, oid4, Julkaistu)
    k5 = createK(true, oid4, Julkaistu)
    t1 = createT(k1.oid, oid1, Julkaistu)
    t2 = createT(k2.oid, oid2, Arkistoitu)
    t3 = createT(k3.oid, oid3, Tallennettu)
    t4 = createT(k4.oid, oid4, Julkaistu)
    h1 = createH(oid1, Julkaistu)
    h2 = createH(oid2, Arkistoitu)
    h3 = createH(oid3, Tallennettu)
    h4 = createH(oid4, Julkaistu)
    v1 = createV(oid1, Julkaistu)
    v2 = createV(oid2, Arkistoitu)
    v3 = createV(oid3, Tallennettu)
    v4 = createV(oid4, Julkaistu)
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

  "Toteutus list" should "list all toteutukset for authorized organizations" in {
    list(ToteutusPath, Map("organisaatioOid" -> oid2), List(t1, t2, t3))
  }
  it should "list all toteutukset for authorized organizations 2" in {
    list(ToteutusPath, Map("organisaatioOid" -> oid4), List(t4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ToteutusPath, Map("organisaatioOid" -> oid5), 403)
  }
  it should "return 404 if oid not given" in {
    list(ToteutusPath, Map[String,String](), 404)
  }

  "Haku list" should "list all haut for authorized organizations" in {
    list(HakuPath, Map("organisaatioOid" -> oid2), List(h1, h2, h3))
  }
  it should "list all haut for authorized organizations 2" in {
    list(HakuPath, Map("organisaatioOid" -> oid4), List(h4))
  }
  it should "return forbidden if oid is unknown" in {
    list(HakuPath, Map("organisaatioOid" -> oid5), 403)
  }
  it should "return 404 if oid not given" in {
    list(HakuPath, Map[String,String](), 404)
  }

  "Valintaperuste list" should "list all valintaperustekuvaukset for authorized organizations" in {
    list(ValintaperustePath, Map("organisaatioOid" -> oid2), List(v1, v2, v3))
  }
  it should "list all valintaperustekuvaukset for authorized organizations 2" in {
    list(ValintaperustePath, Map("organisaatioOid" -> oid4), List(v4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ValintaperustePath, Map("organisaatioOid" -> oid5), 403)
  }
  it should "return 404 if oid not given" in {
    list(ValintaperustePath, Map[String,String](), 404)
  }
}