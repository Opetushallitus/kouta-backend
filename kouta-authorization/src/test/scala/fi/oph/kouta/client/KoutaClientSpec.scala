package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaAuthorizationConfigFactory.Constants._
import fi.oph.kouta.mocks.ServiceMocks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

trait KoutaClientSpec extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {
  this: ServiceMocks =>

  val DEFAULT_TEMPLATE_FILE_PATH = "src/test/resources/dev-vars.yml"

  setupWithTemplate()

  def setupWithTemplate() = {
    logger.info(s"Setting up test template with with path ${DEFAULT_TEMPLATE_FILE_PATH}")
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, DEFAULT_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServiceMocking()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
  }

  override def afterEach {
    super.afterEach()
    clearServiceMocks()
  }
}
