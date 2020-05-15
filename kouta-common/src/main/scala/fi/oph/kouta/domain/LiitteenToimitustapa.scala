package fi.oph.kouta.domain

sealed trait LiitteenToimitustapa extends EnumType

object LiitteenToimitustapa extends Enum[LiitteenToimitustapa] {
  override def name: String = "liitteen toimitustapa"
  def values = List(Lomake, Hakijapalvelu, MuuOsoite)
}

case object Lomake extends LiitteenToimitustapa { val name = "lomake"}
case object Hakijapalvelu extends LiitteenToimitustapa { val name = "hakijapalvelu"}
case object MuuOsoite extends LiitteenToimitustapa { val name = "osoite"}
