package fi.oph.kouta.service

import fi.oph.kouta.security.Role

case class RoleAuthorizationFailedException(acceptedRoles: Seq[Role], existingRoles: Iterable[Role])
  extends RuntimeException({
    val accepted = acceptedRoles.map(_.name).mkString(",")
    val existing = existingRoles.map(_.name).mkString(",")

    s"Authorization failed, missing role. Accepted roles: $accepted. Existing roles: $existing."
  })
