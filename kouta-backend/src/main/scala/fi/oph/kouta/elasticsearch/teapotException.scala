package fi.oph.kouta.elasticsearch

case class TeapotException(msg: String, error: Throwable) extends RuntimeException
