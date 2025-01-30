package fi.oph.kouta.domain.siirtotiedosto

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.domain._

import java.util.UUID

sealed trait KoulutusMetadataRaporttiItem {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[LisatietoRaporttiItem]
  val isMuokkaajaOphVirkailija: Option[Boolean]
}

case class KoulutusEnrichedDataRaporttiItem(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None) {
  def this(e: KoulutusEnrichedData) = this(
    esitysnimi = e.esitysnimi,
    muokkaajanNimi = e.muokkaajanNimi
  )
}

case class KoulutusRaporttiItem(
    oid: KoulutusOid,
    externalId: Option[String] = None,
    johtaaTutkintoon: Option[Boolean],
    koulutustyyppi: Koulutustyyppi,
    koulutuksetKoodiUri: Seq[String] = Seq(),
    tila: Julkaisutila = Tallennettu,
    esikatselu: Option[Boolean] = None,
    tarjoajat: List[OrganisaatioOid] = List(),
    nimi: Kielistetty = Map(),
    sorakuvausId: Option[UUID] = None,
    metadata: Option[KoulutusMetadataRaporttiItem] = None,
    julkinen: Option[Boolean] = None,
    muokkaaja: UserOid,
    organisaatioOid: Option[OrganisaatioOid],
    kielivalinta: Seq[Kieli] = Seq(),
    teemakuva: Option[String] = None,
    ePerusteId: Option[Long] = None,
    modified: Option[Modified] = None,
    enrichedData: Option[KoulutusEnrichedDataRaporttiItem] = None
)

case class KoulutusEnrichmentData(
    oid: KoulutusOid,
    koulutuksetKoodiUri: Seq[String] = Seq(),
    metadata: Option[KoulutusMetadata]
) {
  def opintojenLaajuusNumero(): Option[Double] =
    metadata match {
      case Some(metadata) =>
        metadata match {
          case lukioKoulutusMetadata: LukioKoulutusMetadata => lukioKoulutusMetadata.opintojenLaajuusNumero
          case _ => None
        }
      case _ => None
    }
}

trait KorkeakoulutusKoulutusMetadataRaporttiItem extends KoulutusMetadataRaporttiItem with LaajuusSingle {
  val tutkintonimikeKoodiUrit: Seq[String]
  val koulutusalaKoodiUrit: Seq[String]
}

trait KorkeakoulutusRelatedKoulutusMetadataRaporttiItem extends KoulutusMetadataRaporttiItem with LaajuusMinMax {
  val korkeakoulutustyypit: Seq[Korkeakoulutustyyppi]
}

case class AmmatillinenKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amm,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None
) extends KoulutusMetadataRaporttiItem

case class AmmatillinenTutkinnonOsaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    tutkinnonOsat: Seq[TutkinnonOsaRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem

case class AmmatillinenOsaamisalaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOsaamisala,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    osaamisalaKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem

case class AmmatillinenMuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmMuu,
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    kuvaus: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle

case class YliopistoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem

case class AmmattikorkeakouluKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem

case class AmmOpeErityisopeJaOpoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem

case class OpePedagOpinnotKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = OpePedagOpinnot,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem

case class KkOpintojaksoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintojakso,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem

case class KkOpintokokonaisuusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem

case class LukioKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Lk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(), // koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle

case class TuvaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Tuva,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle

case class TelmaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle

trait VapaaSivistystyoKoulutusMetadataRaporttiItem extends KoulutusMetadataRaporttiItem with LaajuusSingle {
  val kuvaus: Kielistetty
  val linkkiEPerusteisiin: Kielistetty
  val koulutusalaKoodiUrit: Seq[String]
}

case class VapaaSivistystyoOpistovuosiKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadataRaporttiItem

case class VapaaSivistystyoMuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadataRaporttiItem

case class VapaaSivistystyoOsaamismerkkiKoulutusMetadataRaporttiItem(
                                                          tyyppi: Koulutustyyppi = VapaaSivistystyoOsaamismerkki,
                                                          lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
                                                          kuvaus: Kielistetty = Map(),
                                                          linkkiEPerusteisiin: Kielistetty = Map(),
                                                          koulutusalaKoodiUrit: Seq[String] = Seq(),
                                                          opintojenLaajuusNumero: Option[Double] = None,
                                                          opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                                          osaamismerkkiKoodiUri: Option[String],
                                                          isMuokkaajaOphVirkailija: Option[Boolean] = None
                                                        ) extends VapaaSivistystyoKoulutusMetadataRaporttiItem

case class AikuistenPerusopetusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AikuistenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle

case class ErikoislaakariKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem

case class ErikoistumiskoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoistumiskoulutus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    erikoistumiskoulutusKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem

case class TaiteenPerusopetusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = TaiteenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem

case class MuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Muu,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusMinMax
