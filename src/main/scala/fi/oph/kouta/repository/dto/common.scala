package fi.oph.kouta.repository.dto

import java.time.Instant

import fi.oph.kouta.util.KoutaJsonFormats
import slick.jdbc.GetResult

trait KoutaDTOBase extends KoutaJsonFormats {

  implicit val getInstantOptionResult: GetResult[Option[Instant]] = GetResult(r => r.nextTimestampOption().map(_.toInstant))

  case class TarjoajaDTO(oid:String, tarjoajaOid:String)

  object TarjoajaDTO extends fi.oph.kouta.repository.Extractable[TarjoajaDTO] {
    import slick.jdbc.GetResult
    implicit val extractor =
      GetResult(r => TarjoajaDTO(
        oid = r.nextString,
        tarjoajaOid = r.nextString))
  }
}
