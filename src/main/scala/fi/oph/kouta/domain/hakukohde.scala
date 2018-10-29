package fi.oph.kouta.domain

import java.util.UUID

case class Hakukohde(oid:Option[String] = None,
                     koulutusOid:String,
                     hakuOid:String,
                     tila:Julkaisutila = Tallennettu,
                     nimi: Kielistetty = Map(),
                     alkamiskausiKoodiUri: Option[String] = None,
                     alkamisvuosi: Option[String] = None,
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomake: Option[String] = None,
                     aloituspaikat: Option[Int] = None,
                     ensikertalaisenAloituspaikat: Option[Int] = None,
                     pohjakoulutusvaatimusKoodiUri: Option[String] = None,
                     muuPohjakoulutusvaatimus: Kielistetty = Map(),
                     toinenAsteOnkoKaksoistutkinto: Option[Boolean] = None,
                     kaytetaanHaunAikataulua: Option[Boolean] = None,
                     valintaperuste: Option[UUID] = None,
                     metadata: Option[HakukohdeMetadata] = None,
                     hakuajat: List[Hakuaika] = List(),
                     muokkaaja:String,
                     kielivalinta:Seq[Kieli] = Seq())

case class HakukohdeMetadata()