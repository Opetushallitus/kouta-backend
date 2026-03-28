package fi.oph.kouta.security

import fi.oph.kouta.domain.oid.{HakukohderyhmaOid, OrganisaatioOid, RootOrganisaatioOid}

import scala.util.matching.Regex

sealed abstract class Role(val name: String) extends Product with Serializable {
  override def toString: String = name
}

sealed abstract class RoleEntity(val entity: String) {
  case object Read extends Role(s"APP_KOUTA_${entity}_READ")
  case object Update extends Role(s"APP_KOUTA_${entity}_READ_UPDATE")
  case object Crud extends Role(s"APP_KOUTA_${entity}_CRUD")

  val roles: Seq[Role] = Seq(Read, Update, Crud)

  def createRoles: Seq[Role] = Seq(Crud, Role.Paakayttaja, Role.External)
  def readRoles: Seq[Role] = Seq(Crud, Role.Indexer, Read, Update, Role.Paakayttaja, Role.External, Role.Reporter)
  def updateRoles: Seq[Role] = Seq(Crud, Update, Role.Paakayttaja, Role.External)
  def deleteRoles: Seq[Role] = Seq(Crud, Role.Paakayttaja)
}

object RoleEntity {
  val all: List[RoleEntity] = List(Role.Koulutus, Role.Toteutus, Role.Haku, Role.Hakukohde, Role.Valintaperuste, Role.Oppilaitos)
}

object Role {
  case object Indexer extends Role("APP_KOUTA_INDEKSOINTI")
  case object Paakayttaja extends Role("APP_KOUTA_OPHPAAKAYTTAJA")
  case object External extends Role("APP_KOUTA_EXTERNAL")
  case object Reporter extends Role("APP_KOUTA_REPORTER")

  object Koulutus extends RoleEntity("KOULUTUS")
  object Toteutus extends RoleEntity("TOTEUTUS")
  object Haku extends RoleEntity("HAKU")
  object Hakukohde extends RoleEntity("HAKUKOHDE")
  object Valintaperuste extends RoleEntity("VALINTAPERUSTE")
  object Oppilaitos extends RoleEntity("OPPILAITOS")

  case class UnknownRole(override val name: String) extends Role(name)

  val all: Map[String, Role] = (Paakayttaja :: Indexer :: External :: Reporter :: RoleEntity.all.flatMap(_.roles)).map(r => r.name -> r).toMap

  def apply(s: String): Role = all.getOrElse(s, UnknownRole(s))
}

case class Authority(authority: String) {
  import Authority.OrganisaatioRegex

  lazy val  role: Role = Role(OrganisaatioRegex.replaceAllIn(authority, ""))

  lazy val organisaatioId: Option[OrganisaatioOid] = OrganisaatioRegex.findFirstIn(authority)
    .map(_.filterNot(_ == '_'))
    .map(OrganisaatioOid.apply)
}

object Authority {
  val OrganisaatioRegex: Regex = RootOrganisaatioOid.OidPattern.pattern().replace("^", "_").r

  def apply(role: Role, organisaatioOid: OrganisaatioOid): Authority = this(s"${role.name}_$organisaatioOid")
  def apply(role: Role, hakukohderyhmaOid: HakukohderyhmaOid): Authority = this(s"${role.name}_$hakukohderyhmaOid")
}

sealed trait Session {
  def personOid: String
  def authorities: Set[Authority]

  lazy val roleMap: Map[Role, Set[OrganisaatioOid]] = authorities.groupBy(_.role).view.mapValues(_.flatMap(_.organisaatioId)).toMap
  lazy val roles: Set[Role] = roleMap.keySet

  def getOrganizationsForRoles(requiredRoles: Seq[Role]): Set[OrganisaatioOid] =
    requiredRoles.flatMap(roleMap.get).fold(Set())(_ union _)
}

case class ServiceTicket(s: String)
case class CasSession(casTicket: ServiceTicket, personOid: String, authorities: Set[Authority]) extends Session
case class ExternalSession(personOid: String, authorities: Set[Authority]) extends Session
