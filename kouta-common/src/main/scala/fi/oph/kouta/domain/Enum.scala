package fi.oph.kouta.domain

trait EnumType {
  def name: String

  override def toString: String = name
}

trait Enum[T <: EnumType] {
  def name: String

  def values: List[T]

  def withName(n: String): T = values.find(_.name.equals(n))
    .getOrElse(throw new IllegalArgumentException(s"Unknown ${name} '${n}'"))
}
