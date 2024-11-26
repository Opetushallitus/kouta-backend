package db.migration

import fi.oph.kouta.logging.Logging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import io.circe._
import io.circe.parser._
import scala.collection.mutable.ListBuffer

class V103__update_laajuus_for_many_koulutustyyppis_OY_3883 extends BaseJavaMigration with Logging{

  case class LaajuusAndNumber(laajuudenYksikko: String, laajuudenNumero: Int)

  case class OidAndMeta(oid: String, meta: String)

  private val OPINTOPISTE_KOODI = "opintojenlaajuusyksikko_2#1"
  private val OSAAMISPISTE_KOODI = "opintojenlaajuusyksikko_6#1"
  private val VIIKKO_KOODI = "opintojenlaajuusyksikko_8#1"

  private def uriToLaajuusNumber(koodiUri: String): Int = {
    koodiUri match {
      case "opintojenlaajuus_v53#1" => 53
      case "opintojenlaajuus_90#1" => 90
      case "opintojenlaajuus_88#1" => 88
      case "opintojenlaajuus_60#1" => 60
      case "opintojenlaajuus_60" => 60
      case "opintojenlaajuus_38#1" => 38
      case "opintojenlaajuus_360#1" => 360
      case "opintojenlaajuus_330#1" => 330
      case "opintojenlaajuus_30#1" => 30
      case "opintojenlaajuus_300#1" => 300
      case "opintojenlaajuus_270#1" => 270
      case "opintojenlaajuus_240#1" => 240
      case "opintojenlaajuus_210120#1" => 330
      case "opintojenlaajuus_210#1" => 210
      case "opintojenlaajuus_180180#1" => 360
      case "opintojenlaajuus_180150#1" => 330
      case "opintojenlaajuus_180120150#1" => 450
      case "opintojenlaajuus_180120#1" => 300
      case "opintojenlaajuus_180#1" => 180
      case "opintojenlaajuus_150#1" => 150
      case "opintojenlaajuus_140#1" => 140
      case "opintojenlaajuus_120#1" => 120
      case _ => -1
    }
  }


  private def convertToHardcoded(koodiUri: String, koulutustyyppi: String): LaajuusAndNumber = {
    koulutustyyppi match {
      case "lk" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "yo" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "amk" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "vapaa-sivistystyo-opistovuosi" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "telma" => LaajuusAndNumber(OSAAMISPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "tuva" => LaajuusAndNumber(VIIKKO_KOODI, uriToLaajuusNumber(koodiUri))
      case "ope-pedag-opinnot" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
      case "amm-ope-erityisope-ja-opo" => LaajuusAndNumber(OPINTOPISTE_KOODI, uriToLaajuusNumber(koodiUri))
    }
  }

  private def updateLaajuusOnMetadata(metadata: JsonObject, koulutusTyyppi: String): String = {
    val opintojenLaajuus: String = metadata.apply("opintojenLaajuusKoodiUri").getOrElse(Json.Null).asString.getOrElse("")
    val opintojenLaajuusTypeAndNumber = convertToHardcoded(opintojenLaajuus, koulutusTyyppi)
    val convertedMetadata = opintojenLaajuusTypeAndNumber.laajuudenNumero match {
      case laajuus if laajuus >= 0 =>
        metadata.add("opintojenLaajuusNumero", Json.fromInt(opintojenLaajuusTypeAndNumber.laajuudenNumero))
          .add("opintojenLaajuusyksikkoKoodiUri", Json.fromString(opintojenLaajuusTypeAndNumber.laajuudenYksikko))
          .remove("opintojenLaajuusKoodiUri")
      case _ =>
        metadata.add("opintojenLaajuusyksikkoKoodiUri", Json.fromString(opintojenLaajuusTypeAndNumber.laajuudenYksikko))
          .remove("opintojenLaajuusKoodiUri")
    }
    Json.fromJsonObject(convertedMetadata).toString()
  }


  override def migrate(context: Context): Unit = {
    val tyyppis = List("lk", "amk", "yo", "amm-ope-erityisope-ja-opo",
      "ope-pedag-opinnot", "vapaa-sivistystyo-opistovuosi", "tuva", "telma")

    val conn = context.getConnection
    val oidsAndMetas = new ListBuffer[OidAndMeta]()
    tyyppis.foreach(koulutustyyppi => {
      val results = conn.createStatement().executeQuery(s"""SELECT oid, metadata FROM koulutukset WHERE tyyppi = '$koulutustyyppi'""".stripMargin)
      while (results.next) {
        val oid = results.getString(1)
        val metadata = parse(results.getString(2)).getOrElse(Json.Null).asObject.get
        try {
          oidsAndMetas += OidAndMeta(oid, updateLaajuusOnMetadata(metadata, koulutustyyppi))
        } catch {
          case error: IllegalArgumentException => logger.warn(s"Could not process koulutus ${oid} due to : ${error}")
        }
      }

    })
    val updateStmnt = conn.prepareStatement("""UPDATE koulutukset SET metadata = ?::JSON WHERE oid = ?""".stripMargin)
    oidsAndMetas.foreach(oidAndMeta => {
      updateStmnt.setObject(1, oidAndMeta.meta)
      updateStmnt.setString(2, oidAndMeta.oid)
      updateStmnt.executeUpdate()
    })
    conn.commit()
  }

}
