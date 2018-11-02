package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

case class ValintaperusteMetadata()

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
                          kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithId with Validatable {

  override def validate(): IsValid = for {
    _ <- super.validate().right
    _ <- validateOid(organisaatio)
    x <- validateIfTrue(Julkaistu == tila, () => for {
      _ <- validateHakutapaKoodi(hakutapaKoodiUri).right
      _ <- validateKohdejoukkoKoodi(kohdejoukkoKoodiUri).right
      y <- validateKohdejoukonTarkenneKoodi(kohdejoukonTarkenneKoodiUri).right
    } yield y).right
  } yield x
}