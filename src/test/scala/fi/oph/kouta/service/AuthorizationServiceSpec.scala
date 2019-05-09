package fi.oph.kouta.service

import java.util.UUID

import fi.oph.kouta.{OrganisaatioServiceMock, TestSetups}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.Authenticated
import org.scalatra.test.scalatest.ScalatraFlatSpec

class AuthorizationServiceSpec extends ScalatraFlatSpec with OrganisaatioServiceMock {

  object AuthorizationServiceMock extends AuthorizationService

  def testSession(authorities: Set[Authority]): Authenticated =
    Authenticated(UUID.randomUUID(), CasSession(ServiceTicket(""), "", authorities))

  def testSession(role: Role, organisaatioOids: OrganisaatioOid*): Authenticated =
    testSession(organisaatioOids.map(oid => Authority(role, oid)).toSet)

  def mockSingleOrganisaatioResponses(organisaatioOids: OrganisaatioOid*): Unit = {
    organisaatioOids.foreach { oid =>
      mockOrganisaatioResponse(oid.s, singleOidOrganisaatioResponse(oid.s))
    }
  }

  TestSetups.setupWithTemplate(1234)

  val LonelyOid = OrganisaatioOid("1.2.246.562.10.0002")
  val OtherLonelyOid = OrganisaatioOid("1.2.246.562.10.1")

  override def beforeAll: Unit = {
    super.beforeAll()
    startServiceMocking()
    mockSingleOrganisaatioResponses(LonelyOid, OtherLonelyOid)
    mockOrganisaatioResponse(ParentOid)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
  }

  "authorize(role, organisaatio)" should "not authorize if the user doesn't have any roles" in {
    an[RoleAuthorizationFailedException] should be thrownBy
      AuthorizationServiceMock.authorizeAll(Role.CrudUser, Seq())(testSession(Set.empty[Authority]))
  }

  it should "not authorize if the user doesn't have the right role" in {
    val authorities = Set(Authority("APP_OTHER_APP"))
    an[RoleAuthorizationFailedException] should be thrownBy
      AuthorizationServiceMock.authorize(Role.CrudUser, OtherLonelyOid)(testSession(authorities))
  }

  it should "not authorize if the user doesn't have the right role for the right organization" in {
    an[OrganizationAuthorizationFailedException] should be thrownBy
      AuthorizationServiceMock.authorize(Role.CrudUser, OtherLonelyOid)(testSession(Role.CrudUser, LonelyOid))
  }

  it should "authorize if the user has the right role and organization" in {
    AuthorizationServiceMock.authorize(Role.CrudUser, LonelyOid)(testSession(Role.CrudUser, LonelyOid))
  }

  it should "authorize if the user has rights to a parent organization" in {
    AuthorizationServiceMock.authorize(Role.CrudUser, OrganisaatioOid(ChildOid))(testSession(Role.CrudUser, OrganisaatioOid(ParentOid)))
  }

  it should "authorize anything if the user has rights to Oph for the role" in {
    AuthorizationServiceMock.authorize(Role.CrudUser, OrganisaatioOid(ChildOid))(testSession(Role.CrudUser, OrganisaatioOid(OphOid)))
    AuthorizationServiceMock.authorize(Role.CrudUser, LonelyOid)(testSession(Role.CrudUser, OrganisaatioOid(OphOid)))
  }

  "authorizeAll" should "not authorize if the user doesn't have all of the right organizations" in {
    an[OrganizationAuthorizationFailedException] should be thrownBy
      AuthorizationServiceMock.authorizeAll(Role.CrudUser, Seq(OtherLonelyOid, LonelyOid))(testSession(Role.CrudUser, LonelyOid))
  }

  it should "authorize if the user has all of the right organizations" in {
    AuthorizationServiceMock.authorizeAll(Role.CrudUser, Seq(OtherLonelyOid, LonelyOid))(testSession(Role.CrudUser, LonelyOid, OtherLonelyOid))
  }

  it should "authorize anything if the user has rights to Oph for the role" in {
    AuthorizationServiceMock.authorizeAll(Role.CrudUser, Seq(OrganisaatioOid(ChildOid), LonelyOid, OrganisaatioOid(EvilCousin)))(testSession(Role.CrudUser, OrganisaatioOid(OphOid)))
  }
}
