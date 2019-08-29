package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid._

package object hakutieto {
  val HakutietoModel =
    s"""    Hakutieto:
       |      type: object
       |      properties:
       |        toteutusOid:
       |          type: string
       |          description: Toteutuksen yksilöivä tunniste.
       |          example: "1.2.246.562.13.00000000000000000009"
       |        haut:
       |          type: array
       |          items:
       |            $$ref: '#/components/schemas/HakutietoHaku'
       |          description: Koulutuksen toteutuksen hakutiedot
       |""".stripMargin

  val HakutietoHakuModel =
    s"""    HakutietoHaku:
       |      type: object
       |      properties:
       |        hakuOid:
       |          type: string
       |          description: Haun yksilöivä tunniste.
       |          example: "1.2.246.562.29.00000000000000000009"
       |        todo:
       |          type: string
       |          description: Tämän scheman dokumentointi on kesken
       |""".stripMargin

  val models = List(HakutietoModel, HakutietoHakuModel)
}

case class Hakutieto(toteutusOid:ToteutusOid,
                     haut: Seq[HakutietoHaku])

case class HakutietoHaku(hakuOid: HakuOid,
                         nimi: Kielistetty = Map(),
                         hakutapaKoodiUri: Option[String] = None,
                         alkamiskausiKoodiUri: Option[String] = None,
                         alkamisvuosi: Option[String] = None,
                         hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                         hakulomakeAtaruId: Option[UUID] = None,
                         hakulomakeKuvaus: Kielistetty = Map(),
                         hakulomakeLinkki: Kielistetty = Map(),
                         organisaatioOid: OrganisaatioOid,
                         hakuajat: Seq[Ajanjakso] = Seq(),
                         muokkaaja: UserOid,
                         modified: Option[LocalDateTime],
                         hakukohteet: Seq[HakutietoHakukohde])

case class HakutietoHakukohde(hakukohdeOid: HakukohdeOid,
                              nimi: Kielistetty = Map(),
                              alkamiskausiKoodiUri: Option[String] = None,
                              alkamisvuosi: Option[String] = None,
                              kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                              hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                              hakulomakeAtaruId: Option[UUID] = None,
                              hakulomakeKuvaus: Kielistetty = Map(),
                              hakulomakeLinkki: Kielistetty = Map(),
                              kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                              aloituspaikat: Option[Int] = None,
                              minAloituspaikat: Option[Int] = None,
                              maxAloituspaikat: Option[Int] = None,
                              ensikertalaisenAloituspaikat: Option[Int] = None,
                              minEnsikertalaisenAloituspaikat: Option[Int] = None,
                              maxEnsikertalaisenAloituspaikat: Option[Int] = None,
                              kaytetaanHaunAikataulua: Option[Boolean] = None,
                              hakuajat: Seq[Ajanjakso] = Seq(),
                              muokkaaja: UserOid,
                              organisaatioOid: OrganisaatioOid,
                              modified: Option[LocalDateTime])
