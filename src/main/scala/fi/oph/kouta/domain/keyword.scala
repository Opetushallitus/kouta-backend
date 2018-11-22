package fi.oph.kouta.domain

import scala.util.Try

package object keyword {

  sealed trait KeywordType extends EnumType
  object KeywordType extends Enum[KeywordType] {
    override def name: String = "Asiasanan tyyppi"
    override def values(): List[KeywordType] = List(Asiasana, Ammattinimike)
  }
  case object Asiasana extends KeywordType { val name = "asiasana" }
  case object Ammattinimike extends KeywordType { val name = "ammattinimike" }

  case class Keyword(kieli:Kieli, arvo:String)

  case class KeywordSearch(term:String,
                           kieli:Kieli,
                           `type`:KeywordType,
                           limit:Int)
}

