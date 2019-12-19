package fi.oph.kouta.images

import fi.oph.kouta.domain.{HasModified, HasPrimaryId, HasTeemakuva}
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait TeemakuvaService[ID, T <: HasTeemakuva[T] with HasPrimaryId[ID, T] with HasModified[T]] extends Logging {
  val s3Service: S3Service

  def teemakuvaPrefix: String

  def checkTeemakuva(entity: T): DBIO[Option[String]] = checkTempImage("Teemakuva", entity.teemakuva)

  def checkTempImage(field: String, image: Option[String]): DBIO[Option[String]] =
    Try {
      image match {
        case Some(s3Service.tempUrl(filename)) =>
          Some(filename)
        case Some(s3Service.publicUrl(_)) =>
          None
        case None =>
          None
        case Some(other) =>
          logger.warn(s"$field outside the bucket: $other")
          None
      }
    }.toDBIO

  def maybeClearTeemakuva(teemakuva: Option[String], entity: T): DBIO[T] =
    Try {
      teemakuva
        .map(_ => entity.withTeemakuva(None))
        .getOrElse(entity)
    }.toDBIO

  def checkAndMaybeClearTeemakuva(entity: T): DBIO[(Option[String], T)] =
    for {
      tempImage <- checkTeemakuva(entity)
      cleared   <- maybeClearTeemakuva(tempImage, entity)
    } yield (tempImage, cleared)

  def copyTempImage(filename: String, prefix: String, entity: T)(implicit authenticated: Authenticated): String = {
    s3Service.copyImage(s3Service.getTempKey(filename), s"$prefix/${entity.primaryId.get}/$filename")
  }

  def maybeCopyTeemakuva(teemakuva: Option[String], entity: T)(implicit authenticated: Authenticated): DBIO[T] =
    Try {
      teemakuva
        .map(filename => copyTempImage(filename, teemakuvaPrefix, entity))
        .map(url => entity.withTeemakuva(Some(url)))
        .getOrElse(entity)
    }.toDBIO

  def checkAndMaybeCopyTeemakuva(entity: T)(implicit authenticated: Authenticated): DBIO[(Option[String], T)] =
    for {
      teemakuva <- checkTeemakuva(entity)
      e         <- maybeCopyTeemakuva(teemakuva, entity)
    } yield (teemakuva, e)

  def maybeDeleteTempImage(teemakuva: Option[String])(implicit authenticated: Authenticated): DBIO[_] =
    Try {
      teemakuva.foreach(filename => s3Service.deleteImage(s3Service.getTempKey(filename)))
    }.toDBIO
}
