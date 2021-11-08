package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoutaIndexClient, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.util.MiscUtils.isToisenAsteenYhteishaku
import fi.oph.kouta.domain.oid.{HakukohdeOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase, ToteutusDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{HakukohdeServiceUtil, NameHelper}
import fi.oph.kouta.validation.Validations
import org.checkerframework.checker.units.qual.m
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object HakukohdeService
    extends HakukohdeService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, LokalisointiClient)

class HakukohdeService(
    sqsInTransactionService: SqsInTransactionService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    val lokalisointiClient: LokalisointiClient
) extends ValidatingService[Hakukohde]
    with RoleEntityAuthorizationService[Hakukohde] {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  def get(oid: HakukohdeOid)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] = {
    val hakukohde = HakukohdeDAO.get(oid)

    val enrichedHakukohde = hakukohde match {
      case Some((h, i)) =>
        val esitysnimi = generateHakukohdeEsitysnimi(h)
        Some(h.copy(_enrichedData = Some(EnrichedData(esitysnimi = esitysnimi))), i)
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

  def generateHakukohdeEsitysnimi(hakukohde: Hakukohde): Kielistetty = {
    val toteutusOid             = hakukohde.toteutusOid
    val toteutus                = ToteutusDAO.get(toteutusOid)
    toteutus match {
      case Some((toteutus, _)) =>
        toteutus.metadata match {
          case Some(toteutusMetadata) =>
            toteutusMetadata match {
              case tuva: TuvaToteutusMetadata =>
                val kaannokset = lokalisointiClient.getKaannoksetWithKey("yleiset.vaativanaErityisenaTukena")
                NameHelper.generateHakukohdeDisplayNameForTuva(hakukohde, toteutus, kaannokset)
              case _ =>
                hakukohde.nimi
            }
          case None => hakukohde.nimi
        }
      case None => hakukohde.nimi
    }
  }


  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): HakukohdeOid =
    authorizePut(hakukohde) { h =>
      withValidation(h, None) { h =>
        validateDependenciesIntegrity(h)
        doPut(h)
      }
    }.oid.get

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val rules = AuthorizationRules(
      roleEntity.updateRoles,
      additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(hakukohde.oid.get)
    )
    authorizeUpdate(HakukohdeDAO.get(hakukohde.oid.get), hakukohde, rules) { (oldHakukohde, h) =>
      withValidation(h, Some(oldHakukohde)) { h =>
        validateDependenciesIntegrity(h)
        doUpdate(h, notModifiedSince, oldHakukohde)
      }
    }.nonEmpty
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit
      authenticated: Authenticated
  ): HakukohdeSearchResult =
    list(organisaatioOid).map(_.oid) match {
      case Nil           => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  def getOidsByJarjestyspaikka(jarjestyspaikkaOid: OrganisaatioOid)(implicit authenticated: Authenticated) =
    withRootAccess(indexerRoles) {
      HakukohdeDAO.getOidsByJarjestyspaikka(jarjestyspaikkaOid);
    }

  private def validateDependenciesIntegrity(hakukohde: Hakukohde): Unit = {
    import Validations._
    val deps = HakukohdeDAO.getDependencyInformation(hakukohde)
    val hakuOid     = hakukohde.hakuOid.s
    val toteutusOid = hakukohde.toteutusOid.s

    val haku = HakuDAO.get(hakukohde.hakuOid).map(_._1)
    val koulutustyyppi = deps.get(toteutusOid).flatMap(_._2)

    throwValidationErrors(and(
      validateDependency(hakukohde.tila, deps.get(toteutusOid).map(_._1), toteutusOid, "Toteutusta", "toteutusOid"),
      assertDependencyExists(deps.contains(hakuOid), hakuOid, "Hakua", "hakuOid"),
      validateIfDefined[UUID](hakukohde.valintaperusteId, valintaperusteId => and(
        validateDependency(hakukohde.tila, deps.get(valintaperusteId.toString).map(_._1), valintaperusteId, "Valintaperustetta", "valintaperusteId"),
        validateIfDefined[Koulutustyyppi](deps.get(valintaperusteId.toString).flatMap(_._2), valintaperusteTyyppi =>
          validateIfDefined[Koulutustyyppi](deps.get(toteutusOid).flatMap(_._2), toteutusTyyppi =>
            assertTrue(toteutusTyyppi == valintaperusteTyyppi, "valintaperusteId", tyyppiMismatch("Toteutuksen", toteutusOid, "valintaperusteen", valintaperusteId))
          )
        )
      )),
      validateIfDefined[ToteutusMetadata](deps.get(toteutusOid).flatMap(_._3), metadata => and(
        validateIfTrue(metadata.tyyppi == AmmTutkinnonOsa,
          assertTrue(metadata.asInstanceOf[AmmatillinenTutkinnonOsaToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == AmmOsaamisala,
          assertTrue(metadata.asInstanceOf[AmmatillinenOsaamisalaToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == VapaaSivistystyoMuu,
          assertTrue(metadata.asInstanceOf[VapaaSivistystyoMuuToteutusMetadata].hakulomaketyyppi.exists(_ == Ataru), "toteutusOid", cannotLinkToHakukohde(toteutusOid))),
        validateIfTrue(metadata.tyyppi == Lk,
          assertNotOptional(hakukohde.metadata.get.hakukohteenLinja, "metadata.hakukohteenLinja"))
      )),
      validateIfJulkaistu(
        hakukohde.tila,
        validateIfTrue(isToisenAsteenYhteishaku(
          koulutustyyppi, haku.flatMap(_.hakutapaKoodiUri)
        ), assertNotOptional(hakukohde.valintaperusteId, "valintaperusteId")))
    ))
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
}
