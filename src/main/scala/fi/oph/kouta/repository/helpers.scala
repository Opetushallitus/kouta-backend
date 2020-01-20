package fi.oph.kouta.repository

import java.sql.JDBCType
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}
import java.util.UUID

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Ajanjakso, Koulutustyyppi}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO
import slick.jdbc.{PositionedParameters, SetParameter}

import scala.util.{Failure, Success, Try}

trait SQLHelpers extends KoutaJsonFormats with Logging {

  def createOidInParams(x: Seq[Oid]) = x.find(!_.isValid()) match {
    case None if x.isEmpty => s"''"
    case Some(i) => throw new IllegalArgumentException(s"$i ei ole validi oid.")
    case None => x.map(s => s"'$s'").mkString(",")
  }

  def createUUIDInParams(x: Seq[UUID]) = if( x.isEmpty) s"''" else x.map(s => s"'${s.toString}'").mkString(",")

  def createRangeInParams(x: Seq[Ajanjakso]) = if(x.isEmpty) s"''" else x.map(s => s"${toTsrangeString(s)}").mkString(",")

  def formatTimestampParam(value: Option[LocalDateTime]) = value.map(ISO_LOCAL_DATE_TIME_FORMATTER.format).getOrElse(null)

  def toJsonParam(value: AnyRef) = Option(toJson(value)) match {
    case Some(s) if !s.isEmpty & !"{}".equals(s) => s
    case _ => null
  }

  def createKoulutustyypitInParams(x: Seq[Koulutustyyppi]): String = if (x.isEmpty) "''" else x.map(tyyppi => s"'${tyyppi.name}'").mkString(",")

  def toTsrangeString(a: Ajanjakso) = s"'[${ISO_LOCAL_DATE_TIME_FORMATTER.format(a.alkaa)}, ${ISO_LOCAL_DATE_TIME_FORMATTER.format(a.paattyy)})'"

  implicit object SetInstant extends SetParameter[Instant] {
    def apply(v: Instant, pp: PositionedParameters): Unit = {
      pp.setObject(OffsetDateTime.ofInstant(v, ZoneId.of("Europe/Helsinki")), JDBCType.TIMESTAMP_WITH_TIMEZONE.getVendorTypeNumber)
    }
  }

  implicit object SetHakuOid extends SetParameter[HakuOid] {
    def apply(o: HakuOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetHakukohdeOid extends SetParameter[HakukohdeOid] {
    def apply(o: HakukohdeOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetKoulutusOid extends SetParameter[KoulutusOid] {
    def apply(o: KoulutusOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetToteutusOid extends SetParameter[ToteutusOid] {
    def apply(o: ToteutusOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetOrganisaatioOid extends SetParameter[OrganisaatioOid] {
    def apply(o: OrganisaatioOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetUserOid extends SetParameter[UserOid] {
    def apply(o: UserOid, pp: PositionedParameters) {
      pp.setString(o.toString)
    }
  }

  implicit object SetHakuOidOption extends SetParameter[Option[HakuOid]] {
    def apply(o: Option[HakuOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetHakukohdeOidOption extends SetParameter[Option[HakukohdeOid]] {
    def apply(o: Option[HakukohdeOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetKoulutusOidOption extends SetParameter[Option[KoulutusOid]] {
    def apply(o: Option[KoulutusOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetToteutusOidOption extends SetParameter[Option[ToteutusOid]] {
    def apply(o: Option[ToteutusOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetOrganisaatioOidOption extends SetParameter[Option[OrganisaatioOid]] {
    def apply(o: Option[OrganisaatioOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetUserOidOption extends SetParameter[Option[UserOid]] {
    def apply(o: Option[UserOid], pp: PositionedParameters) {
      pp.setStringOption(o.map(_.toString))
    }
  }

  implicit object SetStringSeq extends SetParameter[Seq[String]]   {
    def apply(o: Seq[String], pp: PositionedParameters) {
      pp.setObject(o.toArray, java.sql.Types.ARRAY)
    }
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }
}

object DBIOHelpers {
  def sumIntDBIOs(ints: Seq[DBIO[Int]]): DBIO[Int] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    DBIO.fold(ints, 0)(_ + _)
  }

  def toDBIO[T](value: Try[T]): DBIO[T] = value match {
    case Success(v) => DBIO.successful(v)
    case Failure(e) => DBIO.failed(e)
  }

  class DbioCapableTry[+T](val t: Try[T]) {
    def toDBIO: DBIO[T] = t match {
      case Success(v) => DBIO.successful(v)
      case Failure(e) => DBIO.failed(e)
    }
  }

  import scala.language.implicitConversions
  implicit def tryToDbioCapableTry[T](t: Try[T]): DbioCapableTry[T] = new DbioCapableTry[T](t)
}
