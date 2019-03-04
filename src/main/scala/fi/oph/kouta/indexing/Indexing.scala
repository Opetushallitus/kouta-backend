package fi.oph.kouta.indexing

import fi.oph.kouta.domain._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, render}

trait Indexing[A] {
  def index(a: A): String = a.getClass.getSimpleName.toLowerCase
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
      Indexing[A].indexId(a) map { oid => compact(render(Indexing[A].index(a) -> Seq(oid))) }
    }
  }

  implicit val hakuIndexing: Indexing[Haku] = new Indexing[Haku] with WithOidIndexing[Haku]
  implicit val hakukohdeIndexing: Indexing[Hakukohde] = new Indexing[Hakukohde] with WithOidIndexing[Hakukohde]
  implicit val koulutusIndexing: Indexing[Koulutus] = new Indexing[Koulutus] with WithOidIndexing[Koulutus]
  implicit val toteutusIndexing: Indexing[Toteutus] = new Indexing[Toteutus] with WithOidIndexing[Toteutus]
  implicit val valintaperusteIndexing: Indexing[Valintaperuste] = new Indexing[Valintaperuste] with WithIdIndexing[Valintaperuste]
}
