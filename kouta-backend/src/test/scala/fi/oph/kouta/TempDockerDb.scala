package fi.oph.kouta

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.CommandLine.runBlocking
import fi.oph.kouta.logging.Logging

object TempDockerDb extends Logging {

  val port: Int = KoutaConfigurationFactory.configuration.databaseConfiguration.port
  val dbName        = "kouta"
  val containerName = "kouta-database"

  import TempDbUtils.tryTimes

  import java.io.File

  val dataDirectoryName         = s"kouta-temp-db/$port"
  val dataDirectoryFile         = new File(dataDirectoryName)
  val dataDirectoryPath: String = dataDirectoryFile.getAbsolutePath

  val startStopRetries             = 100
  val startStopRetryIntervalMillis = 100

  def start() {
    try {
      if (!databaseIsRunning()) {
        startDatabaseContainer()
      }
    } finally {
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
    runBlocking(
      s"docker exec $containerName pg_isready -q -t 1 -h localhost -U oph -d $dbName",
      failOnError = false
    ) == 0
  }

  def startDatabaseContainer(): Unit = {
    logger.info(s"Starting PostgreSQL container (localhost:$port):")
    runBlocking(
      s"docker run --rm -d --name $containerName --env POSTGRES_PASSWORD=postgres -p $port:5432 kouta-postgres"
    )
    if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(databaseIsRunning)) {
      throw new RuntimeException(
        s"postgres not accepting connections in port $port after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals"
      )
    }
  }
}

object TempDbUtils {
  import scala.annotation.tailrec

  @tailrec
  def tryTimes(times: Int, sleep: Int)(thunk: () => Boolean): Boolean = times match {
    case n if n < 1 => false
    case 1          => thunk()
    case n =>
      thunk() || {
        Thread.sleep(sleep); tryTimes(n - 1, sleep)(thunk)
      }
  }
}
