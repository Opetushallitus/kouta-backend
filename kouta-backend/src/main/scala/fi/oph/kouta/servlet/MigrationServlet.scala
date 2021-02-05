package fi.oph.kouta.servlet

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.{CallerId, HttpClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Ajanjakso, Arkistoitu, Ataru, En, Fi, Haku, HakuMetadata, Hakukohde, HakukohdeMetadata, Hakulomaketyyppi, Julkaistu, Julkaisutila, Kieli, Kielistetty, Koulutus, Liite, LiitteenToimitusosoite, LiitteenToimitustapa, Lomake, Muu, Sv, Tallennettu, Toteutus, Valintakoe}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, KoulutusOid, Oid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.service.{HakuService, HakukohdeService, KoulutusService, MigrationService, ToteutusService, ValintaperusteService}
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.{NotFound, Ok}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.util.{Failure, Success, Try}

/**
 * Use from browser console (while logged in service):

 var f = async (url) => fetch(url,{method:"POST"}).then(r => r.json())

 * Migrate haku:

 var hakukohteet = await f("/kouta-backend/migration/haku/1.2.246.562.29.98666182252")

 * Migrate hakukohteet:

 for(index in hakukohteet) {console.log(await f("/kouta-backend/migration/hakukohde/" + hakukohteet[index]))}

 */
class MigrationServlet(migrationService: MigrationService,
                       koulutusService: KoulutusService,
                       toteutusService: ToteutusService,
                       hakuService: HakuService,
                       hakukohdeService: HakukohdeService,
                       valintaperusteService: ValintaperusteService) extends KoutaServlet {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  def this() = this(new MigrationService, KoulutusService, ToteutusService, HakuService, HakukohdeService, ValintaperusteService)

  def f(r: JValue, q:String) = r.extract[Map[String, JValue]].filterKeys(_.toLowerCase.contains(q.toLowerCase))

  def tryPutAndPost[A](obj: A, put: A => Oid, post: (A, Instant) => Boolean) = {
    Try(put(obj)) match {
      case Success(value) =>
        logger.info(s"Oid $value migrated as new!")
      case Failure(e) =>
        Try(post(obj, Instant.now())) match {
          case Success(value) =>
            logger.info(s"Oid $value migrated overriding old!")
          case Failure(ex) => {
            logger.error(s"Exception creating new!", e)
            logger.error(s"Exception overriding old!", ex)
            throw ex
          }
        }
    }
  }

  post("/haku/:hakuOid") {

    implicit val authenticated: Authenticated = authenticate()

    val hakuOid = HakuOid(params("hakuOid"))
    logger.warn(s"Migration begins for haku $hakuOid!")
    val result = parse(migrationService.fetch(urlProperties.url("tarjonta-service.haku.oid", hakuOid))) \ "result"
    val haku = migrationService.parseHakuFromResult(result)
    tryPutAndPost(haku, hakuService.put, hakuService.update)
    val hakukohdeOids = (result \ "hakukohdeOids").extract[List[String]]


    Ok(compact(render(hakukohdeOids)))
  }

  post("/hakukohde/:hakukohdeOid") {
    implicit val authenticated: Authenticated = authenticate()

    val hakukohdeOid = HakukohdeOid(params("hakukohdeOid"))
    logger.warn(s"Migration begins for hakukohde $hakukohdeOid!")
    val result = parse(migrationService.fetch(urlProperties.url("tarjonta-service.hakukohde.oid", hakukohdeOid))) \ "result"
    //val hakuOid = (result \ "hakuOid").extract[String]
    //val komotos = (result \ "koulutusmoduuliToteutusTarjoajatiedot").extract[Map[String, JObject]]
    val hakukohdeKoulutusOids = (result \ "hakukohdeKoulutusOids").extract[List[String]]
    val hakukohde = migrationService.parseHakukohdeFromResult(result)
    tryPutAndPost(hakukohde, hakukohdeService.put, hakukohdeService.update)
    for(koulutusOid <- hakukohdeKoulutusOids) {
      //val komotoOids = (komotos \ "tarjoajaOids").extract[List[String]]
      //komoAndKomotos += komoOid -> (komotoOids ++ komoAndKomotos.getOrElse(komoOid, List())).distinct
      //val koulutusOid = komoOid
      logger.warn(s"Migration begins for koulutus $koulutusOid!")
      val result = parse(migrationService.fetch(urlProperties.url("tarjonta-service.koulutus.oid", koulutusOid))) \ "result"
      val komoOid = (result \ "komoOid").extract[String]
      val komo = parse(migrationService.fetch(urlProperties.url("tarjonta-service.komo.oid", komoOid))) \ "result"
      val toteutus: Toteutus = migrationService.parseToteutusFromResult(result)
      val koulutus: Koulutus = migrationService.parseKoulutusFromResult(result, komo)
      tryPutAndPost(koulutus, koulutusService.put, koulutusService.update)
      tryPutAndPost(toteutus, toteutusService.put, toteutusService.update)
      //println(komoAndKomotos(komo))
    }
    Ok(hakukohdeOid)
  }

}
