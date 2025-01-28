package fi.oph.kouta.util

import fi.oph.kouta.logging.Logging

object Timer extends Logging {
  def timed[R](blockname: String = "", thresholdMs: Int = 0)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    val time: Long = (t1 - t0) / 1000000
    if (time >= thresholdMs) logger.info(blockname + " call took: " + time + " ms")
    result
  }
}
