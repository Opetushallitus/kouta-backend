package fi.oph.kouta.scheduler

import com.github.kagkarlsson.scheduler.Scheduler
import fi.oph.kouta.repository.KoutaDatabase
import fi.vm.sade.utils.slf4j.Logging

import java.time.Instant

class SchedulerConfig extends Logging {
  private val numberOfThreads: Int   = 1
  private final val scheduler: Scheduler =
    Scheduler
      .create(KoutaDatabase.dataSource, OrganisaatioHierarkiaCacheTask.startupCacheTask)
      .startTasks(ArkistointiTask.arkistointiTask, OrganisaatioHierarkiaCacheTask.hourlyCacheTask)
      .threads(numberOfThreads)
      .registerShutdownHook()
      .build

  def startScheduler(): Unit = {
    logger.info(s"Käynnistetään scheduler.")
    scheduler.start()
    scheduler.schedule(OrganisaatioHierarkiaCacheTask.startupCacheTask.instance("startup"), Instant.now().plusSeconds(10))
  }

}
