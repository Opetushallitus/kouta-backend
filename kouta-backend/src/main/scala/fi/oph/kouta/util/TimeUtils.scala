package fi.oph.kouta.util

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}

object TimeUtils {
  def instantToLocalDateTime(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Helsinki"))

  def localDateTimeToInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of("Europe/Helsinki")).toInstant

  def timeStampToLocalDateTime(timestamp: Timestamp): LocalDateTime = instantToLocalDateTime(timestamp.toInstant)

  def instantToModifiedAt(instant: Instant): LocalDateTime = instantToLocalDateTime(instant).withSecond(0).withNano(0)

  def renderHttpDate(instant: Instant): String = {
    DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")))
  }

  def parseHttpDate(string: String): Instant = {
    Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string))
  }
}
