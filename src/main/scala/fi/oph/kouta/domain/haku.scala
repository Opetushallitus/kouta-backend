package fi.oph.kouta.domain

import java.time.Instant

import fi.oph.kouta.validation.{IsValid, Validatable}

case class HakuMetadata(yhteystieto: Option[Yhteystieto] = None)

case class Haku(oid:Option[String] = None,
                tila:Julkaisutila = Tallennettu,
                nimi: Kielistetty = Map(),
                hakutapaKoodiUri: Option[String] = None,
                hakukohteenLiittamisenTakaraja: Option[Instant] = None,
                hakukohteenMuokkaamisenTakaraja: Option[Instant] = None,
                alkamiskausiKoodiUri: Option[String] = None,
                alkamisvuosi: Option[String] = None,
                kohdejoukkoKoodiUri: Option[String] = None,
                kohdejoukonTarkenneKoodiUri: Option[String] = None,
                hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                hakulomake: Option[String] = None,
                metadata: Option[HakuMetadata] = None,
                organisaatio: String,
                hakuajat: List[Hakuaika] = List(),
                muokkaaja:String,
                kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = for {
    _ <- super.validate().right
    _ <- validateHakuOid(oid).right
    _ <- validateOid(organisaatio).right
    x <- validateIfTrue(tila == Julkaistu, () => for {
      _ <- validateHakutapaKoodi(hakutapaKoodiUri).right
      _ <- validateKausiKoodi(alkamiskausiKoodiUri).right
      _ <- validateAlkamisvuosi(alkamisvuosi).right
      _ <- validateKohdejoukkoKoodi(kohdejoukkoKoodiUri).right
      _ <- validateKohdejoukonTarkenneKoodi(kohdejoukonTarkenneKoodiUri).right
      _ <- validateHakulomake(hakulomaketyyppi, hakulomake).right
      y <- validateHakuajat(hakuajat).right
    } yield y).right
  } yield x

}

