package fi.oph.kouta.util

import fi.oph.kouta.TestData.{TelmaToteutus, TuvaToteutuksenMetatieto, TuvaToteutus}
import fi.oph.kouta.domain.{Toteutus, TuvaToteutusMetadata}

class HakukohdeServiceUtilSpec extends UnitSpec {
  val tuvaToteutus: Toteutus                        = TuvaToteutus
  val tuvaToteutuksenMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto

  "getJarjestetaanErityisopetuksena" should "return true for TUVA without erityisopetus" in {
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToteutus).contains(true))
  }

  it should "return false for TUVA without erityisopetus" in {
    val tuvaToMetadata                   = tuvaToteutuksenMetadata.copy(jarjestetaanErityisopetuksena = false)
    val tuvaToteutusWithoutErityisopetus = tuvaToteutus.copy(metadata = Some(tuvaToMetadata))
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToteutusWithoutErityisopetus).contains(false))
  }

  it should "return None for TELMA toteutus" in {
    val telma = TelmaToteutus
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(telma).isEmpty)
  }
}
