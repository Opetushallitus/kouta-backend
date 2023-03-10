package fi.oph.kouta.integration.fixture

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.TestSetups

trait DatabaseFixture {

  import slick.dbio.DBIO
  import slick.jdbc.PostgresProfile.api._
  import fi.oph.kouta.repository.KoutaDatabase

  System.setProperty("kouta-backend.useSecureCookies", "false")
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  TestSetups.setupPostgres()

  lazy val db: KoutaDatabase.type = KoutaDatabase

  def setEntityModifiedToPast(
      oid: String,
      duration: String,
      entityTable: String,
      relatedTables: Seq[String],
      selfKey: String,
      foreignKey: String
  ) = {
    db.runBlockingTransactionally(DBIO.seq(sqlu"""ALTER TABLE #$entityTable DISABLE TRIGGER #${entityTable}_history;
          ALTER TABLE #$entityTable DISABLE TRIGGER set_temporal_columns_on_#${entityTable}_on_update;
          ALTER TABLE #$entityTable DISABLE TRIGGER set_#${entityTable}_last_modified_on_change;
          #${relatedTables
      .map(t => s"""ALTER TABLE ${t} DISABLE TRIGGER ${t}_history;
              ALTER TABLE ${t} DISABLE TRIGGER set_temporal_columns_on_${t}_on_update;
              ALTER TABLE ${t} DISABLE TRIGGER set_last_modified_on_${t}_change;""")
      .mkString}
          UPDATE #${entityTable} SET system_time = tstzrange(now() - interval '#$duration', NULL::timestamp with time zone, '[)'::text) WHERE #$selfKey = '#$oid';
          UPDATE #${entityTable} SET last_modified = (now() - interval '#$duration') WHERE #$selfKey = '#$oid';
          #${relatedTables
      .map(t =>
        s"""UPDATE $t SET system_time = tstzrange(now() - interval '$duration', NULL::timestamp with time zone, '[)'::text) where $foreignKey = '$oid';"""
      )
      .mkString}
          ALTER TABLE #$entityTable ENABLE TRIGGER #${entityTable}_history;
          ALTER TABLE #$entityTable ENABLE TRIGGER set_#${entityTable}_last_modified_on_change;
          ALTER TABLE #$entityTable ENABLE TRIGGER set_temporal_columns_on_#${entityTable}_on_update;
          #${relatedTables
      .map(t => s"""ALTER TABLE ${t} ENABLE TRIGGER ${t}_history;
            ALTER TABLE ${t} ENABLE TRIGGER set_temporal_columns_on_${t}_on_update;
            ALTER TABLE ${t} ENABLE TRIGGER set_last_modified_on_${t}_change;""")
      .mkString}"""))
  }

  def deleteAsiasanat(): Int = {
    db.runBlocking(sqlu"""delete from asiasanat""")
    db.runBlocking(sqlu"""delete from ammattinimikkeet""")
  }

  // Slick lisää jostain syystä SQL-kyselyyn loppuun sulut, jos ei laita "where true"
  def getTableHistorySize(tableName: String, idKey: String = "", id: String = ""): Int = db.runBlocking(
    sql"""select count(*) from #${tableName}_history where #${if (id.nonEmpty) s"$idKey = '$id'" else "true"};""".as[Int].head
  )

  def resetTableHistory(tableName: String) = {
    db.runBlocking(sqlu"""delete from #${tableName}_history where true""")
  }
}
