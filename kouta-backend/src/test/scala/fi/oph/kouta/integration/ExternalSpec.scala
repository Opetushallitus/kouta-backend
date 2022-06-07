package fi.oph.kouta.integration

import java.util.UUID
import fi.oph.kouta.TestOids
import fi.oph.kouta.TestOids.{OphOid, TestUserOid}
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{ExternalHakuRequest, ExternalHakukohdeRequest, ExternalKoulutusRequest, ExternalRequest, ExternalSorakuvausRequest, ExternalToteutusRequest, ExternalValintaperusteRequest, Haku, Hakukohde, Julkaisutila, Koulutus, Sorakuvaus, Tallennettu, Toteutus, Valintaperuste}
import fi.oph.kouta.integration.fixture.ExternalFixture
import fi.oph.kouta.security.{Authority, ExternalSession, Role}
import fi.oph.kouta.servlet.Authenticated

class ExternalSpec extends ExternalFixture with CreateTests with ModifyTests {
  override def beforeAll(): Unit = {
    super.beforeAll()
    externalSession = addTestSession(Role.External, OphOid)
  }

  executeCreateTests[Koulutus](
    "koulutus",
    ExternalKoulutusPath,
    (authenticated: Authenticated) => ExternalKoulutusRequest(authenticated, createKoulutus()),
    (oid: String, muokkaaja: Option[UserOid]) => createKoulutus(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Koulutus](
    "koulutus",
    ExternalKoulutusPath,
    (oid: String, authenticated: Authenticated) =>
      ExternalKoulutusRequest(authenticated, createKoulutus(oid, tila = Some(Arkistoitu))),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createKoulutus(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  executeCreateTests[Toteutus](
    "toteutus",
    ExternalToteutusPath,
    (authenticated: Authenticated) => ExternalToteutusRequest(authenticated, createToteutus()),
    (oid: String, muokkaaja: Option[UserOid]) => createToteutus(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Toteutus](
    "toteutus",
    ExternalToteutusPath,
    (oid: String, authenticated: Authenticated) =>
      ExternalToteutusRequest(authenticated, createToteutus(oid, tila = Some(Arkistoitu))),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createToteutus(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  executeCreateTests[Haku](
    "haku",
    ExternalHakuPath,
    (authenticated: Authenticated) => ExternalHakuRequest(authenticated, createHaku()),
    (oid: String, muokkaaja: Option[UserOid]) => createHaku(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Haku](
    "haku",
    ExternalHakuPath,
    (oid: String, authenticated: Authenticated) =>
      ExternalHakuRequest(authenticated, createHaku(oid, tila = Some(Arkistoitu))),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createHaku(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  executeCreateTests[Hakukohde](
    "hakukohde",
    ExternalHakukohdePath,
    (authenticated: Authenticated) => ExternalHakukohdeRequest(authenticated, createHakukohde()),
    (oid: String, muokkaaja: Option[UserOid]) => createHakukohde(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Hakukohde](
    "hakukohde",
    ExternalHakukohdePath,
    (oid: String, authenticated: Authenticated) =>
      ExternalHakukohdeRequest(authenticated, createHakukohde(oid, tila = Some(Arkistoitu))),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createHakukohde(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  executeCreateTests[Valintaperuste](
    "valintaperuste",
    ExternalValintaperustePath,
    (authenticated: Authenticated) => ExternalValintaperusteRequest(authenticated, createValintaperuste()),
    (oid: String, muokkaaja: Option[UserOid]) =>
      createValintaperuste(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Valintaperuste](
    "valintaperuste",
    ExternalValintaperustePath,
    (oid: String, authenticated: Authenticated) =>
      ExternalValintaperusteRequest(authenticated, createValintaperuste(oid, tila = Some(Arkistoitu))),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createValintaperuste(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  executeCreateTests[Sorakuvaus](
    "sorakuvaus",
    ExternalSorakuvausPath,
    (authenticated: Authenticated) =>
      ExternalSorakuvausRequest(
        if (authenticated.session.personOid == OidWithReadOnlyAccess.s) authenticated
        else
          authenticated.copy(session =
            authenticated.session
              .asInstanceOf[ExternalSession]
              .copy(authorities = Set(Authority(Role.Paakayttaja, OphOid)))
          ),
        createSorakuvaus()
      ),
    (oid: String, muokkaaja: Option[UserOid]) =>
      createSorakuvaus(oid, muokkaaja, isMuokkaajaOphVirkailija = Some(false))
  )
  executeModifyTests[Sorakuvaus](
    "sorakuvaus",
    ExternalSorakuvausPath,
    (oid: String, authenticated: Authenticated) =>
      ExternalSorakuvausRequest(
        if (authenticated.session.personOid == OidWithReadOnlyAccess.s) authenticated
        else
          authenticated.copy(session =
            authenticated.session
              .asInstanceOf[ExternalSession]
              .copy(authorities = Set(Authority(Role.Paakayttaja, OphOid)))
          ),
        createSorakuvaus(oid, tila = Some(Arkistoitu))
      ),
    (oid: String, muokkaaja: Option[UserOid], tila: Option[Julkaisutila], isMuokkaajaOphVirkailija: Option[Boolean]) =>
      createSorakuvaus(oid, muokkaaja, tila, isMuokkaajaOphVirkailija)
  )

  "Modifying Koulutus from external" should "fail when adding tarjoajat not belonging to koulutus organisaatio" in {
    val koulutusRequest = ExternalKoulutusRequest(authenticated(), createKoulutus(isMuokkaajaOphVirkailija = Some(false)))
    val oid      = doPut(koulutusRequest, ophSession)
    val koulutus = koulutusRequest.koulutus.copy(oid = Some(KoulutusOid(oid)), muokkaaja = TestUserOid)
    val lastModified = doGet(oid, koulutus)

    val modifyRequest = ExternalKoulutusRequest(authenticated(), koulutus.copy(tarjoajat =
      // Helsingin yliopistoon kuuluvia OIDeja
      List(OrganisaatioOid("1.2.246.562.10.73307006806"), OrganisaatioOid("1.2.246.562.10.445049088710"))))

    update(ExternalKoulutusPath, modifyRequest, lastModified, externalSession, 403)
  }
}

trait TestBase {
  var externalSession: UUID = _
  val OidWithReadOnlyAccess = UserOid("1.2.246.562.24.10000000123")
}

trait CreateTests extends TestBase {
  this: ExternalFixture with AccessControlSpec =>

  def executeCreateTests[E <: AnyRef](
      entityName: String,
      path: String,
      makeRequest: Authenticated => ExternalRequest,
      resultEntity: (String, Option[UserOid]) => E
  ): Unit = {
    s"Create $entityName from external" should s"store new $entityName" in {
      val request = makeRequest(authenticated())
      val oidOrId = doPut(request, externalSession)
      doGet(oidOrId, resultEntity(oidOrId, Some(TestUserOid)))
    }

    it should s"require authentication to be attached to the request when creating new $entityName" in {
      val entityName = path.split("/").last
      put(path, Map(entityName -> resultEntity("", Some(TestUserOid))), externalSession, 400)
    }

    it should s"store muokkaaja from the session sent from kouta-external when creating new $entityName" in {
      val userOid = TestOids.randomUserOid
      val request = makeRequest(authenticated(personOid = userOid))
      val oidOrId = doPut(request, ophSession)
      doGet(oidOrId, resultEntity(oidOrId, Some(userOid)))
    }

    it should s"deny a user with the wrong role when creating new $entityName" in {
      val authentication = authenticated()
      val request        = makeRequest(authentication)
      put(path, request, defaultSessionId, 403)
    }

    it should s"use the attached session for authentication when creating new $entityName" in {
      val authentication =
        authenticated(personOid = OidWithReadOnlyAccess, authorities = Set(Authority(Role.Koulutus.Read, OphOid)))
      val request = makeRequest(authentication)
      put(path, request, externalSession, 403)
    }
  }
}

trait ModifyTests extends TestBase {
  this: ExternalFixture with AccessControlSpec =>

  def executeModifyTests[E](
      entityName: String,
      path: String,
      makeModifyRequest: (String, Authenticated) => ExternalRequest,
      resultEntity: (String, Option[UserOid], Option[Julkaisutila], Option[Boolean]) => E
  ): Unit = {
    s"Update $entityName from external" should s"update $entityName in backend" in {
      val oidOrId      = doPut(resultEntity("", None, None, None), ophSession)
      val lastModified = doGet(oidOrId, resultEntity(oidOrId, None, None, None))

      val request = makeModifyRequest(oidOrId, authenticated())

      doUpdate(request, lastModified, externalSession)
      doGet(oidOrId, resultEntity(oidOrId, Some(TestUserOid), Some(Arkistoitu), Some(false)))
    }

    it should s"require authentication to be attached to the request when updating existing $entityName" in {
      val oidOrId      = doPut(resultEntity("", None, None, None), ophSession)
      val lastModified = doGet(oidOrId, resultEntity(oidOrId, None, None, None))

      val entityName = path.split("/").last
      update(path, Map(entityName -> resultEntity(oidOrId, None, None, None)), lastModified, externalSession, 400)
    }

    it should s"store muokkaaja from the session sent from kouta-external when updating existing $entityName" in {
      val oidOrId      = doPut(resultEntity("", None, None, None), ophSession)
      val lastModified = doGet(oidOrId, resultEntity(oidOrId, None, None, None))

      val userOid        = TestOids.randomUserOid
      val authentication = authenticated(personOid = userOid)
      val request        = makeModifyRequest(oidOrId, authentication)

      doUpdate(request, lastModified, externalSession)
      doGet(oidOrId, resultEntity(oidOrId, Some(userOid), Some(Arkistoitu), Some(false)))
    }

    it should s"deny a user with the wrong role when updating existing $entityName" in {
      val oidOrId      = doPut(resultEntity("", None, None, None), ophSession)
      val lastModified = doGet(oidOrId, resultEntity(oidOrId, None, None, None))

      val request = makeModifyRequest(oidOrId, authenticated())

      update(path, request, lastModified, defaultSessionId, 403)
    }

    it should s"use the attached session for authentication when updating existing $entityName" in {
      val oidOrId      = doPut(resultEntity("", None, None, None), ophSession)
      val lastModified = doGet(oidOrId, resultEntity(oidOrId, None, None, None))

      val authentication =
        authenticated(personOid = OidWithReadOnlyAccess, authorities = Set(Authority(Role.Koulutus.Read, OphOid)))
      val request = makeModifyRequest(oidOrId, authentication)
      update(path, request, lastModified, externalSession, 403)
    }
  }
}
