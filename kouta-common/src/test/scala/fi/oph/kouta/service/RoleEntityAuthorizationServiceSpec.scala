package fi.oph.kouta.service

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Amm, Koulutustyyppi, Yo}
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}

import java.net.InetAddress
import java.time.Instant
import java.util.UUID
import scala.util.{Failure, Try}

case class TestEntity(
    koulutustyyppi: Koulutustyyppi,
    julkinen: Boolean,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid
) extends AuthorizableMaybeJulkinen[TestEntity] {
  override def withMuokkaaja(muokkaaja: UserOid): TestEntity = this.copy(muokkaaja = muokkaaja)
}

case class TestService(organisaatioService: OrganisaatioService) extends RoleEntityAuthorizationService[TestEntity] {
  override protected val roleEntity: RoleEntity = Role.Koulutus
}

trait ServiceSpec extends OrganisaatioFixture {
  protected def authenticated(role: Role, organizations: Seq[OrganisaatioOid]): Authenticated = {
    val authorities = organizations.map(Authority.apply(role, _)).toSet
    Authenticated(
      UUID.randomUUID().toString,
      CasSession(
        ServiceTicket("ST-123"),
        "1.2.3.1234",
        authorities
      ),
      "testAgent",
      InetAddress.getByName("127.0.0.1")
    )
  }
}

class RoleEntityAuthorizationServiceSpec extends ServiceSpec {
  var context: TestService = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    context = TestService(organisaatioService)
    mockOrganisaatioResponse()
  }

  private def allowRead(
                         e: TestEntity,
                         tarjoajat: Seq[OrganisaatioOid]
                       )(implicit auth: Authenticated): Unit = {
    val testEntity = Some(e, Instant.now())
    context.authorizeGet(
      testEntity,
      AuthorizationRules(
        Role.Koulutus.readRoles,
        allowAccessToParentOrganizations = true,
        Some(AuthorizationRuleForReadJulkinen),
        tarjoajat
      )
    ) should equal(testEntity)
  }

  private def denyRead(
                        e: TestEntity,
                        tarjoajat: Seq[OrganisaatioOid]
                      )(implicit auth: Authenticated): Unit = {
    val testEntity = Some(e, Instant.now())
    Try {
      context.authorizeGet(
        testEntity,
        AuthorizationRules(
          Role.Koulutus.readRoles,
          allowAccessToParentOrganizations = true,
          Some(AuthorizationRuleForReadJulkinen),
          tarjoajat
        )
      )
    } match {
      case Failure(_: OrganizationAuthorizationFailedException) =>
      case Failure(_: RoleAuthorizationFailedException) =>
      case Failure(_: KoulutustyyppiAuthorizationFailedException) =>
      case _ => fail("Expecting failure, but it succeeded")
    }
  }

  private def tarjoajaRules(
                             tarjoajat: Seq[OrganisaatioOid]
                           ): AuthorizationRules =
    AuthorizationRules(
      requiredRoles = Role.Koulutus.updateRoles,
      allowAccessToParentOrganizations = true,
      overridingAuthorizationRule = Some(AuthorizationRuleForUpdateTarjoajat),
      additionalAuthorizedOrganisaatioOids = tarjoajat
    )

  "authorizeGet" should "allow access when entity owned by users organization" in {
    implicit val auth: Authenticated = authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid))
    allowRead(TestEntity(Yo, false, YoOid, TestUserOid), Seq())
  }

  it should "allow access when user had rights to one tarjoaja organization" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid, ChildOid, EvilChildOid))
    allowRead(TestEntity(Amm, false, AmmOid, TestUserOid), Seq(YoOid, LukioOid))
  }

  it should "allow access when user had rights to one tarjoaja parent organization" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid, ChildOid, EvilChildOid))
    allowRead(TestEntity(Amm, false, AmmOid, TestUserOid), Seq(ParentOid, LukioOid))
  }

  it should "allow access when julkinen entity and one of user's organisations is of matching koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid, ChildOid, EvilChildOid))
    allowRead(TestEntity(Yo, true, AmmOid, TestUserOid), Seq(LonelyOid, LukioOid))
  }

  it should "allow access when OPH owned julkinen entity and user's organisations match tarjoajat and koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid, ChildOid, EvilChildOid))
    allowRead(TestEntity(Yo, true, OphOid, OphUserOid), Seq(GrandChildOid, LukioOid))
  }

  it should "allow access when user member of OPH organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(OphOid))
    allowRead(TestEntity(Amm, false, AmmOid, TestUserOid), Seq())
  }

  it should "deny access when non-julkinen entity, and user has no rights to organisations" in {
    implicit val auth: Authenticated = authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid, ChildOid))
    denyRead(TestEntity(Amm, false, AmmOid, TestUserOid), Seq(LonelyOid, LukioOid))
  }

  it should "deny access when julkinen entity, but user has no rights to organisations and koulutustyyppi does not match either" in {
    implicit val auth: Authenticated = authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(YoOid))
    denyRead(TestEntity(Amm, true, AmmOid, TestUserOid), Seq(LonelyOid, LukioOid))
  }

  it should "deny access when OPH owned entity and user's organisations match tarjoajat, but koulutustyyppi doesn't" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Read.asInstanceOf[Role], Seq(AmmOid, ChildOid, EvilChildOid))
    denyRead(TestEntity(Yo, false, OphOid, OphUserOid), Seq(GrandChildOid, LukioOid))
  }

  it should "deny access for user with wrong role" in {
    implicit val auth: Authenticated = authenticated(Role.UnknownRole("APP_OTHER"), Seq(YoOid))
    denyRead(TestEntity(Yo, false, YoOid, TestUserOid), Seq())
  }

  private def allowCreate(
                           e: TestEntity,
                           tarjoajat: Seq[OrganisaatioOid]
                         )(implicit auth: Authenticated): Unit = {
    val checkedEntity = authorizePut(e, tarjoajat)
    checkedEntity should equal(e.copy(muokkaaja = UserOid("1.2.3.1234")))
  }

  private def denyCreate(e: TestEntity, tarjoajat: Seq[OrganisaatioOid])(implicit
                                                                         auth: Authenticated
  ): Unit =
    Try {
      authorizePut(e, tarjoajat)
    } match {
      case Failure(_: OrganizationAuthorizationFailedException) =>
      case Failure(_: RoleAuthorizationFailedException) =>
      case _ => fail("Expecting failure, but it succeeded")
    }

  private def authorizePut(e: TestEntity, tarjoajat: Seq[OrganisaatioOid])(implicit
                                                                           auth: Authenticated
  ): TestEntity = {
    var ruleList = List(AuthorizationRules(Role.Koulutus.createRoles))
    ruleList = if (tarjoajat.nonEmpty) ruleList :+ tarjoajaRules(tarjoajat) else ruleList
    context.authorizePut(e, ruleList)((checkedEntity: TestEntity) => {
      checkedEntity
    })
  }

  "authorizePut" should "allow access when user member of owner organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(ChildOid))
    allowCreate(TestEntity(Amm, false, ChildOid, TestUserOid), Seq())
    allowCreate(TestEntity(Amm, false, GrandChildOid, TestUserOid), Seq())
  }

  it should "allow access when user member of owner- and tarjoaja-organisations" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Crud.asInstanceOf[Role],
        Seq(ChildOid, AmmOid, LonelyOid, EvilChildOid, OtherOid, YoOid)
      )
    allowCreate(TestEntity(Amm, false, ChildOid, TestUserOid), Seq(AmmOid, LonelyOid))
    allowCreate(TestEntity(Amm, false, ChildOid, TestUserOid), Seq(ParentOid))
  }

  it should "allow access when user member of OPH organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(OphOid))
    allowCreate(TestEntity(Amm, false, ChildOid, OphUserOid), Seq(AmmOid, LonelyOid))
  }

  it should "deny access when user not member of owner organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(ChildOid))
    denyCreate(TestEntity(Amm, false, AmmOid, TestUserOid), Seq())
  }

  it should "deny access when user not member of all tarjoaja organisations" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(ChildOid, LonelyOid, EvilChildOid, OtherOid, YoOid))
    denyCreate(TestEntity(Amm, false, ChildOid, TestUserOid), Seq(AmmOid, LonelyOid))
  }

  it should "deny access for user with wrong role" in {
    implicit val auth: Authenticated = authenticated(Role.UnknownRole("APP_OTHER"), Seq(ChildOid))
    denyCreate(TestEntity(Amm, false, ChildOid, TestUserOid), Seq())
  }

  private val rulesCheckingOrganizationAndKoulutustyyppi = AuthorizationRules(
    Role.Koulutus.createRoles,
    overridingAuthorizationRule = Some(AuthorizationRuleByOrganizationAndKoulutustyyppi)
  )

  "authorizePut by organization and koulutustyyppi" should "allow access when user is member of owner organisation and has rights to koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(YoOid))
    val testEntity = TestEntity(Yo, false, YoOid, TestUserOid)
    context.authorizePut(testEntity, rulesCheckingOrganizationAndKoulutustyyppi)((checkedEntity: TestEntity) => {
      checkedEntity should equal(testEntity.copy(muokkaaja = UserOid("1.2.3.1234")))
    })
  }

  it should "deny access when user not member of owner organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(YoOid))
    val testEntity = TestEntity(Yo, false, ChildOid, TestUserOid)
    Try {
      context.authorizePut(testEntity, rulesCheckingOrganizationAndKoulutustyyppi)((checkedEntity: TestEntity) => {
        checkedEntity should equal(testEntity.copy(muokkaaja = UserOid("1.2.3.1234")))
      })
    } match {
      case Failure(_) =>
      case _ => fail("Expecting failure, but it succeeded")
    }
  }

  it should "deny access when user has not rights to koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Crud.asInstanceOf[Role], Seq(YoOid))
    val testEntity = TestEntity(Amm, false, YoOid, TestUserOid)
    Try {
      context.authorizePut(testEntity, rulesCheckingOrganizationAndKoulutustyyppi)((checkedEntity: TestEntity) => {
        checkedEntity should equal(testEntity.copy(muokkaaja = UserOid("1.2.3.1234")))
      })
    } match {
      case Failure(_) =>
      case _ => fail("Expecting failure, but it succeeded")
    }
  }

  private def allowUpdate(
                           e: TestEntity,
                           oldE: TestEntity,
                           tarjoajat: Seq[OrganisaatioOid],
                           authorizeTarjoajatOnly: Boolean = false
                         )(implicit auth: Authenticated): Unit = {
    val checkedEntity = authorizeUpdate(e, oldE, tarjoajat, authorizeTarjoajatOnly)
    checkedEntity should equal(e.copy(muokkaaja = UserOid("1.2.3.1234")))
  }

  private def denyUpdate(
                          e: TestEntity,
                          oldE: TestEntity,
                          tarjoajat: Seq[OrganisaatioOid],
                          authorizeTarjoajatOnly: Boolean = false
                        )(implicit
                          auth: Authenticated
                        ): Unit =
    Try {
      authorizeUpdate(e, oldE, tarjoajat, authorizeTarjoajatOnly)
    } match {
      case Failure(_: OrganizationAuthorizationFailedException) =>
      case Failure(_: RoleAuthorizationFailedException) =>
      case Failure(_: KoulutustyyppiAuthorizationFailedException) =>
      case _ => fail("Expecting failure, but it succeeded")
    }

  private def authorizeUpdate(
                               e: TestEntity,
                               oldE: TestEntity,
                               tarjoajat: Seq[OrganisaatioOid],
                               authorizeTarjoajatOnly: Boolean
                             )(implicit
                               auth: Authenticated
                             ): TestEntity = {
    val oldEWithInstant = Some(oldE, Instant.now())
    val ruleList = authorizeTarjoajatOnly match {
      case true => List(tarjoajaRules(tarjoajat))
      case _ =>
        val baseRules = List(AuthorizationRules(Role.Koulutus.updateRoles))
        if (tarjoajat.nonEmpty) baseRules :+ tarjoajaRules(tarjoajat) else baseRules
    }
    context.authorizeUpdate(oldEWithInstant, e, ruleList)((_: TestEntity, checkedEntity: TestEntity) => {
      checkedEntity
    })
  }

  "authorizeUpdate" should "allow access when user member of owner organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid))
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    allowUpdate(testEntity, testEntity, Seq())
    val testEntityOfChildOrg = TestEntity(Amm, false, GrandChildOid, TestUserOid)
    allowUpdate(testEntityOfChildOrg, testEntityOfChildOrg, Seq())
  }

  it should "allow access when owner organisation changed and user member of both new and old organisations" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid, AmmOid))
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    allowUpdate(testEntity.copy(organisaatioOid = AmmOid), testEntity, Seq())
  }

  it should "allow access when user member of owner- and tarjoaja-organisations" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(ChildOid, AmmOid, LonelyOid, EvilChildOid, OtherOid, YoOid)
      )
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    allowUpdate(testEntity, testEntity, Seq(AmmOid, LonelyOid))
    allowUpdate(testEntity, testEntity, Seq(ParentOid))
    allowUpdate(testEntity, testEntity, Seq(GrandChildOid))
  }

  it should "allow access when user member of OPH organisation" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(OphOid)
      )
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    allowUpdate(testEntity, testEntity, Seq(AmmOid, LonelyOid))
  }

  it should "deny access when updated entity not existing" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid))
    Try {
      context.authorizeUpdate(
        None,
        TestEntity(Amm, false, ChildOid, TestUserOid),
        List(AuthorizationRules(Role.Koulutus.updateRoles))
      )((_: TestEntity, checkedEntity: TestEntity) => {
        checkedEntity
      })
    } match {
      case Failure(_: EntityNotFoundException) =>
      case _ => fail("Expecting failure, but it succeeded")
    }
  }

  it should "deny access when user not member of owner organisation of entity" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid))
    val testEntity = TestEntity(Amm, false, AmmOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq())
    val testEntityOfParentOrg = TestEntity(Amm, false, ParentOid, TestUserOid)
    denyUpdate(testEntityOfParentOrg, testEntityOfParentOrg, Seq())
  }

  it should "deny access when owner organisation changed and user not member of new organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid))
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    denyUpdate(testEntity.copy(organisaatioOid = AmmOid), testEntity, Seq())
  }

  it should "deny access when user not member of any tarjoaja organisation" in {
    implicit val auth: Authenticated =
      authenticated(Role.Koulutus.Update.asInstanceOf[Role], Seq(ChildOid))
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq(AmmOid))
  }

  it should "deny access for user with wrong role" in {
    implicit val auth: Authenticated = authenticated(Role.UnknownRole("APP_OTHER"), Seq(ChildOid))
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq())
  }

  "update tarjoajat only" should "allow access when non-julkinen entity and user member of owner organisations" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(ChildOid, YoOid)
      )
    val testEntity = TestEntity(Amm, false, ChildOid, TestUserOid)
    allowUpdate(testEntity, testEntity, Seq(YoOid), true)
    val testEntityOfChildOrg = TestEntity(Amm, false, GrandChildOid, TestUserOid)
    allowUpdate(testEntityOfChildOrg, testEntityOfChildOrg, Seq(YoOid), true)
  }

  it should "allow access when julkinen entity and user has rights to koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(ChildOid, LonelyOid)
      )
    val testEntity = TestEntity(Amm, true, AmmOid, TestUserOid)
    allowUpdate(testEntity, testEntity, Seq(ChildOid, LonelyOid), true)
  }

  it should "deny access when non-julkinen entity and user not member of owner organisations" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(ChildOid, LonelyOid)
      )
    val testEntity = TestEntity(Amm, false, AmmOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq(ChildOid, LonelyOid), true)
    val testEntityOfParentOrg = TestEntity(Amm, false, ParentOid, TestUserOid)
    denyUpdate(testEntityOfParentOrg, testEntityOfParentOrg, Seq(ChildOid, LonelyOid), true)
  }

  it should "deny access when julkinen entity and user not member of all tarjoaja organisations" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(AmmOid, LonelyOid)
      )
    val testEntity = TestEntity(Amm, true, ChildOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq(AmmOid, LonelyOid, OtherOid), true)
  }

  it should "deny access when julkinen entity and user has no rights to koulutustyyppi" in {
    implicit val auth: Authenticated =
      authenticated(
        Role.Koulutus.Update.asInstanceOf[Role],
        Seq(AmmOid, LonelyOid)
      )
    val testEntity = TestEntity(Yo, true, ChildOid, TestUserOid)
    denyUpdate(testEntity, testEntity, Seq(AmmOid, LonelyOid), true)
  }
}

case class TestAuthorizableByKoulutustyyppi(
    koulutustyyppi: Koulutustyyppi,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid
) extends AuthorizableByKoulutustyyppi[TestAuthorizableByKoulutustyyppi] {
  override def withMuokkaaja(muokkaaja: UserOid): TestAuthorizableByKoulutustyyppi = this.copy(muokkaaja = muokkaaja)
}

case class TestAuthorizableByKoulutustyyppiService(organisaatioService: OrganisaatioService)
    extends RoleEntityAuthorizationService[TestAuthorizableByKoulutustyyppi] {
  override protected val roleEntity: RoleEntity = Role.Valintaperuste // Not really used
}

class AuthorizableByKoulutustyyppiServiceSpec extends ServiceSpec {
  var context: TestAuthorizableByKoulutustyyppiService = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    context = TestAuthorizableByKoulutustyyppiService(organisaatioService)
    mockOrganisaatioResponse()
  }

  "authorizeGet" should "allow access when user has rights to koulutustyyppi" in {
    implicit val auth: Authenticated = authenticated(Role.Valintaperuste.Read.asInstanceOf[Role], Seq(YoOid))
    val testEntity                   = TestAuthorizableByKoulutustyyppi(Yo, ChildOid, TestUserOid)
    context.authorizeGet(
      testEntity,
      AuthorizationRules(
        Role.Valintaperuste.readRoles,
        allowAccessToParentOrganizations = true,
        Some(AuthorizationRuleByKoulutustyyppi)
      )
    ) should equal(testEntity)
  }

  it should "deny access when user hasn't rights to koulutustyyppi" in {
    implicit val auth: Authenticated = authenticated(Role.Valintaperuste.Read.asInstanceOf[Role], Seq(YoOid))
    val testEntity                   = TestAuthorizableByKoulutustyyppi(Amm, ChildOid, TestUserOid)
    Try {
      context.authorizeGet(
        testEntity,
        AuthorizationRules(
          Role.Valintaperuste.readRoles,
          allowAccessToParentOrganizations = true,
          Some(AuthorizationRuleByKoulutustyyppi)
        )
      )
    } match {
      case Failure(_) =>
      case _          => fail("Expecting failure, but it succeeded")
    }
  }
}
