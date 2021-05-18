package fi.oph.kouta

import fi.oph.kouta.domain.oid._

import scala.util.Random

object TestOids {
  val OphOid = RootOrganisaatioOid
  val ParentOid = OrganisaatioOid("1.2.246.562.10.594252633210")
  val ChildOid = OrganisaatioOid("1.2.246.562.10.81934895871")
  val EvilChildOid = OrganisaatioOid("1.2.246.562.10.66634895871")
  val GrandChildOid = OrganisaatioOid("1.2.246.562.10.67603619189")
  val EvilGrandChildOid = OrganisaatioOid("1.2.246.562.10.66603619189")
  val EvilCousin = OrganisaatioOid("1.2.246.562.10.66634895666")

  val LonelyOid = OrganisaatioOid("1.2.246.562.10.99999999999")
  val UnknownOid = OrganisaatioOid("1.2.246.562.10.99999999998")
  val YoOid = OrganisaatioOid("1.2.246.562.10.46312206843")
  val AmmOid = OrganisaatioOid("1.2.246.562.10.463122068666")
  val OtherOid = OrganisaatioOid("1.2.246.562.10.67476956288")
  val LukioOid = OrganisaatioOid("1.2.246.562.10.99682778883")

  val TestUserOid = UserOid("1.2.246.562.24.10000000000")
  val OphUserOid = UserOid("1.2.246.562.24.10000000099")

  val random = new Random()

  private def elevenRandomDigits: String = List.fill(11)(random.nextInt(10)).map(_.toString).mkString
  private def randomOid(identifier: String) = s"1.2.246.562.$identifier.$elevenRandomDigits"

  def randomUserOid: UserOid = UserOid(randomOid("24"))
  def randomOrganisaatioOid: OrganisaatioOid = OrganisaatioOid(randomOid("10"))
  def randomKoulutusOid: KoulutusOid = KoulutusOid(randomOid("13"))
  def randomToteutusOid: ToteutusOid = ToteutusOid(randomOid("17"))
  def randomHakukohdeOid: HakukohdeOid = HakukohdeOid(randomOid("20"))
  def randomHakuOid: HakuOid = HakuOid(randomOid("29"))
}
