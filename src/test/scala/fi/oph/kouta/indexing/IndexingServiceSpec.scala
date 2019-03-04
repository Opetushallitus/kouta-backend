package fi.oph.kouta.indexing

import fi.oph.kouta.WaitIfFails
import fi.oph.kouta.integration.{KonfoIndexingQueues, KoutaIntegrationSpec}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar

class IndexingServiceSpec extends KoutaIntegrationSpec with KonfoIndexingQueues with Eventually with SpanSugar with WaitIfFails {
  case class Foo(id: Option[String])

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 500.microseconds)
  implicit val fooIndexing: Indexing[Foo] = new Indexing[Foo] {
    override def index(a: Foo): String = "fooIndex"
    override def indexId(a: Foo): Option[String] = a.id
  }

  "IndexingService.index[A]" should "send indexing message to SQS queue using Indexing type class" in {
    IndexingService.index(Foo(Some("foobar")))
    eventually {
      receiveFromQueue(indexingQueue) should contain ("""{"fooIndex":["foobar"]}""")
    }
  }

  it should "not send indexing message to SQS if `indexId` return None" in {
    IndexingService.index(Foo(None))
    waitIfFails {
      receiveFromQueue(indexingQueue) should be (empty)
    }
  }
}
