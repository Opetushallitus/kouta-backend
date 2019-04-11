package fi.oph.kouta.client

import fi.oph.kouta.{ServiceMocks, TestSetups}
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFlatSpec

trait KoutaClientSpec extends ScalatraFlatSpec with BeforeAndAfterEach { this: ServiceMocks =>

  TestSetups.setupWithTemplate(1234)

  override def beforeAll() = {
    super.beforeAll()
    startServiceMocking()
  }

  override def afterAll() = {
    super.afterAll()
    stopServiceMocking()
  }

  override def afterEach {
    super.afterEach()
    clearServiceMocks()
  }
}
