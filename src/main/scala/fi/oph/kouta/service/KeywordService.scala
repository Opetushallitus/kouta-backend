package fi.oph.kouta.service

import fi.oph.kouta.domain.keyword.{KeywordSearch, Keywords}
import fi.oph.kouta.repository.KeywordDAO

object KeywordService {

  def search(search:KeywordSearch): List[String] = KeywordDAO.search(search)

  def store(keywords:Keywords): Int = KeywordDAO.put(keywords)
}
