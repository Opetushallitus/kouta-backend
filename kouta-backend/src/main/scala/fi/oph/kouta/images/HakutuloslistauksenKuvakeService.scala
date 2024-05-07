package fi.oph.kouta.images

import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait HakutuloslistauksenKuvakeService extends TeemakuvaService[KoulutusOid, Koulutus] {
  val s3ImageService: S3ImageService

  def kuvakePrefix: String

  private def parseTempKuvake(koulutus: Koulutus): DBIO[Option[String]] =
    parseTempImage("Hakutuloslistauksen kuvake", koulutus.hakutuloslistauksenKuvake)

  private def maybeClearKuvake(teemakuva: Option[String], koulutus: Koulutus): DBIO[Koulutus] =
    DBIO.successful(
      teemakuva
        .map(_ => koulutus.withHakutuloslistauksenKuvake(None))
        .getOrElse(koulutus)
    )

  def checkAndMaybeClearKuvake(koulutus: Koulutus): DBIO[(Option[String], Koulutus)] =
    for {
      tempImage <- parseTempKuvake(koulutus)
      cleared   <- maybeClearKuvake(tempImage, koulutus)
    } yield (tempImage, cleared)

  def maybeCopyKuvake(kuvake: Option[String], koulutus: Koulutus)(implicit
      authenticated: Authenticated
  ): DBIO[Koulutus] =
    Try {
      kuvake
        .map(filename => copyTempImage(filename, kuvakePrefix, koulutus))
        .map(url => koulutus.withHakutuloslistauksenKuvake(Some(url)))
        .getOrElse(koulutus)
    }.toDBIO

  def checkAndMaybeCopyKuvake(
      koulutus: Koulutus
  )(implicit authenticated: Authenticated): DBIO[(Option[String], Koulutus)] =
    for {
      kuvake <- parseTempKuvake(koulutus)
      e      <- maybeCopyKuvake(kuvake, koulutus)
    } yield (kuvake, e)
}
