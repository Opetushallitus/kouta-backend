package fi.oph.kouta.validation

import java.time.{ LocalDate}
import java.util.regex.Pattern

import fi.oph.kouta.domain.Hakuaika

object OidValidator {
  val KoulutusOidPrefix = "1.2.246.562.13."
  val ToteutusOidPrefix = "1.2.246.562.17."
  val HakukohdeOidPrefix = "1.2.246.562.20."
  val HakuOidPrefix = "1.2.246.562.29."
  private val oidPattern = Pattern.compile("""^[\d][\d\.]+[\d]$""")

  def isOid(s: String): Boolean = oidPattern.matcher(s).matches
  def isKoulutusOid(s:String): Boolean = isOid(s) && s.startsWith(KoulutusOidPrefix)
  def isToteutusOid(s:String): Boolean = isOid(s) && s.startsWith(ToteutusOidPrefix)
  def isHakukohdeOid(s:String): Boolean = isOid(s) && s.startsWith(HakukohdeOidPrefix)
  def isHakuOid(s:String): Boolean = isOid(s) && s.startsWith(HakuOidPrefix)
}

object KoodiValidator {
  private val KoulutusKoodiPattern = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  private val HakutapaKoodiPattern = Pattern.compile("""hakutapa_\d{1,3}#\d{1,2}""")
  private val KausiKoodiPattern = Pattern.compile("""kausi_\w{1}#\d{1,2}""")
  private val KohdejoukkoKoodiPattern = Pattern.compile("""kohdejoukko_\d+#\d{1,2}""")
  private val KohdejoukonTarkenneKoodiPattern = Pattern.compile("""haunkohdejoukontarkenne_\d+#\d{1,2}""")
  private val PohjakoulutusvaatimusKoodiPattern = Pattern.compile("""pohjakoulutusvaatimus_\d+#\d{1,2}""")

  def isKoulutusKoodi(s:String) = KoulutusKoodiPattern.matcher(s).matches
  def isHakutapaKoodi(s:String) = HakutapaKoodiPattern.matcher(s).matches
  def isKausiKoodi(s:String) = KausiKoodiPattern.matcher(s).matches
  def isKohdejoukkoKoodi(s:String) = KohdejoukkoKoodiPattern.matcher(s).matches
  def isKohdejoukonTarkenneKoodi(s:String) = KohdejoukonTarkenneKoodiPattern.matcher(s).matches
  def isPohjakoulutusvaatimusKoodi(s:String) = PohjakoulutusvaatimusKoodiPattern.matcher(s).matches
}

object TimeValidator {
  private val VuosiPattern = Pattern.compile("""\d{4}""")

  def isValidAlkamisvuosi(s:String) = VuosiPattern.matcher(s).matches && LocalDate.now().getYear <= Integer.parseInt(s)
  def isValidHakuaika(hakuaika:Hakuaika) = hakuaika.alkaa.isBefore(hakuaika.paattyy)

}



