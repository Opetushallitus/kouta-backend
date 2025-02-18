package fi.oph.kouta.scheduler

import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import com.github.kagkarlsson.scheduler.task.{ExecutionContext, TaskInstance, VoidExecutionHandler}
import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.logging.Logging

object OrganisaatioHierarkiaCacheTask extends OrganisaatioHierarkiaCacheTask()

class OrganisaatioHierarkiaCacheTask() extends Logging {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  protected val cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient =
    new CachedOrganisaatioHierarkiaClient with CallerId {
      override val organisaatioUrl: String =
        urlProperties.url("organisaatio-service.organisaatio.oid.jalkelaiset", RootOrganisaatioOid.s)
    }

  private val organisaatioHierarkiaCacheExecutionHandler: VoidExecutionHandler[Void] = new VoidExecutionHandler[Void] {
    override def execute(taskInstance: TaskInstance[Void], executionContext: ExecutionContext): Unit = {
      logger.info(s"Aloitetaan ajastettu organisaatiohierarkia-cachen päivitys.")

      try {
        cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached()
      } catch {
        case error: Exception => logger.error(s"Ajastettu organisaatiohierarkia-cachen päivitys epäonnistui: $error")
      }
    }
  }

  val startupCacheTask = Tasks
    .oneTime("cron-startup-update-organisaatiohierarkia-cache-task")
    .execute(organisaatioHierarkiaCacheExecutionHandler)

  val hourlyCacheTask = Tasks
    .recurring("cron-update-organisaatiohierarkia-cache-task", FixedDelay.ofHours(1))
    .execute(organisaatioHierarkiaCacheExecutionHandler)
}
