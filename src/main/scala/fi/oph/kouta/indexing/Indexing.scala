package fi.oph.kouta.indexing

import fi.oph.kouta.domain._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, render}

trait Indexing[A] {
  def index: String
  def indexId(a: A): Option[String]
}

trait WithOidIndexing[A <: PerustiedotWithOid] {
  this: Indexing[A] =>

  override def indexId(a: A): Option[String] = a.oid map { _.toString }
}

trait WithIdIndexing[A <: PerustiedotWithId] {
  this: Indexing[A] =>

  override def indexId(a: A): Option[String] = a.id map { _.toString }
}

object Indexing {
  def apply[A](implicit in: Indexing[A]): Indexing[A] = in

  object ops {
    def indexMessage[A: Indexing](a: A): Option[String] = {
      Indexing[A].indexId(a) map { oid => compact(render(Indexing[A].index -> Seq(oid))) }
    }
  }

  implicit val hakuIndexing: Indexing[Haku] = new Indexing[Haku] with WithOidIndexing[Haku] {
    val index: String = "haut"
  }
  implicit val hakukohdeIndexing: Indexing[Hakukohde] = new Indexing[Hakukohde] with WithOidIndexing[Hakukohde] {
    val index: String = "hakukohteet"
  }
  implicit val koulutusIndexing: Indexing[Koulutus] = new Indexing[Koulutus] with WithOidIndexing[Koulutus] {
    val index: String = "koulutukset"
  }
  implicit val toteutusIndexing: Indexing[Toteutus] = new Indexing[Toteutus] with WithOidIndexing[Toteutus] {
    val index: String = "toteutukset"
  }
  implicit val valintaperusteIndexing: Indexing[Valintaperuste] = new Indexing[Valintaperuste] with WithIdIndexing[Valintaperuste] {
    val index: String = "valintaperusteet"
  }
}
