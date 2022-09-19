package fi.oph.kouta.scheduler

import com.github.kagkarlsson.scheduler.Scheduler
import fi.oph.kouta.repository.KoutaDatabase
import fi.vm.sade.utils.slf4j.Logging

class SchedulerConfig extends Logging {
  private val numberOfThreads: Int   = 1
  private final val scheduler: Scheduler =
    Scheduler
      .create(KoutaDatabase.dataSource)
      .enableImmediateExecution()
      .startTasks(ArkistointiTask.arkistointiTask, OrganisaatioHierarkiaCacheTask.cacheTask)
      .threads(numberOfThreads)
      .registerShutdownHook()
      .build

  def startScheduler(): Unit = {
    logger.info(s"Käynnistetään scheduler.")
    scheduler.start()
  }

}
