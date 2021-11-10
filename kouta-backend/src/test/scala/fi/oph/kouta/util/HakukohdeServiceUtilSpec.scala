package fi.oph.kouta.util

import fi.oph.kouta.TestData.{TelmaToteutuksenMetatieto, TuvaToteutuksenMetatieto}
import fi.oph.kouta.domain.{TuvaToteutusMetadata}

class HakukohdeServiceUtilSpec extends UnitSpec {
  val tuvaToteutuksenMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto

  "getJarjestetaanErityisopetuksena" should "return true for TUVA without erityisopetus" in {
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToteutuksenMetadata).contains(true))
  }

  it should "return false for TUVA without erityisopetus" in {
    val tuvaToMetadataWithoutErityisopetus                   = tuvaToteutuksenMetadata.copy(jarjestetaanErityisopetuksena = false)
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToMetadataWithoutErityisopetus).contains(false))
  }

  it should "return None for TELMA toteutus" in {
    val telma = TelmaToteutuksenMetatieto
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(telma).isEmpty)
  }
}
