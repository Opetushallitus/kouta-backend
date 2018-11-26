package fi.oph.kouta.repository

import fi.oph.kouta.domain.keyword._
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

trait KeywordDAO {
  def search(search:KeywordSearch): List[String]
  def put(`type`:KeywordType, keywords:List[Keyword]): Int
  def insert(`type`:KeywordType, keywords:List[Keyword]): DBIO[List[Int]]
}

object KeywordDAO extends KeywordDAO with KeywordSQL {

  override def search(search: KeywordSearch): List[String] =
    KoutaDatabase.runBlocking(
      searchKeywordsByPrefix(search).zip(searchKeywordsByMatch(search))
    ) match {
      case (l1, l2) => l1.union(l2).distinct.take(search.limit).toList
    }

  override def put(`type`:KeywordType, keywords:List[Keyword]): Int =
    KoutaDatabase.runBlockingTransactionally(
      insertKeywords(`type`, keywords)
    ) match {
      case Left(t) => throw t
      case Right(i) => i.sum
    }

  override def insert(`type`: KeywordType, keywords: List[Keyword]): DBIO[List[Int]] =
    insertKeywords(`type`, keywords)
}

sealed trait KeywordSQL extends KeywordExtractors with SQLHelpers {

  private def fieldAndTable(`type`:KeywordType) = `type` match {
    case Ammattinimike => ("ammattinimike", "ammattinimikkeet")
    case Asiasana => ("asiasana", "asiasanat")
    case _ => ???
  }

  def searchKeywordsByPrefix(search: KeywordSearch) =
    searchKeywords(search)(s"${search.term}%")

  def searchKeywordsByMatch(search: KeywordSearch) =
    searchKeywords(search)(s"%${search.term}%")

  private def searchKeywords(search: KeywordSearch)(like:String) = {
    val (field, table) = fieldAndTable(search.`type`)
    sql"""select #$field from #$table
          where kieli = ${search.kieli.toString}::kieli
          and #$field like $like
          order by #$field asc
          limit ${search.limit} """.as[String]
  }

  def insertKeywords(`type`:KeywordType, keywords:List[Keyword]) = {
    val (field, table) = fieldAndTable(`type`)
    val pkey = s"${table}_pkey"
    DBIO.sequence(keywords.map{case Keyword(kieli, keyword) =>
      sqlu"""insert into #$table (#$field, kieli)
             values (${keyword.toLowerCase}, ${kieli.toString}::kieli)
             on conflict on constraint #$pkey do nothing""" })
  }
}