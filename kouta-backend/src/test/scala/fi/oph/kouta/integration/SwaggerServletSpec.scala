package fi.oph.kouta.integration
import fi.oph.kouta.SwaggerServlet
import fi.oph.kouta.integration.fixture.{AuthFixture, KeywordFixture, UploadFixture}
import fi.oph.kouta.servlet.HealthcheckServlet
import org.scalatra.test.scalatest.ScalatraFlatSpec
import io.swagger.v3.parser.OpenAPIV3Parser

class SwaggerServletSpec
    extends ScalatraFlatSpec
    with EverythingFixture
    with AuthFixture
    with IndexerFixture
    with SearchFixture
    with KeywordFixture
    with UploadFixture {
  addServlet(new HealthcheckServlet(), "/healthcheck")
  addServlet(new SwaggerServlet(), "/swagger")

  "Swagger" should "have valid spec" in {
    get("/swagger/swagger.yaml") {
      val result  = new OpenAPIV3Parser().readContents(body, null, null)
      val openApi = result.getOpenAPI()
      openApi should not equal (null)     // Parsimisen pitää onnistua (on validia YML:ää)
      result.getMessages() shouldBe empty // Ei virheitä tai varoituksia swaggerin parsinnasta
    }
  }
}
