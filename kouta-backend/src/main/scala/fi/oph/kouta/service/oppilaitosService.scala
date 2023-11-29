package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.images.{LogoService, S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{NameHelper, OppilaitosServiceUtil, ServiceUtils}
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object OppilaitosService extends OppilaitosService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient,  OppilaitosServiceValidation, OppilaitosDAO)

class OppilaitosService(
  sqsInTransactionService: SqsInTransactionService,
  val s3ImageService: S3ImageService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioServiceImpl,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient,
  oppilaitosServiceValidation: OppilaitosServiceValidation,
  oppilaitosDAO: OppilaitosDAO,
) extends RoleEntityAuthorizationService[Oppilaitos] with LogoService {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitos-teemakuva"
  val logoPrefix = "oppilaitos-logo"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitosBase, Option[Instant])] = {
    val oppilaitosWithTime = OppilaitosDAO.get(oid)
    val yhteystieto = OppilaitosServiceUtil.getYhteystieto(organisaatioService, oid, logger)

    oppilaitosWithTime match {
      case Some((o, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(o.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        val authorized = authorizeGet(Some(o.copy(_enrichedData = Some(OppilaitosEnrichedData(
          muokkaajanNimi = Some(muokkaajanNimi),
          organisaationYhteystiedot = yhteystieto))), i))
        authorized match {
          case Some((o: Oppilaitos, i: Instant)) => Some((o, Some(i)))
          case _ => None
        }
      case None =>
        Some(OppilaitosWithOrganisaatioData(
          oid = oid,
          _enrichedData = Some(
            OppilaitosEnrichedData(organisaationYhteystiedot = yhteystieto))), None)
    }
  }

  def get(tarjoajaOids: List[OrganisaatioOid])(implicit authenticated: Authenticated): OppilaitoksetResponse = {
    val hierarkia = organisaatioService.getOrganisaatioHierarkiaWithOids(tarjoajaOids)
    val oids = OppilaitosServiceUtil.getHierarkiaOids(hierarkia)
    val oppilaitokset = oppilaitosDAO.get(oids).groupBy(oppilaitosAndOsa => oppilaitosAndOsa.oppilaitos.oid)

    val oppilaitoksetWithOsat = oppilaitokset.map(oppilaitosAndOsatByOid => {
      val oppilaitos = oppilaitosAndOsatByOid._2.head.oppilaitos
      val osat = oppilaitosAndOsatByOid._2.map(oppilaitosAndOsa => oppilaitosAndOsa.osa).flatten

        try {
          Some(authorizeGet(oppilaitos.copy(osat = Some(osat)), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)))
        } catch {
          case authorizationException: OrganizationAuthorizationFailedException =>
            logger.warn(s"Authorization failed: ${authorizationException}")
            None
        }
    }).toList.flatten
    OppilaitoksetResponse(
      oppilaitokset = oppilaitoksetWithOsat, organisaatioHierarkia = hierarkia
    )
  }

  private def enrichOppilaitosMetadata(oppilaitos: Oppilaitos) : Option[OppilaitosMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(oppilaitos.muokkaaja)
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
      oppilaitosServiceValidation.withValidation(o, None, authenticated)(doPut)
    }.oid
  }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val enrichedMetadata: Option[OppilaitosMetadata] = enrichOppilaitosMetadata(oppilaitos)
    val enrichedOppilaitos = oppilaitos.copy(metadata = enrichedMetadata)
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid), enrichedOppilaitos) { (oldOppilaitos, o) =>
      oppilaitosServiceValidation.withValidation(o, Some(oldOppilaitos), authenticated) {
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

  private def removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitos: Oppilaitos): DBIO[Int] = {
    val jarjestaaUrheilijanAmmatillistaKoulutustaOpt: Option[Boolean] = oppilaitos.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
    jarjestaaUrheilijanAmmatillistaKoulutustaOpt match {
      case Some(jarjestaaUrheilijanAmmatillistaKoulutusta) if !jarjestaaUrheilijanAmmatillistaKoulutusta => HakukohdeDAO.removeJarjestaaUrheilijanAmmatillistaKoulutustaByJarjestyspaikkaOid(oppilaitos.oid)
      case _ => DBIO.successful(0)
    }
  }

  private def doPut(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): Oppilaitos =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (teema, o) <- checkAndMaybeClearTeemakuva(oppilaitos)
        (logo, o)  <- checkAndMaybeClearLogo(o)
        o          <- OppilaitosDAO.getPutActions(o)
        o          <- maybeCopyTeemakuva(teema, o)
        o          <- maybeCopyLogo(logo, o)
        o          <- teema.orElse(logo).map(_ => OppilaitosDAO.updateJustOppilaitos(o)).getOrElse(DBIO.successful(o))
        _          <- removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitos)
        _          <- auditLog.logCreate(o)
      } yield (teema, logo, o)
    }.map { case (teema, logo, o) =>
      maybeDeleteTempImage(teema)
      maybeDeleteTempImage(logo)
      index(Some(o))
      o
    }.get

  def doUpdate(oppilaitos: Oppilaitos, notModifiedSince: Instant, before: Oppilaitos)(implicit authenticated: Authenticated): Option[Oppilaitos] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- OppilaitosDAO.checkNotModified(oppilaitos.oid, notModifiedSince)
        (teema, o) <- checkAndMaybeCopyTeemakuva(oppilaitos)
        (logo, o) <- checkAndMaybeCopyLogo(o)
        o          <- OppilaitosDAO.getUpdateActions(o)
        _          <- removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitos)
        _          <- auditLog.logUpdate(before, o)
      } yield (teema, logo, o)
    }.map { case (teema, logo, o) =>
      maybeDeleteTempImage(teema)
      maybeDeleteTempImage(logo)
      index(o)
      o
    }.get

  private def index(oppilaitos: Option[Oppilaitos]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitos.map(_.oid.toString))
}

object OppilaitoksenOsaService extends OppilaitoksenOsaService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient, OppilaitosServiceValidation, OppilaitoksenOsaServiceValidation)

class OppilaitoksenOsaService(
  sqsInTransactionService: SqsInTransactionService,
  val s3ImageService: S3ImageService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioServiceImpl,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient,
  oppilaitosServiceValidation: OppilaitosServiceValidation,
  oppilaitoksenOsaServiceValidation: OppilaitoksenOsaServiceValidation
) extends RoleEntityAuthorizationService[OppilaitoksenOsa]
    with TeemakuvaService[OrganisaatioOid, OppilaitoksenOsa] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitoksen-osa-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitosBase, Option[Instant])] = {
    val oppilaitoksenOsaWithTime = OppilaitoksenOsaDAO.get(oid)

    val yhteystieto = OppilaitosServiceUtil.getYhteystieto(organisaatioService, oid, logger)

    oppilaitoksenOsaWithTime match {
      case Some((o, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(o.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        val authorized = authorizeGet(Some(o.copy(_enrichedData = Some(OppilaitosEnrichedData(
          muokkaajanNimi = Some(muokkaajanNimi),
          organisaationYhteystiedot = yhteystieto))), i))
        authorized match {
          case Some((o: OppilaitoksenOsa, i: Instant)) => Some((o, Some(i)))
          case _ => None
        }
      case None =>
        Some(OppilaitosWithOrganisaatioData(
          oid = oid,
          _enrichedData = Some(
            OppilaitosEnrichedData(organisaationYhteystiedot = yhteystieto))), None)
    }
  }

  private def enrichOppilaitoksenOsaMetadata(oppilaitoksenOsa: OppilaitoksenOsa) : Option[OppilaitoksenOsaMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(oppilaitoksenOsa.muokkaaja)
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
      oppilaitoksenOsaServiceValidation.withValidation(o, None, authenticated) { o =>
        doPut(o)
      }
    }.oid
  }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {

    val enrichedMetadata: Option[OppilaitoksenOsaMetadata] = enrichOppilaitoksenOsaMetadata(oppilaitoksenOsa)
    val enrichedOppilaitoksenOsa = oppilaitoksenOsa.copy(metadata = enrichedMetadata)
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid), enrichedOppilaitoksenOsa) { (oldOsa, o) =>
      oppilaitoksenOsaServiceValidation.withValidation(o, Some(oldOsa), authenticated) { o =>
        doUpdate(o, notModifiedSince, oldOsa)
      }
    }.nonEmpty
  }

  private def removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitoksenOsa: OppilaitoksenOsa): DBIO[Int] = {
    val jarjestaaUrheilijanAmmatillistaKoulutustaOpt: Option[Boolean] = oppilaitoksenOsa.metadata.flatMap(_.jarjestaaUrheilijanAmmKoulutusta)
    jarjestaaUrheilijanAmmatillistaKoulutustaOpt match {
      case Some(jarjestaaUrheilijanAmmatillistaKoulutusta) if !jarjestaaUrheilijanAmmatillistaKoulutusta => HakukohdeDAO.removeJarjestaaUrheilijanAmmatillistaKoulutustaByJarjestyspaikkaOid(oppilaitoksenOsa.oid)
      case _ => DBIO.successful(0)
    }
  }

  private def doPut(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OppilaitoksenOsa =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- OppilaitoksenOsaDAO.oppilaitosExists(oppilaitoksenOsa)
        (teema, o) <- checkAndMaybeClearTeemakuva(oppilaitoksenOsa)
        o          <- OppilaitoksenOsaDAO.getPutActions(o)
        o          <- maybeCopyTeemakuva(teema, o)
        o          <- teema.map(_ => OppilaitoksenOsaDAO.updateJustOppilaitoksenOsa(o)).getOrElse(DBIO.successful(o))
        _          <- removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitoksenOsa)
        _          <- auditLog.logCreate(o)
      } yield (teema, o)
    }.map { case (teema, o) =>
      maybeDeleteTempImage(teema)
      index(Some(o))
      o
    }.get

  private def doUpdate(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant, before: OppilaitoksenOsa)(implicit authenticated: Authenticated): Option[OppilaitoksenOsa] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- OppilaitoksenOsaDAO.checkNotModified(oppilaitoksenOsa.oid, notModifiedSince)
        (teema, o) <- checkAndMaybeCopyTeemakuva(oppilaitoksenOsa)
        o          <- OppilaitoksenOsaDAO.getUpdateActions(o)
        _          <- removeJarjestaaUrheilijanAmmatillistaKoulutusta(oppilaitoksenOsa)
        _          <- auditLog.logUpdate(before, o)
      } yield (teema, o)
    }.map { case (teema, o) =>
      maybeDeleteTempImage(teema)
      index(o)
      o
    }.get

  private def index(oppilaitoksenOsa: Option[OppilaitoksenOsa]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitoksenOsa.map(_.oid.toString))
}
