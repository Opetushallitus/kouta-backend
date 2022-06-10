package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoutaIndexClient, LokalisointiClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.validateStateChange
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object HakukohdeService
    extends HakukohdeService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, LokalisointiClient, OppijanumerorekisteriClient, KayttooikeusClient, ToteutusService)

case class CopyOids(
  hakukohdeOid: Option[HakukohdeOid],
  toteutusOid: Option[ToteutusOid]
)

case class HakukohdeCopyResultObject(
  oid: HakukohdeOid,
  status: String,
  created: CopyOids
)

class HakukohdeService(
    sqsInTransactionService: SqsInTransactionService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    val lokalisointiClient: LokalisointiClient,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    toteutusService: ToteutusService
) extends ValidatingService[Hakukohde]
    with RoleEntityAuthorizationService[Hakukohde] {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  private def enrichHakukohdeMetadata(hakukohde: Hakukohde) : Option[HakukohdeMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(hakukohde.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    hakukohde.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None => None
    }
  }

  def get(oid: HakukohdeOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] = {
    val hakukohde = HakukohdeDAO.get(oid, tilaFilter)

    val enrichedHakukohde = hakukohde match {
      case Some((h, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(h.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        val hakukohdeEnrichedDataWithMuokkaajanNimi = HakukohdeEnrichedData(muokkaajanNimi = Some(muokkaajanNimi))
        val toteutus = ToteutusDAO.get(h.toteutusOid, TilaFilter.onlyOlemassaolevat())
        toteutus match {
          case Some((t, _)) =>
            val esitysnimi = generateHakukohdeEsitysnimi(h, t.metadata)
            Some(h.copy(_enrichedData = Some(hakukohdeEnrichedDataWithMuokkaajanNimi.copy(esitysnimi = esitysnimi))), i)
          case None => Some(h.copy(_enrichedData = Some(hakukohdeEnrichedDataWithMuokkaajanNimi)), i)
        }

      case None => None
     }

    authorizeGet(
      enrichedHakukohde,
      AuthorizationRules(
        roleEntity.readRoles,
        additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(oid)
      )
    )
  }

  def generateHakukohdeEsitysnimi(hakukohde: Hakukohde, toteutusMetadata: Option[ToteutusMetadata]): Kielistetty = {
    toteutusMetadata match {
      case Some(metadata) =>
        metadata match {
          case tuva: TuvaToteutusMetadata =>
            val kaannokset = lokalisointiClient.getKaannoksetWithKey("yleiset.vaativanaErityisenaTukena")
            NameHelper.generateHakukohdeDisplayNameForTuva(hakukohde.nimi, metadata, kaannokset)
          case _ => hakukohde.nimi
        }
      case None => hakukohde.nimi
    }
  }

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): HakukohdeOid = {
    authorizePut(hakukohde) { hk =>
      val enrichedMetadata: Option[HakukohdeMetadata] = enrichHakukohdeMetadata(hk)
      val enrichedHakukohde = hk.copy(metadata = enrichedMetadata)
      withValidation(enrichedHakukohde, None) { hk =>
        validateDependenciesIntegrity(hk, authenticated, "put")
        doPut(hk)
      }
    }.oid.get
  }


  def copy(hakukohdeOids: List[HakukohdeOid], hakuOid: HakuOid)(implicit authenticated: Authenticated): List[HakukohdeCopyResultObject] = {
    val hakukohdeAndRelatedEntities = HakukohdeDAO.getHakukohdeAndRelatedEntities(hakukohdeOids).groupBy(_._1.oid.get)

    val copyResultObjects = hakukohdeAndRelatedEntities.toList.map(hakukohde => {
      val entities = hakukohde._2
      val hk = entities.head._1
      try {
        val toteutus = entities.head._2
        val liitteet = entities.map(_._3).distinct.filter(_.id.nonEmpty)
        val valintakokeet = hakukohde._2.map(_._4).distinct.filter(_.id.nonEmpty)
        val hakuajat = hakukohde._2.map(_._5).distinct.filter(_.oid.nonEmpty).map(
          hakuaika => Ajanjakso(alkaa = hakuaika.alkaa.get, paattyy = hakuaika.paattyy))

        val toteutusCopy = toteutus.copy(tila = Tallennettu)
        val toteutusCopyOid = toteutusService.put(toteutusCopy)

        val hakukohdeCopyAsLuonnos = hk.copy(
          tila = Tallennettu,
          toteutusOid = toteutusCopyOid,
          hakuOid = hakuOid,
          liitteet = liitteet,
          valintakokeet = valintakokeet,
          hakuajat = hakuajat)

        val hakukohdeCopyOid = put(hakukohdeCopyAsLuonnos)

        HakukohdeCopyResultObject(
          oid = hk.oid.get, status = "success", created = CopyOids(Some(hakukohdeCopyOid), Some(toteutusCopyOid))
        )
      } catch {
        case error: Throwable => {
          logger.error(s"Copying hakukohde failed: $error")
          HakukohdeCopyResultObject(
            oid = hk.oid.get, status = "error", created = CopyOids(None, None)
          )
        }
      }
    })

    val notFound =
      for {
        hkOid <- hakukohdeOids.filterNot(hakukohdeOid => hakukohdeAndRelatedEntities.keySet.contains(hakukohdeOid))
      } yield HakukohdeCopyResultObject(
        oid = hkOid,
        status = "error",
        created = CopyOids(hakukohdeOid = None, toteutusOid = None))

    copyResultObjects ++ notFound
  }

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val oldHakukohdeWithTime = HakukohdeDAO.get(hakukohde.oid.get, TilaFilter.onlyOlemassaolevat())

    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(hakukohde, oldHakukohdeWithTime)

    authorizeUpdate(oldHakukohdeWithTime, hakukohde, rules) { (oldHakukohde, h) =>
      val enrichedMetadata: Option[HakukohdeMetadata] = enrichHakukohdeMetadata(h)
      val enrichedHakukohde = h.copy(metadata = enrichedMetadata)
      withValidation(enrichedHakukohde, Some(oldHakukohde)) { h =>
        throwValidationErrors(validateStateChange("hakukohteelle", oldHakukohde.tila, h.tila))
        validateDependenciesIntegrity(h, authenticated, "update")
        doUpdate(h, notModifiedSince, oldHakukohde)
      }
    }.nonEmpty
  }

  private def getAuthorizationRulesForUpdate(hakukohde: Hakukohde, oldHakukohdeWithTime: Option[(Hakukohde, Instant)]) = {
    val rules = AuthorizationRules(
      roleEntity.updateRoles,
      additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(hakukohde.oid.get)
    )
    rules
  }

  def listOlemassaolevat(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit
      authenticated: Authenticated
  ): HakukohdeSearchResult =
    listOlemassaolevat(organisaatioOid).map(_.oid) match {
      case Nil           => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  def getOidsByJarjestyspaikat(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[String] =
    withRootAccess(indexerRoles) {
      HakukohdeDAO.getOidsByJarjestyspaikka(jarjestyspaikkaOids, tilaFilter)
    }

  private def validateDependenciesIntegrity(hakukohde: Hakukohde, authenticated: Authenticated, method: String): Unit = {
    val isOphPaakayttaja = authenticated.session.roles.contains(Role.Paakayttaja)
    val deps = HakukohdeDAO.getDependencyInformation(hakukohde)
    val haku = HakuDAO.get(hakukohde.hakuOid, TilaFilter.onlyOlemassaolevat()).map(_._1)

    throwValidationErrors(HakukohdeServiceValidation.validate(hakukohde, haku, isOphPaakayttaja, deps, method))
  }

  private def doPut(hakukohde: Hakukohde)(implicit authenticated: Authenticated): Hakukohde =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakukohdeDAO.getPutActions(hakukohde)
        _ <- index(Some(h))
        _ <- auditLog.logCreate(h)
      } yield h
    }.get

  private def doUpdate(hakukohde: Hakukohde, notModifiedSince: Instant, before: Hakukohde)(implicit
      authenticated: Authenticated
  ): Option[Hakukohde] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- HakukohdeDAO.checkNotModified(hakukohde.oid.get, notModifiedSince)
        h <- HakukohdeDAO.getUpdateActions(hakukohde)
        _ <- index(h)
        _ <- auditLog.logUpdate(before, h)
      } yield h
    }.get

  private def index(hakukohde: Option[Hakukohde]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHakukohde, hakukohde.map(_.oid.get.toString))

  override def validateParameterFormatAndExistence(hakukohde: Hakukohde): IsValid = hakukohde.validate()
  override def validateParameterFormatAndExistenceOnJulkaisu(hakukohde: Hakukohde): IsValid = hakukohde.validateOnJulkaisu()

  override def validateDependenciesToExternalServices(hakukohde: Hakukohde): IsValid = NoErrors

  override def validateInternalDependenciesWhenDeletingEntity(hakukohde: Hakukohde): IsValid = NoErrors
}
