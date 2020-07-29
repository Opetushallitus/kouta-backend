package fi.oph.kouta.validation

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import java.util.regex.Pattern

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.Oid
import org.apache.commons.validator.routines.{EmailValidator, UrlValidator}

object Validations {
  private val urlValidator = new UrlValidator(Array("http", "https"))
  private val emailValidator = EmailValidator.getInstance(false, false)

  def error(path: String, msg: String): IsValid = List(ValidationError(path, msg))

  def and(validations: IsValid*): IsValid = validations.flatten.distinct

  def validationMsg(value: String) = s"'${value}' ei ole validi"
  def missingMsg = s"Pakollinen tieto puuttuu"
  val notNegativeMsg = s"ei voi olla negatiivinen"
  def invalidKielistetty(values: Seq[Kieli]) = s"Kielistetystä kentästä puuttuu arvo kielillä [${values.mkString(",")}]"
  def invalidTutkintoonjohtavuus(tyyppi: String) = s"Koulutuksen tyyppiä ${tyyppi} pitäisi olla tutkintoon johtavaa"
  def invalidUrl(url: String) = s"'${url}' ei ole validi URL"
  def invalidEmail(email: String) = s"'${email}' ei ole validi email"
  def invalidAjanjaksoMsg(ajanjakso: Ajanjakso) = s"${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen"
  def pastDateMsg(date: LocalDateTime) = s"$date on menneisyydessä"
  def pastDateMsg(date: String) = s"$date on menneisyydessä"
  def minmaxMsg(minValue: Any, maxValue: Any) = s"$minValue on suurempi kuin $maxValue"

  val KoulutusKoodiPattern: Pattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  val HakutapaKoodiPattern: Pattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  val KausiKoodiPattern: Pattern = Pattern.compile("""kausi_\w+#\d{1,2}""")
  val KohdejoukkoKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  val KohdejoukonTarkenneKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  val PohjakoulutusvaatimusKoodiPattern: Pattern = Pattern.compile("""pohjakoulutusvaatimustoinenaste_\w+#\d{1,2}""")
  val ValintatapajonoKoodiPattern: Pattern = Pattern.compile("""valintatapajono_\w{2}#\d{1,2}""")
  val KoulutuksenLisatiedotOtsikkoKoodiPattern: Pattern = Pattern.compile("""koulutuksenlisatiedot_\d+#\d{1,2}""")
  val TietoaOpiskelustaOtsikkoKoodiPattern: Pattern = Pattern.compile("""organisaationkuvaustiedot_\d+#\d{1,2}""")
  val KoulutusalaKoodiPattern: Pattern = Pattern.compile("""kansallinenkoulutusluokitus2016koulutusalataso[12]_\d+#\d{1,2}""")
  val TutkintonimikeKoodiPattern: Pattern = Pattern.compile("""tutkintonimikekk_[\w*-]+#\d{1,2}""")
  val OpintojenLaajuusKoodiPattern: Pattern = Pattern.compile("""opintojenlaajuus_\d+#\d{1,2}""")
  val OpetuskieliKoodiPattern: Pattern = Pattern.compile("""oppilaitoksenopetuskieli_\d+#\d{1,2}""")
  val OpetusaikaKoodiPattern: Pattern = Pattern.compile("""opetusaikakk_\d+#\d{1,2}""")
  val OpetustapaKoodiPattern: Pattern = Pattern.compile("""opetuspaikkakk_\d+#\d{1,2}""")
  val OsaamisalaKoodiPattern: Pattern = Pattern.compile("""osaamisala_\d+(#\d{1,2})?""")
  val PostinumeroKoodiPattern: Pattern = Pattern.compile("""posti_\d{5}(#\d{1,2})?""")
  val LiiteTyyppiKoodiPattern: Pattern = Pattern.compile("""liitetyypitamm_\d+(#\d{1,2})?""")
  val ValintakokeenTyyppiKoodiPattern: Pattern = Pattern.compile("""valintakokeentyyppi_\d+(#\d{1,2})?""")
  val KieliKoodiPattern: Pattern = Pattern.compile("""kieli_\w+(#\d{1,2})?""")
  val KielitaitoKoodiPattern: Pattern = Pattern.compile("""kielitaidonosoittaminen_\d+(#\d{1,2})?""")
  val KielitaitovaatimusKoodiPattern: Pattern = Pattern.compile("""kielitaitovaatimustyypit_\d+(#\d{1,2})?""")
  val KielitaitovaatimusKuvausKoodiPattern: Pattern = Pattern.compile("""kielitaitovaatimustyypitkuvaus_\d+(#\d{1,2})?""")
  val OsaamistaustaKoodiPattern: Pattern = Pattern.compile("""osaamistausta_\d+(#\d{1,2})?""")

  val VuosiPattern: Pattern = Pattern.compile("""\d{4}""")

  val InvalidKoulutuspaivamaarat = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen"
  val InvalidMetadataTyyppi = "Koulutustyyppi ei vastaa metadatan tyyppiä"

  def assertTrue(b: Boolean, path: String, msg: String): IsValid = if (b) NoErrors else error(path, msg)
  def assertNotNegative(i: Int, path: String): IsValid = assertTrue(i >= 0, path, notNegativeMsg)
  def assertNotNegative(i: Double, path: String): IsValid = assertTrue(i >= 0, path, notNegativeMsg)
  def assertMatch(value: String, pattern: Pattern, path: String): IsValid = assertTrue(pattern.matcher(value).matches(), path, validationMsg(value))
  def assertValid(oid: Oid, path: String): IsValid = assertTrue(oid.isValid, path, validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], path: String): IsValid = assertTrue(value.isDefined, path, missingMsg)
  def assertNotEmpty[T](value: Seq[T], path: String): IsValid = assertTrue(value.nonEmpty, path, missingMsg)

  def validateIfDefined[T](value: Option[T], f: (T) => IsValid): IsValid = value.map(f(_)).getOrElse(NoErrors)

  def validateIfNonEmpty[T](values: Seq[T], path: String, f: (T, String) => IsValid): IsValid =
    values.zipWithIndex.flatMap { case (t, i) => f(t, s"$path[$i]") }

  def validateIfNonEmpty(k: Kielistetty, path: String, f: (String, String) => IsValid): IsValid =
    k.flatMap { case (k, v) => f(v, s"$path.$k") }.toSeq

  def validateIfTrue(b: Boolean, f: => IsValid): IsValid = if(b) f else NoErrors

  def validateIfJulkaistu(tila: Julkaisutila, f: => IsValid): IsValid = validateIfTrue(tila == Julkaistu, f)

  def validateOidList(values: Seq[Oid], path: String): IsValid = validateIfNonEmpty(values, path, assertValid _)

  def findMissingKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter { case (_, arvo) => arvo.isEmpty }.keySet.toSeq)
  }

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid =
    findMissingKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(path, invalidKielistetty(kielet))
    }

  def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, path))

  def assertAlkamisvuosiInFuture(alkamisvuosi: String, path: String): IsValid =
    assertTrue(LocalDate.now().getYear <= Integer.parseInt(alkamisvuosi), path, pastDateMsg(alkamisvuosi))

  def validateHakulomake(hakulomaketyyppi: Option[Hakulomaketyyppi],
                         hakulomakeAtaruId: Option[UUID],
                         hakulomakeKuvaus: Kielistetty,
                         hakulomakeLinkki: Kielistetty,
                         kielivalinta: Seq[Kieli]): IsValid = hakulomaketyyppi match {
    case Some(MuuHakulomake) => and(
      validateKielistetty(kielivalinta, hakulomakeLinkki, "hakulomakeLinkki"),
      hakulomakeLinkki.flatMap { case (_, u) => assertValidUrl(u, "hakulomakeLinkki") }.toSeq
    )
    case Some(Ataru) => assertNotOptional(hakulomakeAtaruId, "hakulomakeAtaruId")
    case Some(EiSähköistä) => validateOptionalKielistetty(kielivalinta, hakulomakeKuvaus, "hakulomakeKuvaus")
    case _ => NoErrors
  }

  def assertValidUrl(url: String, path: String): IsValid = assertTrue(urlValidator.isValid(url), path, invalidUrl(url))
  def assertValidEmail(email: String, path: String): IsValid = assertTrue(emailValidator.isValid(email), path, invalidEmail(email))

  def validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara: Option[LocalDateTime],
                                  koulutuksenPaattymispaivamaara: Option[LocalDateTime],
                                  alkamisPath: String): IsValid = {
    koulutuksenAlkamispaivamaara.flatMap(alku =>
      koulutuksenPaattymispaivamaara.map(loppu =>
        assertTrue(alku.isBefore(loppu), alkamisPath, InvalidKoulutuspaivamaarat)
      )
    ).getOrElse(NoErrors)
  }

  def validateMinMax[T](min: Option[T], max: Option[T], minPath: String)(implicit n: Numeric[T]): IsValid = (min, max) match {
    case (Some(min), Some(max)) => assertTrue(n.toDouble(min) <= n.toDouble(max), minPath, minmaxMsg(min, max))
    case _ => NoErrors
  }

  def assertInFuture(date: LocalDateTime, path: String): IsValid =
    assertTrue(date.isAfter(LocalDateTime.now()), path, pastDateMsg(date))
}
