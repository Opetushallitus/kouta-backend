package fi.oph.kouta.util
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import fi.oph.kouta.TestData.{JulkaistuHakukohde, JulkaistuYoToteutus, LukioKoulutus, LukioToteutuksenMetatieto, LukioToteutus, TelmaToteutuksenMetatieto, TelmaToteutus, TuvaToteutuksenMetatieto, TuvaToteutus, YoKoulutus}
import fi.oph.kouta.domain.{En, Fi, Hakukohde, Kielistetty, Koulutus, LukioToteutusMetadata, Sv, TelmaToteutusMetadata, Toteutus, TuvaToteutusMetadata, toteutus}
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.generic.auto._

class NameHelperSpec extends UnitSpec {
  val tuvaToteutus: Toteutus                        = TuvaToteutus
  val tuvaToteutuksenMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto
  val hakukohde: Hakukohde                          = JulkaistuHakukohde
  val kaannokset: Kielistetty = Map(
    Fi -> "vaativana erityisenä tukena",
    Sv -> "krävande särskilt stöd",
    En -> "demanding special support"
  )

  "generateHakukohdeDisplayName" should "return Hakukohteen nimi as it is for TELMA" in {
    val telmaToteutuksenMetadata: TelmaToteutusMetadata = TelmaToteutuksenMetatieto
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohdeNimi =
          Map(
            Fi -> "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)",
            Sv -> "Utbildning som handleder för arbete och ett självständigt liv (TELMA)"
          ),
        telmaToteutuksenMetadata,
        kaannokset
      ) === Map(
        Fi -> "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)",
        Sv -> "Utbildning som handleder för arbete och ett självständigt liv (TELMA)"
      )
    )
  }

  it should "return TUVA hakukohteen nimi without erityisopetus" in {
    val tuvaToMetadataWithoutErityisopetus = tuvaToteutuksenMetadata.copy(jarjestetaanErityisopetuksena = false)
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohdeNimi =
          Map(
            Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
            Sv -> "Utbildning som handleder för examensutbildning (Hux)"
          ),
        tuvaToMetadataWithoutErityisopetus,
        kaannokset
      ) === Map(
        Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
        Sv -> "Utbildning som handleder för examensutbildning (Hux)"
      )
    )
  }

  it should "default to Finnish translation if a translation is missing" in {
    val kaannokset: Kielistetty = Map(
      Fi -> "vaativana erityisenä tukena",
    )
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohdeNimi =
          Map(
            Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
            Sv -> "Utbildning som handleder för examensutbildning (Hux)"
          ),
        tuvaToteutuksenMetadata,
        kaannokset
      ) === Map(
        Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA) (vaativana erityisenä tukena)",
        Sv -> "Utbildning som handleder för examensutbildning (Hux) (vaativana erityisenä tukena)"
      )
    )
  }

  it should "leave erityisopetus postfix out if there are no translations for it" in {
    val kaannokset: Kielistetty = Map()
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohdeNimi =
          Map(
            Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
            Sv -> "Utbildning som handleder för examensutbildning (Hux)"
          ),
        tuvaToteutuksenMetadata,
        kaannokset
      ) === Map(
        Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
        Sv -> "Utbildning som handleder för examensutbildning (Hux)"
      )
    )
  }
}
