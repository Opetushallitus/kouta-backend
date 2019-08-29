import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.repository.KoutaDatabase
import fi.oph.kouta.security.CasSessionService
import fi.oph.kouta.servlet._
import fi.oph.kouta.{SwaggerServlet}
import fi.vm.sade.utils.slf4j.Logging
import javax.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle with Logging {

  override def init(context: ServletContext): Unit = {
    super.init(context)

    KoutaConfigurationFactory.init()
    KoutaDatabase.init()

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

    context.mount(new AnythingServlet(), "/anything", "anything")
    context.mount(new SwaggerServlet, "/swagger")
  }

  override def destroy(context: ServletContext): Unit = {
    super.destroy(context)
    KoutaDatabase.destroy()
  }
}
