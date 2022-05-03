package fi.oph.kouta.service

import java.time.Instant
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.images.{LogoService, S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.repository.{KoutaDatabase, OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.Validations
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object OppilaitosService extends OppilaitosService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient)

class OppilaitosService(
  sqsInTransactionService: SqsInTransactionService,
  val s3ImageService: S3ImageService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioService,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient
) extends ValidatingService[Oppilaitos] with RoleEntityAuthorizationService[Oppilaitos] with LogoService {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitos-teemakuva"
  val logoPrefix = "oppilaitos-logo"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(Oppilaitos, Instant)] = {
    val oppilaitosWithTime = OppilaitosDAO.get(oid)

    val enrichedOppilaitos = oppilaitosWithTime match {
      case Some((o, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(o.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(o.copy(_enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }
    authorizeGet(enrichedOppilaitos)
  }

  def get(oids: List[OrganisaatioOid])(implicit authenticated: Authenticated): List[OppilaitosByOid] = {
    val oppilaitokset = OppilaitosDAO.get(oids)

    oids.map(oid => {
      val byOid = oppilaitokset.filter(oppilaitosAndOsa => {
        oppilaitosAndOsa.oppilaitos.oid == oid ||
          oppilaitosAndOsa.osa.oid == oid
      })

      if (!byOid.isEmpty) {
        val oppilaitos = authorizeGet(byOid.head.oppilaitos)
        val osat = byOid.map(oppilaitosAndOsa => {
          oppilaitosAndOsa.osa
        })

        val oppilaitosWithOsat = authorizeGet(oppilaitos.copy(osat = Some(osat)))

        OppilaitosByOid(oid = oid, oppilaitos = Some(oppilaitosWithOsat))
      } else {
        OppilaitosByOid(oid = oid, oppilaitos = None)
      }
    })
  }

  private def enrichOppilaitosMetadata(oppilaitos: Oppilaitos) : Option[OppilaitosMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(oppilaitos.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    oppilaitos.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None => None
    }
  }

  def put(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): OrganisaatioOid = {
    val enrichedMetadata: Option[OppilaitosMetadata] = enrichOppilaitosMetadata(oppilaitos)
    val enrichedOppilaitos = oppilaitos.copy(metadata = enrichedMetadata)
    authorizePut(enrichedOppilaitos) { o =>
      withValidation(o, None)(doPut)
    }.oid
  }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val enrichedMetadata: Option[OppilaitosMetadata] = enrichOppilaitosMetadata(oppilaitos)
    val enrichedOppilaitos = oppilaitos.copy(metadata = enrichedMetadata)
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid), enrichedOppilaitos) { (oldOppilaitos, o) =>
      withValidation(o, Some(oldOppilaitos)) {
        doUpdate(_, notModifiedSince, oldOppilaitos)
      }
    }.nonEmpty
  }

  def getOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsa] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.getByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.listByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Oppilaitos.readRoles) {
      OppilaitoksenOsaDAO.listByOppilaitosOidAndOrganisaatioOids(oid, _)
    }

  private def doPut(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): Oppilaitos =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (teema, o) <- checkAndMaybeClearTeemakuva(oppilaitos)
        (logo, o) <- checkAndMaybeClearLogo(o)
        o          <- OppilaitosDAO.getPutActions(o)
        o          <- maybeCopyTeemakuva(teema, o)
        o          <- maybeCopyLogo(logo, o)
        o          <- teema.orElse(logo).map(_ => OppilaitosDAO.updateJustOppilaitos(o)).getOrElse(DBIO.successful(o))
        _          <- index(Some(o))
        _          <- auditLog.logCreate(o)
      } yield (teema, logo, o)
    }.map { case (teema, logo, o) =>
      maybeDeleteTempImage(teema)
      maybeDeleteTempImage(logo)
      o
    }.get

  private def doUpdate(oppilaitos: Oppilaitos, notModifiedSince: Instant, before: Oppilaitos)(implicit authenticated: Authenticated): Option[Oppilaitos] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- OppilaitosDAO.checkNotModified(oppilaitos.oid, notModifiedSince)
        (teema, o) <- checkAndMaybeCopyTeemakuva(oppilaitos)
        (logo, o) <- checkAndMaybeCopyLogo(o)
        o          <- OppilaitosDAO.getUpdateActions(o)
        _          <- index(o)
        _          <- auditLog.logUpdate(before, o)
      } yield (teema, logo, o)
    }.map { case (teema, logo, o) =>
      maybeDeleteTempImage(teema)
      maybeDeleteTempImage(logo)
      o
    }.get

  private def index(oppilaitos: Option[Oppilaitos]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitos.map(_.oid.toString))
}

object OppilaitoksenOsaService extends OppilaitoksenOsaService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient)

class OppilaitoksenOsaService(
  sqsInTransactionService: SqsInTransactionService,
  val s3ImageService: S3ImageService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioService,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient
) extends ValidatingService[OppilaitoksenOsa]
    with RoleEntityAuthorizationService[OppilaitoksenOsa]
    with TeemakuvaService[OrganisaatioOid, OppilaitoksenOsa] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitoksen-osa-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitoksenOsa, Instant)] = {
    val oppilaitoksenOsaWithTime = OppilaitoksenOsaDAO.get(oid)

    val enrichedOppilaitoksenOsa = oppilaitoksenOsaWithTime match {
      case Some((o, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(o.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(o.copy(_enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }
    authorizeGet(enrichedOppilaitoksenOsa)
  }

  private def enrichOppilaitoksenOsaMetadata(oppilaitoksenOsa: OppilaitoksenOsa) : Option[OppilaitoksenOsaMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(oppilaitoksenOsa.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    oppilaitoksenOsa.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None => None
    }
  }

  def put(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OrganisaatioOid = {
    val enrichedMetadata: Option[OppilaitoksenOsaMetadata] = enrichOppilaitoksenOsaMetadata(oppilaitoksenOsa)
    val enrichedOppilaitoksenOsa = oppilaitoksenOsa.copy(metadata = enrichedMetadata)
    authorizePut(enrichedOppilaitoksenOsa) { o =>
      withValidation(o, None) { o =>
        validateOppilaitosIntegrity(o)
        doPut(o)
      }
    }.oid
  }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val enrichedMetadata: Option[OppilaitoksenOsaMetadata] = enrichOppilaitoksenOsaMetadata(oppilaitoksenOsa)
    val enrichedOppilaitoksenOsa = oppilaitoksenOsa.copy(metadata = enrichedMetadata)
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid), enrichedOppilaitoksenOsa) { (oldOsa, o) =>
      withValidation(o, Some(oldOsa)) { o =>
        validateOppilaitosIntegrity(o)
        doUpdate(o, notModifiedSince, oldOsa)
      }
    }.nonEmpty
  }

  private def validateOppilaitosIntegrity(oppilaitoksenOsa: OppilaitoksenOsa): Unit = {
    val oppilaitosTila = OppilaitosDAO.getTila(oppilaitoksenOsa.oppilaitosOid)

    throwValidationErrors(
      Validations.validateDependency(oppilaitoksenOsa.tila, oppilaitosTila, oppilaitoksenOsa.oppilaitosOid, "Oppilaitosta", "oppilaitosOid")
    )
  }

  private def doPut(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OppilaitoksenOsa =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- OppilaitoksenOsaDAO.oppilaitosExists(oppilaitoksenOsa)
        (teema, o) <- checkAndMaybeClearTeemakuva(oppilaitoksenOsa)
        o          <- OppilaitoksenOsaDAO.getPutActions(o)
        o          <- maybeCopyTeemakuva(teema, o)
        o          <- teema.map(_ => OppilaitoksenOsaDAO.updateJustOppilaitoksenOsa(o)).getOrElse(DBIO.successful(o))
        _          <- index(Some(o))
        _          <- auditLog.logCreate(o)
      } yield (teema, o)
    }.map { case (teema, o) =>
      maybeDeleteTempImage(teema)
      o
    }.get

  private def doUpdate(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant, before: OppilaitoksenOsa)(implicit authenticated: Authenticated): Option[OppilaitoksenOsa] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- OppilaitoksenOsaDAO.checkNotModified(oppilaitoksenOsa.oid, notModifiedSince)
        (teema, o) <- checkAndMaybeCopyTeemakuva(oppilaitoksenOsa)
        o          <- OppilaitoksenOsaDAO.getUpdateActions(o)
        _          <- index(o)
        _          <- auditLog.logUpdate(before, o)
      } yield (teema, o)
    }.map { case (teema, o) =>
      maybeDeleteTempImage(teema)
      o
    }.get

  private def index(oppilaitoksenOsa: Option[OppilaitoksenOsa]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitoksenOsa.map(_.oid.toString))
}
