package fi.oph.kouta.domain

import java.util.UUID

case class Valintaperuste(id:Option[UUID] = None,
                          tila:Julkaisutila = Tallennettu,
                          hakutapaKoodiUri: Option[String] = None,
                          kohdejoukkoKoodiUri: Option[String] = None,
                          kohdejoukonTarkenneKoodiUri: Option[String] = None,
                          nimi: Kielistetty = Map(),
                          onkoJulkinen: Boolean = false,
                          metadata: Option[ValintaperusteMetadata] = None,
                          organisaatio: String,
                          muokkaaja:String,
                          kielivalinta:Seq[Kieli] = Seq())

case class ValintaperusteMetadata()