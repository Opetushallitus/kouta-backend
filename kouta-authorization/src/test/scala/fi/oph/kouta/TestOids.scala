package fi.oph.kouta

import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, UserOid}

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

  val TestUserOid = UserOid("1.2.246.562.24.10000000000")
}
