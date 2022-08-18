package fi.oph.kouta.service

import fi.oph.kouta.domain.Haku
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.{IsValid, NoErrors}

object HakuServiceValidation extends HakuServiceValidation(OrganisaatioServiceImpl)
class HakuServiceValidation(val organisaatioService: OrganisaatioService) extends ValidatingService[Haku] {
  override def validateEntity(haku: Haku, crudOperation: CrudOperation): IsValid = ???

  override def validateInternalDependenciesWhenDeletingEntity(haku: Haku): IsValid = ???

  override def validateEntityOnJulkaisu(haku: Haku, crudOperation: CrudOperation): IsValid = ???
}
