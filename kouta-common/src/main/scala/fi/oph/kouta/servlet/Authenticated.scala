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

object AuthenticatedSwagger {
  val authenticatedModel =
    """    Authenticated:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Session id (UUID)
      |          example: b0c9ccd3-9f56-4d20-8df4-21a1239fcf89
      |        ip:
      |          type: string
      |          description: Kutsujan IP
      |          example: 127.0.0.1
      |        userAgent:
      |          type: string
      |          description: Kutsujan user-agent
      |          example: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
      |        session:
      |          type: object
      |          properties:
      |            personOid:
      |              type: string
      |              description: Henkilön oid
      |              example: 1.2.246.562.10.00101010101
      |            authorities:
      |              type: array
      |              items:
      |                type: object
      |                properties:
      |                  authority:
      |                    type: string
      |                    description: Yksittäinen käyttöoikeus
      |                    example: APP_KOUTA_OPHPAAKAYTTAJA_1.2.246.562.10.00000000001
      |""".stripMargin

  val models = List(authenticatedModel)
}
