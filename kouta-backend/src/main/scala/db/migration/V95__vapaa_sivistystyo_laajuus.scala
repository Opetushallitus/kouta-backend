package db.migration

import fi.oph.kouta.logging.Logging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import io.circe._
import io.circe.parser._
import scala.collection.mutable.ListBuffer

class V95__vapaa_sivistystyo_laajuus extends BaseJavaMigration with Logging {

  case class LaajuusAndNumber(laajuudenYksikko: String, laajuudenNumero: Int)

  case class OidAndMeta(oid: String, meta: String)

  /*
  opintojenlaajuusyksikko_1 = opintoviikkoa
  opintojenlaajuusyksikko_2 = opintopistetta
  opintojenlaajuusyksikko_3 = vuosiviikkotuntia
  opintojenlaajuusyksikko_4 = kurssia
  opintojenlaajuusyksikko_5 = tuntia
  opintojenlaajuusyksikko_6 = osaamispistettä
  opintojenlaajuusyksikko_7 = vuotta
  opintojenlaajuusyksikko_8 = viikkoa
   */

  /*
  Käytässä olleet
    opintojenlaajuus_10#1 => 10 op
    opintojenlaajuus_180120#1 => 300 op
    opintojenlaajuus_18#1 => 18 op
    opintojenlaajuus_20#1 => 20 op
    opintojenlaajuus_240#1 => 240 op
    opintojenlaajuus_30#1 => 30 viikkoa
    opintojenlaajuus_32#1 => 32 viikkoa
    opintojenlaajuus_34#1 => 34 viikkoa
    opintojenlaajuus_35#1 => 35 viikkoa
    opintojenlaajuus_36#1 => 36 viikkoa
    opintojenlaajuus_38#1 => 38 viikkoa
    opintojenlaajuus_40#1 => 40 viikkoa
    opintojenlaajuus_60#1 => 60 op
    opintojenlaajuus_75#1 => 75 op
    opintojenlaajuus_v53#1 => 53 op
   */

  private val OPINTOPISTE_KOODI = "opintojenlaajuusyksikko_2#1"
  private val VIIKKO_KOODI = "opintojenlaajuusyksikko_8#1"

  private def convertLaajuusType(opintojenLaajuus: Json): LaajuusAndNumber = {
    opintojenLaajuus.asString match {
      case Some("opintojenlaajuus_10#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 10)
      case Some("opintojenlaajuus_180120#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 300)
      case Some("opintojenlaajuus_18#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 18)
      case Some("opintojenlaajuus_20#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 20)
      case Some("opintojenlaajuus_240#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 240)
      case Some("opintojenlaajuus_30#1") => new LaajuusAndNumber(VIIKKO_KOODI, 30)
      case Some("opintojenlaajuus_32#1") => new LaajuusAndNumber(VIIKKO_KOODI, 32)
      case Some("opintojenlaajuus_34#1") => new LaajuusAndNumber(VIIKKO_KOODI, 34)
      case Some("opintojenlaajuus_35#1") => new LaajuusAndNumber(VIIKKO_KOODI, 35)
      case Some("opintojenlaajuus_36#1") => new LaajuusAndNumber(VIIKKO_KOODI, 36)
      case Some("opintojenlaajuus_38#1") => new LaajuusAndNumber(VIIKKO_KOODI, 38)
      case Some("opintojenlaajuus_40#1") => new LaajuusAndNumber(VIIKKO_KOODI, 40)
      case Some("opintojenlaajuus_60#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 60)
      case Some("opintojenlaajuus_75#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 75)
      case Some("opintojenlaajuus_v53#1") => new LaajuusAndNumber(OPINTOPISTE_KOODI, 53)
      case _ => throw new IllegalArgumentException("opintojenLaajuusKoodiUri ei tuettu " + opintojenLaajuus)
    }
  }

  private def updateLaajuusOnMetadata(metadata: JsonObject): String = {
    val opintojenLaajuus = metadata.apply("opintojenLaajuusKoodiUri").getOrElse(Json.Null)
    val opintojenLaajuusTypeAndNumber = convertLaajuusType(opintojenLaajuus)
    val convertedMeta = metadata.add("opintojenLaajuusNumero", Json.fromInt(opintojenLaajuusTypeAndNumber.laajuudenNumero))
      .add("opintojenLaajuusyksikkoKoodiUri", Json.fromString(opintojenLaajuusTypeAndNumber.laajuudenYksikko))
      .remove("opintojenLaajuusKoodiUri")
    Json.fromJsonObject(convertedMeta).toString()
  }

  override def migrate(context: Context): Unit = {
    val conn = context.getConnection
    val results = conn.createStatement().executeQuery("""SELECT oid, metadata FROM koulutukset WHERE tyyppi = 'vapaa-sivistystyo-muu'""".stripMargin)
    val oidsAndMetas = new ListBuffer[OidAndMeta]();
    while (results.next) {
      val oid = results.getString(1)
      val metadata = parse(results.getString(2)).getOrElse(Json.Null).asObject.get
      try {
        oidsAndMetas += OidAndMeta(oid, updateLaajuusOnMetadata(metadata))
      } catch {
        case error: IllegalArgumentException => logger.warn(s"Could not process koulutus ${oid} due to : ${error}")
      }
    }

    val updateStmnt = conn.prepareStatement("""UPDATE koulutukset SET metadata = ?::JSON WHERE oid = ?""".stripMargin)
    oidsAndMetas.foreach(oidAndMeta => {
      updateStmnt.setObject(1, oidAndMeta.meta)
      updateStmnt.setString(2, oidAndMeta.oid)
      updateStmnt.executeUpdate()
    })

    conn.commit()
  }

}
