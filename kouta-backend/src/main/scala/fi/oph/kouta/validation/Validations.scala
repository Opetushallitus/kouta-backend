package fi.oph.kouta.validation

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.regex.Pattern
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{Oid, OrganisaatioOid, ToteutusOid}
import fi.vm.sade.utils.slf4j.Logging
import org.apache.commons.validator.routines.{EmailValidator, UrlValidator}

object Validations extends Logging {
  private val urlValidator   = new UrlValidator(Array("http", "https"))
  private val emailValidator = EmailValidator.getInstance(false, false)

  def error(path: String, msg: ErrorMessage): IsValid = List(ValidationError(path, msg))

  def and(validations: IsValid*): IsValid          = validations.flatten.distinct
  def or(first: IsValid, second: IsValid): IsValid = if (first.isEmpty) second else first

  def validationMsg(value: String): ErrorMessage = ErrorMessage(msg = s"'$value' ei ole validi", id = "validationMsg")
  val notEmptyMsg: ErrorMessage                  = ErrorMessage(msg = s"Ei saa sisältää arvoa", id = "notEmptyMsg")
  val missingMsg: ErrorMessage                   = ErrorMessage(msg = s"Pakollinen tieto puuttuu", id = "missingMsg")
  val notNegativeMsg: ErrorMessage               = ErrorMessage(msg = s"ei voi olla negatiivinen", id = "notNegativeMsg")
  val tooManyKoodiUris: ErrorMessage =
    ErrorMessage(msg = s"Ainoastaan korkeakoulutuksella voi olla useampi kuin yksi koulutus", id = "tooManyKoodiUris")
  val withoutLukiolinja: ErrorMessage =
    ErrorMessage(msg = "Lukio-toteutuksella täytyy olla vähintään yleislinja", id = "withoutLukiolinja")
  val illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi = ErrorMessage(
    msg =
      "Hakukohteelle ei voi valita erillistä hakulomaketta. Erillisen lomakkeen voi valita vain jos haussa on käytössä 'muu'-tyyppinen hakulomake.",
    id = "illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi"
  )
  val toinenAsteOnkoKaksoistutkintoNotAllowed = ErrorMessage(
    msg =
      "Hakukohteelle ei voi valita kaksoistutkinnon suorittamista, kaksoistutkinto on mahdollinen vain jos kyseessä on ammatillinen perustutkinto tai lukiokoulutus (joko lukion oppimäärä tai ylioppilastutkinto, koulutuskoodi 309902 tai 301101).",
    id = "toinenAsteOnkoKaksoistutkintoNotAllowed"
  )
  def invalidKoulutuskoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutuskoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKoulutuskoodiuri"
  )

  def invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena(koulutustyyppiKoodi: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutuksen koulutustyyppi $koulutustyyppiKoodi on virheellinen, koulutustyyppillä täytyy olla koodistorelaatio koulutustyyppi_1:een että se voidaan järjestää erityisopetuksena",
    id = "invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena"
  )

  def invalidLisatietoOtsikkoKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lisätieto-otsikkokoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidLisatietoOtsikkoKoodiuri"
  )
  def invalidKoulutusAlaKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutusalakoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKoulutusAlaKoodiuri"
  )
  def invalidOpintojenLaajuusyksikkoKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutukselle valittua opintojenlaajuusyksikko-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpintojenLaajuusyksikkoKoodiuri"
  )

  def invalidKoulutusOpintojenLaajuusyksikkoIntegrity(koodiUri: String, toteutukset: Seq[ToteutusOid]): ErrorMessage = ErrorMessage(
    msg = s"Ainakin yhdellä Koulutukseen liitetyllä toteutuksella on eri opintojenlaajuusyksikko-koodiUri kuin koulutuksella ($koodiUri).",
    id = "invalidKoulutusOpintojenLaajuusyksikkoIntegrity",
    meta = Some(Map("toteutukset" -> toteutukset))
  )

  def invalidKoulutusOpintojenLaajuusNumeroIntegrity(laajuusMin: Double, laajuusMax: Double, toteutukset: Seq[ToteutusOid]): ErrorMessage = ErrorMessage(
    msg = s"Ainakin yhdellä koulutukseen liitetyllä julkaistulla toteutuksella opintojen laajuus ei ole koulutuksella määritellyllä välillä ($laajuusMin - $laajuusMax)",
    id = "invalidKoulutusOpintojenLaajuusNumeroIntegrity",
    meta = Some(Map("toteutukset" -> toteutukset))
  )

  def invalidToteutusOpintojenLaajuusyksikkoIntegrity(koulutusLaajuusyksikkoKoodiUri: Option[String], toteutusLaajuusyksikkoKoodiUri: Option[String]) = {
    ErrorMessage(
      msg = s"Toteutuksella on eri opintojen laajuusyksikkö (${toteutusLaajuusyksikkoKoodiUri.getOrElse("-")}) kuin koulutuksella (${koulutusLaajuusyksikkoKoodiUri.getOrElse("-")})",
      id = "invalidToteutusOpintojenLaajuusyksikkoIntegrity"
    )
  }

  def invalidKieliKoodiUri(kieliField: String, koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lukiototeutukselle valittua $kieliField-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidKieliKoodiUri"
  )
  def invalidLukioLinjaKoodiUri(linjaField: String, koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lukiototeutukselle valittua $linjaField-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidLukioLinjaKoodiUri"
  )
  def unknownTarjoajaOid(oid: OrganisaatioOid): ErrorMessage = ErrorMessage(
    msg = s"Tarjoaja-organisaatiota oid:illa $oid ei löydy, tai organisaatio ei ole aktiivinen",
    id = "unknownTarjoajaOid"
  )
  def invalidEPerusteId(ePerusteId: Long): ErrorMessage = ErrorMessage(
    msg = s"EPerustetta id:llä $ePerusteId ei löydy, tai EPeruste ei ole voimassa",
    id = "invalidEPerusteId"
  )
  def invalidEPerusteIdForKoulutusKoodiUri(ePerusteId: Long, koodiUri: String): ErrorMessage =
    ErrorMessage(
      msg = s"Valittu koulutuskoodiuri ($koodiUri) ei ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidEPerusteIdForKoulutus"
    )
  def invalidTutkinnonOsaViiteForEPeruste(ePerusteId: Long, tutkinnonOsaViite: Long): ErrorMessage =
    ErrorMessage(
      msg = s"Tutkinnonosa-viite $tutkinnonOsaViite ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidTukinnonosaViiteForEPeruste"
    )
  def invalidTutkinnonOsaIdForEPeruste(ePerusteId: Long, tutkinnonOsaId: Long): ErrorMessage =
    ErrorMessage(
      msg = s"TutkinnonosaID $tutkinnonOsaId ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidTukinnonosaIdForEPeruste"
    )
  def invalidOsaamisalaForEPeruste(ePerusteId: Long, osaamisalaKoodiUri: String): ErrorMessage =
    ErrorMessage(
      msg = s"Osaamisala $osaamisalaKoodiUri ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidOsaamisalaForEPeruste"
    )
  def invalidTutkintoNimikeKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Tutkintonimike-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidTutkintoNimikeKoodiuri"
  )
  def invalidOpintojenLaajuusKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opintojenlaajuus-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpintojenLaajuusKoodiuri"
  )
  def invalidOpetusKieliKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetuskieli-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusKieliKoodiUri"
  )
  def invalidOpetusAikaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetusaika-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusAikaKoodiUri"
  )
  def invalidOpetusTapaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetustapa-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusTapaKoodiUri"
  )
  def invalidOsaamisalaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Osaamisala-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOsaamisalaKoodiUri"
  )
  def invalidLukioDiplomiKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Diplomi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidLukioDiplomiKoodiUri"
  )
  def invalidOpetusLisatietoOtsikkoKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetuksen lisätiedon otsikkokoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpetusLisatietoOtsikkoKoodiuri"
  )
  def invalidKausiKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetuksen koulutuksenAlkamiskausi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKausiKoodiuri"
  )
  def invalidHakukohdeKooriuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Hakukohde-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidHakukohdeKooriuri"
  )
  def invalidPohjakoulutusVaatimusKooriuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Pohjakoulutusvaatimus-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidPohjakoulutusVaatimusKooriuri"
  )
  def invalidLiitetyyppiKooriuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Liitetyyppi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidLiitetyyppiKooriuri"
  )
  def invalidValintakoeTyyppiKooriuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Valintakoetyyppi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidValintakoeTyyppiKooriuri"
  )
  def invalidOppiaineKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Oppiaine-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOppiaineKoodiuri"
  )
  def invalidOppiaineKieliKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Oppiainekieli-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOppiaineKieliKoodiuri"
  )
  def unknownValintaperusteenValintakoeIdForHakukohde(valintaperusteId: UUID, valintakoeId: UUID): ErrorMessage =
    ErrorMessage(
      msg = s"Valintakoetta ID:llä $valintakoeId ei löydy hakukohteen valintaperusteelle $valintaperusteId",
      id = "unknownValintaperusteenValintakoeIdForHakukohde"
    )
  def invalidPostiosoiteKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Postiosoite-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidPostiosoiteKoodiUri"
  )
  def unknownAtaruId(ataruId: UUID): ErrorMessage = ErrorMessage(
    msg = s"Hakulomaketta ID:llä $ataruId ei löydy, tai se on poistettu tai lukittu",
    id = "invalidAtaruId"
  )
  def lessOrEqualMsg(value: Long, comparedValue: Long): ErrorMessage =
    ErrorMessage(msg = s"$value saa olla pienempi kuin $comparedValue", id = "lessOrEqualMsg")

  def notInTheRangeMsg(min: Option[Double], max: Option[Double], givenValue: Option[Double]): ErrorMessage =
    ErrorMessage(msg = s"$givenValue ei ole välillä min $min - max $max", id = "notInTheRangeMsg")

  def invalidKielistetty(values: Seq[Kieli]): ErrorMessage = ErrorMessage(
    msg = s"Kielistetystä kentästä puuttuu arvo kielillä [${values.mkString(",")}]",
    id = "invalidKielistetty"
  )
  def invalidTutkintoonjohtavuus(tyyppi: String): ErrorMessage =
    ErrorMessage(msg = s"Koulutuksen tyypin $tyyppi pitäisi olla tutkintoon johtava", id = "invalidTutkintoonjohtavuus")
  def invalidUrl(url: String): ErrorMessage = ErrorMessage(msg = s"'$url' ei ole validi URL", id = "invalidUrl")
  def invalidEmail(email: String): ErrorMessage =
    ErrorMessage(msg = s"'$email' ei ole validi email", id = "invalidEmail")
  def invalidAjanjaksoMsg(ajanjakso: Ajanjakso): ErrorMessage =
    ErrorMessage(msg = s"${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen", id = "invalidAjanjaksoMsg")
  def pastDateMsg(date: LocalDateTime): ErrorMessage =
    ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def pastDateMsg(date: String): ErrorMessage = ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def minmaxMsg(minValue: Any, maxValue: Any): ErrorMessage =
    ErrorMessage(msg = s"$minValue on suurempi kuin $maxValue", id = "minmaxMsg")
  def notYetJulkaistu(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"$field ($id) ei ole vielä julkaistu", id = "notYetJulkaistu")
  def nonExistent(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"$field ($id) ei ole olemassa", id = "nonExistent")
  def notMissingMsg(value: Any): ErrorMessage =
    ErrorMessage(msg = s"Arvo $value ei saisi olla määritelty", id = "notMissingMsg")
  def tyyppiMismatch(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"Tyyppi ei vastaa $field ($id) tyyppiä", id = "tyyppiMismatch")
  def tyyppiMismatch(field1: String, id1: Any, field2: String, id2: Any): ErrorMessage =
    ErrorMessage(msg = s"$field1 ($id1) tyyppi ei vastaa $field2 ($id2) tyyppiä", id = "tyyppiMismatch")
  def cannotLinkToHakukohde(oid: String): ErrorMessage =
    ErrorMessage(msg = s"Toteutusta ($oid) ei voi liittää hakukohteeseen", id = "cannotLinkToHakukohde")
  def valuesDontMatch(relatedEntity: String, field: String): ErrorMessage =
    ErrorMessage(msg = s"$relatedEntity kenttä $field ei sisällä samoja arvoja", id = "valuesDontMatch")
  def oneNotBoth(field1: String, field2: String): ErrorMessage =
    ErrorMessage(msg = s"Tarvitaan joko $field1 tai $field2, mutta ei molempia.", id = "oneNotBoth")
  def noneOrOneNotBoth(field1: String, field2: String): ErrorMessage =
    ErrorMessage(msg = s"Voidaan valita joko $field1 tai $field2, mutta ei molempia.", id = "noneOrOneNotBoth")
  def notEmptyAlthoughOtherEmptyMsg(otherField: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska kentässä $otherField ei ole arvoa",
      id = "notEmptyAlthoughOtherEmptyMsg"
    )
  def notEmptyAlthoughBooleanFalseMsg(booleanField: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska $booleanField ei ole asetettu",
      id = "notEmptyAlthoughOtherEmptyMsg"
    )
  def notAllowedDueTo(reason: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska $reason",
      id = "notAllowedDueTo"
    )
  def illegalStateChange(entityDesc: String, oldState: Julkaisutila, newState: Julkaisutila): ErrorMessage =
    ErrorMessage(
      msg = s"Siirtyminen tilasta $oldState tilaan $newState ei ole sallittu $entityDesc",
      id = "illegalStateChange"
    )
  def illegalValueForFixedValueMsg(fixedValDesc: String): ErrorMessage = ErrorMessage(
    msg = s"Kentän täytyy sisältää tietty arvo: $fixedValDesc. Ko. arvo asetetaan automaattisesti jos kenttä on tyhjä",
    id = "illegalValueForFixedValueSeq"
  )
  def illegalValueForFixedValueSeqMsg(fixedValDesc: String): ErrorMessage = ErrorMessage(
    msg =
      s"Kentän täytyy sisältää täsmälleen yksi arvo: $fixedValDesc. Ko. arvo asetetaan automaattisesti jos kenttä on tyhjä",
    id = "illegalValueForFixedValueSeq"
  )
  def integrityViolationMsg(entityDesc: String, relatedEntity: String): ErrorMessage =
    ErrorMessage(msg = s"$entityDesc ei voi poistaa koska siihen on liitetty $relatedEntity", id = "integrityViolation")

  def invalidArkistointiDate(months: Int): ErrorMessage =
    ErrorMessage(msg = s"Arkistointipäivämäärän tulee olla vähintään $months kuukautta haun viimeisimmästä päättymispäivämäärästä.", id = "invalidArkistointiDate")

  val InvalidKoulutuspaivamaarat: ErrorMessage = ErrorMessage(
    msg = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen",
    id = "InvalidKoulutuspaivamaarat"
  )
  val InvalidMetadataTyyppi: ErrorMessage =
    ErrorMessage(msg = "Koulutustyyppi ei vastaa metadatan tyyppiä", id = "InvalidMetadataTyyppi")

  val KoulutusKoodiPattern: Pattern                     = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  val HakutapaKoodiPattern: Pattern                     = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  val KausiKoodiPattern: Pattern                        = Pattern.compile("""kausi_\w+#\d{1,2}""")
  val KohdejoukkoKoodiPattern: Pattern                  = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  val KohdejoukonTarkenneKoodiPattern: Pattern          = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  val PohjakoulutusvaatimusKoodiPattern: Pattern        = Pattern.compile("""pohjakoulutusvaatimuskouta_\w+#\d{1,2}""")
  val ValintatapajonoKoodiPattern: Pattern              = Pattern.compile("""valintatapajono_\w{1,2}#\d{1,2}""")
  val KoulutuksenLisatiedotOtsikkoKoodiPattern: Pattern = Pattern.compile("""koulutuksenlisatiedot_\d+#\d{1,2}""")
  val TietoaOpiskelustaOtsikkoKoodiPattern: Pattern     = Pattern.compile("""organisaationkuvaustiedot_\d+#\d{1,2}""")
  val KoulutusalaKoodiPattern: Pattern =
    Pattern.compile("""kansallinenkoulutusluokitus2016koulutusalataso[12]_\d+(#\d{1,2})?""")
  val TutkintonimikeKoodiPattern: Pattern          = Pattern.compile("""tutkintonimikekk_[\w*-]+#\d{1,2}""")
  val OpintojenLaajuusKoodiPattern: Pattern        = Pattern.compile("""opintojenlaajuus_v?\d+(#\d{1,2})?""")
  val OpintojenLaajuusyksikkoKoodiPattern: Pattern = Pattern.compile("""opintojenlaajuusyksikko_\d+#\d{1,2}""")
  val OpetuskieliKoodiPattern: Pattern             = Pattern.compile("""oppilaitoksenopetuskieli_\d+#\d{1,2}""")
  val OpetusaikaKoodiPattern: Pattern              = Pattern.compile("""opetusaikakk_\d+#\d{1,2}""")
  val OpetustapaKoodiPattern: Pattern              = Pattern.compile("""opetuspaikkakk_\d+#\d{1,2}""")
  val OsaamisalaKoodiPattern: Pattern              = Pattern.compile("""osaamisala_\d+(#\d{1,2})?""")
  val PostinumeroKoodiPattern: Pattern             = Pattern.compile("""posti_\d{5}(#\d{1,2})?""")
  val LiiteTyyppiKoodiPattern: Pattern             = Pattern.compile("""liitetyypitamm_\d+(#\d{1,2})?""")
  val ValintakokeenTyyppiKoodiPattern: Pattern     = Pattern.compile("""valintakokeentyyppi_\d+(#\d{1,2})?""")
  val KieliKoodiPattern: Pattern                   = Pattern.compile("""kieli_\w+(#\d{1,2})?""")
  val LukioPainotusKoodiPattern: Pattern           = Pattern.compile("""lukiopainotukset_\d+(#\d{1,2})?""")
  val LukioErityinenKoulutustehtavaKoodiPattern: Pattern =
    Pattern.compile("""lukiolinjaterityinenkoulutustehtava_\d+(#\d{1,2})?""")
  val LukioDiplomiKoodiPattern: Pattern = Pattern.compile("""moduulikoodistolops2021_\w+(#\d{1,2})?""")
  val OppiaineKoodiPattern: Pattern     = Pattern.compile("""painotettavatoppiaineetlukiossa_\w+(#\d{1,2})?""")
  val HakukohdeKoodiPattern: Pattern =
    Pattern.compile("""hakukohteet(perusopetuksenjalkeinenyhteishaku|erammatillinenerityisopetus)_\w+(#\d{1,2})?$""")

  val VuosiPattern: Pattern = Pattern.compile("""\d{4}""")

  val validStateChanges: Map[Julkaisutila, Seq[Julkaisutila]] =
    Map(
      Poistettu   -> Seq(),
      Tallennettu -> Seq(Julkaistu, Poistettu),
      Julkaistu   -> Seq(Tallennettu, Arkistoitu),
      Arkistoitu  -> Seq(Julkaistu)
    )
  def assertTrue(b: Boolean, path: String, msg: ErrorMessage): IsValid  = if (b) NoErrors else error(path, msg)
  def assertFalse(b: Boolean, path: String, msg: ErrorMessage): IsValid = if (!b) NoErrors else error(path, msg)
  def assertNotNegative(i: Long, path: String): IsValid                 = assertTrue(i >= 0, path, notNegativeMsg)
  def assertNotNegative(i: Double, path: String): IsValid               = assertTrue(i >= 0, path, notNegativeMsg)
  def assertLessOrEqual(i: Int, x: Int, path: String): IsValid          = assertTrue(i <= x, path, lessOrEqualMsg(i, x))
  def assertMatch(value: String, pattern: Pattern, path: String): IsValid =
    assertTrue(pattern.matcher(value).matches(), path, validationMsg(value))
  def assertValid(oid: Oid, path: String): IsValid                  = assertTrue(oid.isValid, path, validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], path: String): IsValid = assertTrue(value.isDefined, path, missingMsg)
  def assertNotEmpty[T](value: Seq[T], path: String): IsValid       = assertTrue(value.nonEmpty, path, missingMsg)
  def assertEmpty[T](value: Seq[T], path: String, errorMessage: ErrorMessage = notEmptyMsg): IsValid =
    assertTrue(value.isEmpty, path, errorMessage)
  def assertEmptyKielistetty(kielistetty: Kielistetty, path: String): IsValid =
    assertTrue(kielistetty.isEmpty, path, notEmptyMsg)

  def assertCertainValue(
      value: Option[String],
      expectedValuePrefix: String,
      path: String,
      expectedValueDescription: Option[String] = None
  ): IsValid =
    assertTrue(
      value.isDefined && value.get.startsWith(expectedValuePrefix),
      path,
      illegalValueForFixedValueMsg(expectedValueDescription.getOrElse(expectedValuePrefix))
    )

  def assertOneAndOnlyCertainValueInSeq(
      value: Seq[String],
      expectedValuePrefix: String,
      path: String,
      expectedValueDescription: Option[String] = None
  ): IsValid =
    if (value.size == 1 && value.head.startsWith(expectedValuePrefix)) {
      NoErrors
    } else {
      error(path, illegalValueForFixedValueSeqMsg(expectedValueDescription.getOrElse(expectedValuePrefix)))
    }

  def assertNotDefined[T](value: Option[T], path: String): IsValid =
    assertTrue(value.isEmpty, path, notMissingMsg(value))
  def assertNotDefinedIfOtherNotDefined[A, B](
      value: Option[A],
      other: Option[B],
      otherField: String,
      path: String
  ): IsValid =
    if (other.isEmpty && value.nonEmpty) error(path, notEmptyAlthoughOtherEmptyMsg(otherField)) else NoErrors
  def assertAlkamisvuosiInFuture(alkamisvuosi: String, path: String): IsValid =
    assertTrue(LocalDate.now().getYear <= Integer.parseInt(alkamisvuosi), path, pastDateMsg(alkamisvuosi))

  def assertValidUrl(url: String, path: String): IsValid = assertTrue(urlValidator.isValid(url), path, invalidUrl(url))
  def assertValidEmail(email: String, path: String): IsValid =
    assertTrue(emailValidator.isValid(email), path, invalidEmail(email))

  def assertInFuture(date: LocalDateTime, path: String): IsValid =
    assertTrue(date.isAfter(LocalDateTime.now()), path, pastDateMsg(date))

  def validateIfDefined[T](value: Option[T], f: T => IsValid): IsValid = value.map(f(_)).getOrElse(NoErrors)

  def validateIfDefinedOrModified[T](value: Option[T], oldValue: Option[T], f: T => IsValid): IsValid = (value, oldValue) match {
    case (Some(value), Some(oldValue)) => if (value != oldValue) f(value) else NoErrors
    case (Some(value), None) => f(value)
    case _ => NoErrors
  }

  def validateIfNonEmpty[T](values: Seq[T], path: String, f: (T, String) => IsValid): IsValid =
    values.zipWithIndex.flatMap { case (t, i) => f(t, s"$path[$i]") }

  def validateIfNonEmpty(k: Kielistetty, path: String, f: (String, String) => IsValid): IsValid =
    k.flatMap { case (k, v) => f(v, s"$path.$k") }.toSeq

  def validateIfTrue(b: Boolean, f: => IsValid): IsValid                      = if (b) f else NoErrors
  def validateIfTrueOrElse(b: Boolean, f: => IsValid, o: => IsValid): IsValid = if (b) f else o
  def validateIfFalse(b: Boolean, f: => IsValid): IsValid                     = if (!b) f else NoErrors

  def validateIfJulkaistu(tila: Julkaisutila, f: => IsValid): IsValid    = validateIfTrue(tila == Julkaistu, f)
  def validateIfAnyDefined(args: Seq[Option[_]], f: => IsValid): IsValid = validateIfTrue(args.exists(_.isDefined), f)
  def validateIfAnyDefinedOrElse(args: Seq[Option[_]], f: => IsValid, o: => IsValid): IsValid =
    if (args.exists(_.isDefined)) f else o
  def validateIfSuccessful(success: IsValid, f: => IsValid): IsValid = if (success.isEmpty) f else success

  def validateOidList(values: Seq[Oid], path: String): IsValid = validateIfNonEmpty(values, path, assertValid _)

  def findMissingKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(k.filter { case (_, arvo) => arvo.isEmpty }.keySet.toSeq)
  }

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid =
    findMissingKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(path, invalidKielistetty(kielet))
    }

  def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, path))

  def validateHakulomake(
      hakulomaketyyppi: Option[Hakulomaketyyppi],
      hakulomakeAtaruId: Option[UUID],
      hakulomakeKuvaus: Kielistetty,
      hakulomakeLinkki: Kielistetty,
      kielivalinta: Seq[Kieli]
  ): IsValid = hakulomaketyyppi match {
    case Some(MuuHakulomake) =>
      and(
        assertNotDefined(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeKuvaus, "hakulomakeKuvaus"),
        validateKielistetty(kielivalinta, hakulomakeLinkki, "hakulomakeLinkki"),
        hakulomakeLinkki.flatMap { case (_, u) => assertValidUrl(u, "hakulomakeLinkki") }.toSeq
      )
    case Some(Ataru) =>
      and(
        assertNotOptional(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeKuvaus, "hakulomakeKuvaus"),
        assertEmptyKielistetty(hakulomakeLinkki, "hakulomakeLinkki")
      )
    case Some(EiSähköistä) =>
      and(
        assertNotDefined(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeLinkki, "hakulomakeLinkki"),
        validateOptionalKielistetty(kielivalinta, hakulomakeKuvaus, "hakulomakeKuvaus")
      )
    case _ =>
      and(
        assertNotDefinedIfOtherNotDefined(hakulomakeAtaruId, hakulomaketyyppi, "hakulomaketyyppi", "hakulomakeAtaruId"),
        assertTrue(hakulomakeKuvaus.isEmpty, "hakulomakeKuvaus", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        assertTrue(hakulomakeLinkki.isEmpty, "hakulomakeLinkki", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi"))
      )
  }

  def validateArkistointiPaivamaara(ajastettuHaunJaHakukohteidenArkistointi: Option[LocalDateTime], haunPaattymisPaivamaarat: List[Option[LocalDateTime]]): IsValid = {
    val arkistointiAikaisintaanKuukautta = 3
    ajastettuHaunJaHakukohteidenArkistointi match {
      case Some(pvm) => haunPaattymisPaivamaarat.flatten.sortWith(_.isBefore(_)) match {
        case paattymisPaivamaarat if paattymisPaivamaarat.nonEmpty && paattymisPaivamaarat.last.toLocalDate.until(pvm.toLocalDate, ChronoUnit.MONTHS) < arkistointiAikaisintaanKuukautta =>
          error("ajastettuHaunJaHakukohteidenArkistointi", invalidArkistointiDate(arkistointiAikaisintaanKuukautta))
        case _ => NoErrors
      }
      case _ => NoErrors
    }
  }

  def validateKoulutusPaivamaarat(
      koulutuksenAlkamispaivamaara: Option[LocalDateTime],
      koulutuksenPaattymispaivamaara: Option[LocalDateTime],
      alkamisPath: String
  ): IsValid = {
    koulutuksenAlkamispaivamaara
      .flatMap(alku =>
        koulutuksenPaattymispaivamaara.map(loppu =>
          assertTrue(alku.isBefore(loppu), alkamisPath, InvalidKoulutuspaivamaarat)
        )
      )
      .getOrElse(NoErrors)
  }

  def validateMinMax[T](min: Option[T], max: Option[T], minPath: String)(implicit n: Numeric[T]): IsValid =
    (min, max) match {
      case (Some(min), Some(max)) => assertTrue(n.toDouble(min) <= n.toDouble(max), minPath, minmaxMsg(min, max))
      case _                      => NoErrors
    }

  def validateDependency(
      validatableTila: Julkaisutila,
      dependencyTila: Option[Julkaisutila],
      dependencyId: Any,
      dependencyName: String,
      dependencyIdPath: String
  ): IsValid = {
    dependencyTila.map { tila =>
      and(
        assertTrue(tila != Poistettu, path = dependencyIdPath, nonExistent(dependencyName, dependencyId)),
        validateIfTrue(
          tila != Poistettu,
          validateIfJulkaistu(
            validatableTila,
            assertTrue(tila == Julkaistu, "tila", notYetJulkaistu(dependencyName, dependencyId))
          )
        )
      )
    }.getOrElse(error(dependencyIdPath, Validations.nonExistent(dependencyName, dependencyId)))
  }

  def validateDependencyExistence(
      dependencyTila: Option[Julkaisutila],
      dependencyId: Any,
      dependencyName: String,
      dependencyIdPath: String
  ): IsValid = {
    dependencyTila.map { tila =>
      assertTrue(tila != Poistettu, path = dependencyIdPath, nonExistent(dependencyName, dependencyId))
    }.getOrElse(error(dependencyIdPath, nonExistent(dependencyName, dependencyId)))
  }

  def validateStateChange(entityDesc: String, oldState: Julkaisutila, newState: Julkaisutila): IsValid = {
    validateIfTrue(
      oldState != newState,
      validateIfDefined[Seq[Julkaisutila]](
        validStateChanges.get(oldState),
        validStates =>
          assertTrue(validStates.contains(newState), "tila", illegalStateChange(entityDesc, oldState, newState))
      )
    )
  }
}
