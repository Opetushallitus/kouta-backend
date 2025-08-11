import fi.oph.kouta.SwaggerServlet
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.repository.KoutaDatabase
import fi.oph.kouta.scheduler.SchedulerConfig
import fi.oph.kouta.servlet._
import fi.oph.kouta.logging.Logging
import org.scalatra._

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with Logging {

  override def init(context: ServletContext): Unit = {
    super.init(context)

    KoutaConfigurationFactory.init()
    KoutaDatabase.init()
    AuditLog.init()

    context.mount(new AuthServlet(), "/auth", "auth")

    context.mount(new HealthcheckServlet(), "/healthcheck", "healthcheck")
    context.mount(new KoulutusServlet(), "/koulutus", "koulutus")
    context.mount(new ToteutusServlet(), "/toteutus", "toteutus")
    context.mount(new HakuServlet(), "/haku", "haku")
    context.mount(new HakukohdeServlet(), "/hakukohde", "hakukohde")
    context.mount(new ValintaperusteServlet(), "/valintaperuste", "valintaperuste")
    context.mount(new SorakuvausServlet(), "/sorakuvaus", "sorakuvaus")
    context.mount(new AsiasanaServlet(), "/asiasana", "asiasana")
    context.mount(new AmmattinimikeServlet(), "/ammattinimike", "ammattinimike")
    context.mount(new LuokittelutermiServlet(), "/luokittelutermi", "luokittelutermi")
    context.mount(new OppilaitosServlet(), "/oppilaitos", "oppilaitos")
    context.mount(new OppilaitoksenOsaServlet(), "/oppilaitoksen-osa", "oppilaitoksen-osa")
    context.mount(new UploadServlet(), "/upload", "upload")
    context.mount(new SearchServlet(), "/search", "search")
    context.mount(new IndexerServlet(), "/indexer", "indexer")
    context.mount(new ExternalServlet(), "/external", "external")
    context.mount(new ArkistointiServlet(), "/archiver","archiver")
    context.mount(new OrganisaatioServlet(), "/organisaatio", "organisaatio")
    context.mount(new KoodistoServlet(), "/koodisto", "koodisto")
    context.mount(new SiirtotiedostoServlet(), "/siirtotiedosto", "siirtotiedosto")
    context.mount(new SwaggerServlet, "/swagger")

    val schedulerConfig = new SchedulerConfig
    schedulerConfig.startScheduler()
  }

  override def destroy(context: ServletContext): Unit = {
    super.destroy(context)
    KoutaDatabase.destroy()
  }
}
