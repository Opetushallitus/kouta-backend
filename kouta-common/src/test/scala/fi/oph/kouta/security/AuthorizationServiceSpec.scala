package fi.oph.kouta.security


import java.net.InetAddress
import java.util.UUID
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.CachedOrganisaatioHierarkiaClient
import fi.oph.kouta.config.KoutaCommonConfigFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.mocks.{OrganisaatioServiceMock, SpecWithMocks}
import fi.oph.kouta.service.{AuthorizationService, OrganisaatioFixture, OrganisaatioService, OrganizationAuthorizationFailedException, RoleAuthorizationFailedException}
import fi.oph.kouta.servlet.Authenticated

class AuthorizationServiceSpec extends SpecWithMocks with OrganisaatioFixture {

  KoutaCommonConfigFactory.setupWithDefaultTestTemplateFile()
  setUrlProperties(KoutaCommonConfigFactory.configuration.urlProperties)

  lazy val oc = organisaatioService

  lazy val TestService: AuthorizationService = new AuthorizationService {
    override val organisaatioService = oc
  }

  val paakayttajaSession = CasSession(ServiceTicket("ST-123"), "1.2.3.1234",
    Set("APP_KOUTA", "APP_KOUTA_OPHPAAKAYTTAJA", s"APP_KOUTA_OPHPAAKAYTTAJA_${OphOid}").map(Authority(_)))

  def testAuthenticated(id: UUID, session: Session) = Authenticated(id.toString, session, "testAgent", InetAddress.getByName("127.0.0.1"))

  "Pääkäyttäjä" should "have root access" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), paakayttajaSession)
    TestService.hasRootAccess(Role.Koulutus.readRoles) should be (true)
  }
  it should "have permission to execute with root access" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), paakayttajaSession)
    var run = false
    TestService.withRootAccess(Role.Koulutus.readRoles) {run = true}
    run should be (true)
  }
  it should "have permission to execute with authorized child oids" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), paakayttajaSession)
    var orgs: Seq[OrganisaatioOid] = Seq()
    TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.readRoles) { o => orgs = o }
    orgs should contain theSameElementsAs(Seq(ChildOid, GrandChildOid, EvilGrandChildOid))
  }

  val readChildOidSession = CasSession(ServiceTicket("ST-123"), "1.2.3.1234",
    Set("APP_KOUTA", "APP_KOUTA_KOULUTUS_READ", s"APP_KOUTA_KOULUTUS_READ_${ChildOid}").map(Authority(_)))

  val readGrandChildOidSession = CasSession(ServiceTicket("ST-123"), "1.2.3.1234",
    Set("APP_KOUTA", "APP_KOUTA_KOULUTUS_READ", s"APP_KOUTA_KOULUTUS_READ_${GrandChildOid}").map(Authority(_)))

  "Normal user with read rights" should "not have root access" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    TestService.hasRootAccess(Role.Koulutus.readRoles) should be (false)
  }
  it should "not have permission to execute with root access" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    assertThrows[OrganizationAuthorizationFailedException] {
      TestService.withRootAccess(Role.Koulutus.readRoles) {}
    }
  }
  it should "have permission to execute with authorized child oids" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    var orgs: Seq[OrganisaatioOid] = Seq()
    TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.readRoles) { o => orgs = o }
    orgs should contain theSameElementsAs(Seq(ChildOid, GrandChildOid, EvilGrandChildOid))
  }
  it should "not have permission to execute write operations" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    assertThrows[RoleAuthorizationFailedException] {
      TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.updateRoles) {o => o}
    }
  }
  it should "not have permission to execute with parent oids" in {
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readGrandChildOidSession)
    assertThrows[OrganizationAuthorizationFailedException] {
      TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.readRoles) { o => o }
    }
  }
}
