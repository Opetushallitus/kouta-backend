package fi.oph.kouta.images

import fi.oph.kouta.domain.Oppilaitos
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait LogoService extends TeemakuvaService[OrganisaatioOid, Oppilaitos] {

  def logoPrefix: String

  private def parseTempLogo(oppilaitos: Oppilaitos): DBIO[Option[String]] = parseTempImage("Logo", oppilaitos.logo)

  private def maybeClearLogo(logo: Option[String], oppilaitos: Oppilaitos): DBIO[Oppilaitos] =
    DBIO.successful {
      logo
        .map(_ => oppilaitos.copy(logo = None))
        .getOrElse(oppilaitos)
    }

  def checkAndMaybeClearLogo(oppilaitos: Oppilaitos): DBIO[(Option[String], Oppilaitos)] =
    for {
      tempImage <- parseTempLogo(oppilaitos)
      cleared <- maybeClearLogo(tempImage, oppilaitos)
    } yield (tempImage, cleared)

  def maybeCopyLogo(logo: Option[String], oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): DBIO[Oppilaitos] =
    Try {
      logo
        .map(filename => copyTempImage(filename, logoPrefix, oppilaitos))
        .map(url => oppilaitos.copy(logo = Some(url)))
        .getOrElse(oppilaitos)
    }.toDBIO

  def checkAndMaybeCopyLogo(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): DBIO[(Option[String], Oppilaitos)] =
    for {
      tempImage <- parseTempLogo(oppilaitos)
      o         <- maybeCopyLogo(tempImage, oppilaitos)
    } yield (tempImage, o)
}
