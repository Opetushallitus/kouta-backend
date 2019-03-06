package fi.oph.kouta.integration

import org.scalactic.source.Position
import org.scalatest.concurrent.{Eventually, PatienceConfiguration}
import org.scalatest.enablers.Retrying
import org.scalatest.Assertion

import org.scalatest.time.SpanSugar._

trait EventuallyMessages extends Eventually {
  this: KonfoIndexingQueues with PatienceConfiguration =>

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 100.microseconds)

  def eventuallyMessages(queue: String)
                        (check: Seq[String] => Assertion)
                        (implicit patienceConfig: PatienceConfig, retrying: Retrying[Assertion], pos: Position): Seq[String] = {
    eventually {
      val received = receiveFromQueue(queue)
      val messages = received map { _.getBody }

      check(messages)
      received map { _.getReceiptHandle } foreach { sqs.deleteMessage(queue, _) }
      messages
    }
  }

  def eventuallyIndexingMessages(check: Seq[String] => Assertion)
                                (implicit patienceConfig: PatienceConfig, retrying: Retrying[Assertion], pos: Position): Seq[String] = {
    eventuallyMessages(indexingQueue)(check)(patienceConfig, retrying, pos)
  }
}
