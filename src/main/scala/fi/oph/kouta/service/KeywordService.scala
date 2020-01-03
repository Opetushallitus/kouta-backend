package fi.oph.kouta.service

import fi.oph.kouta.auditlog.{AuditLog, AuditResource}
import fi.oph.kouta.domain.keyword.{Keyword, KeywordSearch, KeywordType}
import fi.oph.kouta.repository.{KeywordDAO, KoutaDatabase}
import fi.oph.kouta.security.RoleEntity
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object KeywordService extends KeywordService(AuditLog)

class KeywordService(auditLog: AuditLog) extends AuthorizationService {

  def search(search: KeywordSearch): List[String] = KeywordDAO.search(search)

  def store(`type`: KeywordType, keywords: List[Keyword])(implicit authenticated: Authenticated, request: HttpServletRequest): Int =
    withRootAccess(RoleEntity.all.flatMap(_.createRoles)) {
      KoutaDatabase.runBlockingTransactionally(insert(`type`, auditLog.getUser, keywords)).get
    }.size

  def insert(`type`: KeywordType, user: User, keywords: Seq[Keyword]): DBIO[Vector[Keyword]] = {
    for {
      k <- KeywordDAO.putActions(`type`, keywords)
      _ <- audit(`type`, k, user)
    } yield k
  }

  private def audit(`type`: KeywordType, keywords: Seq[Keyword], user: User): DBIO[_] = {
    val logActions = keywords.map { keyword =>
      val targets = Seq("kieli" -> keyword.kieli.name, `type`.name -> keyword.arvo)
      auditLog.logCreate(keyword, AuditResource(`type`), user, targets)
    }
    DBIO.sequence(logActions)
  }

}
