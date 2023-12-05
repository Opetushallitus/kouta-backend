package fi.oph.kouta.domain.oid

import java.util.regex.Pattern

sealed trait Oid {
  val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.\d+\.\d+$""")
  val s: String

  override def toString: String = s

  def isValid: Boolean = OidPattern.matcher(s).matches()
}

case class GenericOid(s: String) extends Oid

case class KoulutusOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.13\.\d+$""")
}

case class ToteutusOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.17\.\d+$""")
}

case class HakukohdeOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.20\.\d+$""")
}

case class HakukohderyhmaOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.28\.\d+$""")
}

case class HakuOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.29\.\d+$""")
}

case class OrganisaatioOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.(10|99)\.\d+$""")
}

object RootOrganisaatioOid extends OrganisaatioOid("1.2.246.562.10.00000000001")

case class UserOid(s: String) extends Oid {
  override val OidPattern: Pattern = Pattern.compile("""^1\.2\.246\.562\.24\.\d{11}$""")
}
