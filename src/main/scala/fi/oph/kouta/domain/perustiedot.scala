package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.{Oid, OrganisaatioOid, UserOid}
import fi.oph.kouta.security.Authorizable
import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Perustiedot[ID, T] extends Validatable with Authorizable with HasPrimaryId[ID, T] with HasModified[T] {
  val tila: Julkaisutila
  val nimi: Kielistetty
  val muokkaaja: UserOid
  val kielivalinta: Seq[Kieli]
  val organisaatioOid: OrganisaatioOid
  val modified: Option[LocalDateTime]

  def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfTrue(tila == Julkaistu, () => and(
      assertTrue(kielivalinta.nonEmpty, MissingKielivalinta),
      validateKielistetty(kielivalinta, nimi, "nimi")
    )))
}

abstract class PerustiedotWithOid[ID <: Oid, T] extends Perustiedot[ID, T] {
  val oid: Option[Oid]

  override def validate(): IsValid = and(
    super.validate(),
    validateIfDefined[Oid](oid, assertValid(_))
  )
}

abstract class PerustiedotWithId[T] extends Perustiedot[UUID, T] {
  val id: Option[UUID]

  override def validate(): IsValid = super.validate()
}
