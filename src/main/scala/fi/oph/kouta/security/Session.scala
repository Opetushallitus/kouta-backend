package fi.oph.kouta.security

sealed abstract class Role(val name: String)

object Role {
  //TODO: meaningful roles for this project
  case object CrudUser extends Role("APP_TARJONTA_CRUD")

  case class UnknownRole(override val name: String) extends Role(name)

  val all: Map[String, Role] = List(CrudUser).map(r => r.name -> r).toMap

  def apply(s: String): Role = all.getOrElse(s, UnknownRole(s))
}

sealed trait Session {
  def hasAnyRole(roles: Set[Role]): Boolean
  def hasEveryRole(roles: Set[Role]): Boolean
  def personOid: String
  def roles: Set[Role]
}

case class ServiceTicket(s: String)
case class CasSession(casTicket: ServiceTicket, personOid: String, roles: Set[Role]) extends Session {
  override def hasAnyRole(roles: Set[Role]): Boolean = this.roles.intersect(roles).nonEmpty
  override def hasEveryRole(roles: Set[Role]): Boolean = roles.subsetOf(this.roles)
}

/*
case class AuditSession(personOid:String, roles:Set[Role]) extends Session {
  override def hasAnyRole(roles: Set[Role]): Boolean = this.roles.intersect(roles).nonEmpty
  override def hasEveryRole(roles: Set[Role]): Boolean = roles.subsetOf(this.roles)
}
  */
