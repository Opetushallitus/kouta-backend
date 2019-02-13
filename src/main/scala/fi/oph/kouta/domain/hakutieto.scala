package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid._

case class Hakutieto(toteutusOid:ToteutusOid,
                     haut: Seq[HakutietoHaku])

case class HakutietoHaku(hakuOid: HakuOid,
                         nimi: Kielistetty = Map(),
                         hakutapaKoodiUri: Option[String] = None,
                         alkamiskausiKoodiUri: Option[String] = None,
                         alkamisvuosi: Option[String] = None,
                         hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                         hakulomake: Option[String] = None,
                         organisaatioOid: OrganisaatioOid,
                         hakuajat: Seq[Ajanjakso] = Seq(),
                         muokkaaja: UserOid,
                         modified: Option[LocalDateTime],
                         hakukohteet: Seq[HakutietoHakukohde])

case class HakutietoHakukohde(hakukohdeOid: HakukohdeOid,
                              nimi: Kielistetty = Map(),
                              alkamiskausiKoodiUri: Option[String] = None,
                              alkamisvuosi: Option[String] = None,
                              hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                              hakulomake: Option[String] = None,
                              aloituspaikat: Option[Int] = None,
                              ensikertalaisenAloituspaikat: Option[Int] = None,
                              kaytetaanHaunAikataulua: Option[Boolean] = None,
                              hakuajat: Seq[Ajanjakso] = Seq(),
                              muokkaaja: UserOid,
                              organisaatioOid: OrganisaatioOid,
                              modified: Option[LocalDateTime])
