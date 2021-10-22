package fi.oph.kouta.util
import fi.oph.kouta.TestData.{JulkaistuHakukohde, TelmaToteutus, TuvaToteutuksenMetatieto, TuvaToteutus}
import fi.oph.kouta.domain.{En, Fi, Hakukohde, Kielistetty, Sv, Toteutus, TuvaToteutusMetadata}

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
    val telmaToteutus: Toteutus = TelmaToteutus
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohde.copy(nimi =
          Map(
            Fi -> "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)",
            Sv -> "Utbildning som handleder för arbete och ett självständigt liv (TELMA)"
          )
        ),
        telmaToteutus,
        kaannokset
      ) === Map(
        Fi -> "Työhön ja itsenäiseen elämään valmentava koulutus (TELMA)",
        Sv -> "Utbildning som handleder för arbete och ett självständigt liv (TELMA)"
      )
    )
  }

  it should "return TUVA hakukohteen nimi without erityisopetus" in {
    val tuvaToMetadata                   = tuvaToteutuksenMetadata.copy(jarjestetaanErityisopetuksena = false)
    val tuvaToteutusWithoutErityisopetus = tuvaToteutus.copy(metadata = Some(tuvaToMetadata))
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohde.copy(nimi =
          Map(
            Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
            Sv -> "Utbildning som handleder för examensutbildning (Hux)"
          )
        ),
        tuvaToteutusWithoutErityisopetus,
        kaannokset
      ) === Map(
        Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
        Sv -> "Utbildning som handleder för examensutbildning (Hux)"
      )
    )
  }

  it should "return TUVA hakukohteen nimi with erityisopetus" in {
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohde.copy(nimi =
          Map(
            Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA)",
            Sv -> "Utbildning som handleder för examensutbildning (Hux)"
          )
        ),
        tuvaToteutus,
        kaannokset
      ) === Map(
        Fi -> "Tutkintokoulutukseen valmentava koulutus (TUVA) (vaativana erityisenä tukena)",
        Sv -> "Utbildning som handleder för examensutbildning (Hux) (krävande särskilt stöd)"
      )
    )
  }
}
