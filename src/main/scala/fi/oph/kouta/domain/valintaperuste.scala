package fi.oph.kouta.domain

import java.util.UUID

sealed trait ValintaperusteenKohde extends EnumType
object ValintaperusteenKohde extends Enum[ValintaperusteenKohde] {
  override def name: String = "valintaperusteen kohde"
  def values() = List(KohdeKkYhteishaku, KohdeKkSiirtohaku, KohdeAmmatillinen, KohdeLukio, KohdeMuu)
}
case object KohdeKkYhteishaku extends ValintaperusteenKohde { val name = "kk yhteishaku" }
case object KohdeKkSiirtohaku extends ValintaperusteenKohde { val name = "kk siirtohaku" }
case object KohdeAmmatillinen extends ValintaperusteenKohde { val name = "amm" }
case object KohdeLukio extends ValintaperusteenKohde { val name = "lk" }
case object KohdeMuu extends ValintaperusteenKohde { val name = "muu kohde" }

case class Valintaperuste(id:Option[UUID] = None,
                          tila:Julkaisutila = Tallennettu,
                          kohde:Option[ValintaperusteenKohde] = None,
                          nimi: Kielistetty = Map(),
                          onkoJulkinen: Boolean = false,
                          metadata: Option[ValintaperusteMetadata] = None,
                          organisaatio: String,
                          muokkaaja:String,
                          kielivalinta:Seq[Kieli] = Seq())

case class ValintaperusteMetadata()