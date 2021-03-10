package fi.oph.kouta.validation

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import java.util.regex.Pattern

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.Oid
import org.apache.commons.validator.routines.{EmailValidator, UrlValidator}


object ValidationsUusi {
  private val urlValidator = new UrlValidator(Array("http", "https"))
  private val emailValidator = EmailValidator.getInstance(false, false)

  def error(path: String, msg: ErrorMessage): IsValidUusi = List(ValidationErrorUusi(path, msg))

  def and(validations: IsValidUusi*): IsValidUusi = validations.flatten.distinct
  def or(first: IsValidUusi, second: IsValidUusi): IsValidUusi = if (first.isEmpty) second else first

  def validationMsg(value: String): ErrorMessage = ErrorMessage(msg = s"'$value' ei ole validi", id = "validationMsg")
  val missingMsg: ErrorMessage = ErrorMessage(msg = s"Pakollinen tieto puuttuu", id = "missingMsg")
  val notNegativeMsg: ErrorMessage = ErrorMessage(msg = s"ei voi olla negatiivinen", id = "notNegativeMsg")
  def invalidKielistetty(values: Seq[Kieli]): ErrorMessage = ErrorMessage(msg = s"Kielistetystä kentästä puuttuu arvo kielillä [${values.mkString(",")}]", id = "invalidKielistetty")
  def invalidTutkintoonjohtavuus(tyyppi: String): ErrorMessage = ErrorMessage(msg = s"Koulutuksen tyyppiä $tyyppi pitäisi olla tutkintoon johtavaa", id = "invalidTutkintoonjohtavuus")
  def invalidUrl(url: String): ErrorMessage = ErrorMessage(msg = s"'$url' ei ole validi URL", id = "invalidUrl")
  def invalidEmail(email: String): ErrorMessage = ErrorMessage(msg = s"'$email' ei ole validi email", id = "invalidEmail")
  def invalidAjanjaksoMsg(ajanjakso: Ajanjakso): ErrorMessage = ErrorMessage(msg = s"${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen", id = "invalidAjanjaksoMsg")
  def pastDateMsg(date: LocalDateTime): ErrorMessage = ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def pastDateMsg(date: String): ErrorMessage = ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def minmaxMsg(minValue: Any, maxValue: Any): ErrorMessage = ErrorMessage(msg = s"$minValue on suurempi kuin $maxValue", id = "minmaxMsg")
  def notYetJulkaistu(field: String, id: Any): ErrorMessage = ErrorMessage(msg = s"$field ($id) ei ole vielä julkaistu", id = "notYetJulkaistu")
  def nonExistent(field: String, id: Any): ErrorMessage = ErrorMessage(msg = s"$field ($id) ei ole olemassa", id = "nonExistent")
  def notMissingMsg(value: Any): ErrorMessage = ErrorMessage(msg = s"Arvo $value ei saisi olla määritelty", id = "notMissingMsg")
  def tyyppiMismatch(field: String, id: Any): ErrorMessage = ErrorMessage(msg = s"Tyyppi ei vastaa $field ($id) tyyppiä", id = "tyyppiMismatch")
  def tyyppiMismatch(field1: String, id1: Any, field2: String, id2: Any): ErrorMessage = ErrorMessage(msg = s"$field1 ($id1) tyyppi ei vastaa $field2 ($id2) tyyppiä", id = "tyyppiMismatch")
  def cannotLinkToHakukohde(oid: String): ErrorMessage = ErrorMessage(msg = s"Toteutusta ($oid) ei voi liittää hakukohteeseen", id = "cannotLinkToHakukohde")

  val InvalidKoulutuspaivamaarat: ErrorMessage = ErrorMessage(msg = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen", id = "InvalidKoulutuspaivamaarat")
  val InvalidMetadataTyyppi: ErrorMessage = ErrorMessage(msg = "Koulutustyyppi ei vastaa metadatan tyyppiä", id = "InvalidMetadataTyyppi")

  val KoulutusKoodiPattern: Pattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  val HakutapaKoodiPattern: Pattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  val KausiKoodiPattern: Pattern = Pattern.compile("""kausi_\w+#\d{1,2}""")
  val KohdejoukkoKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  val KohdejoukonTarkenneKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  val PohjakoulutusvaatimusKoodiPattern: Pattern = Pattern.compile("""pohjakoulutusvaatimuskouta_\w+#\d{1,2}""")
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

  def assertTrue(b: Boolean, path: String, msg: ErrorMessage): IsValidUusi = if (b) NoErrorsUusi else error(path, msg)
  def assertNotNegative(i: Long, path: String): IsValidUusi = assertTrue(i >= 0, path, notNegativeMsg)
  def assertNotNegative(i: Double, path: String): IsValidUusi = assertTrue(i >= 0, path, notNegativeMsg)
  def assertMatch(value: String, pattern: Pattern, path: String): IsValidUusi = assertTrue(pattern.matcher(value).matches(), path, validationMsg(value))
  def assertValid(oid: Oid, path: String): IsValidUusi = assertTrue(oid.isValid, path, validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], path: String): IsValidUusi = assertTrue(value.isDefined, path, missingMsg)
  def assertNotEmpty[T](value: Seq[T], path: String): IsValidUusi = assertTrue(value.nonEmpty, path, missingMsg)
  def assertNotDefined[T](value: Option[T], path: String): IsValidUusi = assertTrue(value.isEmpty, path, notMissingMsg(value))

  def validateIfDefined[T](value: Option[T], f: T => IsValidUusi): IsValidUusi = value.map(f(_)).getOrElse(NoErrorsUusi)

  def validateIfNonEmpty[T](values: Seq[T], path: String, f: (T, String) => IsValidUusi): IsValidUusi =
    values.zipWithIndex.flatMap { case (t, i) => f(t, s"$path[$i]") }

  def validateIfNonEmpty(k: Kielistetty, path: String, f: (String, String) => IsValidUusi): IsValidUusi =
    k.flatMap { case (k, v) => f(v, s"$path.$k") }.toSeq

  def validateIfTrue(b: Boolean, f: => IsValidUusi): IsValidUusi = if(b) f else NoErrorsUusi

  def validateIfJulkaistu(tila: Julkaisutila, f: => IsValidUusi): IsValidUusi = validateIfTrue(tila == Julkaistu, f)

  def validateOidList(values: Seq[Oid], path: String): IsValidUusi = validateIfNonEmpty(values, path, assertValid _)

  def findMissingKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter { case (_, arvo) => arvo.isEmpty }.keySet.toSeq)
  }

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValidUusi =
    findMissingKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrorsUusi
      case kielet         => error(path, invalidKielistetty(kielet))
    }

  def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValidUusi =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, path))

  def assertAlkamisvuosiInFuture(alkamisvuosi: String, path: String): IsValidUusi =
    assertTrue(LocalDate.now().getYear <= Integer.parseInt(alkamisvuosi), path, pastDateMsg(alkamisvuosi))

  def validateHakulomake(hakulomaketyyppi: Option[Hakulomaketyyppi],
                         hakulomakeAtaruId: Option[UUID],
                         hakulomakeKuvaus: Kielistetty,
                         hakulomakeLinkki: Kielistetty,
                         kielivalinta: Seq[Kieli]): IsValidUusi = hakulomaketyyppi match {
    case Some(MuuHakulomake) => and(
      validateKielistetty(kielivalinta, hakulomakeLinkki, "hakulomakeLinkki"),
      hakulomakeLinkki.flatMap { case (_, u) => assertValidUrl(u, "hakulomakeLinkki") }.toSeq
    )
    case Some(Ataru) => assertNotOptional(hakulomakeAtaruId, "hakulomakeAtaruId")
    case Some(EiSähköistä) => validateOptionalKielistetty(kielivalinta, hakulomakeKuvaus, "hakulomakeKuvaus")
    case _ => NoErrorsUusi
  }

  def assertValidUrl(url: String, path: String): IsValidUusi = assertTrue(urlValidator.isValid(url), path, invalidUrl(url))
  def assertValidEmail(email: String, path: String): IsValidUusi = assertTrue(emailValidator.isValid(email), path, invalidEmail(email))

  def validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara: Option[LocalDateTime],
                                  koulutuksenPaattymispaivamaara: Option[LocalDateTime],
                                  alkamisPath: String): IsValidUusi = {
    koulutuksenAlkamispaivamaara.flatMap(alku =>
      koulutuksenPaattymispaivamaara.map(loppu =>
        assertTrue(alku.isBefore(loppu), alkamisPath, InvalidKoulutuspaivamaarat)
      )
    ).getOrElse(NoErrorsUusi)
  }

  def validateMinMax[T](min: Option[T], max: Option[T], minPath: String)(implicit n: Numeric[T]): IsValidUusi = (min, max) match {
    case (Some(min), Some(max)) => assertTrue(n.toDouble(min) <= n.toDouble(max), minPath, minmaxMsg(min, max))
    case _ => NoErrorsUusi
  }

  def assertInFuture(date: LocalDateTime, path: String): IsValidUusi =
    assertTrue(date.isAfter(LocalDateTime.now()), path, pastDateMsg(date))

  def validateDependency(validatableTila: Julkaisutila,
                         dependencyTila: Option[Julkaisutila],
                         dependencyId: Any,
                         dependencyName: String,
                         dependencyIdPath: String): IsValidUusi = {
    dependencyTila.map { tila =>
      validateIfJulkaistu(validatableTila, assertTrue(tila == Julkaistu, "tila", notYetJulkaistu(dependencyName, dependencyId)))
    }.getOrElse(error(dependencyIdPath, nonExistent(dependencyName, dependencyId)))
  }
}
