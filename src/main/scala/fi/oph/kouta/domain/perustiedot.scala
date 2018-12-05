package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Perustiedot extends Validatable {
  val tila:Julkaisutila
  val nimi: Kielistetty
  val muokkaaja:String
  val kielivalinta:Seq[Kieli]
  val organisaatioOid:String

  def validate(): IsValid = and(
    assertMatch(muokkaaja, OidPattern),
    assertMatch(organisaatioOid, OidPattern),
    validateIfTrue(tila == Julkaistu, () => and(
      assertTrue(kielivalinta.size > 0, MissingKielivalinta),
      validateKielistetty(kielivalinta, nimi, "nimi")
    )))
}

abstract class PerustiedotWithOid extends Perustiedot with Validatable {
  val oid:Option[String]

  override def validate(): IsValid = and(
    super.validate(),
    validateIfDefined[String](oid, assertMatch(_, OidPattern))
  )
}

abstract class PerustiedotWithId extends Perustiedot with Validatable {
  val id:Option[UUID]

  override def validate(): IsValid = super.validate()
}