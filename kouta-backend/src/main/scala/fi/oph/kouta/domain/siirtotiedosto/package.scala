package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.service.Pistetieto

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID

package object siirtotiedosto {
  val SiirtotiedostoDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")


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
    val values                = List(Oppilaitos, OppilaitoksenOsa)
  }

  case object Oppilaitos       extends Organisaatiotyyppi { val name = "oppilaitos"       }
  case object OppilaitoksenOsa extends Organisaatiotyyppi { val name = "oppilaitoksenOsa" }

  case class SiirtotiedostoOperationResults(
      keys: Seq[String],
      count: Int,
      success: Boolean
  )

  case class SiirtotiedostoCounts(
      koulutukset: Option[Int],
      toteutukset: Option[Int],
      hakukohteet: Option[Int],
      haut: Option[Int],
      valintaperusteet: Option[Int],
      sorakuvaukset: Option[Int],
      oppilaitoksetJaOsat: Option[Int],
      pistetiedot: Option[Int],
  ) {
    def this() = this(None, None, None, None, None, None, None, None)
  }

  case class SiirtotiedostoInfo(counts: Option[SiirtotiedostoCounts])
  case class Siirtotiedosto(
      id: UUID,
      windowStart: Option[String],
      windowEnd: String,
      runStart: LocalDateTime,
      runEnd: Option[LocalDateTime],
      info: Option[SiirtotiedostoInfo],
      success: Option[Boolean],
      errorMessage: Option[String]
  ) {
    def windowStartAsLocalDate() =
      windowStart match {
        case Some(dateStr) => Some(LocalDateTime.parse(dateStr, SiirtotiedostoDateTimeFormat))
        case _ => None
      }
  }
}
