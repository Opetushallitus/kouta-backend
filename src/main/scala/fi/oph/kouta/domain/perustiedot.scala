package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

sealed trait Perustiedot extends Validatable {
  val tila:Julkaisutila
  val nimi: Kielistetty
  val muokkaaja:String
  val kielivalinta:Seq[Kieli]

  def validate(): IsValid = for {
    _ <- validateMuokkaaja(muokkaaja).right
    x <- validateIfTrue(tila == Julkaistu, () => for {
      _ <- validateKielivalinta(kielivalinta).right
      y <- validateKielistetty(kielivalinta, nimi, "nimi").right
    } yield y ).right
  } yield x
}

abstract class PerustiedotWithOid extends Perustiedot with Validatable {
  val oid:Option[String]

  override def validate(): IsValid = for {
    _ <- super.validate().right
    x <- validateOid(oid).right
  } yield x
}

abstract class PerustiedotWithId extends Perustiedot with Validatable {
  val id:Option[UUID]

  override def validate(): IsValid = super.validate()
}