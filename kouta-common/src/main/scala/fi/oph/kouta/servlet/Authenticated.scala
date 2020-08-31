package fi.oph.kouta.servlet

import java.net.InetAddress
import java.util.UUID
import fi.vm.sade.javautils.http.HttpServletRequestUtils
import javax.servlet.http.HttpServletRequest

import fi.oph.kouta.security.Session

case class Authenticated(id: String, session: Session, userAgent: String, ip: InetAddress)

object Authenticated {
  def apply(id: UUID, session: Session)(implicit request: HttpServletRequest): Authenticated = {
    val userAgent = Option(request.getHeader("User-Agent")).getOrElse(throw new IllegalArgumentException("Otsake User-Agent on pakollinen."))
    val ip = InetAddress.getByName(HttpServletRequestUtils.getRemoteAddress(request))
    new Authenticated(id.toString, session, userAgent, ip)
  }
}
