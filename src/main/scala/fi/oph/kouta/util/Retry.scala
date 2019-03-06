package fi.oph.kouta.util

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

import fi.vm.sade.utils.slf4j.Logging
import odelay.Delay

trait Retry extends Logging {
  def retry[T](actionName: => String, delay: FiniteDuration, retries: Int)(op: => T): Future[T] = {
    Future { op } recoverWith {
      case t if retries > 0 =>
        logger.warn(s"Retrying '$actionName'. $t")
        Delay(delay) { retry(actionName, delay, retries - 1)(op) }.future.flatten
    } andThen {
      case Failure(t) => logger.error(s"'$actionName' failed after retrying.", t)
    }
  }
}
