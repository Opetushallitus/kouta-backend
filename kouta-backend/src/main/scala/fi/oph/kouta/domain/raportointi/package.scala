package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.service.Pistetieto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

package object raportointi {
  val RaportointiDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  case class YhteyshenkiloRaporttiItem(
      nimi: Kielistetty = Map(),
      titteli: Kielistetty = Map(),
      sahkoposti: Kielistetty = Map(),
      puhelinnumero: Kielistetty = Map(),
      wwwSivu: Kielistetty = Map(),
      wwwSivuTeksti: Kielistetty = Map()
  ) {
    def this(y: Yhteyshenkilo) = this(
      y.nimi,
      y.titteli,
      y.sahkoposti,
      y.puhelinnumero,
      y.wwwSivu,
      y.wwwSivuTeksti
    )
  }

  case class LisatietoRaporttiItem(otsikkoKoodiUri: String, teksti: Kielistetty) {
    def this(l: Lisatieto) = this(l.otsikkoKoodiUri, l.teksti)
  }
  case class TutkinnonOsaRaporttiItem(
      ePerusteId: Option[Long] = None,
      koulutusKoodiUri: Option[String] = None,
      tutkinnonosaId: Option[Long] = None,
      tutkinnonosaViite: Option[Long] = None
  ) {
    def this(t: TutkinnonOsa) = {
      this(t.ePerusteId, t.koulutusKoodiUri, t.tutkinnonosaId, t.tutkinnonosaViite)
    }
  }

  case class ValintakoeMetadataRaporttiItem(
      tietoja: Kielistetty = Map(),
      vahimmaispisteet: Option[Double] = None,
      liittyyEnnakkovalmistautumista: Option[Boolean] = None,
      ohjeetEnnakkovalmistautumiseen: Kielistetty = Map(),
      erityisjarjestelytMahdollisia: Option[Boolean] = None,
      ohjeetErityisjarjestelyihin: Kielistetty = Map()
  )

  case class ValintakoetilaisuusRaporttiItem(
      osoite: Option[Osoite],
      aika: Option[Ajanjakso] = None,
      jarjestamispaikka: Kielistetty = Map(),
      lisatietoja: Kielistetty = Map()
  )

  case class ValintakoeRaporttiItem(
      id: UUID,
      parentOidOrUUID: String,
      tyyppiKoodiUri: Option[String] = None,
      nimi: Kielistetty = Map(),
      metadata: Option[ValintakoeMetadataRaporttiItem] = None,
      tilaisuudet: Seq[ValintakoetilaisuusRaporttiItem] = Seq(),
      muokkaaja: String
  )

  case class PistetietoRaporttiItem(
      tarjoaja: OrganisaatioOid,
      hakukohdekoodi: String,
      pisteet: Option[Double],
      vuosi: String,
      valintatapajonoOid: Option[String],
      hakukohdeOid: HakukohdeOid,
      hakuOid: HakuOid,
      valintatapajonoTyyppi: Option[String]
  ) {
    def this(p: Pistetieto) = this(
      p.tarjoaja,
      p.hakukohdekoodi,
      p.pisteet,
      p.vuosi,
      p.valintatapajonoOid,
      p.hakukohdeOid,
      p.hakuOid,
      p.valintatapajonoTyyppi
    )
  }

  case class KoulutuksenAlkamiskausiRaporttiItem(
      alkamiskausityyppi: Option[Alkamiskausityyppi] = None,
      henkilokohtaisenSuunnitelmanLisatiedot: Kielistetty = Map(),
      koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
      koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
      koulutuksenAlkamiskausiKoodiUri: Option[String] = None,
      koulutuksenAlkamisvuosi: Option[String] = None
  ) {
    def this(k: KoulutuksenAlkamiskausi) = this(
      k.alkamiskausityyppi,
      k.henkilokohtaisenSuunnitelmanLisatiedot,
      k.koulutuksenAlkamispaivamaara,
      k.koulutuksenPaattymispaivamaara,
      k.koulutuksenAlkamiskausiKoodiUri,
      k.koulutuksenAlkamisvuosi
    )
  }
}
