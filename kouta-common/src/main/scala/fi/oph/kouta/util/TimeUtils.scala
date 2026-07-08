package fi.oph.kouta.util

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}

import fi.oph.kouta.domain.Modified

import scala.util.Try

object TimeUtils {
  private val fiTimezoneId = ZoneId.of("Europe/Helsinki")

  def instantToLocalDateTime(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, fiTimezoneId)

  def localDateTimeToInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(fiTimezoneId).toInstant

  def instantToModified(instant: Instant): Modified = Modified(instantToLocalDateTime(instant))

  def modifiedToInstant(modified: Modified) = localDateTimeToInstant(modified.value)

  def timeStampToModified(timestamp: Timestamp): Modified = instantToModified(timestamp.toInstant)

  def instantToModifiedAt(instant: Instant): Modified = Modified(instantToLocalDateTime(instant).withNano(0))

  def renderHttpDate(instant: Instant): String = {
    DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")))
  }

  def parseHttpDate(string: String): Instant = {
    // Ensisijaisesti RFC-1123 (esim. indeksoijan käyttämä muoto), mutta hyväksytään myös ISO-8601
    Try(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string))).getOrElse(Instant.parse(string))
  }
}
