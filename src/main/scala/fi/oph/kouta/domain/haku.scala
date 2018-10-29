package fi.oph.kouta.domain

import java.time.Instant

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
                kielivalinta:Seq[Kieli] = Seq())

