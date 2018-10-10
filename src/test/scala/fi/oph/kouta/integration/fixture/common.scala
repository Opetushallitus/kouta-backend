package fi.oph.kouta.integration.fixture

import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s.jackson.Serialization.{read, write}

case class Oid(oid:String)
case class Updated(updated:Boolean)

trait CommonFixture { this: KoutaJsonFormats =>

  def headersIfUnmodifiedSince(lastModified:String) = List(("If-Unmodified-Since", lastModified))

  def bytes(o:AnyRef) = write(o).getBytes

  def oid(body: String) = (read[Oid](body)).oid

  def updated(body: String) = read[Updated](body).updated
}