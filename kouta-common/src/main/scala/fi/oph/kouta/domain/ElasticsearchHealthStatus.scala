package fi.oph.kouta.domain

sealed abstract class ElasticsearchHealthStatus(val name: String, val healthy: Boolean) extends BasicType

object ElasticsearchHealthStatus extends BasicTypeCompanion[ElasticsearchHealthStatus] {

  case object Unreachable extends ElasticsearchHealthStatus("unreachable", false)
  case object Red         extends ElasticsearchHealthStatus("red", false)
  case object Yellow      extends ElasticsearchHealthStatus("yellow", true)
  case object Green       extends ElasticsearchHealthStatus("green", true)

  val all: List[ElasticsearchHealthStatus] = List(Unreachable, Red, Yellow, Green)
}
