package fi.oph.kouta.repository

import fi.oph.kouta.domain.oid.{Oid}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

object MigrationDAO extends MigrationExtractors with SQLHelpers {
  this: ExtractorBase =>

  def oldToNewOidMapping(oldOid: Oid): DBIO[Option[String]] = {
    val s = oldOid.toString
    sql"""select new_oid
          from migration_old_to_new_oid_lookup
          where old_oid = $s""".as[String].headOption
  }

  def insertOidMapping(oldOid: Oid, newOid: Oid): DBIO[String] = {
    sql"""insert into migration_old_to_new_oid_lookup (
            old_oid,
            new_oid)
          values (
            ${oldOid.toString},
            ${newOid.toString}) returning new_oid""".as[String].head
  }

}
