package fi.oph.kouta.service

import fi.oph.kouta.domain.keyword.{Keyword, KeywordSearch, KeywordType}
import fi.oph.kouta.repository.KeywordDAO
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object KeywordService extends AuthorizationService {

  val roleEntity: RoleEntity = Role.Haku

  def search(search: KeywordSearch): List[String] = KeywordDAO.search(search)

  def store(`type`: KeywordType, keywords: List[Keyword])(implicit authenticated: Authenticated): Int =
    withRootAccess(RoleEntity.all.flatMap(_.createRoles)) (KeywordDAO.put(`type`, keywords))
}
