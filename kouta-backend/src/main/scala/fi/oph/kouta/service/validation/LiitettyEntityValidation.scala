package fi.oph.kouta.service.validation

import fi.oph.kouta.domain.oid.Oid
import fi.oph.kouta.domain.{Julkaistu, LiitettyEntity, MinimalExistingToteutus}
import fi.oph.kouta.validation.Validations.invalidStateChangeForLiitetty
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidationError}

object LiitettyEntityValidation {
  def validateLiitettyEntityIntegrity(entity: LiitettyEntity, toteutukset: Vector[MinimalExistingToteutus]): IsValid = {
    var errors: List[ValidationError] = List()
    var errorMap: Map[String, List[Option[Oid]]] = Map()

    val addErrorOid = (errorKey: String, oid: Option[Oid]) => {
      errorMap += (errorKey -> (errorMap.getOrElse(errorKey, List()) ++ List(oid)))
    }

    if (entity.tila != Julkaistu) {
      entity.oid match {
        case Some(_) =>
          toteutukset.foreach(t => {
            if (t.tila == Julkaistu) {
              addErrorOid("metadata.tila", Some(t.oid))
            }
          })
        case None =>
      }
    }

    errors = errorMap.toList.map(value => {
      val errorKey = value._1
      val mihinLiitettyOid = value._2
      ValidationError(
        errorKey,
        errorKey match {
          case "metadata.tila" =>
            invalidStateChangeForLiitetty(entity.oid.get, mihinLiitettyOid, toteutukset.map(_.oid))
        }
      )
    })

    if (errors.isEmpty) NoErrors else errors
  }
}
