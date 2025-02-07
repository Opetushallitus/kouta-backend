package fi.oph.kouta.servlet

import fi.oph.kouta.logging.Logging

import java.net.InetAddress
import java.util.UUID

import javax.servlet.http.HttpServletRequest
import fi.oph.kouta.security.Session

case class Authenticated(id: String, session: Session, userAgent: String, ip: InetAddress)

object Authenticated extends Logging {

  def getRemoteAddress(httpServletRequest: HttpServletRequest): String = getRemoteAddress(httpServletRequest.getHeader("X-Real-IP"), httpServletRequest.getHeader("X-Forwarded-For"), httpServletRequest.getRemoteAddr, httpServletRequest.getRequestURI)

  def getRemoteAddress(xRealIp: String, xForwardedFor: String, remoteAddr: String, requestURI: String): String = {
    val isNotBlank = (txt: String) => txt != null && !txt.isEmpty
    if (isNotBlank(xRealIp)) return xRealIp
    if (isNotBlank(xForwardedFor)) {
      if (xForwardedFor.contains(",")) logger.error("Could not find X-Real-IP header, but X-Forwarded-For contains multiple values: {}, " + "this can cause problems", xForwardedFor)
      return xForwardedFor
    }
    remoteAddr
  }

  def apply(id: UUID, session: Session)(implicit request: HttpServletRequest): Authenticated = {
    val userAgent = Option(request.getHeader("User-Agent")).getOrElse(throw new IllegalArgumentException("Otsake User-Agent on pakollinen."))
    val ip = InetAddress.getByName(getRemoteAddress(request))
    new Authenticated(id.toString, session, userAgent, ip)
  }
}
