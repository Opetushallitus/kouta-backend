package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.domain.searchResults.HakukohdeSearchResult
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus}
import fi.oph.kouta.util.NameHelper.{mergeNames, notFullyPopulated}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object HakukohdeService
    extends HakukohdeService(
      SqsInTransactionService,
      AuditLog,
      OrganisaatioServiceImpl,
      LokalisointiClient,
      OppijanumerorekisteriClient,
      KayttooikeusClient,
      CachedKoodistoClient,
      ToteutusService,
      HakukohdeServiceValidation,
      KoutaIndeksoijaClient
    )

case class CopyOids(
    hakukohdeOid: Option[HakukohdeOid],
    toteutusOid: Option[ToteutusOid]
)

case class HakukohdeCopyResultObject(
    oid: HakukohdeOid,
    status: String,
    created: CopyOids
)

case class HakukohdeTilaChangeResultObject(
    oid: HakukohdeOid,
    status: String,
    errorPaths: List[String] = List(),
    errorMessages: List[String] = List(),
    errorTypes: List[String] = List()
)

class HakukohdeService(
    sqsInTransactionService: SqsInTransactionService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    val lokalisointiClient: LokalisointiClient,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    koodistoClient: CachedKoodistoClient,
    toteutusService: ToteutusService,
    hakukohdeServiceValidation: HakukohdeServiceValidation,
    koutaIndeksoijaClient: KoutaIndeksoijaClient
) extends RoleEntityAuthorizationService[Hakukohde] {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  private def enrichHakukohdeMetadata(hakukohde: Hakukohde) : Option[HakukohdeMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(hakukohde.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    hakukohde.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None           => None
    }
  }

  def get(oid: HakukohdeOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Option[(Hakukohde, Instant)] = {
    val hakukohde = HakukohdeDAO.get(oid, tilaFilter)

    val enrichedHakukohde = hakukohde match {
      case Some((h, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(h.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        val hakukohdeEnrichedDataWithMuokkaajanNimi = HakukohdeEnrichedData(muokkaajanNimi = Some(muokkaajanNimi))
        val toteutus                                = ToteutusDAO.get(h.toteutusOid, TilaFilter.onlyOlemassaolevat())
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
      AuthorizationRules(roleEntity.readRoles)
    )
  }

  def generateHakukohdeEsitysnimi(hakukohde: Hakukohde, toteutusMetadata: Option[ToteutusMetadata]): Kielistetty =
    toteutusMetadata match {
      case Some(metadata) if metadata.tyyppi == Tuva =>
        val kaannokset = lokalisointiClient.getKaannoksetWithKeyFromCache("yleiset.vaativanaErityisenaTukena")
        NameHelper.generateHakukohdeDisplayNameForTuva(hakukohde.nimi, toteutusMetadata.get, kaannokset)
      case _ => hakukohde.nimi
    }

  def populateNimiFromToteutusAsNeeded(hakukohde: Hakukohde): Hakukohde = {
    if (notFullyPopulated(hakukohde.nimi, hakukohde.kielivalinta)) {
      ToteutusDAO.get(hakukohde.toteutusOid, TilaFilter.onlyOlemassaolevat()) match {
        case Some((toteutus, _)) if toteutus.metadata.isDefined =>
          toteutus.metadata.get match {
            case _: TuvaToteutusMetadata =>
              hakukohde.copy(nimi = mergeNames(toteutus.nimi, hakukohde.nimi, hakukohde.kielivalinta))
            case _: LukioToteutusMetadata =>
              if (isDIAlukiokoulutus(toteutus.koulutuksetKoodiUri) || isEBlukiokoulutus(toteutus.koulutuksetKoodiUri)) {
                hakukohde.copy(nimi = mergeNames(toteutus.nimi, hakukohde.nimi, hakukohde.kielivalinta))
              } else {
                val hkLinja = hakukohde.metadata.flatMap(_.hakukohteenLinja).flatMap(_.linja)
                if (hkLinja.isDefined) {
                    koodistoClient.getKoodistoElementVersionOrLatestFromCache(hkLinja.get) match {
                    case Left(exception) => throw exception
                    case Right(koodistoElement) =>
                      hakukohde.copy(nimi = mergeNames(koodistoElement.asKielistetty, hakukohde.nimi, hakukohde.kielivalinta))
                    case _ => hakukohde
                  }
                } else
                  hakukohde.copy(nimi =
                    mergeNames(
                      lokalisointiClient.getKaannoksetWithKeyFromCache("hakukohdelomake.lukionYleislinja"),
                      hakukohde.nimi,
                      hakukohde.kielivalinta
                    )
                  )
              }

            case _ => hakukohde

          }
        case _ => hakukohde
      }
    } else {
      hakukohde
    }
  }

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): CreateResult = {
    val rules = hakukohde.jarjestyspaikkaOid match {
      case Some(oid) =>
        AuthorizationRules(
          roleEntity.createRoles,
          overridingAuthorizationRule = Some(AuthorizedToAllOfGivenOrganizationsRule),
          additionalAuthorizedOrganisaatioOids = Seq(oid)
        )
      case _ => AuthorizationRules(roleEntity.createRoles)
    }
    authorizePut(hakukohde, rules) { hk =>
      val hkWithNamePopulatedMaybe                    = populateNimiFromToteutusAsNeeded(hk)
      val enrichedMetadata: Option[HakukohdeMetadata] = enrichHakukohdeMetadata(hkWithNamePopulatedMaybe)
      val enrichedHakukohde                           = hkWithNamePopulatedMaybe.copy(metadata = enrichedMetadata)
      hakukohdeServiceValidation.withValidation(enrichedHakukohde, None, authenticated) { hk =>
        doPut(hk)
      }
    }
  }

  def copy(hakukohdeOids: List[HakukohdeOid], hakuOid: HakuOid)(implicit
      authenticated: Authenticated
  ): List[HakukohdeCopyResultObject] = {
    val hakukohdeAndRelatedEntities = HakukohdeDAO.getHakukohdeAndRelatedEntities(hakukohdeOids).groupBy(_._1.oid.get)

    val copyResultObjects = hakukohdeAndRelatedEntities.toList.map(hakukohde => {
      val entities = hakukohde._2
      val hk       = entities.head._1
      try {
        val toteutus      = entities.head._2
        val liitteet      = entities.map(_._3).distinct.filter(_.id.nonEmpty)
        val valintakokeet = hakukohde._2.map(_._4).distinct.filter(_.id.nonEmpty)
        val hakuajat = hakukohde._2
          .map(_._5)
          .distinct
          .filter(_.oid.nonEmpty)
          .map(hakuaika => Ajanjakso(alkaa = hakuaika.alkaa.get, paattyy = hakuaika.paattyy))

        val toteutusCopy    = toteutus.copy(oid = None, tila = Tallennettu)
        val toteutusCopyOid = toteutusService.put(toteutusCopy).oid.asInstanceOf[ToteutusOid]

        val hakukohdeCopyAsLuonnos = hk.copy(
          oid = None,
          tila = Tallennettu,
          toteutusOid = toteutusCopyOid,
          hakuOid = hakuOid,
          liitteet = liitteet.map(_.copy(id = None)),
          valintakokeet = valintakokeet.map(_.copy(id = None)),
          hakuajat = hakuajat
        )

        val hakukohdeCopyOid = put(hakukohdeCopyAsLuonnos).oid.asInstanceOf[HakukohdeOid]

        HakukohdeCopyResultObject(
          oid = hk.oid.get,
          status = "success",
          created = CopyOids(Some(hakukohdeCopyOid), Some(toteutusCopyOid))
        )
      } catch {
        case error: Throwable => {
          logger.error(s"Copying hakukohde failed: $error")
          HakukohdeCopyResultObject(
            oid = hk.oid.get,
            status = "error",
            created = CopyOids(None, None)
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
        created = CopyOids(hakukohdeOid = None, toteutusOid = None)
      )

    copyResultObjects ++ notFound
  }

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): UpdateResult = {
    val oldHakukohdeWithTime = HakukohdeDAO.get(hakukohde.oid.get, TilaFilter.onlyOlemassaolevat())

    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(hakukohde, oldHakukohdeWithTime)

    authorizeUpdate(oldHakukohdeWithTime, hakukohde, rules) { (oldHakukohde, h) =>
      val hkWithNamePopulatedMaybe                    = populateNimiFromToteutusAsNeeded(h)
      val enrichedMetadata: Option[HakukohdeMetadata] = enrichHakukohdeMetadata(hkWithNamePopulatedMaybe)
      val enrichedHakukohde                           = hkWithNamePopulatedMaybe.copy(metadata = enrichedMetadata)
      hakukohdeServiceValidation.withValidation(enrichedHakukohde, Some(oldHakukohde), authenticated) { h =>
        doUpdate(h, notModifiedSince, oldHakukohde)
      }
    }
  }

  private def getAuthorizationRulesForUpdate(
      newHakukohde: Hakukohde,
      oldHakukohdeWithTime: Option[(Hakukohde, Instant)]
  ) = {
    oldHakukohdeWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää hakukohdetta ei löytynyt")
      case Some((oldHakukohde, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldHakukohde.tila, newHakukohde.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        } else {
          newHakukohde.jarjestyspaikkaOid match {
            case Some(oid) =>
              AuthorizationRules(
                roleEntity.updateRoles,
                overridingAuthorizationRule = Some(AuthorizedToAllOfGivenOrganizationsRule),
                additionalAuthorizedOrganisaatioOids = Seq(oid)
              )
            case _ => AuthorizationRules(roleEntity.updateRoles)
          }
        }
    }
  }

  def listOlemassaolevat(organisaatioOid: OrganisaatioOid)(implicit
      authenticated: Authenticated
  ): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit
      authenticated: Authenticated
  ): HakukohdeSearchResult = {
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(orgOids =>
      KoutaSearchClient.searchHakukohteetDirect(orgOids, params)
    )
  }

  def getOidsByJarjestyspaikat(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[String] =
    withRootAccess(indexerRoles) {
      HakukohdeDAO.getOidsByJarjestyspaikka(jarjestyspaikkaOids, tilaFilter)
    }

  def changeTila(hakukohdeOids: Seq[HakukohdeOid], tila: String, unModifiedSince: Instant)(implicit
      authenticated: Authenticated
  ): List[HakukohdeTilaChangeResultObject] = {
    val hakukohteet: Seq[Hakukohde] = hakukohdeOids.map(oid => {
      HakukohdeDAO.get(oid, TilaFilter.all())
    }).collect {
      case Some(result) => result._1
    }

    val updatedHakukohdeOids = scala.collection.mutable.Set[HakukohdeOid]()

    val tilaChangeResults = hakukohteet.toList.map(hakukohde => {
      try {
        val hakukohdeWithNewTila = hakukohde.copy(tila = Julkaisutila.withName(tila), muokkaaja = UserOid(authenticated.id))
        if (update(hakukohdeWithNewTila, unModifiedSince).updated) {
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "success"
          )
        } else {
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "error",
            errorPaths = List("hakukohde"),
            errorMessages = List("Hakukohteen tilaa ei voitu päivittää"),
            errorTypes = List("possible transaction error")
          )
        }
      } catch {
        case error: KoutaValidationException =>
          logger.error(s"Changing of tila of hakukohde: ${hakukohde.oid.get} failed: $error")
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "error",
            errorPaths = error.getPaths,
            errorMessages =  error.getMsgs,
            errorTypes = error.getErrorTypes
          )
        case error: OrganizationAuthorizationFailedException =>
          logger.error(s"Changing of tila of hakukohde: ${hakukohde.oid.get} failed: $error")
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "error",
            errorPaths = List("hakukohde"),
            errorMessages = List(error.getMessage),
            errorTypes = List("organizationauthorization")
          )
        case error: RoleAuthorizationFailedException =>
          logger.error(s"Changing of tila of hakukohde: ${hakukohde.oid.get} failed: $error")
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "error",
            errorPaths = List("hakukohde"),
            errorMessages = List(error.getMessage),
            errorTypes = List("roleAuthorization")
          )
        case error: Exception =>
          logger.error(s"Changing of tila of hakukohde: ${hakukohde.oid.get} failed: $error")
          updatedHakukohdeOids += hakukohde.oid.get
          HakukohdeTilaChangeResultObject(
            oid = hakukohde.oid.get,
            status = "error",
            errorPaths = List("hakukohde"),
            errorMessages = List(error.getMessage),
            errorTypes = List("internalServerError")
          )
      }
    })

    val notFound =
      for {
        hkOid <- hakukohdeOids.filterNot(hakukohdeOid => updatedHakukohdeOids.contains(hakukohdeOid))
      } yield HakukohdeTilaChangeResultObject(
        oid = hkOid,
        status = "error",
        errorPaths = List("hakukohde"),
        errorMessages = List("Hakukohdetta ei löytynyt"),
        errorTypes = List("not found")
      )

    tilaChangeResults ++ notFound
  }

  private def doPut(hakukohde: Hakukohde)(implicit authenticated: Authenticated): CreateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakukohdeDAO.getPutActions(hakukohde)
        _ <- auditLog.logCreate(h)
      } yield h
    }.map { h =>
      val warnings = quickIndex(h.oid) ++ index(Some(h))
      CreateResult(h.oid.get, warnings)
    }.get

  private def doUpdate(hakukohde: Hakukohde, notModifiedSince: Instant, before: Hakukohde)(implicit
      authenticated: Authenticated
  ): UpdateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- HakukohdeDAO.checkNotModified(hakukohde.oid.get, notModifiedSince)
        h <- HakukohdeDAO.getUpdateActions(hakukohde)
        _ <- auditLog.logUpdate(before, h)
      } yield h
    }.map { h =>
      val warnings = quickIndex(h.flatMap(_.oid)) ++ index(h)
      UpdateResult(updated = h.isDefined, warnings)
    }.get

  private def index(hakukohde: Option[Hakukohde]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHakukohde, hakukohde.map(_.oid.get.toString))

  private def quickIndex(hakukohdeOid: Option[HakukohdeOid]): List[String] = {
    hakukohdeOid match {
      case Some(oid) => koutaIndeksoijaClient.quickIndexEntity("hakukohde", oid.toString)
      case None => List.empty
    }
  }
}
