package fi.oph.kouta.domain

trait BasicType {
  def name: String

  override def toString: String = name
}

abstract class BasicTypeCompanion[A <: BasicType: Manifest] {

  private val baseClass: Class[_] = implicitly[Manifest[A]].runtimeClass

  // https://gist.github.com/sidharthkuruvila/3154845#gistcomment-1346145
  private def camel2spaces(x: String): String = {
    "_?[A-Z][a-z\\d]+".r.findAllMatchIn(x).map(_.group(0).toLowerCase).mkString(" ")
  }

  protected val baseClassName: String = camel2spaces(baseClass.getSimpleName)

  def all: List[A]

  lazy val map: Map[String, A] = all.map(a => a.name -> a).toMap

  def apply(name: String): A = map.getOrElse(name, throw new IllegalArgumentException(s"Invalid $baseClassName: $name"))
}
