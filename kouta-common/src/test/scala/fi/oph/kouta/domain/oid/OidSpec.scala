package fi.oph.kouta.domain.oid

import fi.vm.sade.utils.slf4j.Logging
import org.scalatest.{FlatSpec, Matchers}

class OidSpec extends FlatSpec with Matchers with Logging {

  it should "Pass" in {
    KoulutusOid.apply("1.2.246.562.13.444").isValid shouldEqual true
  }

}
