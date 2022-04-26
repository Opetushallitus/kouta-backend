package fi.oph.kouta.integration

import java.util.UUID
import fi.oph.kouta.TestOids
import fi.oph.kouta.TestOids.{OphOid, TestUserOid}
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.domain.{ExternalKoulutusRequest, ExternalToteutusRequest, Tallennettu}
import fi.oph.kouta.integration.fixture.ExternalFixture
import fi.oph.kouta.security.{Authority, Role}

class ExternalSpec extends ExternalFixture with AccessControlSpec {

  var externalSession: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    externalSession = addTestSession(Role.External, OphOid)
  }

  "Create koulutus from external" should "store koulutus" in {
    val request = ExternalKoulutusRequest(authenticated(), yoKoulutus)
    val oid = put(request, externalSession)
    get(oid, yoKoulutus.copy(oid =  Some(KoulutusOid(oid)), muokkaaja = TestUserOid))
  }

  it should "require authentication to be attached to the request when creating new entity" in {
    put(ExternalKoulutusPath, Map("entity" -> yoKoulutus), externalSession, 400)
  }

  it should "store muokkaaja from the session sent from kouta-external when creating new entity" in {
    val userOid = TestOids.randomUserOid
    val request = ExternalKoulutusRequest(authenticated(personOid = userOid), yoKoulutus)
    val oid = put(request, externalSession)
    get(oid, yoKoulutus.copy(oid =  Some(KoulutusOid(oid)),muokkaaja = userOid))
  }

  it should "deny a user with the wrong role when creating new entity" in {
    val authentication = authenticated()
    val request = ExternalKoulutusRequest(authentication, koulutus)
    put(ExternalKoulutusPath, request, defaultSessionId, 403)
  }

  it should "use the attached session for authentication when creating new entity" in {
    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid, authorities = Set(Authority(Role.Koulutus.Read, OphOid)))
    val request = ExternalKoulutusRequest(authentication, koulutus)
    put(ExternalKoulutusPath, request, externalSession, 403)
  }

  /*
  "Update koulutus from external" should "update koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))

    val authentication = authenticated()
    val request = ExternalKoulutusRequest(authentication, koulutus(oid, Tallennettu))

    update(request, lastModified, externalSession)
    get(oid, haku(oid, Tallennettu))
  }

  it should "require authentication to be attached to the request when updating existing entity" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))

    update(KoulutusPath, koulutus(oid), lastModified, externalSession, 400)
    update(KoulutusPath, Map("entity" -> koulutus(oid)), lastModified, externalSession, 400)
  }


  it should "store muokkaaja from the session sent from kouta-external when updating existing entity" in {
    val oid = put(koulutus)
    val lastModified = get(oid, haku(oid))

    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid)

    val request = ExternalHakuRequest(authentication, haku(oid, Tallennettu))
    update(request, lastModified, externalSession)
    get(oid, haku(oid, Tallennettu).copy(muokkaaja = userOid))
  }

  it should "deny a user with the wrong role when updating existing entity" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))

    val authentication = authenticated()
    val request = ExternalKoulutusRequest(authentication, koulutus(oid, Tallennettu))

    update(KoulutusPath, request, lastModified, defaultSessionId, 403)
  }

  it should "use the attached session for authentication when updating existing entity" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))

    val userOid = TestOids.randomUserOid
    val authentication = authenticated(personOid = userOid, authorities = Set(Authority(Role.Koulutus.Read, OphOid)))

    val request = ExternalKoulutusRequest(authentication, koulutus(oid, Tallennettu))
    update(KoulutusPath, request, lastModified, externalSession, 403)
  }
  */
}
