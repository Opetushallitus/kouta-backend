package fi.oph.kouta.util
import com.softwaremill.diffx.scalatest.DiffMatcher.matchTo
import fi.oph.kouta.TestData.{JulkaistuHakukohde, LukioToteutuksenMetatieto, TelmaToteutuksenMetatieto, TuvaToteutuksenMetatieto, TuvaToteutus}
import fi.oph.kouta.domain.{En, Fi, Hakukohde, Kieli, Kielistetty, LukioKoulutusMetadata, Sv, TelmaToteutusMetadata, Toteutus, TuvaToteutusMetadata}
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.generic.auto._
import fi.oph.kouta.client.Henkilo

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
        hakukohdeNimi = Map(
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
        hakukohdeNimi = Map(
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
      Fi -> "vaativana erityisenä tukena"
    )
    assert(
      NameHelper.generateHakukohdeDisplayNameForTuva(
        hakukohdeNimi = Map(
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
        hakukohdeNimi = Map(
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
    "toteutuslomake.lukionYleislinjaNimiOsa" -> Map(Fi -> "Lukio", Sv -> "Gymnasium", En -> "High school"),
    "yleiset.opintopistetta"                 -> Map(Fi -> "opintopistettä", Sv -> "studiepoäng", En -> "credits")
  )

  val koodiKaannokset = Map(
    "lukiopainotukset_1" -> Map(Fi -> "lukion painotus 1 fi", Sv -> "lukion painotus 1 sv"),
    "lukiolinjaterityinenkoulutustehtava_1" -> Map(
      Fi -> "lukio erityinen koulutustehtävä 1 fi",
      Sv -> "lukio erityinen koulutustehtävä 1 sv"
    ),
    "opintojenlaajuus_40#1" -> Map(Fi -> "40", Sv -> "40")
  )

  val koulutusMetadata = LukioKoulutusMetadata(
      opintojenLaajuusNumero = Some(40),
      opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1")
    )


  "generateToteutusDisplayName" should "generate Toteutus display name for lukio with yleislinja, painotus and erityinen koulutustehtävä" in {
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
      toteutusMetadata = LukioToteutuksenMetatieto,
      koulutusMetadata,
      toteutusKaannokset,
      koodiKaannokset
    )
    esitysnimi should matchTo(
      Map(
        Fi -> "Lukio, 40 opintopistettä\nlukion painotus 1 fi, 40 opintopistettä\nlukio erityinen koulutustehtävä 1 fi, 40 opintopistettä",
        Sv -> "Gymnasium, 40 studiepoäng\nlukion painotus 1 sv, 40 studiepoäng\nlukio erityinen koulutustehtävä 1 sv, 40 studiepoäng"
      )
    )
  }

  it should "generate Toteutus display name for lukio with yleislinja only" in {
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
      toteutusMetadata = LukioToteutuksenMetatieto.copy(painotukset = List(), erityisetKoulutustehtavat = List()),
      koulutusMetadata,
      toteutusKaannokset,
      koodiKaannokset
    )
    esitysnimi should matchTo(
      Map(
        Fi -> "Lukio, 40 opintopistettä",
        Sv -> "Gymnasium, 40 studiepoäng",
        En -> "High school, 40 credits"
      )
    )
  }

  "generateMuokkaajanNimi" should "create nimi for display from a person's kutsumanimi and sukunimi" in {
    val henkilo = Henkilo(kutsumanimi = Some("Testi"), sukunimi = Some("Muokkaaja"), etunimet = Some("Testi Tyyppi"))
    assert(NameHelper.generateMuokkaajanNimi(henkilo) === "Testi Muokkaaja")
  }

  it should "use etunimet if kutsumanimi is not specified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = Some("Muokkaaja"), etunimet = Some("Testi Tyyppi"))
    assert(NameHelper.generateMuokkaajanNimi(henkilo) === "Testi Tyyppi Muokkaaja")
  }

  it should "use sukunimi only if kutsumanimi and etunimet are not specified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = Some("Muokkaaja"), etunimet = None)
    assert(NameHelper.generateMuokkaajanNimi(henkilo) === "Muokkaaja")
  }

  it should "return empty string if kutsumanimi, etunimet and sukunimi are all unspecified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = None, etunimet = None)
    assert(NameHelper.generateMuokkaajanNimi(henkilo) === "")
  }

  "mergeNames" should "merge names from kielistetty to another" in {
    val from = Map(Fi -> "suomi", Sv -> "ruotsi", En -> "englanti")
    assert(NameHelper.mergeNames(from, Map(), Seq(Fi, Sv, En)) === from)
  }

  it should "merge only selected languauges" in {
    val from = Map(Fi -> "suomi", Sv -> "ruotsi", En -> "englanti")
    val expected = Map(Fi -> "suomi", Sv -> "ruotsi")
    assert(NameHelper.mergeNames(from, Map(), Seq(Fi, Sv)) === expected)
  }

  it should "preserve existing language versions" in {
    val from = Map(Fi -> "suomi", Sv -> "ruotsi")
    val target = Map(Fi -> "suomeksi", Sv -> "", En -> "englanti")
    val expected = Map(Fi -> "suomeksi", Sv -> "ruotsi")
    assert(NameHelper.mergeNames(from, target, Seq(Fi, Sv)) === expected)
    assert(NameHelper.mergeNames(from, target - Sv, Seq(Fi, Sv)) === expected)
  }

  "notFullyPopulated" should "find missing language versions" in {
    assert(NameHelper.notFullyPopulated(Map[Kieli, String](), Seq(Fi, Sv)))
    assert(NameHelper.notFullyPopulated(Map(Fi -> "suomeksi"), Seq(Fi, Sv)))
    assert(!NameHelper.notFullyPopulated(Map(Fi -> "suomeksi", Sv -> "Ruåtsiksi"), Seq(Fi, Sv)))
  }
}
