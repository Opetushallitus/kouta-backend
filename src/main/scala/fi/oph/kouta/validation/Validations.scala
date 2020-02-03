package fi.oph.kouta.validation

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import java.util.regex.Pattern

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.Oid
import org.apache.commons.validator.routines.{EmailValidator, UrlValidator}

trait Validations {
  private val urlValidator = new UrlValidator(Array("http", "https"))
  private val emailValidator = EmailValidator.getInstance(false, false)

  protected def error(msg: String): IsValid = List(msg)

  protected def and(validations: IsValid*): IsValid = validations.flatten.distinct

  protected def validationMsg(value: String) = s"'${value}' ei ole validi"
  protected def missingMsg(name: String) = s"Pakollinen tieto '$name' puuttuu"
  protected def invalidOidsMsg(oids: Seq[Oid]) = s"Arvot [${oids.map(_.toString).mkString(",")}] eivät ole valideja oideja"
  protected def notNegativeMsg(name: String) = s"'$name' ei voi olla negatiivinen"
  protected def invalidKielistetty(field: String, values: Seq[Kieli]) = s"Kielistetystä kentästä $field puuttuu arvo kielillä [${values.mkString(",")}]"
  protected def invalidTutkintoonjohtavuus(tyyppi: String) = s"Koulutuksen tyyppiä ${tyyppi} pitäisi olla tutkintoon johtavaa"
  protected def invalidUrl(url: String) = s"'${url}' ei ole validi URL"
  protected def invalidEmail(email: String) = s"'${email}' ei ole validi email"
  protected def invalidAjanjakso(ajanjakso: Ajanjakso, field: String) = s"$field ${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen"
  protected def pastAjanjaksoMsg(ajanjakso: Ajanjakso, field: String) = s"$field ${ajanjakso.alkaa} - ${ajanjakso.paattyy} on menneisyydessä"
  protected def pastDateMsg(date: LocalDateTime, field: String) = s"$field ($date) on menneisyydessä"
  protected def minmaxMsg(basename: String) = s"min$basename on suurempi kuin max$basename"

  protected val KoulutusKoodiPattern: Pattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  protected val HakutapaKoodiPattern: Pattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  protected val KausiKoodiPattern: Pattern = Pattern.compile("""kausi_\w+#\d{1,2}""")
  protected val KohdejoukkoKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  protected val KohdejoukonTarkenneKoodiPattern: Pattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  protected val PohjakoulutusvaatimusKoodiPattern: Pattern = Pattern.compile("""pohjakoulutusvaatimustoinenaste_\w+#\d{1,2}""")
  protected val ValintatapajonoKoodiPattern: Pattern = Pattern.compile("""valintatapajono_\w{2}\d{1,2}""")
  protected val KoulutuksenLisatiedotOtsikkoKoodiPattern: Pattern = Pattern.compile("""koulutuksenlisatiedot_\d+#\d{1,2}""")
  protected val KoulutusalaKoodiPattern: Pattern = Pattern.compile("""kansallinenkoulutusluokitus2016koulutusalataso2_\d+#\d{1,2}""")
  protected val TutkintonimikeKoodiPattern: Pattern = Pattern.compile("""tutkintonimikekk_\d+#\d{1,2}""")
  protected val OpintojenLaajuusKoodiPattern: Pattern = Pattern.compile("""opintojenlaajuus_\d+#\d{1,2}""")
  protected val OpetuskieliKoodiPattern: Pattern = Pattern.compile("""oppilaitoksenopetuskieli_\d+#\d{1,2}""")
  protected val OpetusaikaKoodiPattern: Pattern = Pattern.compile("""opetusaikakk_\d+#\d{1,2}""")
  protected val OpetustapaKoodiPattern: Pattern = Pattern.compile("""opetuspaikkakk_\d+#\d{1,2}""")
  protected val OsaamisalaKoodiPattern: Pattern = Pattern.compile("""osaamisala_\d+(#\d{1,2})?""")
  protected val PostinumeroKoodiPattern: Pattern = Pattern.compile("""posti_\d{5}(#\d{1,2})?""")
  protected val LiiteTyyppiKoodiPattern: Pattern = Pattern.compile("""liitetyypitamm_\d+(#\d{1,2})?""")
  protected val ValintakokeenTyyppiKoodiPattern: Pattern = Pattern.compile("""valintakokeentyyppi_\d+(#\d{1,2})?""")

  protected val VuosiPattern: Pattern = Pattern.compile("""\d{4}""")

  protected val MissingKielivalinta = "Kielivalinta puuttuu"
  protected val InvalidKoulutuspaivamaarat = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen"
  protected val InvalidMetadataTyyppi = "Koulutustyyppi ei vastaa metadatan tyyppiä"

  protected def assertTrue(b: Boolean, msg: String): IsValid = if (b) NoErrors else error(msg)
  protected def assertFalse(b: Boolean, msg: String): IsValid = assertTrue(!b, msg)
  protected def assertNotNegative(i: Int, name: String): IsValid = assertTrue(i >= 0, notNegativeMsg(name))
  protected def assertNotNegative(i: Double, name: String): IsValid = assertTrue(i >= 0, notNegativeMsg(name))
  protected def assertOption[E](o: Option[E], f: (E) => Boolean, msg: String, optional: Boolean = true): IsValid = assertTrue(o.map(f).getOrElse(optional), msg)
  protected def assertOptionPresent[E](o: Option[E], msg: String): IsValid = assertTrue(o.isDefined, msg)
  protected def assertMatch(value: String, pattern: Pattern): IsValid = assertTrue(pattern.matcher(value).matches(), validationMsg(value))
  protected def assertValid(oid: Oid): IsValid = assertTrue(oid.isValid(), validationMsg(oid.toString))
  protected def assertNotOptional[T](value: Option[T], name: String): IsValid = assertTrue(value.isDefined, missingMsg(name))
  protected def assertNotEmpty[T](value: Seq[T], name: String): IsValid = assertTrue(value.nonEmpty, missingMsg(name))

  protected def validateNotOptional[E](o: Option[E], name: String, f: (E) => IsValid): IsValid = and(
    assertNotOptional(o, name),
    validateIfDefined(o, f)
  )

  protected def validateIfDefined[T](value: Option[T], f: T => IsValid): IsValid = value.map(f(_)).getOrElse(NoErrors)

  protected def validateIfNonEmpty[T](values: Seq[T], f: T => IsValid): IsValid = values.flatMap(f(_))

  protected def validateIfTrue(b: Boolean, f: => IsValid): IsValid = if(b) f else NoErrors

  protected def validateIfJulkaistu(tila: Julkaisutila, f: => IsValid): IsValid = validateIfTrue(tila == Julkaistu, f)

  protected def findInvalidOids(l: Seq[Oid]): Seq[Oid] = l.filter(!_.isValid())
  protected def validateOidList(values: Seq[Oid]): IsValid = findInvalidOids(values) match {
    case x if x.isEmpty => NoErrors
    case oids => error(invalidOidsMsg(oids))
  }

  protected def findPuuttuvatKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter{case (kieli, arvo) => arvo.isEmpty}.keySet.toSeq)}

  protected def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, field: String): IsValid =
    findPuuttuvatKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(invalidKielistetty(field, kielet))
    }

  protected def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, field: String): IsValid =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, field))

  protected def validateAjanjakso(ajanjakso: Ajanjakso, field: String): IsValid =
    assertTrue(ajanjakso.alkaa.isBefore(ajanjakso.paattyy), invalidAjanjakso(ajanjakso, field))

  protected def assertAjanjaksoEndsInFuture(ajanjakso: Ajanjakso, field: String): IsValid =
    assertTrue(ajanjakso.paattyy.isAfter(LocalDateTime.now()), pastAjanjaksoMsg(ajanjakso, field))

  protected def assertHakuajatInFuture(hakuajat: Seq[Ajanjakso]): IsValid =
    hakuajat.flatMap(ajanjakso => assertTrue(ajanjakso.paattyy.isAfter(LocalDateTime.now()), validationMsg(ajanjakso.toString)))

  protected def isValidAlkamisvuosi(s: String): Boolean = VuosiPattern.matcher(s).matches && LocalDate.now().getYear <= Integer.parseInt(s)
  protected def validateAlkamisvuosi(alkamisvuosi: String): IsValid = assertTrue(isValidAlkamisvuosi(alkamisvuosi), validationMsg(alkamisvuosi))

  protected def validateHakulomake(hakulomaketyyppi: Option[Hakulomaketyyppi],
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

  protected def assertValidUrl(url: String): IsValid = assertTrue(urlValidator.isValid(url), invalidUrl(url))
  protected def assertValidEmail(email: String): IsValid = assertTrue(emailValidator.isValid(email), invalidEmail(email))

  protected def validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara: Option[LocalDateTime],
                                            koulutuksenPaattymispaivamaara: Option[LocalDateTime]): IsValid = {
    koulutuksenAlkamispaivamaara.flatMap(alku =>
      koulutuksenPaattymispaivamaara.map(loppu =>
        assertTrue(alku.isBefore(loppu), InvalidKoulutuspaivamaarat)
      )
    ).getOrElse(NoErrors)
  }

  protected def validateMinMax(min: Option[Int], max: Option[Int], basename: String): IsValid = (min, max) match {
    case (Some(min), Some(max)) => assertTrue(min <= max, minmaxMsg(basename))
    case _ => NoErrors
  }

  protected def assertInFuture(date: LocalDateTime, field: String): IsValid =
    assertTrue(date.isAfter(LocalDateTime.now()), pastDateMsg(date, field))
}
