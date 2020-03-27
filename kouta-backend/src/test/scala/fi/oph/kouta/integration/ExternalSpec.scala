package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestOids
import fi.oph.kouta.TestOids.OphOid
import fi.oph.kouta.integration.fixture.{ExternalFixture, HakuFixture}
import fi.oph.kouta.security.Role.Indexer
import fi.oph.kouta.security.{Authority, Role}
import fi.oph.kouta.servlet.ExternalHakuRequest

class ExternalSpec extends KoutaIntegrationSpec with AccessControlSpec with HakuFixture with ExternalFixture {

  var externalSession: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    externalSession = addTestSession(Indexer, OphOid)
  }

  "Create haku from external" should "store haku" in {
    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku)
    val oid = put(request, externalSession)
    get(oid, haku(oid))
  }

  it should "store muokkaaja from the session sent from kouta-external" in {
    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid)
    val request = ExternalHakuRequest(authentication, haku)
    val oid = put(request, externalSession)
    get(oid, haku(oid).copy(muokkaaja = userOid))
  }

  it should "deny a user with the wrong role" in {
    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku)
    put(ExternalHakuPath, request, defaultSessionId, 403)
  }

  it should "use the attached session for authentication" in {
    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid, authorities = Set(Authority(Role.Haku.Read, OphOid)))
    val request = ExternalHakuRequest(authentication, haku)
    put(ExternalHakuPath, request, externalSession, 403)
  }
}
