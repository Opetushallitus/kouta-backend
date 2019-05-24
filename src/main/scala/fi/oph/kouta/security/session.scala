package fi.oph.kouta.security

import fi.oph.kouta.domain.oid.OrganisaatioOid

import scala.util.matching.Regex

sealed abstract class Role(val name: String)

object Role {
  case object CrudUser extends Role("APP_TARJONTA_CRUD")

  case class UnknownRole(override val name: String) extends Role(name)

  val all: Map[String, Role] = List(CrudUser).map(r => r.name -> r).toMap

  def apply(s: String): Role = all.getOrElse(s, UnknownRole(s))
}

case class Authority(authority: String) {
  import Authority.OrganisaatioRegex

  lazy val role: Role = Role(OrganisaatioRegex.replaceAllIn(authority, ""))

  lazy val organisaatioId: Option[OrganisaatioOid] = OrganisaatioRegex.findFirstIn(authority)
    .map(_.filterNot(_ == '_'))
    .map(OrganisaatioOid.apply)
}

object Authority {
  val OrganisaatioRegex: Regex = """_1\.2\.246\.562\.10.[\d]+$""".r

  def apply(role: Role, organisaatioOid: OrganisaatioOid): Authority = this(s"${role.name}_$organisaatioOid")
}

sealed trait Session {
  def hasAnyRole(roles: Set[Role]): Boolean
  def hasEveryRole(roles: Set[Role]): Boolean
  def personOid: String
  def authorities: Set[Authority]

  lazy val roleMap: Map[Role, Set[OrganisaatioOid]] = authorities.groupBy(_.role).mapValues(_.flatMap(_.organisaatioId))
  lazy val roles: Set[Role] = roleMap.keySet
}

case class ServiceTicket(s: String)
case class CasSession(casTicket: ServiceTicket, personOid: String, authorities: Set[Authority]) extends Session {
  override def hasAnyRole(roles: Set[Role]): Boolean = this.roles.intersect(roles).nonEmpty
  override def hasEveryRole(roles: Set[Role]): Boolean = roles.subsetOf(this.roles)
}

/*
case class AuditSession(personOid:String, roles:Set[Role]) extends Session {
  override def hasAnyRole(roles: Set[Role]): Boolean = this.roles.intersect(roles).nonEmpty
  override def hasEveryRole(roles: Set[Role]): Boolean = roles.subsetOf(this.roles)
}
  */
