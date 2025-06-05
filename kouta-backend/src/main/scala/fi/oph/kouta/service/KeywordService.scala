package fi.oph.kouta.service

import fi.oph.kouta.auditlog.{AuditLog, AuditResource}
import fi.oph.kouta.domain.keyword.{Keyword, KeywordSearchBase, KeywordType, Luokittelutermi}
import fi.oph.kouta.repository.{KeywordDAO, KoutaDatabase}
import fi.oph.kouta.security.RoleEntity
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object KeywordService extends KeywordService(AuditLog, OrganisaatioServiceImpl)

class KeywordService(auditLog: AuditLog, val organisaatioService: OrganisaatioService) extends AuthorizationService {

  def search(search: KeywordSearchBase): List[String] = KeywordDAO.search(search)

  def store(`type`: KeywordType, keywords: List[Keyword])(implicit authenticated: Authenticated): Int =
    withRootAccess(RoleEntity.all.flatMap(_.createRoles)) {
      KoutaDatabase.runBlockingTransactionally(insert(`type`, keywords)).get
    }.size

  def insert(`type`: KeywordType, keywords: Seq[Keyword])(implicit authenticated: Authenticated): DBIO[Vector[Keyword]] = {
    for {
      k <- KeywordDAO.putActions(`type`, keywords)
      _ <- audit(`type`, k)
    } yield k
  }

  private def audit(`type`: KeywordType, keywords: Seq[Keyword])(implicit authenticated: Authenticated): DBIO[_] = {
    val logActions = keywords.map { keyword =>
      val targets = Seq("kieli" -> keyword.kieli.name, `type`.name -> keyword.arvo)
      auditLog.logCreate(keyword, AuditResource(`type`), targets)
    }
    DBIO.sequence(logActions)
  }

  def insertLuokittelutermit(luokittelutermit: Seq[String])(implicit authenticated: Authenticated): DBIO[Vector[String]] = {
    for {
      k <- KeywordDAO.insertLuokittelutermit(luokittelutermit)
      _ <- auditLuokittelutermit(k)
    } yield k
  }

  private def auditLuokittelutermit(luokittelutermit: Seq[String])(implicit authenticated: Authenticated): DBIO[_] = {
    val logActions = luokittelutermit.map { luokittelutermi =>
      val targets = Seq(Luokittelutermi.name -> luokittelutermi)
      auditLog.logCreate(Map("luokittelutermi" -> luokittelutermi), AuditResource(Luokittelutermi), targets)
    }

    DBIO.sequence(logActions)
  }
}
