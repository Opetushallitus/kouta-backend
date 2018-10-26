package fi.oph.kouta.domain

import java.util.UUID

case class Valintaperuste(id:Option[UUID] = None,
                          tila:Julkaisutila = Tallennettu,
                          hakutapa: Option[String] = None,
                          kohdejoukko: Option[String] = None,
                          kohdejoukonTarkenne: Option[String] = None,
                          nimi: Kielistetty = Map(),
                          onkoJulkinen: Boolean = false,
                          metadata: Option[ValintaperusteMetadata] = None,
                          organisaatio: String,
                          muokkaaja:String,
                          kielivalinta:Seq[Kieli] = Seq())

case class ValintaperusteMetadata()