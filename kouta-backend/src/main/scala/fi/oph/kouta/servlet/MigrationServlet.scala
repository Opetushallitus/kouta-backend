package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.{CallerId, HttpClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{CreateResult, Haku, Hakukohde, Koulutus, PerustiedotWithOid, TilaFilter, Toteutus, UpdateResult}
import fi.oph.kouta.repository.{KoutaDatabase, MigrationDAO}
import fi.oph.kouta.service._
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.{BadRequest, Ok}

import java.time.Instant
import java.util.UUID
import java.util.regex.Pattern
import fi.oph.kouta.domain

import scala.util.{Failure, Success, Try}

/**
 * Use from browser console (while logged in service):

 var f = async (url) => fetch(url,{method:"POST"}).then(r => r.json())

 * Migrate haku:

 * Korkeakoulujen yhteishaku syksy 2020
 var hakukohteet = await f("/kouta-backend/migration/haku/1.2.246.562.29.98666182252")

 * or

 * Korkeakoulujen kevään 2021 ensimmäinen yhteishaku
 var hakukohteet = await f("/kouta-backend/migration/haku/1.2.246.562.29.72389663526")

 * or

 * Korkeakoulujen kevään 2021 toinen yhteishaku
 var hakukohteet = await f("/kouta-backend/migration/haku/1.2.246.562.29.98117005904")

 * Migrate hakukohteet:

 var count = 1; for(index in hakukohteet) {var resp = await f("/kouta-backend/migration/hakukohde/" + hakukohteet[index]); console.log(count++ + '/' + hakukohteet.length + ': ' + JSON.stringify(resp)); }

 */
trait LookupDb {
  def findMappedOid(oldOid: String): Option[String]
  def insertOidMapping(oldOld: String, newOid: String): Unit
  def updateAllowed(oldOid: String): Option[Boolean]
}
class MigrationDb extends LookupDb with Logging {
  def findMappedOid(oldOid: String): Option[String] = {
    KoutaDatabase.runBlockingTransactionally {
      MigrationDAO.oldToNewOidMapping(oldOid)
    } match {
      case Success(oid) => oid
      case Failure(exception) =>
        logger.error(s"Unable to read migration oid lookup for $oldOid")
        throw exception
    }
  }
  def insertOidMapping(originalOid: String, newOid: String): Unit = {
    KoutaDatabase.runBlockingTransactionally {
      MigrationDAO.insertOidMapping(originalOid, newOid)
    } match {
      case Failure(e) => {
        logger.error(s"Unable to store lookup oid $originalOid -> $newOid", e)
        throw e
      }
      case _ =>
        logger.info(s"Oid $newOid migrated as new!")
    }
  }
  def updateAllowed(oldOid: String): Option[Boolean] = {
    KoutaDatabase.runBlockingTransactionally {
      MigrationDAO.updateAllowed(oldOid)
    } match {
      case Success(allowed) => allowed
      case Failure(exception) =>
        logger.error(s"Unable to read update allowed for $oldOid")
        throw exception
    }
  }
}
class MigrationServlet(koulutusService: KoulutusService,
                       toteutusService: ToteutusService,
                       hakuService: HakuService,
                       hakukohdeService: HakukohdeService,
                       organisaatioServiceImpl: OrganisaatioServiceImpl,
                       urlProperties: OphProperties,
                       client: HttpClient,
                       db: LookupDb) extends KoutaServlet {

  private val migrationService = new MigrationService(organisaatioServiceImpl)

  def fetch(url: String): String = {
    client.get(url)(response => response)
  }

  def this() = this(
    KoulutusService,
    ToteutusService,
    HakuService,
    HakukohdeService,
    OrganisaatioServiceImpl,
    KoutaConfigurationFactory.configuration.urlProperties,
    new HttpClient with CallerId {},
    new MigrationDb)

  def f(r: JValue, q:String) = r.extract[Map[String, JValue]].filterKeys(_.toLowerCase.contains(q.toLowerCase))

  def getMappedOidOrExisting(obj: Oid): String = {
    db.findMappedOid(obj.toString).getOrElse(obj.toString)
  }
  def getMappedOid(oid: Oid): String = {
    db.findMappedOid(oid.toString).getOrElse(throw new RuntimeException(s"Expected $oid not found in migration lookup table!"))
  }

  def getMappedOidAsOption(oid: Oid): Option[String] = {
    db.findMappedOid(oid.toString)
  }

  def updateAllowed(oldOid: Oid): Boolean = {
    db.updateAllowed(oldOid.toString).getOrElse(true)
  }

  def tryPutAndPost[A <: PerustiedotWithOid[_ <: Oid, _]](originalOid: Oid, obj: A, put: A => CreateResult, post: (A, Instant) => UpdateResult): String = {

    //if(!originalOid.toString.equals(obj.oid.get.toString)) {
    if(obj.oid.isDefined) {
      Try(post(obj, Instant.now())) match {
        case Success(res) if res.updated=>
          logger.info(s"Oid $originalOid migrated overriding old!")
          obj.oid.get.toString
        case Failure(ex) => {
          logger.error(s"Exception migrating $originalOid!", ex)
          throw ex
        }
      }
    } else {
      Try(put(obj).oid) match {
        case Success(createdOid) =>
          db.findMappedOid(originalOid.toString) match {
            case Some(existing) =>
              if(existing.equals(createdOid.toString)) {
                logger.debug(s"Mapping already existed! ${originalOid.toString} -> ${createdOid.toString}")
              } else {
                throw new RuntimeException(s"Mapping ${originalOid.toString} -> ${existing} exists, but tried to add mapping ${originalOid.toString} -> ${createdOid.toString}!")
              }
            case None =>
              db.insertOidMapping(originalOid.toString, createdOid.toString)
          }
          createdOid.toString
        case Failure(e) =>
          logger.error(s"Exception migrating $originalOid!", e)
          throw e
      }
    }
  }

  def tryPutAndPostForKoulutus[A <: PerustiedotWithOid[_ <: Oid, _]](originalOid: Oid, obj: A, put: A => CreateResult, post: (A, Instant, Boolean) => UpdateResult): String = {

    if (obj.oid.isDefined) {
      Try(post(obj, Instant.now(), false)) match {
        case Success(_) =>
          logger.info(s"Oid $originalOid migrated overriding old!")
          obj.oid.get.toString
        case Failure(ex) => {
          logger.error(s"Exception migrating $originalOid!", ex)
          throw ex
        }
      }
    } else {
      Try(put(obj).oid) match {
        case Success(createdOid) =>
          db.findMappedOid(originalOid.toString) match {
            case Some(existing) =>
              if (existing.equals(createdOid.toString)) {
                logger.debug(s"Mapping already existed! ${originalOid.toString} -> ${createdOid.toString}")
              } else {
                throw new RuntimeException(s"Mapping ${originalOid.toString} -> ${existing} exists, but tried to add mapping ${originalOid.toString} -> ${createdOid.toString}!")
              }
            case None =>
              db.insertOidMapping(originalOid.toString, createdOid.toString)
          }
          createdOid.toString
        case Failure(e) =>
          logger.error(s"Exception migrating $originalOid!", e)
          throw e
      }
    }
  }

  registerPath("/migration/{oid}",
    """    get:
      |      summary: Hae migraatiossa mäpätty oid
      |      operationId: Hae migraatiossa mäpätty oid
      |      description: Hae migraatiossa mäpätty oid
      |      tags:
      |        - Migraatio
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oid
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  get("/:oid") {
    val oid = GenericOid(params("oid"))
    Ok(getMappedOid(oid))
  }

  registerPath("/migration/haku/{hakuOid}",
    """    post:
      |      summary: Migroi tarjonnan haku Koutaan
      |      operationId: Migroi tarjonnan haku Koutaan
      |      description: Migroi tarjonnan haku Koutaan
      |      tags:
      |        - Migraatio
      |      parameters:
      |        - in: path
      |          name: hakuOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oid
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/haku/:hakuOid") {
    println("n")
    implicit val authenticated: Authenticated = authenticate()

    val hakuOid = HakuOid(params("hakuOid"))
    logger.warn(s"Migration begins for haku $hakuOid!")
    val result = parse(fetch(urlProperties.url("tarjonta-service.haku.oid", hakuOid))) \ "result"
    val koutaHakuOid = getMappedOidAsOption(hakuOid)
    val hakuTemp: Haku =
      migrationService.parseHakuFromResult(result)
    val haku = koutaHakuOid.map(oid => hakuTemp.withOid(HakuOid(oid))).getOrElse(hakuTemp)

    if(updateAllowed(hakuOid)) {
      tryPutAndPost(hakuOid,
        haku.copy(nimi = haku.nimi.mapValues(nimi => s"$nimi (migraatio)")),
        hakuService.put, hakuService.update)
    } else {
      logger.warn(s"Skipped haku update because updating is not allowed | TarjontaOid: ${hakuOid} | KoutaOid: ${getMappedOidOrExisting(hakuOid)}")
    }

    val hakukohdeOids = (result \ "hakukohdeOids").extract[List[String]]

    Ok(compact(render(hakukohdeOids)))
  }

  registerPath("/migration/hakukohde/{hakukohdeOid}",
    """    post:
      |      summary: Migroi tarjonnan hakukohde Koutaan
      |      operationId: Migroi tarjonnan hakukohde Koutaan
      |      description: Migroi tarjonnan hakukohde Koutaan
      |      tags:
      |        - Migraatio
      |      parameters:
      |        - in: path
      |          name: hakukohdeOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oid
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/hakukohde/:hakukohdeOid") {
    implicit val authenticated: Authenticated = authenticate()

    def codeelement2koodi(obj: JObject): String = {
      val uri = (obj \ "codeElementUri").extract[String]
      val versio = (obj \ "codeElementVersion").extract[Int]
      s"$uri#$versio"
    }
    val KoulutusalaKoodiPattern: Pattern = Pattern.compile("""kansallinenkoulutusluokitus2016koulutusalataso2_\d+#\d{1,2}""")
    def koulutuskoodi2koulutusala(uri: String, versio: Int): Seq[String] = {
      val result = parse(fetch(urlProperties.url("koodisto-service.codeelement", uri, s"$versio")))
      val codeelements = (result \ "includesCodeElements")
        .extract[List[JObject]]
        .map(codeelement2koodi)
          .filter(koodi =>
            KoulutusalaKoodiPattern.matcher(koodi).matches())

      codeelements
    }
    val hakukohdeOid = HakukohdeOid(params("hakukohdeOid"))
    logger.warn(s"Migration begins for hakukohde $hakukohdeOid!")
    val result = parse(fetch(urlProperties.url("tarjonta-service.hakukohde.oid", hakukohdeOid))) \ "result"
    val hakukohdeTila: String = (result \ "tila").extract[String]
    if(hakukohdeTila.equals("JULKAISTU")) {
      val originalHakuOid = HakuOid((result \ "hakuOid").extract[String])
      db.findMappedOid(originalHakuOid.toString) match {
        case Some(hakuOid) =>
          val hakukohdeKoulutusOids = (result \ "hakukohdeKoulutusOids").extract[List[String]]
          val hakukohde = migrationService.parseHakukohdeFromResult(result).copy(hakuOid = HakuOid(hakuOid))
          for(koulutusOid <- hakukohdeKoulutusOids) {
            logger.warn(s"Migration begins for koulutus $koulutusOid!")
            val result = parse(fetch(urlProperties.url("tarjonta-service.koulutus.oid", koulutusOid))) \ "result"
            //val tarjoajanKoulutusOid = (result \ "tarjoajanKoulutus").extractOpt[String]
            val tarjoajanKoulutusOid = Try((result \ "tarjoajanKoulutus").extractOpt[String]).getOrElse(None)
            val tarjojanKoulutusResult = tarjoajanKoulutusOid match {
              case Some(oid) => Some(parse(fetch(urlProperties.url("tarjonta-service.koulutus.oid", oid))) \ "result")
              case None => None
            }
            val tarjontaKomotoTila: String = (result \ "tila").extract[String]
            if(tarjontaKomotoTila.equals("JULKAISTU")) {
              val komoOid = (result \ "komoOid").extract[String]
              val komo = parse(fetch(urlProperties.url("tarjonta-service.komo.oid", komoOid))) \ "result"
              val tarjojanKomo = tarjoajanKoulutusOid match {
                case Some(_) => {
                  val tarjoajanKomoOid = (result \ "komoOid").extract[String]
                  Some(parse(fetch(urlProperties.url("tarjonta-service.komo.oid", tarjoajanKomoOid))) \ "result")
                }
                case None => None
              }
              val koulutusTemp: Koulutus = migrationService.parseKoulutusFromResult(result, komo, tarjojanKoulutusResult, tarjojanKomo, koulutuskoodi2koulutusala)
              val komoOidForUpdate = tarjojanKoulutusResult match {
                case Some(r) => (r \ "komoOid").extract[String]
                case None => komoOid
              }
              val koutaKoulutusOid = getMappedOidAsOption(KoulutusOid(komoOidForUpdate))
              if(updateAllowed(KoulutusOid(komoOidForUpdate))) {
                val koulutus = koutaKoulutusOid.map(oid => koulutusTemp.withOid(KoulutusOid(oid))).getOrElse(koulutusTemp)
                tryPutAndPostForKoulutus(KoulutusOid(komoOidForUpdate),
                  koulutus,
                  koulutusService.put, koulutusService.update)
              } else {
                logger.warn(s"Skipped koulutus update because updating is not allowed | TarjontaOid: ${komoOidForUpdate} | KoutaOid: ${koutaKoulutusOid.get}")
              }

              val komotoOid = (result \ "oid").extract[String]
              val koutaToteutusOid = getMappedOidAsOption(ToteutusOid(komotoOid))
              val toteutusTemp: Toteutus = migrationService.parseToteutusFromResult(result)
                .copy(koulutusOid = KoulutusOid(getMappedOid(KoulutusOid(komoOidForUpdate))))
              if(updateAllowed(ToteutusOid(komotoOid))) {
                val toteutus = koutaToteutusOid.map(oid => toteutusTemp.withOid(ToteutusOid(oid))).getOrElse(toteutusTemp)
                tryPutAndPost(ToteutusOid(komotoOid),
                  toteutus,
                  toteutusService.put, toteutusService.update)
              } else {
                logger.warn(s"Skipped toteutus update because updating is not allowed | TarjontaOid: ${komotoOid} | KoutaOid: ${koutaToteutusOid.get}")
              }
            } else {
              logger.warn(s"Migration skipped for koulutus $koulutusOid is not JULKAISTU!")
            }
          }
          val valintakokeet: Map[String, domain.Valintakoe] = migrationService.parseValintakokeetFromResult(result)
          val finalHakukohdeTemp =
            hakukohde
              .copy(
                valintakokeet = valintakokeet.map {
                  case (id, koe) =>
                    db.findMappedOid(id).map(id => koe.copy(id = Some(UUID.fromString(id)))).getOrElse(koe)
                }.toSeq,
                toteutusOid = ToteutusOid(getMappedOid(hakukohde.toteutusOid)))

          val koutaHakukohdeOid = getMappedOidAsOption(hakukohdeOid)
          if(updateAllowed(hakukohdeOid)) {
            val finalHakukohde = koutaHakukohdeOid.map(oid => finalHakukohdeTemp.withOid(HakukohdeOid(oid))).getOrElse(finalHakukohdeTemp)
            val newOid = tryPutAndPost(hakukohdeOid,
              finalHakukohde,
              hakukohdeService.put, hakukohdeService.update)

            if(valintakokeet.nonEmpty) {
              val saved: (Hakukohde, Instant) = hakukohdeService.get(HakukohdeOid(newOid), TilaFilter.onlyOlemassaolevat()).get

              for(vk <- saved._1.valintakokeet;
                  (id, old) <- valintakokeet) {
                if(old.nimi.equals(vk.nimi) && db.findMappedOid(id).isEmpty) {
                  db.insertOidMapping(id, vk.id.get.toString)
                }
              }
            }
          } else {
            logger.warn(s"Skipped hakukohde update because updating is not allowed | TarjontaOid: ${hakukohdeOid} | KoutaOid: ${koutaHakukohdeOid.get}")
          }
          Ok(MigrationStatus(hakukohdeOid, "SUCCESSFULLY MIGRATED"))
        case None => BadRequest(s"Haku $originalHakuOid must be migrated first!")
      }
    } else {
      logger.warn(s"Migration skipped for hakukohde $hakukohdeOid because it is not JULKAISTU!")
      Ok(MigrationStatus(hakukohdeOid, "SKIPPED MIGRATION"))
    }

  }
}

case class MigrationStatus(hakukohdeOid: HakukohdeOid, status: String)
