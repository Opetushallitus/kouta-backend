package db.migration

import fi.oph.kouta.domain.{Amk, Korkeakoulutustyyppi, Koulutustyyppi, Yo}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.{OrganisaatioService, OrganisaatioServiceImpl}
import fi.vm.sade.utils.slf4j.Logging
import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import io.circe.syntax._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class V120__update_korkeakoulutustyypit extends BaseJavaMigration with Logging {
  case class OidAndMeta(oid: String, meta: String)

  private def updateKorkeakoulutustyypitOnMetadata(metadata: JsonObject, tarjoajat: Array[String]): String = {
    Try[Map[OrganisaatioOid, Set[Koulutustyyppi]]] {
      tarjoajat map (tarjoaja =>
        OrganisaatioOid(tarjoaja) ->
          OrganisaatioServiceImpl
            .getAllChildOidsAndKoulutustyypitFlat(OrganisaatioOid(tarjoaja))
            ._2
            .intersect(Seq(Amk, Yo))
            .toSet
      ) toMap
    } match {
      case Success(allKoulutustyypit) => {
        var korkeakoulutustyypit = allKoulutustyypit
          .foldLeft(Map[Koulutustyyppi, Seq[OrganisaatioOid]]().withDefaultValue(Seq())) {
            case (initialMap, (tarjoaja, koulutustyypit)) =>
              koulutustyypit.foldLeft(initialMap)((subMap, koulutustyyppi) =>
                subMap.updated(koulutustyyppi, initialMap(koulutustyyppi) :+ tarjoaja)
              )
          }
          .map(entry => Korkeakoulutustyyppi(entry._1, entry._2))
          .toSeq
        korkeakoulutustyypit =
          if (korkeakoulutustyypit.size == 1)
            Seq(Korkeakoulutustyyppi(korkeakoulutustyypit.head.koulutustyyppi, Seq()))
          else korkeakoulutustyypit
        val korkeakoulutustyypitJson = korkeakoulutustyypit.map(tyyppi => Json.fromFields(List(("koulutustyyppi", Json.fromString(tyyppi.koulutustyyppi.toString())), ("tarjoajat", tyyppi.tarjoajat.map(_.s).asJson))))
        val convertedMetadata = metadata.add("korkeakoulutustyypit", korkeakoulutustyypitJson.asJson)
        Json.fromJsonObject(convertedMetadata).toString()
      }
      case Failure(exception) => throw exception
    }
  }

  override def migrate(context: Context): Unit = {
    val tyyppis      = List("kk-opintojakso", "kk-opintokokonaisuus", "erikoistumiskoulutus")
    val conn         = context.getConnection
    val oidsAndMetas = new ListBuffer[OidAndMeta]()
    tyyppis.foreach(koulutustyyppi => {
      val results =
        conn
          .createStatement()
          .executeQuery(s"""SELECT k.oid, k.metadata, tarjoaja.tarjoajat FROM koulutukset k JOIN
                           |    (SELECT koulutus_oid koid, string_agg(tarjoaja_oid, ',') tarjoajat
                           |      FROM koulutusten_tarjoajat GROUP BY koulutus_oid) tarjoaja on k.oid = koid
                           |WHERE k.tyyppi = '$koulutustyyppi'""".stripMargin)
      while (results.next) {
        val oid       = results.getString(1)
        val metadata  = parse(results.getString(2)).getOrElse(Json.Null).asObject.get
        val tarjoajat = results.getString(3).split(",")
        try {
          oidsAndMetas += OidAndMeta(oid, updateKorkeakoulutustyypitOnMetadata(metadata, tarjoajat))
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
