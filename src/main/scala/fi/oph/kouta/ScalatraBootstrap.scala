import fi.oph.kouta.config.{KoutaConfiguration, KoutaConfigurationFactory}
import fi.oph.kouta.repository.KoutaDatabase
import fi.oph.kouta.{KoutaBackendSwagger, SwaggerServlet}
import fi.oph.kouta.servlet.{HealthcheckServlet, KoulutusServlet}
import fi.vm.sade.utils.slf4j.Logging
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with Logging {

  override def init(context: ServletContext) = {
    super.init(context)

    KoutaConfigurationFactory.init()
    KoutaDatabase.init()

    implicit val swagger = new KoutaBackendSwagger

    context.mount(new HealthcheckServlet(), "/healthcheck", "healthcheck")
    context.mount(new KoulutusServlet(), "/komo", "komo")

    context.mount(new SwaggerServlet, "/swagger")
  }

  override def destroy(context: ServletContext) = {
    super.destroy(context)
    KoutaDatabase.destroy()
  }
}
