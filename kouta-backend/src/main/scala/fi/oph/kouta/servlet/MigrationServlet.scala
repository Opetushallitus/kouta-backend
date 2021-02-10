package fi.oph.kouta.servlet

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.{CallerId, HttpClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Ajanjakso, Arkistoitu, Ataru, En, Fi, Haku, HakuMetadata, Hakukohde, HakukohdeMetadata, Hakulomaketyyppi, Julkaistu, Julkaisutila, Kieli, Kielistetty, Koulutus, Liite, LiitteenToimitusosoite, LiitteenToimitustapa, Lomake, Muu, PerustiedotWithOid, Sv, Tallennettu, Toteutus, Valintakoe}
import fi.oph.kouta.domain.oid.{GenericOid, HakuOid, HakukohdeOid, KoulutusOid, Oid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.repository.{KoutaDatabase, MigrationDAO}
import fi.oph.kouta.service.{HakuService, HakukohdeService, KoulutusService, MigrationService, ToteutusService, ValintaperusteService}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.{BadRequest, NotFound, Ok}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import slick.dbio.DBIO

import scala.util.{Failure, Success, Try}

/**
 * Use from browser console (while logged in service):

 var f = async (url) => fetch(url,{method:"POST"}).then(r => r.json())

 * Migrate haku:

 var hakukohteet = await f("/kouta-backend/migration/haku/1.2.246.562.29.98666182252")

 * Migrate hakukohteet:

 for(index in hakukohteet) {console.log(await f("/kouta-backend/migration/hakukohde/" + hakukohteet[index]))}

 */
trait LookupDb {
  def findMappedOid(oldOid: Oid): Option[String]
  def insertOidMapping(oldOld: Oid, newOid: Oid): Unit
}
class MigrationDb extends LookupDb with Logging {
  def findMappedOid(oldOid: Oid): Option[String] = {
    KoutaDatabase.runBlockingTransactionally {
      MigrationDAO.oldToNewOidMapping(oldOid)
    } match {
      case Success(oid) => oid
      case Failure(exception) =>
        logger.error(s"Unable to read migration oid lookup for $oldOid")
        throw exception
    }
  }
  def insertOidMapping(originalOid: Oid, newOid: Oid): Unit = {
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
}
class MigrationServlet(koulutusService: KoulutusService,
                       toteutusService: ToteutusService,
                       hakuService: HakuService,
                       hakukohdeService: HakukohdeService,
                       urlProperties: OphProperties,
                       client: HttpClient,
                       db: LookupDb) extends KoutaServlet {

  private val migrationService = new MigrationService

  def fetch(url: String) = {
    client.get(url) {
      response => response
    }
  }

  def this() = this(
    KoulutusService,
    ToteutusService,
    HakuService,
    HakukohdeService,
    KoutaConfigurationFactory.configuration.urlProperties,
    new HttpClient with CallerId {},
    new MigrationDb)

  def f(r: JValue, q:String) = r.extract[Map[String, JValue]].filterKeys(_.toLowerCase.contains(q.toLowerCase))

  def getMappedOidOrExisting(obj: Oid): String = {
    db.findMappedOid(obj).getOrElse(obj.toString)
  }
  def getMappedOid(oid: Oid): String = {
    db.findMappedOid(oid).getOrElse(throw new RuntimeException(s"Expected $oid not found in migration lookup table!"))
  }

  def tryPutAndPost[A <: PerustiedotWithOid[_ <: Oid, _]](originalOid: Oid, obj: A, put: A => Oid, post: (A, Instant) => Boolean): Unit = {

    if(!originalOid.toString.equals(obj.oid.get.toString)) {
      Try(post(obj, Instant.now())) match {
        case Success(_) =>
          logger.info(s"Oid $originalOid migrated overriding old!")
        case Failure(ex) => {
          logger.error(s"Exception migrating $originalOid!", ex)
          throw ex
        }
      }
    } else {
      Try(put(obj)) match {
        case Success(value) =>
          db.insertOidMapping(originalOid, value)
        case Failure(e) =>
          logger.error(s"Exception migrating $originalOid!", e)
          throw e
      }
    }
  }

  get("/:oid") {
    val oid = GenericOid(params("oid"))
    Ok(getMappedOid(oid))
  }

  post("/haku/:hakuOid") {
    println("n")
    implicit val authenticated: Authenticated = authenticate()

    val hakuOid = HakuOid(params("hakuOid"))
    logger.warn(s"Migration begins for haku $hakuOid!")
    val result = parse(fetch(urlProperties.url("tarjonta-service.haku.oid", hakuOid))) \ "result"
    val haku: Haku =
      migrationService.parseHakuFromResult(result)
        .withOid(HakuOid(getMappedOidOrExisting(hakuOid)))

    tryPutAndPost(hakuOid, haku, hakuService.put, hakuService.update)
    val hakukohdeOids = (result \ "hakukohdeOids").extract[List[String]]


    Ok(compact(render(hakukohdeOids)))
  }

  post("/hakukohde/:hakukohdeOid") {
    implicit val authenticated: Authenticated = authenticate()

    val hakukohdeOid = HakukohdeOid(params("hakukohdeOid"))
    logger.warn(s"Migration begins for hakukohde $hakukohdeOid!")
    val result = parse(fetch(urlProperties.url("tarjonta-service.hakukohde.oid", hakukohdeOid))) \ "result"
    val originalHakuOid = HakuOid((result \ "hakuOid").extract[String])
    db.findMappedOid(originalHakuOid) match {
      case Some(hakuOid) =>
        //val komotos = (result \ "koulutusmoduuliToteutusTarjoajatiedot").extract[Map[String, JObject]]
        val hakukohdeKoulutusOids = (result \ "hakukohdeKoulutusOids").extract[List[String]]
        val hakukohde = migrationService.parseHakukohdeFromResult(result).copy(hakuOid = HakuOid(hakuOid))
        for(koulutusOid <- hakukohdeKoulutusOids) {
          //val komotoOids = (komotos \ "tarjoajaOids").extract[List[String]]
          //komoAndKomotos += komoOid -> (komotoOids ++ komoAndKomotos.getOrElse(komoOid, List())).distinct
          //val koulutusOid = komoOid
          logger.warn(s"Migration begins for koulutus $koulutusOid!")
          val result = parse(fetch(urlProperties.url("tarjonta-service.koulutus.oid", koulutusOid))) \ "result"
          val komoOid = (result \ "komoOid").extract[String]
          val komo = parse(fetch(urlProperties.url("tarjonta-service.komo.oid", komoOid))) \ "result"
          val koulutus: Koulutus = migrationService.parseKoulutusFromResult(result, komo)
          tryPutAndPost(koulutus.oid.get,
            koulutus.withOid(KoulutusOid(getMappedOidOrExisting(koulutus.oid.get))),
            koulutusService.put, koulutusService.update)

          val toteutus: Toteutus = migrationService.parseToteutusFromResult(result)
            .copy(koulutusOid = KoulutusOid(getMappedOid(koulutus.oid.get)))
          tryPutAndPost(toteutus.oid.get,
            toteutus.withOid(ToteutusOid(getMappedOidOrExisting(toteutus.oid.get))),
            toteutusService.put, toteutusService.update)
        }
        tryPutAndPost(hakukohdeOid,
          hakukohde
              .copy(toteutusOid = ToteutusOid(getMappedOid(hakukohde.toteutusOid)))
            .withOid(HakukohdeOid(getMappedOidOrExisting(hakukohdeOid))),
          hakukohdeService.put, hakukohdeService.update)
        Ok(hakukohdeOid)
      case None => BadRequest(s"Haku $originalHakuOid must be migrated first!")
    }


  }

}
