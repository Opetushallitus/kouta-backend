package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.service.Pistetieto

import java.time.{LocalDateTime, ZoneId}
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
  )

  case class LisatietoRaporttiItem(otsikkoKoodiUri: String, teksti: Kielistetty)

  case class TutkinnonOsaRaporttiItem(
      ePerusteId: Option[Long] = None,
      koulutusKoodiUri: Option[String] = None,
      tutkinnonosaId: Option[Long] = None,
      tutkinnonosaViite: Option[Long] = None
  )

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
      muokkaaja: Option[String]
  )

  case class PistetietoRaporttiItem(
      tarjoaja: Option[OrganisaatioOid],
      hakukohdekoodi: Option[String],
      pisteet: Option[Double],
      vuosi: Option[String],
      valintatapajonoOid: Option[String],
      hakukohdeOid: Option[HakukohdeOid],
      hakuOid: Option[HakuOid],
      valintatapajonoTyyppi: Option[String],
      aloituspaikat: Option[Int],
      ensisijaisestiHakeneet: Option[Int],
      modified: Option[Modified]
  )

  case class KoulutuksenAlkamiskausiRaporttiItem(
      alkamiskausityyppi: Option[Alkamiskausityyppi] = None,
      henkilokohtaisenSuunnitelmanLisatiedot: Kielistetty = Map(),
      koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
      koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
      koulutuksenAlkamiskausiKoodiUri: Option[String] = None,
      koulutuksenAlkamisvuosi: Option[String] = None
  )

  sealed trait Organisaatiotyyppi extends EnumType

  object Organisaatiotyyppi extends Enum[Organisaatiotyyppi] {
    override def name: String = "organisaatiotyyppi"
    val values = List(Oppilaitos, OppilaitoksenOsa)
  }

  case object Oppilaitos extends Organisaatiotyyppi { val name = "oppilaitos" }
  case object OppilaitoksenOsa extends Organisaatiotyyppi { val name = "oppilaitoksenOsa" }

}
