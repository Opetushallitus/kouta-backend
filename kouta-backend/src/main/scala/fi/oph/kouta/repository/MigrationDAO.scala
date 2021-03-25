package fi.oph.kouta.repository

import fi.oph.kouta.domain.oid.{Oid}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

object MigrationDAO extends MigrationExtractors with SQLHelpers {
  this: ExtractorBase =>

  def oldToNewOidMapping(oldOid: String): DBIO[Option[String]] = {
    sql"""select new_oid
          from migration_old_to_new_oid_lookup
          where old_oid = $oldOid""".as[String].headOption
  }

  def insertOidMapping(oldOid: String, newOid: String): DBIO[String] = {
    sql"""insert into migration_old_to_new_oid_lookup (
            old_oid,
            new_oid)
          values (
            $oldOid,
            $newOid) returning new_oid""".as[String].head
  }

  def updateAllowed(oldOid: String): DBIO[Option[Boolean]] = {
    sql"""select update_allowed
          from migration_old_to_new_oid_lookup
          where old_oid = $oldOid""".as[Boolean].headOption
  }

}
