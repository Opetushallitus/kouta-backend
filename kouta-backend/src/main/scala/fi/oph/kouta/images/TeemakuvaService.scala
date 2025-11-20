package fi.oph.kouta.images

import fi.oph.kouta.domain.{HasModified, HasPrimaryId, HasTeemakuva}
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.logging.Logging
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait TeemakuvaService[ID, T <: HasTeemakuva[T] with HasPrimaryId[ID, T] with HasModified[T]] extends Logging {
  val s3ImageService: ImageService

  def teemakuvaPrefix: String

  private def parseTempTeemakuva(entity: T): DBIO[Option[String]] = parseTempImage("Teemakuva", entity.teemakuva)

  protected def parseTempImage(field: String, image: Option[String]): DBIO[Option[String]] =
    Try {
      image match {
        case Some(s3ImageService.tempUrl(filename)) =>
          Some(filename)
        case Some(s3ImageService.publicUrl(_)) =>
          None
        case None =>
          None
        case Some(other) =>
          logger.warn(s"$field outside the bucket: $other")
          None
      }
    }.toDBIO

  private def maybeClearTeemakuva(teemakuva: Option[String], entity: T): DBIO[T] =
    DBIO.successful(
      teemakuva
        .map(_ => entity.withTeemakuva(None))
        .getOrElse(entity)
    )

  def checkAndMaybeClearTeemakuva(entity: T): DBIO[(Option[String], T)] =
    for {
      tempImage <- parseTempTeemakuva(entity)
      cleared   <- maybeClearTeemakuva(tempImage, entity)
    } yield (tempImage, cleared)

  protected def copyTempImage(filename: String, prefix: String, entity: T)(implicit authenticated: Authenticated): String =
    s3ImageService.copyImage(s3ImageService.getTempKey(filename), s"$prefix/${entity.primaryId.get}/$filename")

  def maybeCopyTeemakuva(teemakuva: Option[String], entity: T)(implicit authenticated: Authenticated): DBIO[T] =
    Try {
      teemakuva
        .map(filename => copyTempImage(filename, teemakuvaPrefix, entity))
        .map(url => entity.withTeemakuva(Some(url)))
        .getOrElse(entity)
    }.toDBIO

  def checkAndMaybeCopyTeemakuva(entity: T)(implicit authenticated: Authenticated): DBIO[(Option[String], T)] =
    for {
      teemakuva <- parseTempTeemakuva(entity)
      e         <- maybeCopyTeemakuva(teemakuva, entity)
    } yield (teemakuva, e)

  def maybeDeleteTempImage(teemakuva: Option[String])(implicit authenticated: Authenticated): DBIO[_] =
    Try {
      teemakuva.foreach(filename => s3ImageService.deleteImage(s3ImageService.getTempKey(filename)))
    }.toDBIO
}
