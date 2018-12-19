package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.domain.oid.{Oid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Perustiedot extends Validatable {
  val tila:Julkaisutila
  val nimi: Kielistetty
  val muokkaaja:UserOid
  val kielivalinta:Seq[Kieli]
  val organisaatioOid:OrganisaatioOid

  def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfTrue(tila == Julkaistu, () => and(
      assertTrue(kielivalinta.size > 0, MissingKielivalinta),
      validateKielistetty(kielivalinta, nimi, "nimi")
    )))
}

abstract class PerustiedotWithOid extends Perustiedot with Validatable {
  val oid:Option[Oid]

  override def validate(): IsValid = and(
    super.validate(),
    validateIfDefined[Oid](oid, assertValid(_))
  )
}

abstract class PerustiedotWithId extends Perustiedot with Validatable {
  val id:Option[UUID]

  override def validate(): IsValid = super.validate()
}