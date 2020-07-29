package fi.oph.kouta.domain.oid

import fi.vm.sade.utils.slf4j.Logging
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class OidSpec extends AnyFlatSpec with Matchers with Logging {

  it should "Pass" in {
    KoulutusOid.apply("1.2.246.562.13.444").isValid shouldEqual true
  }

}
