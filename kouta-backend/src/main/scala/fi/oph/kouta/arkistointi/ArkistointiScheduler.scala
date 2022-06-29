package fi.oph.kouta.arkistointi

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.{CronSchedule, Schedule}
import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase}
import fi.vm.sade.utils.slf4j.Logging

object ArkistointiScheduler

class ArkistointiScheduler extends Logging {
  private val cronSchedule: Schedule     = new CronSchedule("*/5 * * * * ?")
  private val testCronSchedule: Schedule = new CronSchedule("* * * * * ?")
  private val numberOfThreads: Int       = 1




  logger.info("---")
  logger.info(KoutaDatabase.dataSource.toString)
  logger.info("---")



  private val executionHandler: VoidExecutionHandler[Void] = new VoidExecutionHandler[Void] {
    override def execute(taskInstance: TaskInstance[Void], executionContext: ExecutionContext): Unit = {
      logger.info(s"Aloitetaan ajastettu hakujen ja hakukohteiden arkistointi.")

      try {
        archiveHautJaHakukohteet()
      } catch {
        case error: Exception => logger.error(s"Haun ja haukohteiden arkistointi epäonnistui: ${error}")
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

  private def archiveHautJaHakukohteet(): Unit = {
    var archivedHakuCount: Int      = 0
    var archivedHakukohdeCount: Int = 0

    HakuDAO.listArchivableHakuOids().toSet match {
      case hakuOids: Seq[HakuOid] =>
        HakukohdeDAO.listArchivableHakukohdeOidsByHakuOids(hakuOids) match {
          case hakukohdeOids: Seq[HakukohdeOid] =>
            logger.info(s"Arkistoidaan julkaistut haut: $hakuOids ja niiden julkaistut hakukohteet: $hakukohdeOids.")
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
            archivedHakukohdeCount = HakukohdeDAO.archiveHakukohdesByHakukohdeOids(hakukohdeOids)
          case _ =>
            logger.info(s"Ei löytynyt arkistoitavia hakukohteita hauille: $hakuOids, arkistoidaan haut.")
            archivedHakuCount = HakuDAO.archiveHakusByHakuOids(hakuOids)
        }
      case _ =>
        logger.info(s"Ei löytynyt yhtään arkistoitavaa hakua, ei arkistoida myöskään yhtään hakukohdetta.")
    }
    logger.info(s"Arkistointi valmis, arkistoitiin $archivedHakuCount hakua ja $archivedHakukohdeCount hakukohdetta.")
  }
}
