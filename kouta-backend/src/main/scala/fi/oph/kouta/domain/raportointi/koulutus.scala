package fi.oph.kouta.domain.raportointi

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.domain._

import java.util.UUID

sealed trait KoulutusMetadataRaporttiItem {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val isMuokkaajaOphVirkailija: Option[Boolean]
}

case class KoulutusEnrichedDataRaporttiItem(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None)

case class KoulutusRaporttiItem(
    oid: KoulutusOid,
    externalId: Option[String] = None,
    johtaaTutkintoon: Boolean,
    koulutustyyppi: Koulutustyyppi,
    koulutuksetKoodiUri: Seq[String] = Seq(),
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    tarjoajat: List[OrganisaatioOid] = List(),
    nimi: Kielistetty = Map(),
    sorakuvausId: Option[UUID] = None,
    metadata: Option[KoulutusMetadataRaporttiItem] = None,
    julkinen: Boolean = false,
    muokkaaja: UserOid,
    organisaatioOid: OrganisaatioOid,
    kielivalinta: Seq[Kieli] = Seq(),
    teemakuva: Option[String] = None,
    ePerusteId: Option[Long] = None,
    modified: Modified,
    _enrichedData: Option[KoulutusEnrichedDataRaporttiItem] = None
) {
  def this(k: Koulutus) = {
    this(
      k.oid.getOrElse(KoulutusOid("")),
      k.externalId,
      k.johtaaTutkintoon,
      k.koulutustyyppi,
      k.koulutuksetKoodiUri,
      k.tila,
      k.esikatselu,
      k.tarjoajat,
      k.nimi,
      k.sorakuvausId,
      k.metadata match {
        case Some(metadata) =>
          Some(metadata match {
            case m: AmmatillinenKoulutusMetadata => new AmmatillinenKoulutusMetadataRaporttiItem(m)
            case m: AmmatillinenTutkinnonOsaKoulutusMetadata =>
              new AmmatillinenTutkinnonOsaKoulutusMetadataRaporttiItem(m)
            case m: AmmatillinenOsaamisalaKoulutusMetadata => new AmmatillinenOsaamisalaKoulutusMetadataRaporttiItem(m)
            case m: AmmatillinenMuuKoulutusMetadata        => new AmmatillinenMuuKoulutusMetadataRaporttiItem(m)
            case m: YliopistoKoulutusMetadata              => new YliopistoKoulutusMetadataRaporttiItem(m)
            case m: AmmattikorkeakouluKoulutusMetadata     => new AmmattikorkeakouluKoulutusMetadataRaporttiItem(m)
            case m: AmmOpeErityisopeJaOpoKoulutusMetadata  => new AmmOpeErityisopeJaOpoKoulutusMetadataRaporttiItem(m)
            case m: OpePedagOpinnotKoulutusMetadata        => new OpePedagOpinnotKoulutusMetadataRaporttiItem(m)
            case m: KkOpintojaksoKoulutusMetadata          => new KkOpintojaksoKoulutusMetadataRaporttiItem(m)
            case m: KkOpintokokonaisuusKoulutusMetadata    => new KkOpintokokonaisuusKoulutusMetadataRaporttiItem(m)
            case m: LukioKoulutusMetadata                  => new LukioKoulutusMetadataRaporttiItem(m)
            case m: TuvaKoulutusMetadata                   => new TuvaKoulutusMetadataRaporttiItem(m)
            case m: TelmaKoulutusMetadata                  => new TelmaKoulutusMetadataRaporttiItem(m)
            case m: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
              new VapaaSivistystyoOpistovuosiKoulutusMetadataRaporttiItem(m)
            case m: VapaaSivistystyoMuuKoulutusMetadata  => new VapaaSivistystyoMuuKoulutusMetadataRaporttiItem(m)
            case m: AikuistenPerusopetusKoulutusMetadata => new AikuistenPerusopetusKoulutusMetadataRaporttiItem(m)
            case m: ErikoislaakariKoulutusMetadata       => new ErikoislaakariKoulutusMetadataRaporttiItem(m)
            case m: ErikoistumiskoulutusMetadata         => new ErikoistumiskoulutusMetadataRaporttiItem(m)
            case m: TaiteenPerusopetusKoulutusMetadata   => new TaiteenPerusopetusKoulutusMetadataRaporttiItem(m)
            case m: MuuKoulutusMetadata                  => new MuuKoulutusMetadataRaporttiItem(m)

          })
        case _ => None
      },
      k.julkinen,
      k.muokkaaja,
      k.organisaatioOid,
      k.kielivalinta,
      k.teemakuva,
      k.ePerusteId,
      k.modified.get,
      k._enrichedData match {
        case Some(e) => Some(KoulutusEnrichedDataRaporttiItem(e.esitysnimi, e.muokkaajanNimi))
        case _ => None
      }
    )
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
    lisatiedot: Seq[Lisatieto] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None
) extends KoulutusMetadataRaporttiItem {
  def this(m: AmmatillinenKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.isMuokkaajaOphVirkailija,
      m.koulutusalaKoodiUrit,
      m.tutkintonimikeKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri
    )
  }
}

case class AmmatillinenTutkinnonOsaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    tutkinnonOsat: Seq[TutkinnonOsaRaporttiItem] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem {
  def this(m: AmmatillinenTutkinnonOsaKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.tutkinnonOsat.map(t => new TutkinnonOsaRaporttiItem(t)),
      m.isMuokkaajaOphVirkailija
    )
  }
}

case class AmmatillinenOsaamisalaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOsaamisala,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    osaamisalaKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem {
  def this(m: AmmatillinenOsaamisalaKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.osaamisalaKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
  }
}

case class AmmatillinenMuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmMuu,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: AmmatillinenMuuKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.lisatiedot,
      m.kuvaus,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.opintojenLaajuusNumero,
      m.isMuokkaajaOphVirkailija
    )
  }
}

case class YliopistoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem {
  def this(m: YliopistoKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.tutkintonimikeKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
  }
}

case class AmmattikorkeakouluKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem {
  def this(m: AmmattikorkeakouluKoulutusMetadata) = {
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.tutkintonimikeKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
  }
}

case class AmmOpeErityisopeJaOpoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem {
  def this(m: AmmOpeErityisopeJaOpoKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.tutkintonimikeKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

case class OpePedagOpinnotKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = OpePedagOpinnot,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadataRaporttiItem {
  def this(m: OpePedagOpinnotKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.tutkintonimikeKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

case class KkOpintojaksoKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintojakso,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem {
  def this(m: KkOpintojaksoKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusNumeroMin,
      m.opintojenLaajuusNumeroMax,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija,
      m.isAvoinKorkeakoulutus,
      m.tunniste,
      m.opinnonTyyppiKoodiUri,
      m.korkeakoulutustyypit
    )
}

case class KkOpintokokonaisuusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem {
  def this(m: KkOpintokokonaisuusKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusNumeroMin,
      m.opintojenLaajuusNumeroMax,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija,
      m.isAvoinKorkeakoulutus,
      m.tunniste,
      m.opinnonTyyppiKoodiUri,
      m.korkeakoulutustyypit
    )
}

case class LukioKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Lk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(), // koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: LukioKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.koulutusalaKoodiUrit,
      m.isMuokkaajaOphVirkailija
    )
}

case class TuvaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Tuva,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: TuvaKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.linkkiEPerusteisiin,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

case class TelmaKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: TelmaKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.linkkiEPerusteisiin,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

trait VapaaSivistystyoKoulutusMetadataRaporttiItem extends KoulutusMetadataRaporttiItem with LaajuusSingle {
  val kuvaus: Kielistetty
  val linkkiEPerusteisiin: Kielistetty
  val koulutusalaKoodiUrit: Seq[String]
}

case class VapaaSivistystyoOpistovuosiKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadataRaporttiItem {
  def this(m: VapaaSivistystyoOpistovuosiKoulutusMetadata) =
    this(
      m.tyyppi,
      m.lisatiedot,
      m.kuvaus,
      m.linkkiEPerusteisiin,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

case class VapaaSivistystyoMuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadataRaporttiItem {
  def this(m: VapaaSivistystyoMuuKoulutusMetadata) =
    this(
      m.tyyppi,
      m.lisatiedot,
      m.kuvaus,
      m.linkkiEPerusteisiin,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusNumero,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.isMuokkaajaOphVirkailija
    )
}

case class AikuistenPerusopetusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = AikuistenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusSingle {
  def this(m: AikuistenPerusopetusKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.linkkiEPerusteisiin,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.opintojenLaajuusNumero,
      m.isMuokkaajaOphVirkailija
    )
}

case class ErikoislaakariKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem {
  def this(m: ErikoislaakariKoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.tutkintonimikeKoodiUrit,
      m.koulutusalaKoodiUrit,
      m.isMuokkaajaOphVirkailija
    )
}

case class ErikoistumiskoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Erikoistumiskoulutus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    erikoistumiskoulutusKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadataRaporttiItem {
  def this(m: ErikoistumiskoulutusMetadata) =
    this(
      m.tyyppi,
      m.kuvaus,
      m.lisatiedot,
      m.erikoistumiskoulutusKoodiUri,
      m.koulutusalaKoodiUrit,
      m.opintojenLaajuusyksikkoKoodiUri,
      m.opintojenLaajuusNumeroMin,
      m.opintojenLaajuusNumeroMax,
      m.isMuokkaajaOphVirkailija,
      m.korkeakoulutustyypit
    )
}

case class TaiteenPerusopetusKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = TaiteenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem {
  def this(m: TaiteenPerusopetusKoulutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.lisatiedot,
    m.linkkiEPerusteisiin,
    m.isMuokkaajaOphVirkailija
  )
}

case class MuuKoulutusMetadataRaporttiItem(
    tyyppi: Koulutustyyppi = Muu,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadataRaporttiItem
    with LaajuusMinMax {
  def this(m: MuuKoulutusMetadata) = this(
    m.tyyppi,
    m.kuvaus,
    m.lisatiedot,
    m.koulutusalaKoodiUrit,
    m.opintojenLaajuusyksikkoKoodiUri,
    m.opintojenLaajuusNumeroMin,
    m.opintojenLaajuusNumeroMax,
    m.isMuokkaajaOphVirkailija
  )
}
