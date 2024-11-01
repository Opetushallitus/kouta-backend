package fi.oph.kouta.logging

import org.slf4j.LoggerFactory

trait Logging {
  protected lazy val logger = LoggerFactory.getLogger(getClass())
  protected def withErrorLogging[T](f: => T)(errorMsg: String): T = {
    try {
      f
    } catch {
      case e: Exception => {
        logger.error(errorMsg, e)
        throw e
      }
    }
  }
  protected def withWarnLogging[T](f: => T)(errorMsg: String, defaultValue: T): T = {
    try {
      f
    } catch {
      case e: Exception => {
        logger.warn(errorMsg, e)
        defaultValue
      }
    }
  }
}
