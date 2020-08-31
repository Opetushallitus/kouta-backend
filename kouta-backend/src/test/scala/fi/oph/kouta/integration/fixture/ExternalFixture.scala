package fi.oph.kouta.integration.fixture

import java.net.InetAddress
import java.util.UUID

import fi.oph.kouta.TestOids.TestUserOid
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.security.{Authority, ExternalSession}
import fi.oph.kouta.servlet.{Authenticated, ExternalHakuRequest, ExternalServlet}

trait ExternalFixture extends KoutaIntegrationSpec with HakuFixture {

  val ExternalHakuPath: String = "/external/haku"

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new ExternalServlet(hakuService), "/external")
  }

  def put(request: ExternalHakuRequest): String = put(ExternalHakuPath, request, oid)
  def put(request: ExternalHakuRequest, sessionId: UUID): String = put(ExternalHakuPath, request, sessionId, oid)

  def update(request: ExternalHakuRequest, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(ExternalHakuPath, request, lastModified, expectUpdate)

  def update(request: ExternalHakuRequest, lastModified: String, sessionId: UUID): Unit =
    update(ExternalHakuPath, request, lastModified, expectUpdate = true, sessionId)

  def authenticated(id: String = UUID.randomUUID().toString,
                    personOid: UserOid = TestUserOid,
                    authorities: Set[Authority] = defaultAuthorities,
                    userAgent: String = "test-agent",
                    ip: InetAddress = InetAddress.getByName("192.168.1.19")) = {
    val session = ExternalSession(personOid.s, authorities)
    Authenticated(id, session, userAgent, ip)
  }

}
