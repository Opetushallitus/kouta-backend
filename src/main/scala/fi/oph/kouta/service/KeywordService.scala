package fi.oph.kouta.service

import fi.oph.kouta.domain.keyword.{Keyword, KeywordSearch, KeywordType}
import fi.oph.kouta.repository.KeywordDAO

object KeywordService {

  def search(search:KeywordSearch): List[String] = KeywordDAO.search(search)

  def store(`type`:KeywordType, keywords:List[Keyword]): Int = KeywordDAO.put(`type`, keywords)
}
