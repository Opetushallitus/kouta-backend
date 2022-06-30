package fi.oph.kouta.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.{CronSchedule, Schedule}
import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase}
import fi.oph.kouta.service.HakuService
import fi.vm.sade.utils.slf4j.Logging

class ArkistointiScheduler(hakuService: HakuService) extends Logging {

  def this() = this(HakuService)

  private val cronSchedule: Schedule     = new CronSchedule("*/5 * * * * ?")
  private val testCronSchedule: Schedule = new CronSchedule("* * * * * ?")
  private val numberOfThreads: Int       = 1

  private val executionHandler: VoidExecutionHandler[Void] = new VoidExecutionHandler[Void] {
    override def execute(taskInstance: TaskInstance[Void], executionContext: ExecutionContext): Unit = {
      logger.info(s"Aloitetaan ajastettu hakujen ja hakukohteiden arkistointi.")

      try {
        archiveHautJaHakukohteet() match {
          case Some(hakuoids) =>
            hakuoids.map(hakuOid => {
              logger.info(s"Lähetetään arkistoitu haku $hakuOid SQS-jonoon indeksoitavaksi.")
              hakuService.indexByOid(hakuOid)
            })
        }
        //todo: Audit-logitus
        //todo: swagger-trigger
      } catch {
        case error: Exception => logger.error(s"Haun ja haukohteiden arkistointi epäonnistui: $error")
      }
    }
  }

  private val cronTask = Tasks
    .recurring("cron-archive-hakus-and-hakukohdes-task", testCronSchedule)
    .execute(executionHandler)

  private final val scheduler: Scheduler =
    Scheduler
      .create(KoutaDatabase.dataSource)
      .startTasks(cronTask)
      .threads(numberOfThreads)
      .build

  def startScheduler(): Unit = {
    logger.info(s"Käynnistetään haun ja hakukohteiden arkistointi-scheduler.")
    scheduler.start()
  }

  private def archiveHautJaHakukohteet(): Option[Seq[HakuOid]] = {
    var archivedHakuCount: Int      = 0
    var archivedHakukohdeCount: Int = 0
    val archivedHakuOids: Option[Seq[HakuOid]] = HakuDAO.listArchivableHakuOids() match {
      case hakuOids: Seq[HakuOid] =>
        HakukohdeDAO.listArchivableHakukohdeOidsByHakuOids(hakuOids) match {
          case hakukohdeOids: Seq[HakukohdeOid] =>
            logger.info(s"Arkistoidaan julkaistut haut: $hakuOids ja niiden julkaistut hakukohteet: $hakukohdeOids.")
            archivedHakukohdeCount = HakukohdeDAO.archiveHakukohdesByHakukohdeOids(hakukohdeOids)
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
            Some(hakuOids)
          case _ =>
            logger.info(s"Ei löytynyt arkistoitavia hakukohteita hauille: $hakuOids, arkistoidaan haut.")
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
            Some(hakuOids)
        }
      case _ =>
        logger.info(s"Ei löytynyt yhtään arkistoitavaa hakua, ei arkistoida myöskään yhtään hakukohdetta.")
//        Some(Seq(HakuOid("x"), HakuOid("y")))
        None
    }
    logger.info(s"Arkistointi valmis, arkistoitiin $archivedHakuCount hakua ja $archivedHakukohdeCount hakukohdetta.")
    archivedHakuOids
  }
}
