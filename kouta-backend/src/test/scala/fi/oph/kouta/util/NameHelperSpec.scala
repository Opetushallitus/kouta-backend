package fi.oph.kouta.util
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids
import fi.oph.kouta.client.Henkilo
import fi.oph.kouta.domain._

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
      ) == Map(
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
      ) == Map(
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
      ) == Map(
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
      ) == Map(
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
    )
  )

  val koulutusMetadata = LukioKoulutusMetadata(
    opintojenLaajuusNumero = Some(40),
    opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1")
  )

  "generateToteutusDisplayName" should "generate Toteutus display name for lukio with yleislinja, painotus and erityinen koulutustehtävä" in {
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
      ToteutusEnrichmentSourceData(
        Map(),
        Seq(),
        TestOids.TestUserOid,
        isLukioToteutus = true,
        LukioToteutuksenMetatieto.painotukset ++ LukioToteutuksenMetatieto.erityisetKoulutustehtavat,
        LukioToteutuksenMetatieto.yleislinja,
        koulutusMetadata.opintojenLaajuusNumero
      ),
      toteutusKaannokset,
      koodiKaannokset
    )
    esitysnimi shouldMatchTo (
      Map(
        Fi -> "Lukio, 40 opintopistettä\nlukion painotus 1 fi, 40 opintopistettä\nlukio erityinen koulutustehtävä 1 fi, 40 opintopistettä",
        Sv -> "Gymnasium, 40 studiepoäng\nlukion painotus 1 sv, 40 studiepoäng\nlukio erityinen koulutustehtävä 1 sv, 40 studiepoäng"
      )
    )
  }

  it should "generate Toteutus display name for lukio with yleislinja only" in {
    val esitysnimi = NameHelper.generateLukioToteutusDisplayName(
      ToteutusEnrichmentSourceData(
        Map(),
        Seq(),
        TestOids.TestUserOid,
        isLukioToteutus = true,
        Seq(),
        LukioToteutuksenMetatieto.yleislinja,
        koulutusMetadata.opintojenLaajuusNumero
      ),
      toteutusKaannokset,
      koodiKaannokset
    )
    esitysnimi shouldMatchTo (
      Map(
        Fi -> "Lukio, 40 opintopistettä",
        Sv -> "Gymnasium, 40 studiepoäng",
        En -> "High school, 40 credits"
      )
    )
  }

  "generateMuokkaajanNimi" should "create nimi for display from a person's kutsumanimi and sukunimi" in {
    val henkilo = Henkilo(kutsumanimi = Some("Testi"), sukunimi = Some("Muokkaaja"), etunimet = Some("Testi Tyyppi"))
    assert(NameHelper.generateMuokkaajanNimi(henkilo) == "Testi Muokkaaja")
  }

  it should "use etunimet if kutsumanimi is not specified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = Some("Muokkaaja"), etunimet = Some("Testi Tyyppi"))
    assert(NameHelper.generateMuokkaajanNimi(henkilo) == "Testi Tyyppi Muokkaaja")
  }

  it should "use sukunimi only if kutsumanimi and etunimet are not specified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = Some("Muokkaaja"), etunimet = None)
    assert(NameHelper.generateMuokkaajanNimi(henkilo) == "Muokkaaja")
  }

  it should "return empty string if kutsumanimi, etunimet and sukunimi are all unspecified" in {
    val henkilo = Henkilo(kutsumanimi = None, sukunimi = None, etunimet = None)
    assert(NameHelper.generateMuokkaajanNimi(henkilo) == "")
  }

  "mergeNames" should "merge names from kielistetty to another" in {
    val from = Map(Fi -> "suomi", Sv -> "ruotsi", En -> "englanti")
    assert(NameHelper.mergeNames(from, Map(), Seq(Fi, Sv, En)) == from)
  }

  it should "ignore null values" in {
    val from = Map(Fi -> "suomi", Sv -> "ruotsi", En -> null)
    assert(
      NameHelper.mergeNames(from, Map(Fi -> "", Sv -> null, En -> null), Seq(Fi, Sv, En)) == Map(
        Fi -> "suomi",
        Sv -> "ruotsi"
      )
    )
  }

  it should "merge only selected languauges" in {
    val from     = Map(Fi -> "suomi", Sv -> "ruotsi", En -> "englanti")
    val expected = Map(Fi -> "suomi", Sv -> "ruotsi")
    assert(NameHelper.mergeNames(from, Map(), Seq(Fi, Sv)) == expected)
  }

  it should "preserve existing language versions" in {
    val from     = Map(Fi -> "suomi", Sv -> "ruotsi")
    val target   = Map(Fi -> "suomeksi", Sv -> "", En -> "englanti")
    val expected = Map(Fi -> "suomeksi", Sv -> "ruotsi")
    assert(NameHelper.mergeNames(from, target, Seq(Fi, Sv)) == expected)
    assert(NameHelper.mergeNames(from, target - Sv, Seq(Fi, Sv)) == expected)
  }

  "notFullyPopulated" should "find missing language versions" in {
    assert(NameHelper.notFullyPopulated(Map[Kieli, String](), Seq(Fi, Sv)))
    assert(NameHelper.notFullyPopulated(Map(Fi -> "suomeksi"), Seq(Fi, Sv)))
    assert(NameHelper.notFullyPopulated(Map(Fi -> "suomeksi", Sv -> null), Seq(Fi, Sv)))
    assert(!NameHelper.notFullyPopulated(Map(Fi -> "suomeksi", Sv -> "Ruåtsiksi"), Seq(Fi, Sv)))
  }

  "concatAsEntityName" should "return only first part of name without separator because end part is empty" in {
    val first = Map(En -> "Osaamismerkki en", Fi -> "Osaamismerkki fi", Sv -> "Osaamismerkki sv")
    val expected = Map(En -> "Osaamismerkki en", Fi -> "Osaamismerkki fi", Sv -> "Osaamismerkki sv")
    assert(NameHelper.concatAsEntityName(first, Some(":"), Map(), Seq(Fi, En, Sv)) == expected)
  }

  it should "return only Fi name as Fi is the default selected language for the koulutus" in {
    val first = Map(En -> "Osaamismerkki en", Fi -> "Osaamismerkki fi", Sv -> "Osaamismerkki sv")
    val expected = Map(Fi -> "Osaamismerkki fi")
    assert(NameHelper.concatAsEntityName(first, Some(":")) == expected)
  }

  it should "return start and end parts concatenated and separated with colon for the selected languages" in {
    val start = Map(Sv -> "Osaamismerkki sv", En -> "Osaamismerkki en", Fi -> "Osaamismerkki fi")
    val end = Map(En -> "Nimi en", Fi -> "Nimi fi", Sv -> "Nimi sv")
    val expected = Map(Fi -> "Osaamismerkki fi: Nimi fi", Sv -> "Osaamismerkki sv: Nimi sv")
    assert(NameHelper.concatAsEntityName(start, Some(":"), end, Seq(Fi, Sv)) == expected)
  }

  it should "default to finnish language name if selected language does not have a translation for osaamismerkki" in {
    val start = Map(Sv -> "Osaamismerkki sv", Fi -> "Osaamismerkki fi")
    val end = Map(Fi -> "Nimi fi", Sv -> "Nimi sv")
    val expected = Map(En -> "Osaamismerkki fi: Nimi fi")
    assert(NameHelper.concatAsEntityName(start, Some(":"), end, Seq(En)) == expected)
  }
}
