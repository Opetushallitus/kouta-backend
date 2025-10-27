package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestOids.{OphUserOid, TestUserOid}
import fi.oph.kouta.client.KoodistoClient
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.security.{Authority, ExternalSession}
import fi.oph.kouta.service.KoodistoService
import fi.oph.kouta.servlet.{Authenticated, ExternalServlet}
import slick.jdbc.PostgresProfile.api._

import java.net.InetAddress
import java.util.UUID

trait ExternalFixture
    extends KoulutusFixture
    with ToteutusFixture
    with HakuFixture
    with HakukohdeFixture
    with ValintaperusteFixture
    with SorakuvausFixture {
  this: KoutaIntegrationSpec =>

  override val koodistoService = new KoodistoService(new KoodistoClient(urlProperties.get))

  val ExternalKoulutusPath       = "/external/koulutus"
  val ExternalToteutusPath       = "/external/toteutus"
  val ExternalHakuPath           = "/external/haku"
  val ExternalHakukohdePath      = "/external/hakukohde"
  val ExternalValintaperustePath = "/external/valintaperuste"
  val ExternalSorakuvausPath     = "/external/sorakuvaus"

  var koulutusOidForToteutus: KoulutusOid        = _
  var toteutusOidForHakukohde: ToteutusOid       = _
  var hakuOidForHakukohde: HakuOid               = _
  var valintaperusteIdForHakukohde: Option[UUID] = None
  var valintaperusteenValintakoeId: Option[UUID] = None

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(
      new ExternalServlet(
        koulutusService,
        toteutusService,
        hakuService,
        hakukohdeService,
        valintaperusteService,
        sorakuvausService
      ),
      "/external"
    )
    koulutusOidForToteutus = KoulutusOid(doPut(koulutus, ophSession))
    toteutusOidForHakukohde = ToteutusOid(doPut(toteutus, ophSession))
    hakuOidForHakukohde = HakuOid(doPut(haku, ophSession))
    valintaperusteIdForHakukohde = Some(UUID.fromString(doPut(valintaperuste, ophSession)))
  }

  def createKoulutus(
      oid: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Koulutus =
    yoKoulutus.copy(
      oid = if (oid.isEmpty) None else Some(KoulutusOid(oid)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata = Some(
        yoKoulutus.metadata.get
          .asInstanceOf[YliopistoKoulutusMetadata]
          .copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true)))
      )
    )

  def createToteutus(
      oid: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Toteutus =
    toteutus.copy(
      koulutusOid = koulutusOidForToteutus,
      oid = if (oid.isEmpty) None else Some(ToteutusOid(oid)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata = Some(
        toteutus.metadata.get
          .asInstanceOf[AmmatillinenToteutusMetadata]
          .copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true)))
      )
    )

  def createHaku(
      oid: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Haku =
    haku.copy(
      oid = if (oid.isEmpty) None else Some(HakuOid(oid)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata = Some(haku.metadata.get.copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true))))
    )

  lazy val tallennettuHakukohde: String => Hakukohde = { oid: String =>
    getIds(hakukohde(oid, toteutusOidForHakukohde.s, hakuOidForHakukohde.s, valintaperusteIdForHakukohde.get))
  }

  def createHakukohde(
      oid: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Hakukohde = {
    var metadata =
      hakukohde.metadata.get.copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true)))
    val lisatilaisuudet =
      metadata.valintaperusteenValintakokeidenLisatilaisuudet.map(_.copy(id = db.runBlocking(
        sql"""select id from valintaperusteiden_valintakokeet where valintaperuste_id = ${valintaperusteIdForHakukohde.get}""".as[String]).headOption.map(UUID.fromString)))
    metadata = metadata.copy(valintaperusteenValintakokeidenLisatilaisuudet = lisatilaisuudet)
    hakukohde.copy(
      toteutusOid = toteutusOidForHakukohde,
      hakuOid = hakuOidForHakukohde,
      valintaperusteId = valintaperusteIdForHakukohde,
      oid = if (oid.isEmpty) None else Some(HakukohdeOid(oid)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata = Some(metadata)
    )
  }

  def createValintaperuste(
      id: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Valintaperuste =
    valintaperuste.copy(
      id = if (id.isEmpty) None else Some(UUID.fromString(id)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata = Some(
        valintaperuste.metadata.get
          .asInstanceOf[GenericValintaperusteMetadata]
          .copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true)))
      )
    )

  def createSorakuvaus(
      id: String = "",
      muokkaaja: Option[UserOid] = None,
      tila: Option[Julkaisutila] = None,
      isMuokkaajaOphVirkailija: Option[Boolean] = None
  ): Sorakuvaus =
    sorakuvaus.copy(
      id = if (id.isEmpty) None else Some(UUID.fromString(id)),
      muokkaaja = muokkaaja.getOrElse(OphUserOid),
      tila = tila.getOrElse(Julkaistu),
      metadata =
        Some(sorakuvaus.metadata.get.copy(isMuokkaajaOphVirkailija = Some(isMuokkaajaOphVirkailija.getOrElse(true))))
    )

  def doPut[E](request: E, sessionId: UUID = defaultSessionId): String = {
    request match {
      case koulutusRequest: ExternalKoulutusRequest => put(ExternalKoulutusPath, koulutusRequest, sessionId, oid)
      case toteutusRequest: ExternalToteutusRequest =>
        put(
          ExternalToteutusPath,
          toteutusRequest.copy(toteutus = toteutusRequest.toteutus.copy(koulutusOid = koulutusOidForToteutus)),
          sessionId,
          oid
        )
      case hakuRequest: ExternalHakuRequest           => put(ExternalHakuPath, hakuRequest, sessionId, oid)
      case hakukohdeRequest: ExternalHakukohdeRequest => put(ExternalHakukohdePath, hakukohdeRequest, sessionId, oid)
      case valintaperusteRequest: ExternalValintaperusteRequest =>
        put(ExternalValintaperustePath, valintaperusteRequest, sessionId, id(_).toString)
      case sorakuvausRequest: ExternalSorakuvausRequest =>
        put(ExternalSorakuvausPath, sorakuvausRequest, sessionId, id(_).toString)

      case koulutus: Koulutus             => put(koulutus, sessionId)
      case toteutus: Toteutus             => put(toteutus.copy(koulutusOid = koulutusOidForToteutus), sessionId)
      case haku: Haku                     => put(haku, sessionId)
      case hakukohde: Hakukohde           => put(hakukohde, sessionId)
      case valintaperuste: Valintaperuste => put(valintaperuste, sessionId).toString
      case sorakuvaus: Sorakuvaus         => put(sorakuvaus, sessionId).toString
    }
  }

  def doGet[E](oidOrId: String, expected: E, sessionId: UUID = defaultSessionId): String = {
    expected match {
      case koulutus: Koulutus =>
        get(KoulutusPath, oidOrId, sessionId, koulutus.copy(modified = Some(readKoulutusModified(oidOrId))))
      case toteutus: Toteutus =>
        get(
          ToteutusPath,
          oidOrId,
          sessionId,
          toteutus.copy(koulutusOid = koulutusOidForToteutus, modified = Some(readToteutusModified(oidOrId)))
        )
      case haku: Haku => get(HakuPath, oidOrId, sessionId, haku.copy(modified = Some(readHakuModified(oidOrId))))
      case hakukohde: Hakukohde =>
        get(
          HakukohdePath,
          oidOrId,
          sessionId,
          tallennettuHakukohde(oidOrId).copy(
            muokkaaja = hakukohde.muokkaaja,
            tila = hakukohde.tila,
            modified = Some(readHakukohdeModified(oidOrId)),
            metadata = hakukohde.metadata
          )
        )
      case valintaperuste: Valintaperuste =>
        get(
          ValintaperustePath,
          oidOrId,
          sessionId,
          valintaperuste.copy(modified = Some(readValintaperusteModified(oidOrId)))
        )
      case sorakuvaus: Sorakuvaus =>
        get(SorakuvausPath, oidOrId, sessionId, sorakuvaus.copy(modified = Some(readSorakuvausModified(oidOrId))))
    }
  }

  def doUpdate[E](request: E, lastModified: String, sessionId: UUID = defaultSessionId): Unit = {
    request match {
      case koulutusRequest: ExternalKoulutusRequest =>
        update(ExternalKoulutusPath, koulutusRequest, lastModified, expectUpdate = true, sessionId)
      case toteutusRequest: ExternalToteutusRequest =>
        update(ExternalToteutusPath, toteutusRequest, lastModified, expectUpdate = true, sessionId)
      case hakuRequest: ExternalHakuRequest =>
        update(ExternalHakuPath, hakuRequest, lastModified, expectUpdate = true, sessionId)
      case hakukohdeRequest: ExternalHakukohdeRequest =>
        update(ExternalHakukohdePath, hakukohdeRequest, lastModified, expectUpdate = true, sessionId)
      case valintaperusteRequest: ExternalValintaperusteRequest =>
        update(ExternalValintaperustePath, valintaperusteRequest, lastModified, expectUpdate = true, sessionId)
      case sorakuvausRequest: ExternalSorakuvausRequest =>
        update(ExternalSorakuvausPath, sorakuvausRequest, lastModified, expectUpdate = true, sessionId)
    }
  }

  def authenticated(
      id: String = UUID.randomUUID().toString,
      personOid: UserOid = TestUserOid,
      authorities: Set[Authority] = defaultAuthorities,
      userAgent: String = "test-agent",
      ip: InetAddress = InetAddress.getByName("192.168.1.19")
  ) = {
    val session = ExternalSession(personOid.s, authorities)
    Authenticated(id, session, userAgent, ip)
  }
}
