package fi.oph.kouta.repository

import fi.oph.kouta.domain.keyword._
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

trait KeywordDAO {
  def search(search: KeywordSearchBase): List[String]
  def putActions(`type`: KeywordType, keywords: Seq[Keyword]): DBIO[Vector[Keyword]]
  def insertLuokittelutermit(luokittelutermit: Seq[String]): DBIO[Vector[String]]
}

object KeywordDAO extends KeywordDAO with KeywordSQL {

  override def search(search: KeywordSearchBase): List[String] =
    KoutaDatabase.runBlocking(
      searchKeywordsByPrefix(search).zip(searchKeywordsByMatch(search))
    ) match {
      case (l1, l2) => l1.union(l2).distinct.take(search.limit).toList
    }

  def putActions(`type`: KeywordType, keywords: Seq[Keyword]): DBIO[Vector[Keyword]] = insertKeywords(`type`, keywords)

  def insertLuokittelutermit(luokittelutermit: Seq[String]): DBIO[Vector[String]] = insertLuokittelutermitSQL(luokittelutermit)
}

sealed trait KeywordSQL extends KeywordExtractors with SQLHelpers {

  private def fieldAndTable(`type`: KeywordType): (String, String) = `type` match {
    case Ammattinimike => ("ammattinimike", "ammattinimikkeet")
    case Asiasana => ("asiasana", "asiasanat")
    case Luokittelutermi => ("luokittelutermi", "luokittelutermit")
    case _ => throw new IllegalArgumentException(s"Unknown keyword type: ${`type`}")
  }

  def searchKeywordsByPrefix(search: KeywordSearchBase): DBIO[Vector[String]] = {
    search match {
      case s: KeywordSearch => searchKeywords(s)(s"${s.term}%")
      case s: LuokittelutermiSearch => searchLuokittelutermit(s)(s"${s.term}%")
    }
  }

  def searchKeywordsByMatch(search: KeywordSearchBase): DBIO[Vector[String]] =
    search match {
      case s: KeywordSearch => searchKeywords(s)(s"%${s.term}%")
      case s: LuokittelutermiSearch => searchLuokittelutermit(s)(s"%${s.term}%")
    }

  private def searchKeywords(search: KeywordSearch)(like: String): DBIO[Vector[String]] = {
    val (field, table) = fieldAndTable(search.`type`)
    val kieli: String = search.kieli.toString
    val likePattern: String = like.toLowerCase
    val limit: Int = search.limit
    sql"""select #$field from #$table
          where kieli = $kieli::kieli
          and #$field like $likePattern
          order by #$field asc
          limit $limit """.as[String]
  }

  def insertKeywords(`type`: KeywordType, keywords: Seq[Keyword]): DBIO[Vector[Keyword]] = {
    val (field, table) = fieldAndTable(`type`)
    val pkey = s"${table}_pkey"
    val inserts = keywords.map { case Keyword(kieli, keyword) =>
      val kieliStr: String = kieli.toString
      val keywordLower: String = keyword.toLowerCase
      sql"""insert into #$table (#$field, kieli)
            values ($keywordLower, $kieliStr::kieli)
            on conflict on constraint #$pkey do nothing
            returning kieli, #$field""".as[Keyword]
    }
    DBIO.fold(inserts, Vector()) { (first, second) => first ++ second }
  }

  private def searchLuokittelutermit(search: LuokittelutermiSearch)(like: String): DBIO[Vector[String]] = {
    val (field, table) = fieldAndTable(search.`type`)
    val likePattern: String = like.toLowerCase
    val limit: Int = search.limit
    sql"""select #$field from #$table
          where #$field like $likePattern
          order by #$field asc
          limit $limit """.as[String]
  }

  def insertLuokittelutermitSQL(luokittelutermit: Seq[String]): DBIO[Vector[String]] = {
    val (field, table) = fieldAndTable(Luokittelutermi)
    val pkey = s"${table}_pkey"
    val inserts = luokittelutermit.map { case s: String =>
      val sLower: String = s.toLowerCase
      sql"""insert into #$table
            values ($sLower)
            on conflict on constraint #$pkey do nothing
            returning #$field""".as[String]
    }
    DBIO.fold(inserts, Vector()) { (first, second) => first ++ second }
  }
}
