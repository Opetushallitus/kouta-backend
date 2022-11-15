package fi.oph.kouta.mocks

import fi.vm.sade.properties.OphProperties
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatra.test.scalatest.ScalatraFlatSpec

object TestGlobals {
  var urlProperties: Option[OphProperties] = None
}

trait UrlProperties {
  def urlProperties: Option[OphProperties] = TestGlobals.urlProperties match {
      case Some(_) => TestGlobals.urlProperties
      case _ => throw new Error("Trying to read urlProperties before it's set!")
    }
  def setUrlProperties(newUrlProperties: OphProperties) = {
    TestGlobals.urlProperties = Some(newUrlProperties)
  }
}

trait SpecWithMocks
    extends ScalatraFlatSpec
    with UrlProperties
    with BeforeAndAfterEach
    with BeforeAndAfterAll {
  val mocker = ServiceMocker

  override def beforeAll(): Unit = {
    super.beforeAll()
    val hostVirkailijaPort = urlProperties.get.getProperty("host.virkailija").split(":").last.toInt
    mocker.startServiceMocking(hostVirkailijaPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    mocker.stopServiceMocking()
  }
}
