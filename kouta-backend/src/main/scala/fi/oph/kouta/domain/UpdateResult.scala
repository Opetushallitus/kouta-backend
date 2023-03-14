package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{Oid}

import java.util.UUID


case class CreateResult(oid: Oid,
                        warnings: List[String])

case class ValintaperusteCreateResult(id: Option[UUID],
                                      created: Boolean,
                                      warnings: List[String]) {
}

case class UpdateResult(updated: Boolean,
                        warnings: List[String]) {
}



