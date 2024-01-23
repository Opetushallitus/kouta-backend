package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.{AikuistenPerusopetus, AikuistenPerusopetusToteutusMetadata, Ajanjakso, Amk, Amm, AmmMuu, AmmOpeErityisopeJaOpo, AmmOpeErityisopeJaOpoToteutusMetadata, AmmOsaamisala, AmmTutkinnonOsa, AmmatillinenMuuToteutusMetadata, AmmatillinenOsaamisala, AmmatillinenOsaamisalaToteutusMetadata, AmmatillinenToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, AmmattikorkeakouluToteutusMetadata, Apuraha, Apurahayksikko, Erikoislaakari, ErikoislaakariToteutusMetadata, Erikoistumiskoulutus, ErikoistumiskoulutusToteutusMetadata, Hakulomaketyyppi, Hakutermi, Julkaisutila, Kieli, Kielistetty, Kielivalikoima, KkOpintojakso, KkOpintojaksoToteutusMetadata, KkOpintokokonaisuus, KkOpintokokonaisuusToteutusMetadata, KoulutuksenAlkamiskausi, Koulutustyyppi, LaajuusMinMax, LaajuusSingle, Lisatieto, Lk, LukioToteutusMetadata, LukiodiplomiTieto, LukiolinjaTieto, Maksullisuustyyppi, Modified, Muu, MuuToteutusMetadata, OpePedagOpinnotToteutusMetadata, Opetus, TaiteenPerusopetus, TaiteenPerusopetusToteutusMetadata, Tallennettu, Telma, TelmaToteutusMetadata, Toteutus, Tuva, TuvaToteutusMetadata, VapaaSivistystyoMuu, VapaaSivistystyoMuuToteutusMetadata, VapaaSivistystyoOpistovuosi, VapaaSivistystyoOpistovuosiToteutusMetadata, Yhteyshenkilo, YliopistoToteutusMetadata, Yo}
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
) {
  def this(t: Toteutus) = this(
    t.oid.getOrElse(ToteutusOid("")),
    t.externalId,
    t.koulutusOid,
    t.tila,
    t.esikatselu,
    t.tarjoajat,
    t.nimi,
    t.metadata match {
      case Some(metadata) =>
        Some(metadata match {
          case m: AmmatillinenToteutusMetadata => new AmmatillinenToteutusMetadataRaporttiItem(m)
          case m: AmmatillinenTutkinnonOsaToteutusMetadata => new AmmatillinenTutkinnonOsaToteutusMetadataRaporttiItem(m)
          case m: AmmatillinenOsaamisalaToteutusMetadata => new AmmatillinenOsaamisalaToteutusMetadataRaporttiItem(m)
          case m: AmmatillinenMuuToteutusMetadata => new AmmatillinenMuuToteutusMetadataRaporttiItem(m)
          case m: YliopistoToteutusMetadata => new YliopistoToteutusMetadataRaporttiItem(m)
          case m: AmmattikorkeakouluToteutusMetadata => new AmmattikorkeakouluToteutusMetadataRaporttiItem(m)
          case m: AmmOpeErityisopeJaOpoToteutusMetadata => new AmmOpeErityisopeJaOpoToteutusMetadataRaporttiItem(m)
          case m: OpePedagOpinnotToteutusMetadata => new OpePedagOpinnotToteutusMetadataRaporttiItem(m)
          case m: KkOpintojaksoToteutusMetadata => new KkOpintojaksoToteutusMetadataRaporttiItem(m)
          case m: KkOpintokokonaisuusToteutusMetadata => new KkOpintokokonaisuusToteutusMetadataRaporttiItem(m)
          case m: LukioToteutusMetadata => new LukioToteutusMetadataRaporttiItem(m)
          case m: TuvaToteutusMetadata => new TuvaToteutusMetadataRaporttiItem(m)
          case m: TelmaToteutusMetadata => new TelmaToteutusMetadataRaporttiItem(m)
          case m: VapaaSivistystyoOpistovuosiToteutusMetadata => new VapaaSivistystyoOpistovuosiToteutusMetadataRaporttiItem(m)
          case m: VapaaSivistystyoMuuToteutusMetadata => new VapaaSivistystyoMuuToteutusMetadataRaporttiItem(m)
          case m: AikuistenPerusopetusToteutusMetadata => new AikuistenPerusopetusToteutusMetadataRaporttiItem(m)
          case m: ErikoislaakariToteutusMetadata => new ErikoislaakariToteutusMetadataRaporttiItem(m)
          case m: ErikoistumiskoulutusToteutusMetadata => new ErikoistumiskoulutusToteutusMetadataRaporttiItem(m)
          case m: TaiteenPerusopetusToteutusMetadata => new TaiteenPerusopetusToteutusMetadataRaporttiItem(m)
          case m: MuuToteutusMetadata => new MuuToteutusMetadataRaporttiItem(m)
        })
      case _ => None
    },
    t.sorakuvausId,
    t.muokkaaja,
    t.organisaatioOid,
    t.kielivalinta,
    t.teemakuva,
    t.modified.get,
    t._enrichedData.map(e => ToteutusEnrichedDataRaporttiItem(e.esitysnimi, e.muokkaajanNimi))
  )
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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: AmmatillinenToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.osaamisalat.map(o => new AmmatillinenOsaamisalaRaporttiItem(o)),
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.ammatillinenPerustutkintoErityisopetuksena,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: AmmatillinenTutkinnonOsaToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: AmmatillinenOsaamisalaToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: AmmatillinenMuuToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class YliopistoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: YliopistoToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class AmmattikorkeakouluToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: AmmattikorkeakouluToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class AmmOpeErityisopeJaOpoToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: AmmOpeErityisopeJaOpoToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class OpePedagOpinnotToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: OpePedagOpinnotToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: KkOpintojaksoToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opintojenLaajuusyksikkoKoodiUri,
    m.opintojenLaajuusNumero,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus,
    m.isAvoinKorkeakoulutus,
    m.tunniste,
    m.opinnonTyyppiKoodiUri
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false,
    liitetytOpintojaksot: Seq[ToteutusOid] = Seq(),
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: KkOpintokokonaisuusToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opintojenLaajuusyksikkoKoodiUri,
    m.opintojenLaajuusNumero,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus,
    m.liitetytOpintojaksot,
    m.isAvoinKorkeakoulutus,
    m.tunniste,
    m.opinnonTyyppiKoodiUri
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: LukioToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.kielivalikoima.map(new KielivalikoimaRaporttiItem(_)),
    m.yleislinja,
    m.painotukset.map(p => new LukiolinjaTietoRaporttiItem(p)),
    m.erityisetKoulutustehtavat.map(e => new LukiolinjaTietoRaporttiItem(e)),
    m.diplomit.map(d => new LukiodiplomiTietoRaporttiItem(d)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: TuvaToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.jarjestetaanErityisopetuksena,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class TelmaToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: TelmaToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class AmmatillinenOsaamisalaRaporttiItem(koodiUri: String, linkki: Kielistetty = Map(), otsikko: Kielistetty = Map()) {
  def this(a: AmmatillinenOsaamisala) = this(
    a.koodiUri,
    a.linkki,
    a.otsikko
  )
}

case class ApurahaRaporttiItem(
    min: Option[Int] = None,
    max: Option[Int] = None,
    yksikko: Option[Apurahayksikko] = None,
    kuvaus: Kielistetty = Map()
) {
  def this(a: Apuraha) = this(
    a.min,
    a.max,
    a.yksikko,
    a.kuvaus
  )
}

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
) {
  def this(o: Opetus) = this(
    o.opetuskieliKoodiUrit,
    o.opetuskieletKuvaus,
    o.opetusaikaKoodiUrit,
    o.opetusaikaKuvaus,
    o.opetustapaKoodiUrit,
    o.opetustapaKuvaus,
    o.maksullisuustyyppi,
    o.maksullisuusKuvaus,
    o.maksunMaara,
    o.koulutuksenAlkamiskausi.map(new KoulutuksenAlkamiskausiRaporttiItem(_)),
    o.lisatiedot.map(l => new LisatietoRaporttiItem(l)),
    o.onkoApuraha,
    o.apuraha.map(new ApurahaRaporttiItem(_)),
    o.suunniteltuKestoVuodet,
    o.suunniteltuKestoKuukaudet,
    o.maksullisuusKuvaus
  )
}

case class KielivalikoimaRaporttiItem(
    A1Kielet: Seq[String] = Seq(),
    A2Kielet: Seq[String] = Seq(),
    B1Kielet: Seq[String] = Seq(),
    B2Kielet: Seq[String] = Seq(),
    B3Kielet: Seq[String] = Seq(),
    aidinkielet: Seq[String] = Seq(),
    muutKielet: Seq[String] = Seq()
) {
  def this(k: Kielivalikoima) = this(
    k.A1Kielet,
    k.A2Kielet,
    k.B1Kielet,
    k.B2Kielet,
    k.B3Kielet,
    k.aidinkielet,
    k.muutKielet
  )
}

case class LukiolinjaTietoRaporttiItem(koodiUri: String, kuvaus: Kielistetty) {
  def this(l: LukiolinjaTieto) = this(l.koodiUri, l.kuvaus)
}

case class LukiodiplomiTietoRaporttiItem(koodiUri: String, linkki: Kielistetty = Map(), linkinAltTeksti: Kielistetty = Map()) {
  def this(l: LukiodiplomiTieto) = this(l.koodiUri, l.linkki, l.linkinAltTeksti)
}

case class VapaaSivistystyoOpistovuosiToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: VapaaSivistystyoOpistovuosiToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: VapaaSivistystyoMuuToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: AikuistenPerusopetusToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

case class ErikoislaakariToteutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    opetus: Option[OpetusRaporttiItem] = None,
    asiasanat: List[Keyword] = List(),
    ammattinimikkeet: List[Keyword] = List(),
    yhteyshenkilot: Seq[YhteyshenkiloRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    hasJotpaRahoitus: Option[Boolean] = None,
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends ToteutusMetadataRaporttiItem {
  def this(m: ErikoislaakariToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem {
  def this(m: ErikoistumiskoulutusToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusMinMax {
  def this(m: TaiteenPerusopetusToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opintojenLaajuusyksikkoKoodiUri,
    m.opintojenLaajuusNumeroMin,
    m.opintojenLaajuusNumeroMax,
    m.taiteenalaKoodiUrit,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

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
    isTaydennyskoulutus: Boolean = false,
    isTyovoimakoulutus: Boolean = false
) extends TutkintoonJohtamatonToteutusMetadataRaporttiItem
    with LaajuusMinMax {
  def this(m: MuuToteutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.opintojenLaajuusyksikkoKoodiUri,
    m.opintojenLaajuusNumeroMin,
    m.opintojenLaajuusNumeroMax,
    m.opetus.map(new OpetusRaporttiItem(_)),
    m.asiasanat,
    m.ammattinimikkeet,
    m.yhteyshenkilot.map(y => new YhteyshenkiloRaporttiItem(y)),
    m.isHakukohteetKaytossa,
    m.hakutermi,
    m.hakulomaketyyppi,
    m.hakulomakeLinkki,
    m.lisatietoaHakeutumisesta,
    m.lisatietoaValintaperusteista,
    m.hakuaika,
    m.aloituspaikat,
    m.aloituspaikkakuvaus,
    m.isMuokkaajaOphVirkailija,
    m.hasJotpaRahoitus,
    m.isTaydennyskoulutus,
    m.isTyovoimakoulutus
  )
}

