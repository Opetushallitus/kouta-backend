package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{
  AikuistenPerusopetus,
  Ajanjakso,
  Amk,
  Amm,
  AmmMuu,
  AmmOpeErityisopeJaOpo,
  AmmOsaamisala,
  AmmTutkinnonOsa,
  Apurahayksikko,
  Erikoislaakari,
  Erikoistumiskoulutus,
  Hakulomaketyyppi,
  Hakutermi,
  Julkaisutila,
  Kieli,
  Kielistetty,
  KkOpintojakso,
  KkOpintokokonaisuus,
  KoulutuksenAlkamiskausi,
  Koulutustyyppi,
  LaajuusMinMax,
  LaajuusSingle,
  Lisatieto,
  Lk,
  Maksullisuustyyppi,
  Modified,
  Muu,
  TaiteenPerusopetus,
  Tallennettu,
  Telma,
  Tuva,
  VapaaSivistystyoMuu,
  VapaaSivistystyoOpistovuosi,
  Yhteyshenkilo,
  Yo
}
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}

import java.util.UUID

sealed trait ToteutusMetadataRaporttiItem {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[OpetusRaporttiItem]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilot: Seq[Yhteyshenkilo]
  val isMuokkaajaOphVirkailija: Option[Boolean]
  val hasJotpaRahoitus: Option[Boolean]
  val isTaydennyskoulutus: Boolean
  val isTyovoimakoulutus: Boolean
}

case class ToteutusEnrichedDataRaporttiItem(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None)

case class ToteutusRaporttiItem(
    oid: ToteutusOid,
    externalId: Option[String] = None,
    koulutusOid: KoulutusOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    tarjoajat: List[OrganisaatioOid] = List(),
    nimi: Kielistetty = Map(),
    metadata: Option[ToteutusMetadataRaporttiItem] = None,
    sorakuvausId: Option[UUID] = None,
    muokkaaja: UserOid,
    organisaatioOid: OrganisaatioOid,
    kielivalinta: Seq[Kieli] = Seq(),
    teemakuva: Option[String] = None,
    modified: Modified,
    _enrichedData: Option[ToteutusEnrichedDataRaporttiItem] = None
)

case class AmmatillinenToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amm,
    kuvaus: Kielistetty = Map(),
    osaamisalat: List[AmmatillinenOsaamisalaRaporttiItem] = List(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    ammatillinenPerustutkintoErityisopetuksena: Option[Boolean] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
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
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AmmatillinenOsaamisalaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOsaamisala,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AmmatillinenMuuToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmMuu,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class YliopistoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class AmmattikorkeakouluToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class AmmOpeErityisopeJaOpoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class OpePedagOpinnotToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class KkOpintojaksoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintojakso,
    kuvaus: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false,
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
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false,
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
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    kielivalikoima: Option[KielivalikoimaRaporttiItem] = None,
    yleislinja: Boolean = false,
    painotukset: Seq[LukiolinjaTietoRaporttiItem] = Seq(),
    erityisetKoulutustehtavat: Seq[LukiolinjaTietoRaporttiItem] = Seq(),
    diplomit: Seq[LukiodiplomiTietoRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class TuvaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Tuva,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    jarjestetaanErityisopetuksena: Boolean = false,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class TelmaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class AmmatillinenOsaamisalaRaporttiItem(koodiUri: String, linkki: Kielistetty = Map(), otsikko: Kielistetty = Map())

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
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] = None,
    lisatiedot: Seq[Lisatieto] = Seq(),
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

case class LukiodiplomiTietoRaporttiItem(koodiUri: String, linkki: Kielistetty = Map(), linkinAltTeksti: Kielistetty = Map())

case class VapaaSivistystyoOpistovuosiToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class VapaaSivistystyoMuuToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class AikuistenPerusopetusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AikuistenPerusopetus,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem

case class ErikoislaakariToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem

case class ErikoistumiskoulutusToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoistumiskoulutus,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
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
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
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
    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusMinMax
