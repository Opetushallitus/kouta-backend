package fi.oph.kouta

import scala.annotation.tailrec

import org.scalactic.source
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Nanoseconds, Span}

trait WaitIfFails {
  this: PatienceConfiguration =>

  def waitIfFails[T](fun: => T)(implicit config: PatienceConfig, pos: source.Position): T = {
    val startNanos = System.nanoTime
    val initialInterval = Span(config.interval.totalNanos * 0.1, Nanoseconds) // config.interval scaledBy 0.1

    def checkIfStillOk(): T = fun

    @tailrec
    def tryTryAgain(attempt: Int): T = {
      val duration = System.nanoTime - startNanos
      if (duration < config.timeout.totalNanos) {
        checkIfStillOk()

        // For first interval, we wake up every 1/10 of the interval.  This is mainly for optimization purpose.
        if (duration < config.interval.totalNanos) Thread.sleep(initialInterval.millisPart, initialInterval.nanosPart)
        else Thread.sleep(config.interval.millisPart, config.interval.nanosPart)

        tryTryAgain(attempt + 1)
      } else {
        checkIfStillOk()
      }
    }

    tryTryAgain(1)
  }
}
