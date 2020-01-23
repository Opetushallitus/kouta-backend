package fi.oph.kouta.security

import java.net.InetAddress
import java.util.UUID

import fi.oph.kouta.client.KoutaClientSpec
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.mocks.OrganisaatioServiceMock
import fi.oph.kouta.service.{AuthorizationService, OrganizationAuthorizationFailedException, RoleAuthorizationFailedException}
import fi.oph.kouta.servlet.Authenticated

class AuthorizationServiceSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  val TestService = new AuthorizationService {}

  val paakayttajaSession = CasSession(ServiceTicket("ST-123"), "1.2.3.1234",
    Set("APP_KOUTA", "APP_KOUTA_OPHPAAKAYTTAJA", s"APP_KOUTA_OPHPAAKAYTTAJA_${OphOid}").map(Authority(_)))

  def testAuthenticated(id: UUID, session: Session) = Authenticated(id, session, "testAgent", InetAddress.getByName("127.0.0.1"))

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
    mockOrganisaatioResponse(ChildOid)
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
    mockOrganisaatioResponse(ChildOid)
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    var orgs: Seq[OrganisaatioOid] = Seq()
    TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.readRoles) { o => orgs = o }
    orgs should contain theSameElementsAs(Seq(ChildOid, GrandChildOid, EvilGrandChildOid))
  }
  it should "not have permission to execute write operations" in {
    mockOrganisaatioResponse(ChildOid)
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    assertThrows[RoleAuthorizationFailedException] {
      TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.updateRoles) {o => o}
    }
  }
  it should "not have permission to execute with parent oids" in {
    mockOrganisaatioResponse(ChildOid)
    mockOrganisaatioResponse(GrandChildOid, singleOidOrganisaatioResponse(GrandChildOid.s))
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readGrandChildOidSession)
    assertThrows[OrganizationAuthorizationFailedException] {
      TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.readRoles) { o => o }
    }
  }
  it should "not have permission to execute if organization oid is uknown" in {
    mockOrganisaatioResponse(ChildOid, NotFoundOrganisaatioResponse)
    implicit val authenticated = testAuthenticated(UUID.randomUUID(), readChildOidSession)
    assertThrows[RoleAuthorizationFailedException] {
      TestService.withAuthorizedChildOrganizationOids(ChildOid, Role.Koulutus.updateRoles) {o => o}
    }
  }
}
