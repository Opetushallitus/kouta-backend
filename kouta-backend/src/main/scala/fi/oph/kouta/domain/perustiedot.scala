package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.domain.oid.{Oid, OrganisaatioOid, UserOid}
import fi.oph.kouta.security.AuthorizableEntity
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Perustiedot[ID, T]
    extends Validatable
    with AuthorizableEntity[T]
    with HasPrimaryId[ID, T]
    with HasModified[T]
    with HasMuokkaaja[T] {
  val tila: Julkaisutila
  val nimi: Kielistetty
  val muokkaaja: UserOid
  val kielivalinta: Seq[Kieli]
  val organisaatioOid: OrganisaatioOid
  val modified: Option[Modified]

  def validate(): IsValid = and(
    assertValid(organisaatioOid, "organisaatioOid"),
    validateKielistetty(kielivalinta, nimi, "nimi"),
    assertNotEmpty(kielivalinta, "kielivalinta")
  )
}

abstract class PerustiedotWithOid[ID <: Oid, T] extends Perustiedot[ID, T] {
  val oid: Option[ID]
  def withOid(oid: ID): T

  override def validate(): IsValid = and(
    super.validate(),
    validateIfDefined[Oid](oid, assertValid(_, "oid"))
  )

  override def primaryId: Option[ID] = oid

  override def withPrimaryID(oid: ID): T = withOid(oid)
}

abstract class PerustiedotWithOidAndOptionalNimi[ID <: Oid, T] extends PerustiedotWithOid[ID, T] {
  override def validate(): IsValid = and(
    validateIfDefined[Oid](oid, assertValid(_, "oid")),
    assertValid(organisaatioOid, "organisaatioOid"),
    assertNotEmpty(kielivalinta, "kielivalinta")
  )
}

abstract class PerustiedotWithId[T] extends Perustiedot[UUID, T] {
  val id: Option[UUID]
  def withId(id: UUID): T

  override def validate(): IsValid = super.validate()

  override def primaryId: Option[UUID] = id

  override def withPrimaryID(id: UUID): T = withId(id)
}
