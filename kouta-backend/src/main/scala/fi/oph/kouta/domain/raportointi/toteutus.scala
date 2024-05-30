package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{AikuistenPerusopetus, AikuistenPerusopetusToteutusMetadata, Ajanjakso, Amk, Amm, AmmMuu, AmmOpeErityisopeJaOpo, AmmOpeErityisopeJaOpoToteutusMetadata, AmmOsaamisala, AmmTutkinnonOsa, AmmatillinenMuuToteutusMetadata, AmmatillinenOsaamisala, AmmatillinenOsaamisalaToteutusMetadata, AmmatillinenToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, AmmattikorkeakouluToteutusMetadata, Apuraha, Apurahayksikko, Erikoislaakari, ErikoislaakariToteutusMetadata, Erikoistumiskoulutus, ErikoistumiskoulutusToteutusMetadata, Hakulomaketyyppi, Hakutermi, Julkaisutila, Kieli, Kielistetty, Kielivalikoima, KkOpintojakso, KkOpintojaksoToteutusMetadata, KkOpintokokonaisuus, KkOpintokokonaisuusToteutusMetadata, KoulutuksenAlkamiskausi, Koulutustyyppi, LaajuusMinMax, LaajuusSingle, Lisatieto, Lk, LukioToteutusMetadata, LukiodiplomiTieto, LukiolinjaTieto, Maksullisuustyyppi, Modified, Muu, MuuToteutusMetadata, OpePedagOpinnotToteutusMetadata, Opetus, TaiteenPerusopetus, TaiteenPerusopetusToteutusMetadata, Tallennettu, Telma, TelmaToteutusMetadata, Toteutus, ToteutusEnrichedData, Tuva, TuvaToteutusMetadata, VapaaSivistystyoMuu, VapaaSivistystyoMuuToteutusMetadata, VapaaSivistystyoOpistovuosi, VapaaSivistystyoOpistovuosiToteutusMetadata, Yhteyshenkilo, YliopistoToteutusMetadata, Yo}
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}

import java.util.UUID

sealed trait ToteutusMetadataRaporttiItem {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[OpetusRaporttiItem]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem]
  val isMuokkaajaOphVirkailija: Option[Boolean]
  val hasJotpaRahoitus: Option[Boolean]
  val isTaydennyskoulutus: Option[Boolean]
  val isTyovoimakoulutus: Option[Boolean]
}

case class ToteutusEnrichedDataRaporttiItem(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None) {
  def this(e: ToteutusEnrichedData) = this(
    e.esitysnimi,
    e.muokkaajanNimi
  )
}

case class ToteutusRaporttiItem(
    oid: ToteutusOid,
    externalId: Option[String] = None,
    koulutusOid: KoulutusOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Option[Boolean] = None,
    tarjoajat: List[OrganisaatioOid] = List(),
    nimi: Kielistetty = Map(),
    metadata: Option[ToteutusMetadataRaporttiItem] = None,
    sorakuvausId: Option[UUID] = None,
    muokkaaja: Option[UserOid] = None,
    organisaatioOid: Option[OrganisaatioOid],
    kielivalinta: Seq[Kieli] = Seq(),
    teemakuva: Option[String] = None,
    modified: Option[Modified] = None,
    enrichedData: Option[ToteutusEnrichedDataRaporttiItem] = None
) {
  def lukioToteutusMetadata(): Option[LukioToteutusMetadataRaporttiItem] =
    metadata match {
      case Some(metadata) =>
        metadata match {
          case lukioToteutusMetadataRaporttiItem: LukioToteutusMetadataRaporttiItem =>
            Some(lukioToteutusMetadataRaporttiItem)
          case _ => None
        }
      case _ => None
    }

  def isLukioToteutus: Boolean = lukioToteutusMetadata().isDefined
}

case class AmmatillinenToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amm,
    kuvaus: Kielistetty = Map(),
    osaamisalat: List[AmmatillinenOsaamisalaRaporttiItem] = List(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    ammatillinenPerustutkintoErityisopetuksena: Option[Boolean] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

trait TutkintoonJohtamatonToteutusMetadataRaporttiItem extends ToteutusMetadataRaporttiItem {
  def isHakukohteetKaytossa: Option[Boolean]
  def hakutermi: Option[Hakutermi]
  def hakulomaketyyppi: Option[Hakulomaketyyppi]
  def hakulomakeLinkki: Kielistetty
  def lisatietoaHakeutumisesta: Kielistetty
  def lisatietoaValintaperusteista: Kielistetty
  def hakuaika: Option[Ajanjakso]
  def aloituspaikat: Option[Int]
  def aloituspaikkakuvaus: Kielistetty
}

case class AmmatillinenTutkinnonOsaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AmmatillinenOsaamisalaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOsaamisala,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AmmatillinenMuuToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmMuu,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class YliopistoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class AmmattikorkeakouluToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class AmmOpeErityisopeJaOpoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class OpePedagOpinnotToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class KkOpintojaksoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintojakso,
    kuvaus: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusSingle

case class KkOpintokokonaisuusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
    kuvaus: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None,
    liitetytOpintojaksot: Seq[ToteutusOid] = Seq(),
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusSingle

case class LukioToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Lk,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    kielivalikoima: Option[KielivalikoimaRaporttiItem] = None,
    yleislinja: Boolean = false,
    painotukset: Seq[LukiolinjaTietoRaporttiItem] = Seq(),
    erityisetKoulutustehtavat: Seq[LukiolinjaTietoRaporttiItem] = Seq(),
    diplomit: Seq[LukiodiplomiTietoRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class TuvaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Tuva,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    jarjestetaanErityisopetuksena: Boolean = false,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class TelmaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class AmmatillinenOsaamisalaRaporttiItem(
    koodiUri: String,
    linkki: Kielistetty = Map(),
    otsikko: Kielistetty = Map()
)

case class ApurahaRaporttiItem(
    min: Option[Int] = None,
    max: Option[Int] = None,
    yksikko: Option[Apurahayksikko] = None,
    kuvaus: Kielistetty = Map()
)

case class OpetusRaporttiItem(
    opetuskieliKoodiUrit: Seq[String] = Seq(),
    opetuskieletKuvaus: Kielistetty = Map(),
    opetusaikaKoodiUrit: Seq[String] = Seq(),
    opetusaikaKuvaus: Kielistetty = Map(),
    opetustapaKoodiUrit: Seq[String] = Seq(),
    opetustapaKuvaus: Kielistetty = Map(),
    maksullisuustyyppi: Option[Maksullisuustyyppi] = None,
    maksullisuusKuvaus: Kielistetty = Map(),
    maksunMaara: Option[Double] = None,
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausiRaporttiItem] = None,
    lisatiedot: Seq[LisatietoRaporttiItem] = Seq(),
    onkoApuraha: Boolean = false,
    apuraha: Option[ApurahaRaporttiItem] = None,
    suunniteltuKestoVuodet: Option[Int] = None,
    suunniteltuKestoKuukaudet: Option[Int] = None,
    suunniteltuKestoKuvaus: Kielistetty = Map()
)

case class KielivalikoimaRaporttiItem(
    A1Kielet: Seq[String] = Seq(),
    A2Kielet: Seq[String] = Seq(),
    B1Kielet: Seq[String] = Seq(),
    B2Kielet: Seq[String] = Seq(),
    B3Kielet: Seq[String] = Seq(),
    aidinkielet: Seq[String] = Seq(),
    muutKielet: Seq[String] = Seq()
)

case class LukiolinjaTietoRaporttiItem(koodiUri: String, kuvaus: Kielistetty)

case class LukiodiplomiTietoRaporttiItem(
    koodiUri: String,
    linkki: Kielistetty = Map(),
    linkinAltTeksti: Kielistetty = Map()
)

case class VapaaSivistystyoOpistovuosiToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class VapaaSivistystyoMuuToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AikuistenPerusopetusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AikuistenPerusopetus,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class ErikoislaakariToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends ToteutusMetadataRaporttiItem

case class ErikoistumiskoulutusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoistumiskoulutus,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class TaiteenPerusopetusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = TaiteenPerusopetus,
    kuvaus: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    taiteenalaKoodiUrit: Seq[String] = Seq(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusMinMax

case class MuuToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Muu,
    kuvaus: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isHakukohteetKaytossa: Option[Boolean] = None,
    hakutermi: Option[Hakutermi] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeLinkki: Kielistetty = Map(),
    lisatietoaHakeutumisesta: Kielistetty = Map(),
    lisatietoaValintaperusteista: Kielistetty = Map(),
    hakuaika: Option[Ajanjakso] = None,
    aloituspaikat: Option[Int] = None,
    aloituspaikkakuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Option[Boolean] = None,
    isTyovoimakoulutus: Option[Boolean] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusMinMax
