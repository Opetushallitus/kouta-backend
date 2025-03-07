package fi.oph.kouta.scheduler

import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.{CronSchedule, Schedule}
import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import fi.oph.kouta.auditlog.{AuditLog, AuditResource}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, RootOrganisaatioOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO}
import fi.oph.kouta.service.HakuService
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.auditlog.User
import fi.oph.kouta.logging.Logging
import org.ietf.jgss.Oid

import java.net.InetAddress
import java.time.ZoneId

object ArkistointiTask extends ArkistointiTask(HakuService, AuditLog)

class ArkistointiTask(hakuService: HakuService, auditLog: AuditLog) extends Logging {

  def this() = this(HakuService, AuditLog)

  private val cronSchedule: Schedule = new CronSchedule("0 5 0 * * ?", ZoneId.of("Europe/Helsinki"))

  private val user: User =
    new User(new Oid(RootOrganisaatioOid.toString), InetAddress.getLocalHost, "scheduler", "scheduler")

  private val arkistointiExecutionHandler: VoidExecutionHandler[Void] = new VoidExecutionHandler[Void] {
    override def execute(taskInstance: TaskInstance[Void], executionContext: ExecutionContext): Unit = {
      logger.info(s"Aloitetaan ajastettu hakujen ja hakukohteiden arkistointi.")

      try {
        archiveHautJaHakukohteet(user) match {
          case Some(hakuoids) =>
            hakuoids.map(hakuOid => {
              logger.info(s"Lähetetään arkistoitu haku $hakuOid SQS-jonoon indeksoitavaksi.")
              hakuService.indexByOid(hakuOid)
            })
          case _ =>
        }
      } catch {
        case error: Exception => logger.error(s"Haun ja haukohteiden arkistointi epäonnistui: $error")
      }
    }
  }

  val arkistointiTask = Tasks
    .recurring("cron-archive-hakus-and-hakukohdes-task", cronSchedule)
    .execute(arkistointiExecutionHandler)

  def runScheduler()(implicit authenticated: Authenticated): Unit = {
    logger.info(s"Käynnistetään käsin haun ja hakukohteiden arkistointi-scheduler.")
    archiveHautJaHakukohteet(auditLog.getUser(authenticated))
  }

  private def archiveHautJaHakukohteet(user: User): Option[Seq[HakuOid]] = {
    var archivedHakuCount: Int      = 0
    var archivedHakukohdeCount: Int = 0
    val archivedHakuOids: Option[Seq[HakuOid]] = HakuDAO.listArchivableHakuOids() match {
      case hakuOids: Seq[HakuOid] =>
        if (hakuOids.nonEmpty) {
          HakukohdeDAO.listArchivableHakukohdeOidsByHakuOids(hakuOids) match {
            case hakukohdeOids: Seq[HakukohdeOid] =>
              logger.info(s"Arkistoidaan julkaistut haut: $hakuOids ja niiden julkaistut hakukohteet: $hakukohdeOids.")

              archivedHakukohdeCount = HakukohdeDAO.archiveHakukohdesByHakukohdeOids(hakukohdeOids)
              hakukohdeOids.map(hakukohdeOid =>
                auditLog.logArchive(hakukohdeOid.toString, AuditResource.Hakukohde, user)
              )

              archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
              hakuOids.map(hakuOid => auditLog.logArchive(hakuOid.toString, AuditResource.Haku, user))

              return Some(hakuOids)
            case _ =>
              logger.info(s"Ei löytynyt arkistoitavia hakukohteita hauille: $hakuOids, arkistoidaan haut.")
              archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
              return Some(hakuOids)
          }
        } else None
    }
    logger.info(s"Arkistointi valmis, arkistoitiin $archivedHakuCount hakua ja $archivedHakukohdeCount hakukohdetta.")
    archivedHakuOids
  }
}
