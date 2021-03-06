package fi.oph.kouta

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.utils.tcp.PortFromSystemPropertyOrFindFree

object TempDockerDb extends Logging {

  val port: Int = new PortFromSystemPropertyOrFindFree("kouta-backend.db.port").chosenPort
  val dbName = "kouta"
  val containerName = "kouta-database"

  import java.io.File

  import TempDbUtils.tryTimes
  import CommandLine._

  val dataDirectoryName = s"kouta-temp-db/$port"
  val dataDirectoryFile = new File(dataDirectoryName)
  val dataDirectoryPath: String = dataDirectoryFile.getAbsolutePath

  val startStopRetries = 100
  val startStopRetryIntervalMillis = 100

  def start() {
    try {
      if(!databaseIsRunning()){
        startDatabaseContainer()
      }
    }  finally {
      Runtime.getRuntime.addShutdownHook(new Thread(() => stop()))
    }
  }

  def stop() {
    try {
      logger.info("Killing PostgreSQL container")
      runBlocking(s"docker kill $containerName")
    } catch {
      case _: Throwable => logger.warn("PostgreSQL container didn't stop gracefully")
    }
  }

  private val databaseIsRunning: () => Boolean = () => {
    runBlocking(s"docker exec $containerName pg_isready -q -t 1 -h localhost -U oph -d $dbName", failOnError = false) == 0
  }

  def startDatabaseContainer(): Unit = {
    logger.info("Starting PostgreSQL container:")
    runBlocking(s"docker run --rm -d --name $containerName --env POSTGRES_PASSWORD=postgres -p $port:5432 kouta-postgres")
    if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(databaseIsRunning)) {
      throw new RuntimeException(s"postgres not accepting connections in port $port after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals")
    }
  }
}