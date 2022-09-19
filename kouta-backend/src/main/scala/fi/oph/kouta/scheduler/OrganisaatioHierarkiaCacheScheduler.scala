package fi.oph.kouta.scheduler

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.repository.KoutaDatabase
import fi.vm.sade.utils.slf4j.Logging

object OrganisaatioHierarkiaCacheScheduler extends OrganisaatioHierarkiaCacheScheduler()

class OrganisaatioHierarkiaCacheScheduler() extends Logging {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private val numberOfThreads: Int   = 1

  protected val cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient =
    new CachedOrganisaatioHierarkiaClient with CallerId {
      override val organisaatioUrl: String =
        urlProperties.url("organisaatio-service.organisaatio.oid.jalkelaiset", RootOrganisaatioOid.s)
    }

  private val executionHandler: VoidExecutionHandler[Void] = new VoidExecutionHandler[Void] {
    override def execute(taskInstance: TaskInstance[Void], executionContext: ExecutionContext): Unit = {
      logger.info(s"Aloitetaan ajastettu organisaatiohierarkia-cachen päivitys.")

      try {
        cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached()
      } catch {
        case error: Exception => logger.error(s"Ajastettu organisaatiohierarkia-cachen päivitys epäonnistui: $error")
      }
    }
  }

  private val hourlyTask = Tasks
    .recurring("cron-update-organisaatiohierarkia-cache-task", FixedDelay.ofHours(1))
    .execute(executionHandler)

  private final val scheduler: Scheduler =
    Scheduler
      .create(KoutaDatabase.dataSource)
      .enableImmediateExecution()
      .startTasks(hourlyTask)
      .threads(numberOfThreads)
      .registerShutdownHook()
      .build

  def startScheduler(): Unit = {
    logger.info(s"Käynnistetään organisaatiohierarkia-cachen päivitys-scheduler.")
    scheduler.start()
  }
}
