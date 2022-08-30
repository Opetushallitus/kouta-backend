package fi.oph.kouta.elasticsearch
import com.sksamuel.elastic4s.ElasticError

case class ElasticSearchException(error: ElasticError) extends RuntimeException
