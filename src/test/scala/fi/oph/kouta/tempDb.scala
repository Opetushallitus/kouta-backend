package fi.oph.kouta

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.utils.tcp.PortFromSystemPropertyOrFindFree

object TempDb extends Logging {

  val port = new PortFromSystemPropertyOrFindFree("kouta-backend.db.port").chosenPort
  val dbName = "kouta"

  import java.io.File
  import java.nio.file.Files
  import org.apache.commons.io.FileUtils

  import CommandLine._

  val dataDirectoryName = s"kouta-temp-db/$port"
  val dataDirectoryFile = new File(dataDirectoryName)
  val dataDirectoryPath = dataDirectoryFile.getAbsolutePath

  val startStopRetries = 100
  val startStopRetryIntervalMillis = 100

  initDbDirectory()

  def start() {
    readPid match {
      case Some(pid) => {
        logger.info(s"PostgreSQL pid $pid is found in pid file, not touching the database.")
      }
      case None => {
        logger.info(s"PostgreSQL pid file cannot be read, starting:")
        run(s"postgres --config_file=postgresql/postgresql.conf -D $dataDirectoryPath -p $port")
        if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(isAcceptingConnections)) {
          throw new RuntimeException(s"postgres not accepting connections in port $port after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals")
        }
        runBlocking(s"dropdb -p $port --if-exists $dbName")
        runBlocking(s"createdb -E UTF8 --lc-collate C --lc-ctype C -p $port $dbName")
        runBlocking(s"psql -h localhost -p $port -d $dbName -f postgresql/init_it_postgresql.sql")

        Runtime.getRuntime.addShutdownHook(new Thread(() => stop()))
      }
    }
  }

  def stop() {
    try {
      readPid match {
        case Some(pid) => {
          logger.info(s"Killing PostgreSQL process $pid")
          runBlocking(s"kill -s SIGINT $pid")
          if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(() => readPid.isEmpty)) {
            logger.warn(s"postgres in pid $pid did not stop gracefully after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals")
          }
        }
        case None => logger.warn("No PostgreSQL pid found, not trying to stop it.")
      }
    } finally {
      if (dataDirectoryFile.exists()) {
        logger.warn(s"Nuking PostgreSQL data directory $dataDirectoryPath")
        FileUtils.forceDelete(dataDirectoryFile)
      }
    }
  }

  private def initDbDirectory() = {

    if (!dataDirectoryFile.isDirectory) {
      logger.info(s"PostgreSQL data directory $dataDirectoryPath does not exist, initing new database there.")
      Files.createDirectories(dataDirectoryFile.toPath)
      runBlocking(s"chmod 0700 $dataDirectoryPath")
      runBlocking(s"initdb -D $dataDirectoryPath -E UTF8 --lc-collate C --lc-ctype C")
    }
    logger.info(s"Using PostgreSQL in port $port with data directory $dataDirectoryPath")
  }

  private def readPid: Option[Int] = {
    val pidFile = new File(dataDirectoryFile, "postmaster.pid")
    if(pidFile.canRead) Some(FileUtils.readFileToString(pidFile, "UTF-8").split("\n")(0).toInt) else None
  }

  private def tryTimes(times: Int, sleep: Int)(thunk: () => Boolean): Boolean = times match {
    case n if n < 1 => false
    case 1 => thunk()
    case n => thunk() || { Thread.sleep(sleep); tryTimes(n - 1, sleep)(thunk) }
  }

  private def isAcceptingConnections(): Boolean = {
    runBlocking(s"pg_isready -q -t 1 -h localhost -p $port -d $dbName", failOnError = false) == 0
  }
}

object CommandLine {

  import scala.sys.process.stringToProcess

  def run(command: String) = command.run()


  def runBlocking(command: String, failOnError: Boolean = true): Int = {
    val returnValue = command.!
    if (failOnError && returnValue != 0) {
      throw new RuntimeException(s"Command '$command' exited with $returnValue")
    }
    returnValue
  }
}