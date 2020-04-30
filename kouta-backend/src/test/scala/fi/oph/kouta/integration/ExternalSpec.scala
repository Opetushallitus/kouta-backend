package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestOids
import fi.oph.kouta.TestOids.OphOid
import fi.oph.kouta.domain.Tallennettu
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

  it should "require authentication to be attached to the request" in {
    put(ExternalHakuPath, haku, externalSession, 400)
    put(ExternalHakuPath, Map("haku" -> haku), externalSession, 400)
  }

  it should "validate the haku" in {
    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku.copy(hakutapaKoodiUri = Some("virhe")))
    put(ExternalHakuPath, request, externalSession, 400)
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

  "Update haku from external" should "update haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu))

    update(request, lastModified, externalSession)
    get(oid, haku(oid, Tallennettu))
  }

  it should "require authentication to be attached to the request" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    update(ExternalHakuPath, haku(oid), lastModified, externalSession, 400)
    update(ExternalHakuPath, Map("haku" -> haku(oid)), lastModified, externalSession, 400)
  }

  it should "validate the haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu).copy(hakutapaKoodiUri = Some("virhe")))

    update(ExternalHakuPath, request, lastModified, externalSession, 400)
  }

  it should "store muokkaaja from the session sent from kouta-external" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid)

    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu))
    update(request, lastModified, externalSession)
    get(oid, haku(oid, Tallennettu).copy(muokkaaja = userOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    val authentication = authenticated()
    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu))

    update(ExternalHakuPath, request, lastModified, defaultSessionId, 403)
  }

  it should "use the attached session for authentication" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid, authorities = Set(Authority(Role.Haku.Read, OphOid)))

    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu))
    update(ExternalHakuPath, request, lastModified, externalSession, 403)
  }
}
