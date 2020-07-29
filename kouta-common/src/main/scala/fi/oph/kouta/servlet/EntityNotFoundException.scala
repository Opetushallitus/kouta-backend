package fi.oph.kouta.servlet

case class EntityNotFoundException(message: String) extends RuntimeException(message)
