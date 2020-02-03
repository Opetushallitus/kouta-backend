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

  def error(msg: String): IsValid = List(msg)

  def and(validations: IsValid*): IsValid = validations.flatten.distinct

  def validationMsg(value: String) = s"'${value}' ei ole validi"
  def missingMsg(name: String) = s"Pakollinen tieto '$name' puuttuu"
  def invalidOidsMsg(oids: Seq[Oid]) = s"Arvot [${oids.map(_.toString).mkString(",")}] eivät ole valideja oideja"
  def notNegativeMsg(name: String) = s"'$name' ei voi olla negatiivinen"
  def invalidKielistetty(field: String, values: Seq[Kieli]) = s"Kielistetystä kentästä $field puuttuu arvo kielillä [${values.mkString(",")}]"
  def invalidTutkintoonjohtavuus(tyyppi: String) = s"Koulutuksen tyyppiä ${tyyppi} pitäisi olla tutkintoon johtavaa"
  def invalidUrl(url: String) = s"'${url}' ei ole validi URL"
  def invalidEmail(email: String) = s"'${email}' ei ole validi email"
  def invalidAjanjakso(ajanjakso: Ajanjakso, field: String) = s"$field ${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen"
  def pastAjanjaksoMsg(ajanjakso: Ajanjakso, field: String) = s"$field ${ajanjakso.alkaa} - ${ajanjakso.paattyy} on menneisyydessä"
  def pastDateMsg(date: LocalDateTime, field: String) = s"$field ($date) on menneisyydessä"
  def minmaxMsg(basename: String) = s"min$basename on suurempi kuin max$basename"

  val KoulutusKoodiPattern: Pattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  val HakutapaKoodiPattern: Pattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  val KausiKoodiPattern: Pattern = Pattern.compile("""kausi_\w+#\d{1,2}""")
  val KohdejoukkoKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  val KohdejoukonTarkenneKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  val PohjakoulutusvaatimusKoodiPattern: Pattern = Pattern.compile("""pohjakoulutusvaatimustoinenaste_\w+#\d{1,2}""")
  val ValintatapajonoKoodiPattern: Pattern = Pattern.compile("""valintatapajono_\w{2}\d{1,2}""")
  val KoulutuksenLisatiedotOtsikkoKoodiPattern: Pattern = Pattern.compile("""koulutuksenlisatiedot_\d+#\d{1,2}""")
  val KoulutusalaKoodiPattern: Pattern = Pattern.compile("""kansallinenkoulutusluokitus2016koulutusalataso2_\d+#\d{1,2}""")
  val TutkintonimikeKoodiPattern: Pattern = Pattern.compile("""tutkintonimikekk_\d+#\d{1,2}""")
  val OpintojenLaajuusKoodiPattern: Pattern = Pattern.compile("""opintojenlaajuus_\d+#\d{1,2}""")
  val OpetuskieliKoodiPattern: Pattern = Pattern.compile("""oppilaitoksenopetuskieli_\d+#\d{1,2}""")
  val OpetusaikaKoodiPattern: Pattern = Pattern.compile("""opetusaikakk_\d+#\d{1,2}""")
  val OpetustapaKoodiPattern: Pattern = Pattern.compile("""opetuspaikkakk_\d+#\d{1,2}""")
  val OsaamisalaKoodiPattern: Pattern = Pattern.compile("""osaamisala_\d+(#\d{1,2})?""")
  val PostinumeroKoodiPattern: Pattern = Pattern.compile("""posti_\d{5}(#\d{1,2})?""")
  val LiiteTyyppiKoodiPattern: Pattern = Pattern.compile("""liitetyypitamm_\d+(#\d{1,2})?""")
  val ValintakokeenTyyppiKoodiPattern: Pattern = Pattern.compile("""valintakokeentyyppi_\d+(#\d{1,2})?""")

  val VuosiPattern: Pattern = Pattern.compile("""\d{4}""")

  val MissingKielivalinta = "Kielivalinta puuttuu"
  val InvalidKoulutuspaivamaarat = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen"
  val InvalidMetadataTyyppi = "Koulutustyyppi ei vastaa metadatan tyyppiä"

  def assertTrue(b: Boolean, msg: String): IsValid = if (b) NoErrors else error(msg)
  def assertFalse(b: Boolean, msg: String): IsValid = assertTrue(!b, msg)
  def assertNotNegative(i: Int, name: String): IsValid = assertTrue(i >= 0, notNegativeMsg(name))
  def assertNotNegative(i: Double, name: String): IsValid = assertTrue(i >= 0, notNegativeMsg(name))
  def assertOption[E](o: Option[E], f: (E) => Boolean, msg: String, optional: Boolean = true): IsValid = assertTrue(o.map(f).getOrElse(optional), msg)
  def assertOptionPresent[E](o: Option[E], msg: String): IsValid = assertTrue(o.isDefined, msg)
  def assertMatch(value: String, pattern: Pattern): IsValid = assertTrue(pattern.matcher(value).matches(), validationMsg(value))
  def assertValid(oid: Oid): IsValid = assertTrue(oid.isValid(), validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], name: String): IsValid = assertTrue(value.isDefined, missingMsg(name))
  def assertNotEmpty[T](value: Seq[T], name: String): IsValid = assertTrue(value.nonEmpty, missingMsg(name))

  def validateNotOptional[E](o: Option[E], name: String, f: (E) => IsValid): IsValid = and(
    assertNotOptional(o, name),
    validateIfDefined(o, f)
  )

  def validateIfDefined[T](value: Option[T], f: T => IsValid): IsValid = value.map(f(_)).getOrElse(NoErrors)

  def validateIfNonEmpty[T](values: Seq[T], f: T => IsValid): IsValid = values.flatMap(f(_))

  def validateIfTrue(b: Boolean, f: => IsValid): IsValid = if(b) f else NoErrors

  def validateIfJulkaistu(tila: Julkaisutila, f: => IsValid): IsValid = validateIfTrue(tila == Julkaistu, f)

  def findInvalidOids(l: Seq[Oid]): Seq[Oid] = l.filter(!_.isValid())
  def validateOidList(values: Seq[Oid]): IsValid = findInvalidOids(values) match {
    case x if x.isEmpty => NoErrors
    case oids => error(invalidOidsMsg(oids))
  }

  def findPuuttuvatKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter{case (kieli, arvo) => arvo.isEmpty}.keySet.toSeq)}

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, field: String): IsValid =
    findPuuttuvatKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(invalidKielistetty(field, kielet))
    }

  def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, field: String): IsValid =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, field))

  def validateAjanjakso(ajanjakso: Ajanjakso, field: String): IsValid =
    assertTrue(ajanjakso.alkaa.isBefore(ajanjakso.paattyy), invalidAjanjakso(ajanjakso, field))

  def assertAjanjaksoEndsInFuture(ajanjakso: Ajanjakso, field: String): IsValid =
    assertTrue(ajanjakso.paattyy.isAfter(LocalDateTime.now()), pastAjanjaksoMsg(ajanjakso, field))

  def assertHakuajatInFuture(hakuajat: Seq[Ajanjakso]): IsValid =
    hakuajat.flatMap(ajanjakso => assertTrue(ajanjakso.paattyy.isAfter(LocalDateTime.now()), validationMsg(ajanjakso.toString)))

  def isValidAlkamisvuosi(s: String): Boolean = VuosiPattern.matcher(s).matches && LocalDate.now().getYear <= Integer.parseInt(s)
  def validateAlkamisvuosi(alkamisvuosi: String): IsValid = assertTrue(isValidAlkamisvuosi(alkamisvuosi), validationMsg(alkamisvuosi))

  def validateHakulomake(hakulomaketyyppi: Option[Hakulomaketyyppi],
                                   hakulomakeAtaruId: Option[UUID],
                                   hakulomakeKuvaus: Kielistetty,
                                   hakulomakeLinkki: Kielistetty,
                                   kielivalinta: Seq[Kieli]): IsValid = hakulomaketyyppi match {
    case Some(MuuHakulomake) => and(
      validateKielistetty(kielivalinta, hakulomakeLinkki, "hakulomakeLinkki"),
      hakulomakeLinkki.flatMap { case (_, u) => assertValidUrl(u) }.toSeq
    )
    case Some(Ataru) => assertNotOptional(hakulomakeAtaruId, "hakulomakeAtaruId")
    case Some(EiSähköistä) => validateOptionalKielistetty(kielivalinta, hakulomakeKuvaus, "hakulomakeKuvaus")
    case _ => NoErrors
  }

  def assertValidUrl(url: String): IsValid = assertTrue(urlValidator.isValid(url), invalidUrl(url))
  def assertValidEmail(email: String): IsValid = assertTrue(emailValidator.isValid(email), invalidEmail(email))

  def validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara: Option[LocalDateTime],
                                            koulutuksenPaattymispaivamaara: Option[LocalDateTime]): IsValid = {
    koulutuksenAlkamispaivamaara.flatMap(alku =>
      koulutuksenPaattymispaivamaara.map(loppu =>
        assertTrue(alku.isBefore(loppu), InvalidKoulutuspaivamaarat)
      )
    ).getOrElse(NoErrors)
  }

  def validateMinMax(min: Option[Int], max: Option[Int], basename: String): IsValid = (min, max) match {
    case (Some(min), Some(max)) => assertTrue(min <= max, minmaxMsg(basename))
    case _ => NoErrors
  }

  def assertInFuture(date: LocalDateTime, field: String): IsValid =
    assertTrue(date.isAfter(LocalDateTime.now()), pastDateMsg(date, field))
}
