package fi.oph.kouta.mocks

import fi.oph.kouta.client.SiirtotiedostoPalveluClient
import org.json4s.jackson.Serialization.write

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import scala.collection.mutable.ListBuffer


class MockSiirtotiedostoPalveluClient extends SiirtotiedostoPalveluClient {
  val contentBuffer: ListBuffer[String] = new ListBuffer[String]();
  override def saveSiirtotiedosto[T](
      contentType: String,
      content: Seq[T],
      operationId: String,
      operationSubId: Int
  ): String = {
    contentBuffer += write(content)
    s"$contentType, yhteensä ${content.size} kpl tallennettu S3 buckettiin avaimella kouta/kouta_entiteetit-12345.json"
  }

  def last(): String = contentBuffer.last
  def head(): String = contentBuffer.head
  def clearContents() = contentBuffer.clear()
  def numberOfContentItems = contentBuffer.size
}
