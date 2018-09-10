import fi.oph.kouta.{KoutaBackendSwagger, SwaggerServlet}
import fi.oph.kouta.servlet.KomoServlet
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) = {
    super.init(context)

    implicit val swagger = new KoutaBackendSwagger

    context.mount(new KomoServlet(), "/komo", "komo")

    context.mount(new SwaggerServlet, "/swagger")

  }

  override def destroy(context: ServletContext) = {
    super.destroy(context)
  }
}
