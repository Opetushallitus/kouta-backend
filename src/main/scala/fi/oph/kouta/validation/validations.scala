package fi.oph.kouta.validation

import java.time.LocalDate
import java.util.UUID
import java.util.regex.Pattern
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.Oid

trait Validations {
  def validationMsg(value:String) = s"'${value}' ei ole validi"
  def missingMsg(name:String) = s"Pakollinen tieto '$name' puuttuu"
  def invalidOidsMsg(oids:Seq[Oid]) = s"Arvot [${oids.map(_.toString).mkString(",")}] eivät ole valideja oideja"
  def notNegativeMsg(name:String) = s"'$name' ei voi olla negatiivinen"
  def invalidKielistetty(field:String, values:Seq[Kieli]) = s"Kielistetystä kentästä $field puuttuu arvo kielillä [${values.mkString(",")}]"
  def invalidTutkintoonjohtavuus(tyyppi:String) = s"Koulutuksen tyyppiä ${tyyppi} pitäisi olla tutkintoon johtavaa"

  //Don't use vals! They might appear to Swagger randomly
  def KoulutusKoodiPattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  def HakutapaKoodiPattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  def KausiKoodiPattern = Pattern.compile("""kausi_\w+#\d{1,2}""")
  def KohdejoukkoKoodiPattern = Pattern.compile("""haunkohdejoukko_\d+#\d{1,2}""")
  def KohdejoukonTarkenneKoodiPattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  def PohjakoulutusvaatimusKoodiPattern = Pattern.compile("""pohjakoulutusvaatimustoinenaste_\w+#\d{1,2}""")
  def ValintatapajonoKoodiPattern = Pattern.compile("""valintatapajono_\w{2}\d{1,2}""")

  def VuosiPattern = Pattern.compile("""\d{4}""")

  def MissingKielivalinta = "Kielivalinta puuttuu"
  def InvalidHakuaika = "Hakuaika on virheellinen"
  def MissingTarjoajat = "Tarjoajat puuttuvat"

  def toLeft(msg: String) = Left(List(msg))

  def assertTrue(b: Boolean, msg: String): IsValid = Either.cond(b, (), List(msg))
  def assertNotNegative(i: Int, name: String): IsValid = assertTrue(i >= 0, notNegativeMsg(name))
  def assertOption[E](o: Option[E], f: (E) => Boolean, msg: String, optional: Boolean = true): IsValid = assertTrue(o.map(f).getOrElse(optional), msg)
  def assertOptionPresent[E](o: Option[E], msg: String): IsValid = assertTrue(o.isDefined, msg)
  def assertMatch(value: String, pattern: Pattern): IsValid = assertTrue(pattern.matcher(value).matches(), validationMsg(value))
  def assertValid(oid: Oid): IsValid = assertTrue(oid.isValid(), validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], name: String): IsValid = assertTrue(value.isDefined, missingMsg(name))
  def assertNotEmpty[T](s: Seq[T], msg: String): IsValid = assertTrue(s.nonEmpty, msg)

  def validateIfDefined[T](value: Option[T], f: T => IsValid): IsValid = value.map(f(_)).getOrElse(Right(()))
  def validateIfDefined[T <: Validatable](value: Option[T]): IsValid = value.map(_.validate()).getOrElse(Right(()))

  def validateIfNonEmpty[T](values: Seq[T], f: T => IsValid): IsValid = {
    val messages = values
      .map(f(_))
      .collect {
        case Left(msgList) => msgList
      }.flatten

    Either.cond(messages.isEmpty, (), messages.toList)
  }

  def validateIfTrue(b: Boolean, f: () => IsValid): IsValid = if (b) {
    f()
  } else {
    Right(())
  }

  def findInvalidOids(l: Seq[Oid]): Seq[Oid] = l.filter(!_.isValid())
  def validateOidList(values: Seq[Oid]): IsValid = findInvalidOids(values) match {
    case x if !x.isEmpty => toLeft(invalidOidsMsg(x))
    case _ => Right(())
  }

  def findPuuttuvatKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter{case (kieli, arvo) => arvo.isEmpty}.keySet.toSeq)}

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, msg: String): IsValid =
    findPuuttuvatKielet(kielivalinta, k) match {
      case x if !x.isEmpty => toLeft(invalidKielistetty(msg, x))
      case _ => Right(())
    }

  def isValidHakuaika(hakuaika: Ajanjakso): Boolean = hakuaika.alkaa.isBefore(hakuaika.paattyy)
  def validateHakuajat(hakuajat: List[Ajanjakso]): IsValid = hakuajat.filterNot(isValidHakuaika) match {
    case x if x.isEmpty => Right(())
    case x => toLeft(InvalidHakuaika)
  }

  def isValidAlkamisvuosi(s: String): Boolean = VuosiPattern.matcher(s).matches && LocalDate.now().getYear <= Integer.parseInt(s)
  def validateAlkamisvuosi(alkamisvuosi: String): IsValid = assertTrue(isValidAlkamisvuosi(alkamisvuosi), validationMsg(alkamisvuosi))

  def validateAtaruId(hakulomaketyyppi: Option[Hakulomaketyyppi], hakulomakeAtaruId: Option[UUID]): IsValid = hakulomaketyyppi match {
    case Some(Ataru) => assertNotOptional(hakulomakeAtaruId, "hakemuspalvelu ID")
    case _ => Right(())
  }
}
