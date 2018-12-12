package fi.oph.kouta.client

import fi.oph.kouta.{ServiceMocks, TestSetups}
import org.scalatest.{BeforeAndAfterEach, DoNotDiscover}
import org.scalatra.test.scalatest.ScalatraFlatSpec


@DoNotDiscover
class KoutaClientSpec extends ScalatraFlatSpec with BeforeAndAfterEach {

  TestSetups.setupWithTemplate(1234)

  override def afterAll() = {
    super.afterAll()
    ServiceMocks.stopServiceMocks()
  }

  override def afterEach {
    super.afterEach()
    ServiceMocks.resetServiceMocks()
  }
}
