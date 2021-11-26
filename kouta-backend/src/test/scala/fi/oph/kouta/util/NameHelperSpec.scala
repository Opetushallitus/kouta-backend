package fi.oph.kouta.util
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import fi.oph.kouta.TestData.{JulkaistuHakukohde, LukioKoulutus, LukioToteutuksenMetatieto, LukioToteutus, TelmaToteutuksenMetatieto, TuvaToteutuksenMetatieto, TuvaToteutus}
import fi.oph.kouta.domain.{En, Fi, Hakukohde, Kielistetty, Koulutus, Sv, Toteutus, TelmaToteutusMetadata, TuvaToteutusMetadata}
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.generic.auto._

class NameHelperSpec extends UnitSpec {
  val tuvaToteutus: Toteutus                        = TuvaToteutus
  val tuvaToteutuksenMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto
  val hakukohde: Hakukohde                          = JulkaistuHakukohde
  val hakukohdeKaannokset: Kielistetty = Map(
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
        hakukohdeKaannokset
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
        hakukohdeKaannokset
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
  val toteutusKaannokset = Map(
    "toteutuslomake.lukionYleislinjaNimiOsa" -> Map(Fi -> "Lukio", Sv -> "Gymnasium"),
    "yleiset.opintopistetta" -> Map(Fi -> "opintopistettä", Sv -> "studiepoäng", En -> "credits")
  )

  val koodiKaannokset = Map(
    "lukiopainotukset_1#1" -> Map(Fi -> "lukion painotus 1 fi", Sv -> "lukion painotus 1 sv"),
    "lukiolinjaterityinenkoulutustehtava_1#1" -> Map(Fi -> "lukio erityinen koulutustehtävä 1 fi", Sv -> "lukio erityinen koulutustehtävä 1 sv"),
    "opintojenlaajuus_40#1" -> Map(Fi -> "40", Sv -> "40"),
  )

  "generateToteutusDisplayName" should "generate Toteutus display name for lukio with yleislinja, painotus and erityinen koulutustehtävä" in {
    val toteutus: Toteutus = LukioToteutus
    val koulutus: Koulutus = LukioKoulutus
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
        toteutus,
        koulutus,
        toteutusKaannokset,
        koodiKaannokset
    )
    esitysnimi should matchTo(Map(
        Fi -> "Lukio, 40 opintopistettä\nlukion painotus 1 fi, 40 opintopistettä\nlukio erityinen koulutustehtävä 1 fi, 40 opintopistettä",
        Sv -> "Gymnasium, 40 studiepoäng\nlukion painotus 1 sv, 40 studiepoäng\nlukio erityinen koulutustehtävä 1 sv, 40 studiepoäng",
      ))
  }

  it should "generate Toteutus display name for lukio with yleislinja only" in {
    val koulutus: Koulutus = LukioKoulutus
    val toteutus: Toteutus = LukioToteutus.copy(metadata = Some(LukioToteutuksenMetatieto.copy(painotukset = Seq(), erityisetKoulutustehtavat = Seq())))
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
      toteutus,
      koulutus,
      toteutusKaannokset,
      koodiKaannokset
    )
    esitysnimi should matchTo(Map(
      Fi -> "Lukio, 40 opintopistettä",
      Sv -> "Gymnasium, 40 studiepoäng",
    ))
  }

}
