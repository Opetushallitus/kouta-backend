package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.filterTypes.koulutusTyyppi
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{KoulutusServiceValidation, OrganisaatioService}
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._
import org.mockito.ArgumentMatchers
import org.scalatest.Assertion

import java.util.UUID

class KoulutusServiceValidationSpec extends BaseServiceValidationSpec[Koulutus] {
  val koulutusKoodiClient          = mock[KoulutusKoodiClient]
  var organisaatioService          = mock[OrganisaatioService]
  val toteutusDao                  = mock[ToteutusDAO]
  val sorakuvausDao                = mock[SorakuvausDAO]
  val ammKoulutusServiceValidation = mock[AmmatillinenKoulutusServiceValidation]

  val sorakuvausId                  = UUID.randomUUID()
  val sorakuvausId2                 = UUID.randomUUID()
  val sorakuvausId3                 = UUID.randomUUID()
  val sorakuvausId4                 = UUID.randomUUID()
  val sorakuvausId5                 = UUID.randomUUID()
  val sorakuvausId6                 = UUID.randomUUID()
  val koulutusOid                   = KoulutusOid("1.2.246.562.13.00000000000000000123")
  val koulutusOid2                  = KoulutusOid("1.2.246.562.13.00000000000000000124")
  val organisaatioOidCausingFailure = OrganisaatioOid("1.2.246.562.10.66666666666")

  val yo  = YoKoulutus
  val amk = AmkKoulutus
  val min = MinKoulutus.copy(koulutustyyppi = Yo)

  val kkOpintokokonaisuusKoulutus = KkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.133")))

  private def yoKoulutusWithParameters(
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
      tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
      opintojenLaajuusYksikkoKoodiUri: String = "opintojenlaajuusyksikko_2#1",
      lisatiedot: Seq[Lisatieto] = Seq(Lisatieto1)
  ) = yo.copy(
    sorakuvausId = Some(sorakuvausId),
    metadata = Some(
      yo.metadata.get
        .asInstanceOf[YliopistoKoulutusMetadata]
        .copy(
          koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
          tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
          opintojenLaajuusyksikkoKoodiUri = Some(opintojenLaajuusYksikkoKoodiUri),
          opintojenLaajuusNumero = Some(10),
          lisatiedot = lisatiedot
        )
    )
  )

  private def amkKoulutusWithParameters(
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
      tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
      opintojenLaajuusYksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_2#1")
  ) = amk.copy(metadata =
    Some(
      amk.metadata.get
        .asInstanceOf[AmmattikorkeakouluKoulutusMetadata]
        .copy(
          koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
          tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusYksikkoKoodiUri
        )
    )
  )

  private def vstMuuWithParams(
      koulutusalaKoodiUrit: Seq[String] = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_6#1"),
      linkkiEPerusteisiin: Kielistetty = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
      opintojenLaajuusNumero: Option[Double] = Some(12),
      lisatiedot: Seq[Lisatieto] = Seq() // Lisätiedot eivät ole sallittuja vstMuulle
  ): Koulutus =
    VapaaSivistystyoMuuKoulutus.copy(
      metadata = Some(
        VapaaSivistystyoMuuKoulutusMetadata(
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          linkkiEPerusteisiin = linkkiEPerusteisiin,
          opintojenLaajuusNumero = opintojenLaajuusNumero,
          lisatiedot = lisatiedot
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu
    )

  private def kkOpintojaksoWithParams(
      koulutusalaKoodiUrit: Seq[String] = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_2#1"),
      opintojenLaajuusNumeroMin: Option[Double] = Some(12),
      opintojenLaajuusNumeroMax: Option[Double] = Some(20),
      opinnonTyyppiKoodiUri: Option[String] = Some("opinnontyyppi_1#1"),
      isAvoinKorkeakoulutus: Option[Boolean] = Some(false),
      tarjoajat: List[OrganisaatioOid] = List(),
      oid: KoulutusOid = KoulutusOid("1.2.246.562.13.129")
  ): Koulutus =
    KkOpintojaksoKoulutus.copy(
      metadata = Some(
        KkOpintojaksoKoulutusMetadata(
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          opintojenLaajuusNumeroMin = opintojenLaajuusNumeroMin,
          opintojenLaajuusNumeroMax = opintojenLaajuusNumeroMax,
          opinnonTyyppiKoodiUri = opinnonTyyppiKoodiUri,
          isAvoinKorkeakoulutus = isAvoinKorkeakoulutus
        )
      ),
      oid = Some(oid),
      tila = Tallennettu,
      tarjoajat = tarjoajat
    )

  private def muuKoulutusWithParams(
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_2#1"),
      opintojenLaajuusNumeroMin: Option[Double] = Some(12),
      opintojenLaajuusNumeroMax: Option[Double] = Some(20),
      tarjoajat: List[OrganisaatioOid] = List()
  ): Koulutus =
    MuuKoulutus.copy(
      metadata = Some(
        MuuKoulutusMetadata(
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          opintojenLaajuusNumeroMin = opintojenLaajuusNumeroMin,
          opintojenLaajuusNumeroMax = opintojenLaajuusNumeroMax,
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.139")),
      tila = Tallennettu,
      tarjoajat = tarjoajat
    )

  private def kkOpintokokonaisuusWithParams(
      koulutusalaKoodiUrit: Seq[String] = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_2#1"),
      opinnonTyyppiKoodiUri: Option[String] = Some("opinnontyyppi_1#1"),
      isAvoinKorkeakoulutus: Option[Boolean] = Some(false)
  ): Koulutus =
    KkOpintokokonaisuusKoulutus.copy(
      metadata = Some(
        KkOpintokokonaisuusKoulutusMetadata(
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          opinnonTyyppiKoodiUri = opinnonTyyppiKoodiUri,
          isAvoinKorkeakoulutus = isAvoinKorkeakoulutus
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.126")),
      tila = Tallennettu
    )

  private def aikuistenPerusopetusWithParams(
      lisatiedot: Seq[Lisatieto] = Seq(),
      linkkiEPerusteisiin: Kielistetty = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_6#1"),
      opintojenLaajuusNumero: Option[Double] = Some(12)
  ): Koulutus =
    AikuistenPerusopetusKoulutus.copy(
      metadata = Some(
        AikuistenPerusopetusKoulutusMetadata(
          lisatiedot = lisatiedot,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          opintojenLaajuusNumero = opintojenLaajuusNumero,
          linkkiEPerusteisiin = linkkiEPerusteisiin
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu
    )

  override val validator =
    new KoulutusServiceValidation(
      koulutusKoodiClient,
      organisaatioService,
      toteutusDao,
      sorakuvausDao,
      ammKoulutusServiceValidation
    )

  private def acceptKoulutusKoodiUri(filter: KoulutusKoodiFilter, koodiUri: String): Unit = {
    if (filter.filterType() == koulutusTyyppi)
      when(
        koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(filter.koulutusTyypit, koodiUri)
      ).thenAnswer(itemFound)
    else
      when(
        koulutusKoodiClient.koulutusKoodiUriExists(filter.koulutusKoodiUrit, koodiUri)
      ).thenAnswer(itemFound)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(GrandChildOid))
      .thenAnswer(Seq(GrandChildOid), Koulutustyyppi.values)
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(EvilGrandChildOid))
      .thenAnswer(Seq(EvilGrandChildOid), Koulutustyyppi.values)
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(EvilCousin))
      .thenAnswer(Seq(EvilCousin), Koulutustyyppi.values)
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(YoOid))
      .thenAnswer(Seq(YoOid), Seq(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoistumiskoulutus))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(HkiYoOid))
      .thenAnswer(
        Seq(HkiYoOid),
        Seq(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoislaakari, OpePedagOpinnot, Erikoistumiskoulutus)
      )
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(AmkOid))
      .thenAnswer(Seq(AmkOid), Seq(Amk, AmmOpeErityisopeJaOpo, Erikoistumiskoulutus))

    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(UnknownOid)).thenAnswer(Seq(UnknownOid), Seq())
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(LonelyOid)).thenAnswer(Seq(LonelyOid), Seq())
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto,"koulutuksenlisatiedot_03#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto,"koulutuksenlisatiedot_04#1")).thenAnswer(itemNotFound)

    // sorakuvaukset
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId))
      .thenAnswer((Some(Julkaistu), Some(Yo), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId2)).thenAnswer((None, None, None))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId3))
      .thenAnswer((Some(Tallennettu), Some(Yo), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId4))
      .thenAnswer((Some(Poistettu), Some(Yo), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId5))
      .thenAnswer((Some(Julkaistu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId6))
      .thenAnswer((Some(Julkaistu), Some(Yo), Some(Seq("koulutus_000001#1"))))

    // yleiset metadatat
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso1_01"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso1_001#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_080#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_020#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_2#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto,"opintojenlaajuusyksikko_5#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto,"opintojenlaajuusyksikko_6#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto,"opintojenlaajuusyksikko_8#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto, "tutkintonimikekk_110#2")).thenAnswer(itemFound)
    when(koulutusKoodiClient.koodiUriExistsInKoodisto(ErikoistumiskoulutusKoodisto, "erikoistumiskoulutukset_001#2")).thenAnswer(itemFound)

    // korkeakoulu
    acceptKoulutusKoodiUri(YoKoulutusKoodit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(YoKoulutusKoodit, "koulutus_201000#1")
    acceptKoulutusKoodiUri(YoKoulutusKoodit, "koulutus_201111#1")
    acceptKoulutusKoodiUri(AmkKoulutusKoodit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(AmkKoulutusKoodit, "koulutus_201000#1")
    acceptKoulutusKoodiUri(AmmOpeErityisopeJaOpoKoulutusKoodit, "koulutus_000002#12")
    // lukio
    acceptKoulutusKoodiUri(LukioKoulutusKoodit, "koulutus_301101#1")
    // erikoislaakari
    acceptKoulutusKoodiUri(ErikoislaakariKoulutusKoodit, "koulutus_775101#1")
    // toteutukset
    when(toteutusDao.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(
      Seq(
        JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
        JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000124")))
      )
    )
    when(toteutusDao.getByKoulutusOid(koulutusOid2, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[Toteutus]())
    when(toteutusDao.getByKoulutusOid(kkOpintokokonaisuusKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[Toteutus]())

    when(organisaatioService.withoutOppilaitostyypit(anySeq[OrganisaatioOid], anySeq[String])).thenReturn(Seq())
  }

  it should "succeed when new valid koulutus" in {
    passesValidation(yoKoulutusWithParameters())
  }

  it should "succeed when incomplete luonnos" in {
    passesValidation(min)
  }

  it should "succeed when new incomplete luonnos Yo koulutus" in {
    passesValidation(
      yo.copy(
        tila = Tallennettu,
        metadata = Some(YliopistoKoulutusMetadata())
      )
    )
  }

  it should "succeed when new valid Yo koulutus and just one common koulutusKoodiUri with sorakuvaus" in {
    passesValidation(
      yoKoulutusWithParameters().copy(koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201111#1"))
    )
  }

  it should "succeed when new valid Amk koulutus" in {
    passesValidation(amk)
  }

  it should "succeed when incomplete luonnos Amk koulutus" in {
    passesValidation(amk.copy(tila = Tallennettu, metadata = Some(AmmattikorkeakouluKoulutusMetadata())))
  }

  it should "succeed when new valid AmmOpeErityisopeJaOpo koulutus" in {
    passesValidation(AmmOpettajaKoulutus)
  }

  it should "succeed when new valid Opettajan Pedagogiset Opinnot -koulutus" in {
    passesValidation(YoOpettajaKoulutus)
  }

  it should "succeed when new valid lukiokoulutus" in {
    passesValidation(LukioKoulutus)
  }

  it should "succeed when new incomplete luonnos lukiokoulutus" in {
    passesValidation(
      LukioKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          LukioKoulutusMetadata(koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_00#1"))
        )
      )
    )
  }

  it should "succeed when new valid Tuva koulutus" in {
    passesValidation(TuvaKoulutus)
  }

  it should "succeed when new incomplete luonnos Tuva koulutus" in {
    passesValidation(
      TuvaKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(TuvaKoulutusMetadata(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_8#1")))
      )
    )
  }

  it should "succeed when new valid Telma koulutus" in {
    passesValidation(TelmaKoulutus)
  }

  it should "succeed when new incomplete luonnos Telma koulutus" in {
    passesValidation(
      TelmaKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(TelmaKoulutusMetadata(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1")))
      )
    )
  }

  it should "succeed when new valid Vapaa sivistystyö opistovuosi koulutus" in {
    passesValidation(VapaaSivistystyoOpistovuosiKoulutus)
  }

  it should "succeed when new incomplete luonnos Vapaa sivistystyö opistovuosi koulutus" in {
    passesValidation(
      VapaaSivistystyoOpistovuosiKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          VapaaSivistystyoOpistovuosiKoulutusMetadata(opintojenLaajuusyksikkoKoodiUri =
            Some("opintojenlaajuusyksikko_2#1")
          )
        )
      )
    )
  }

  it should "succeed when new valid Vapaa sivistystyö muu koulutus" in {
    passesValidation(VapaaSivistystyoMuuKoulutus)
  }

  it should "succeed when new incomplete luonnos Vapaa sivistystyö muu koulutus" in {
    passesValidation(
      VapaaSivistystyoMuuKoulutus.copy(tila = Tallennettu, metadata = Some(VapaaSivistystyoMuuKoulutusMetadata()))
    )
  }

  it should "succeed when new valid Aikuisten perusopetus koulutus" in {
    passesValidation(AikuistenPerusopetusKoulutus)
  }

  it should "succeed when new incomplete luonnos Aikuisten perusopetus koulutus" in {
    passesValidation(
      AikuistenPerusopetusKoulutus.copy(tila = Tallennettu, metadata = Some(AikuistenPerusopetusKoulutusMetadata()))
    )
  }

  it should "succeed when new valid Kk-opintojakso koulutus" in {
    passesValidation(KkOpintojaksoKoulutus)
  }

  it should "succeed when new incomplete luonnos Kk-opintojakso koulutus" in {
    passesValidation(
      KkOpintojaksoKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(KkOpintojaksoKoulutusMetadata())
      )
    )
  }

  it should "succeed when new incomplete luonnos Kk-opintokokonaisuus koulutus" in {
    passesValidation(
      KkOpintokokonaisuusKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(KkOpintokokonaisuusKoulutusMetadata())
      )
    )
  }

  it should "Succeed when new valid Erikoislääkäri koulutus" in {
    passesValidation(ErikoislaakariKoulutus)
  }

  it should "Succeed when new valid Erikoistumiskoulutus" in {
    passesValidation(ErikoistumisKoulutus)
  }

  it should "Succeed when new incomplete luonnos Erikoistumiskoulutus" in {
    passesValidation(ErikoistumisKoulutus.copy(tila = Tallennettu, metadata = Some(ErikoistumiskoulutusMetadata())))
  }

  it should "Succeed when new valid ammatillinen koulutus" in {
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmKoulutus),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    ).thenAnswer(NoErrors)
    passesValidation(AmmKoulutus)
  }

  it should "Succeed when new valid TaiteenPerusopetus koulutus" in {
    passesValidation(TaiteenPerusopetusKoulutus)
  }

  it should "succeed when tarjoajat not changed in modify operation, even though unknown tarjoajat" in {
    val koulutus =
      yo.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid))
    passesValidation(koulutus, koulutus)
  }

  it should "succeed when koulutuksetKoodiUri not changed in modify operation, even though unknown Uris" in {
    val yoKoulutus =
      yo.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), koulutuksetKoodiUri = Seq("koulutus_000000#1"))
    passesValidation(yoKoulutus, yoKoulutus)
    val amkKoulutus =
      amk.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), koulutuksetKoodiUri = Seq("koulutus_000000#1"))
    passesValidation(amkKoulutus, amkKoulutus)
    val ammOpeErityisopeJaOpo = AmmOpettajaKoulutus.copy(
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      koulutuksetKoodiUri = Seq("koulutus_000000#1")
    )
    passesValidation(ammOpeErityisopeJaOpo, ammOpeErityisopeJaOpo)
    val lkKoulutus =
      LukioKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), koulutuksetKoodiUri = Seq("koulutus_000000#1"))
    passesValidation(lkKoulutus, lkKoulutus)
    val elKoulutus = ErikoislaakariKoulutus.copy(
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      koulutuksetKoodiUri = Seq("koulutus_000000#1")
    )
    passesValidation(elKoulutus, elKoulutus)
  }

  it should "succeed when lisatiedot not changed in modify operation, even though unknown otsikkoKoodiUri" in {
    val invalidLisatieto = Lisatieto1.copy(otsikkoKoodiUri = "koulutuksenlisatiedot_XX#1")
    val koulutus =
      yoKoulutusWithParameters(lisatiedot = Seq(invalidLisatieto)).copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(koulutus, koulutus)
  }

  it should "Succeed when koulutusalaKoodiUrit not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedYo = yoKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_66").copy(oid =
      Some(KoulutusOid("1.2.246.562.13.125"))
    )
    passesValidation(nonChangedYo, nonChangedYo)
    val nonChangedVst =
      vstMuuWithParams(koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_66"))
    passesValidation(nonChangedVst, nonChangedVst)

    val koulutusOid = KoulutusOid("1.2.246.562.13.129")
    when(toteutusDao.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[Toteutus]())
    val nonChangedKkOj =
      kkOpintojaksoWithParams(koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_66"), oid = koulutusOid)
    passesValidation(nonChangedKkOj, nonChangedKkOj)
  }

  it should "Succeed when laajuusYksikkoKoodiUri not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedVst = vstMuuWithParams(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_9#1"))
    passesValidation(nonChangedVst, nonChangedVst)
    val nonChangedAikPer =
      aikuistenPerusopetusWithParams(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_9#1"))
    passesValidation(nonChangedAikPer, nonChangedAikPer)
  }

  it should "Succeed when tutkintonimikeKoodiUrit not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedYo = yoKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_99#2")
      .copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(nonChangedYo, nonChangedYo)
    val nonChangedEl = ErikoislaakariKoulutus.copy(
      metadata = Some(
        ErikoislaakariKoulutusMetadata(
          tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_66#2"),
          koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso2_091#1")
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu
    )
    passesValidation(nonChangedEl, nonChangedEl)
  }

  it should "Succeed when erikoistumiskoulutusKoodiUri not changed in modify operation, even though invalid koodiUri" in {
    val nonChangedEk = ErikoistumisKoulutus.copy(
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu,
      metadata = Some(
        ErikoistumiskoulutusMetadata(
          opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
          erikoistumiskoulutusKoodiUri = Some("puppu")
        )
      )
    )
    passesValidation(nonChangedEk, nonChangedEk)
  }

  it should "Fail with more than 1 koulutuksetKoodiUri" in {
    failsValidation(
      ErikoislaakariKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_775101#1", "koulutus_775201#1")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
  }

  it should "Fail if unknown koulutusKoodiUri for Erikoislääkäri koulutus" in {
    failsValidation(
      ErikoislaakariKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "succeed when new valid Kk-opintokokonaisuuskoulutus" in {
    passesValidation(KkOpintokokonaisuusKoulutus)
  }

  it should "succeed when new incomplete luonnos Kk-opintokokonaisuuskoulutus" in {
    passesValidation(
      KkOpintokokonaisuusKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          KkOpintokokonaisuusKoulutusMetadata(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"))
        )
      )
    )
  }

  it should "fail if perustiedot is invalid" in {
    failsValidation(
      yo.copy(oid = Some(KoulutusOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(KoulutusOid("1.2.3"))))
      )
    )
    failsValidation(min.copy(nimi = Map(), kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(yo.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(yo.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(
      min.copy(organisaatioOid = OrganisaatioOid("1.2.3")),
      "organisaatioOid",
      validationMsg("1.2.3")
    )
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }

  it should "fail if koulutustyyppi changed in modify operation" in {
    failsModifyValidation(
      yo.copy(
        oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")),
        koulutustyyppi = Amk
      ),
      yo,
      Seq(
        ValidationError("koulutustyyppi", notModifiableMsg("koulutustyyppiä", "koulutukselle")),
        ValidationError("metadata.tyyppi", InvalidMetadataTyyppi)
      )
    )
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(yo, yo, Seq(ValidationError("oid", missingMsg)))
  }

  it should "fail if invalid tarjoaja OIDs" in {
    failsValidation(
      yo.copy(tarjoajat = List(OrganisaatioOid("1.2.3"), OrganisaatioOid("4.5.6"))),
      Seq(
        ValidationError("tarjoajat[0]", validationMsg("1.2.3")),
        ValidationError("tarjoajat[1]", validationMsg("4.5.6"))
      )
    )
  }

  it should "fail if tarjoaja OIDs not matching koulutustyyppi" in {
    failsValidation(
      yo.copy(tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid)),
      Seq(
        ValidationError("tarjoajat[1]", tarjoajaOidWoRequiredKoulutustyyppi(UnknownOid, Yo)),
        ValidationError("tarjoajat[2]", tarjoajaOidWoRequiredKoulutustyyppi(LonelyOid, Yo))
      )
    )
  }

  it should "fail if organisaatio-service not working when checking tarjoajat" in {
    failsValidation(
      yo.copy(tarjoajat = List(LonelyOid, organisaatioOidCausingFailure)),
      "tarjoajat",
      organisaatioServiceFailureMsg
    )
  }

  private def failSorakuvausValidation(koulutus: Koulutus): Assertion = {
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmMuuKoulutus.copy(sorakuvausId = Some(sorakuvausId))),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    )
      .thenAnswer(NoErrors)
    failsValidation(koulutus.copy(sorakuvausId = Some(sorakuvausId)), "sorakuvausId", notMissingMsg(Some(sorakuvausId)))
  }

  it should "fail if sorakuvausId given for koulutustyyppi not accepting sorakuvaus" in {
    failSorakuvausValidation(AikuistenPerusopetusKoulutus)
    failSorakuvausValidation(AmmMuuKoulutus)
    failSorakuvausValidation(TuvaKoulutus)
    failSorakuvausValidation(TelmaKoulutus)
    failSorakuvausValidation(VapaaSivistystyoOpistovuosiKoulutus)
    failSorakuvausValidation(VapaaSivistystyoMuuKoulutus)
  }

  it should "fail if sorakuvaus doesn't exist" in {
    failsValidation(
      yo.copy(sorakuvausId = Some(sorakuvausId2)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId2)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus is not yet julkaistu" in {
    failsValidation(
      yo.copy(sorakuvausId = Some(sorakuvausId3)),
      "tila",
      notYetJulkaistu("Sorakuvausta", sorakuvausId3)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    failsValidation(
      yo.copy(sorakuvausId = Some(sorakuvausId4)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId4)
    )
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    failsValidation(
      yo.copy(sorakuvausId = Some(sorakuvausId5)),
      "koulutustyyppi",
      tyyppiMismatch("sorakuvauksen", sorakuvausId5)
    )
  }

  it should "fail to store koulutus if koulutusKoodit doesn't match sorakuvaus koulutuskoodit" in {
    failsValidation(
      yo.copy(sorakuvausId = Some(sorakuvausId6)),
      "koulutuksetKoodiUri",
      valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
    )
  }

  def failTutkintoonjohtavuusValidation(koulutus: Koulutus, johtaaTutkintoon: Boolean): Unit = {
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmKoulutus.copy(johtaaTutkintoon = false)),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    )
      .thenAnswer(NoErrors)
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmTutkinnonOsaKoulutus.copy(johtaaTutkintoon = true)),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    )
      .thenAnswer(NoErrors)
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmOsaamisalaKoulutus.copy(johtaaTutkintoon = true)),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    )
      .thenAnswer(NoErrors)

    failsValidation(
      koulutus.copy(johtaaTutkintoon = johtaaTutkintoon),
      "johtaaTutkintoon",
      invalidTutkintoonjohtavuus(koulutus.koulutustyyppi.toString)
    )
  }

  it should "fail if wrong tutkintoonJohtavuus" in {
    failTutkintoonjohtavuusValidation(AmmKoulutus, false)
    failTutkintoonjohtavuusValidation(yo, false)
    failTutkintoonjohtavuusValidation(AmmTutkinnonOsaKoulutus, true)
    failTutkintoonjohtavuusValidation(AmmOsaamisalaKoulutus, true)
    failsValidation(yo.copy(johtaaTutkintoon = false), "johtaaTutkintoon", invalidTutkintoonjohtavuus(Yo.toString()))
  }

  it should "fail if invalid teemakuva" in {
    failsValidation(yo.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
  }

  it should "fail if invalid lisätieto" in {
    val invalidLisatieto = Lisatieto1.copy(otsikkoKoodiUri = "koulutuksenlisatiedot_04#1")
    failsValidation(
      yoKoulutusWithParameters(lisatiedot = Seq(invalidLisatieto)),
      "metadata.lisatiedot[0].otsikkoKoodiUri",
      invalidLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_04#1")
    )
  }

  it should "fail if lisätieto given for koulutustyyppi not accepting lisätiedot" in {
    failsValidation(
      vstMuuWithParams(lisatiedot = Seq(Lisatieto1)).copy(oid = None),
      "metadata.lisatiedot",
      notEmptyMsg
    )
  }

  it should "fail if metadata missing from julkaistu koulutus" in {
    failsValidation(yo.copy(metadata = None), "metadata", missingMsg)
  }

  it should "fail if the tyyppi of the metadata differs from the tyyppi of the koulutus" in {
    failsValidation(
      amk.copy(metadata = Some(AmmatillinenKoulutusMetadata(tyyppi = Yo))),
      "metadata.tyyppi",
      InvalidMetadataTyyppi
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with optional kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    passesValidation(
      amk.copy(metadata =
        Some(AmmattikorkeakouluKoulutusMetadata(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1")))
      )
    )
    failsValidation(
      amk.copy(metadata =
        Some(
          AmmattikorkeakouluKoulutusMetadata(
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
            kuvaus = invalidKuvaus
          )
        )
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with mandatory kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    val m = TuvaKoulutusMetadata(
      opintojenLaajuusNumero = Some(10),
      opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_8#1")
    )
    failsValidation(
      TuvaKoulutus.copy(metadata = Some(m)),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Fi, Sv))
    )
    failsValidation(
      TuvaKoulutus.copy(metadata = Some(m.copy(kuvaus = invalidKuvaus))),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  private def failValidationWithKoulutuksetKoodiUri(koulutus: Koulutus): Assertion = {
    val urit = Seq("koulutus_371101#1")
    failsValidation(koulutus.copy(koulutuksetKoodiUri = urit), "koulutuksetKoodiUri", notEmptyMsg)
  }

  it should "fail if koulutuksetKoodiUri given for koulutustyyppi not using koulutusKoodit" in {
    failValidationWithKoulutuksetKoodiUri(VapaaSivistystyoMuuKoulutus)
    failValidationWithKoulutuksetKoodiUri(KkOpintojaksoKoulutus)
  }

  private def failValidationWithePerusteId(koulutus: Koulutus): Assertion = {
    failsValidation(koulutus.copy(ePerusteId = Some(11L)), "ePerusteId", notMissingMsg(Some(11L)))
  }

  it should "fail if ePerusteId given for koulutustyyppi not using ePeruste" in {
    failValidationWithePerusteId(yo)
    failValidationWithePerusteId(amk)
    failValidationWithePerusteId(AmmOpettajaKoulutus)
    failValidationWithePerusteId(LukioKoulutus)
    failValidationWithePerusteId(TuvaKoulutus)
    failValidationWithePerusteId(TelmaKoulutus)
    failValidationWithePerusteId(VapaaSivistystyoOpistovuosiKoulutus)
    failValidationWithePerusteId(VapaaSivistystyoMuuKoulutus)
    failValidationWithePerusteId(AikuistenPerusopetusKoulutus)
    failValidationWithePerusteId(KkOpintojaksoKoulutus)
    failValidationWithePerusteId(KkOpintokokonaisuusKoulutus)
    failValidationWithePerusteId(ErikoistumisKoulutus)
  }

  it should "fail if invalid koulutusKoodiUri for Yo koulutus" in {
    failsValidation(yo.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(
      yo.copy(sorakuvausId = None, koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_000000#1")
    )
  }

  it should "fail if unknown koulutusalaKoodiUri for Yo koulutus" in {
    failsValidation(
      yoKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if invalid tutkintonimikeKoodiUri for Yo koulutus" in {
    failsValidation(
      yoKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "metadata.tutkintonimikeKoodiUrit[0]",
      invalidTutkintoNimikeKoodiuri("tutkintonimikekk_110#70")
    )
  }

  it should "fail if invalid opintojenLaajuusyksikkoKoodiUri for Yo koulutus" in {
    failsValidation(
      yoKoulutusWithParameters(opintojenLaajuusYksikkoKoodiUri = "opintojenlaajuusyksikko_5#1"),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
    )
  }

  it should "fail if invalid koulutusKoodiUri for Amk koulutus" in {
    failsValidation(amk.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(
      amk.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_000000#1")
    )
  }

  it should "fail if unknown koulutusalaKoodiUri for Amk koulutus" in {
    failsValidation(
      amkKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if unknown tutkintonimikeKoodiUri for Amk koulutus" in {
    failsValidation(
      amkKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "metadata.tutkintonimikeKoodiUrit[0]",
      invalidTutkintoNimikeKoodiuri("tutkintonimikekk_110#70")
    )
  }

  it should "fail if invalid opintojenLaajuusyksikkoKoodiUri for Amk koulutus" in {
    failsValidation(
      amkKoulutusWithParameters(opintojenLaajuusYksikkoKoodiUri = Some("opintojenlaajuusyksikko_5#1")),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
    )
  }

  it should "fail if invalid koulutuksetKoodiUri for AmmOpeErityisopeJaOpo koulutus" in {
    failsValidation(AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(
      AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1", "koulutus_222222#2")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
    failsValidation(
      AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "fail if invalid metadata for AmmOpeErityisopeJaOpo koulutus" in {
    failsValidation(
      AmmOpettajaKoulutus.copy(metadata =
        Some(
          AmmOpeErityisopeJaOpoKoulutusMetadata(
            tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01")
          )
        )
      ),
      Seq(
        ValidationError("metadata.tutkintonimikeKoodiUrit", notEmptyMsg),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError("metadata.opintojenLaajuusNumero", illegalOpintojenLaajuusNumero(60)),
        ValidationError(
          "metadata.koulutusalaKoodiUrit",
          illegalValueForFixedValueSeqMsg(koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso1_01"))
        )
      )
    )
  }

  it should "fail if invalid Opettajien Pedagogiset Opinnot -koulutus" in {
    failsValidation(
      YoOpettajaKoulutus.copy(
        koulutuksetKoodiUri = Seq("koulutus_111111#1"),
        metadata = Some(
          OpePedagOpinnotKoulutusMetadata(
            tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01")
          )
        )
      ),
      Seq(
        ValidationError("koulutuksetKoodiUri", illegalValueForFixedValueSeqMsg(koodiUriTipText("koulutus_919999"))),
        ValidationError("metadata.tutkintonimikeKoodiUrit", notEmptyMsg),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError("metadata.opintojenLaajuusNumero", illegalOpintojenLaajuusNumero(60)),
        ValidationError(
          "metadata.koulutusalaKoodiUrit",
          illegalValueForFixedValueSeqMsg(koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso1_01"))
        )
      )
    )
  }

  it should "fail if invalid koulutusKoodiUri for Lukio koulutus" in {
    failsValidation(LukioKoulutus.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(
      LukioKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "fail if invalid metadata for Lukio koulutus" in {
    failsValidation(
      LukioKoulutus.copy(metadata =
        Some(
          LukioKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01"),
            opintojenLaajuusNumero = Some(10),
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_XX#1")
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit",
          illegalValueForFixedValueSeqMsg(
            "kansallinenkoulutusluokitus2016koulutusalataso1_00#<versionumero>, esim. kansallinenkoulutusluokitus2016koulutusalataso1_00#1"
          )
        ),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        )
      )
    )
  }

  it should "fail if invalid metadata for luonnos Tuva koulutus" in {
    failsValidation(
      TuvaKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          TuvaKoulutusMetadata(
            lisatiedot = Seq(Lisatieto1),
            linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv")
          )
        )
      ),
      Seq(
        ValidationError("metadata.lisatiedot", notEmptyMsg),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_8"))
        ),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv"))
      )
    )
  }

  it should "fail if invalid metadata for julkaistu Tuva koulutus" in {
    failsValidation(
      TuvaKoulutus.copy(metadata =
        Some(
          TuvaKoulutusMetadata(
            linkkiEPerusteisiin = Map(Fi -> "http://www.vain.suomeksi.fi"),
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_99#1")
          )
        )
      ),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_8"))
        ),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for Telma koulutus" in {
    failsValidation(
      TelmaKoulutus.copy(metadata =
        Some(
          TelmaKoulutusMetadata(
            lisatiedot = Seq(Lisatieto1),
            linkkiEPerusteisiin = Map(Fi -> "puppu"),
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_99#1")
          )
        )
      ),
      Seq(
        ValidationError("metadata.lisatiedot", notEmptyMsg),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_6"))
        ),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos vapaa sivistystyo opistovuosi koulutus" in {
    failsValidation(
      VapaaSivistystyoOpistovuosiKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          VapaaSivistystyoOpistovuosiKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
            linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv")
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
        ),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv")),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        )
      )
    )
  }

  it should "fail if invalid metadata for julkaistu vapaa sivistystyo opistovuosi koulutus" in {
    failsValidation(
      VapaaSivistystyoOpistovuosiKoulutus.copy(
        metadata = Some(
          VapaaSivistystyoOpistovuosiKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("puppu"),
            linkkiEPerusteisiin = Map(Fi -> "http://www.vain.suomeksi.fi")
          )
        )
      ),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("puppu")
        ),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv))),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg)
      )
    )
  }

  it should "fail if invalid metadata for luonnos vapaa sivistystyo muu koulutus" in {
    failsValidation(
      vstMuuWithParams(
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
        linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv"),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
        opintojenLaajuusNumero = Some(-1)
      ).copy(tila = Tallennettu, oid = None),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
        ),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
        ),
        ValidationError("metadata.opintojenLaajuusNumero", notNegativeMsg),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv"))
      )
    )
  }

  it should "fail if invalid metadata for julkaistu vapaa sivistystyo muu koulutus" in {
    failsValidation(
      vstMuuWithParams(
        koulutusalaKoodiUrit = Seq("puppu"),
        linkkiEPerusteisiin = Map(Fi -> "http://www.vain.suomeksi.fi"),
        opintojenLaajuusyksikkoKoodiUri = None,
        opintojenLaajuusNumero = None
      ).copy(tila = Julkaistu, oid = None),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("puppu")
        ),
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid koulutusKoodiUri for Aikuisten perusopetus -koulutus" in {
    failsValidation(
      AikuistenPerusopetusKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri",
      illegalValueForFixedValueSeqMsg("koulutus_201101#<versionumero>, esim. koulutus_201101#1")
    )
  }

  it should "fail if invalid metadata for luonnos Aikuisten perusopetus -koulutus" in {
    failsValidation(
      aikuistenPerusopetusWithParams(
        lisatiedot = Seq(Lisatieto1),
        linkkiEPerusteisiin = Map(Fi -> "puppu"),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
        opintojenLaajuusNumero = Some(-1)
      ).copy(oid = None),
      Seq(
        ValidationError("metadata.lisatiedot", notEmptyMsg),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
        ),
        ValidationError("metadata.opintojenLaajuusNumero", notNegativeMsg)
      )
    )
  }

  it should "fail if invalid metadata for julkaistu Aikuisten perusopetus -koulutus" in {
    failsValidation(
      aikuistenPerusopetusWithParams(
        linkkiEPerusteisiin = Map(Fi -> "http://vain.suomeksi.fi"),
        opintojenLaajuusyksikkoKoodiUri = None,
        opintojenLaajuusNumero = None
      ).copy(oid = None, tila = Julkaistu),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos Kk-opintojakso koulutus" in {
    failsValidation(
      kkOpintojaksoWithParams(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
        opintojenLaajuusNumeroMin = Some(-1.0),
        opintojenLaajuusNumeroMax = Some(-5.0),
        koulutusalaKoodiUrit = Seq("puppu")
      ).copy(oid = None),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("puppu")
        ),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError(
          "metadata.opinnonTyyppiKoodiUri",
          invalidOpinnonTyyppiKoodiuri("opinnontyyppi_1#1")
        ),
        ValidationError("metadata.opintojenLaajuusNumeroMin", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMax", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMin", minmaxMsg(-1.0, -5.0))
      )
    )
  }

  it should "fail if missing metadata for julkaistu Kk-opintojakso koulutus" in {
    failsValidation(
      KkOpintojaksoKoulutus.copy(metadata = Some(KkOpintojaksoKoulutusMetadata())),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos Kk-opintokokonaisuus koulutus" in {
    val opintojenLaajuusNumeroMin = -5.0
    val opintojenLaajuusNumeroMax = -15.0
    failsValidation(
      KkOpintokokonaisuusKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          KkOpintokokonaisuusKoulutusMetadata(
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
            opintojenLaajuusNumeroMin = Some(opintojenLaajuusNumeroMin),
            opintojenLaajuusNumeroMax = Some(opintojenLaajuusNumeroMax),
            koulutusalaKoodiUrit = Seq("puppu"),
            opinnonTyyppiKoodiUri = Some("opinnontyyppi_100#1")
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("puppu")
        ),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError(
          "metadata.opinnonTyyppiKoodiUri",
          invalidOpinnonTyyppiKoodiuri("opinnontyyppi_100#1")
        ),
        ValidationError("metadata.opintojenLaajuusNumeroMin", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMax", notNegativeMsg),
        ValidationError(
          "metadata.opintojenLaajuusNumeroMin",
          minmaxMsg(opintojenLaajuusNumeroMin, opintojenLaajuusNumeroMax)
        )
      )
    )
  }

  it should "fail if missing metadata for julkaistu Kk-opintokokonaisuus koulutus" in {
    failsValidation(
      KkOpintokokonaisuusKoulutus.copy(metadata = Some(KkOpintokokonaisuusKoulutusMetadata())),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv)))
      )
    )
  }

  it should "fail if julkaistu kk-opintokokonaisuus koulutus has at least one julkaistu toteutus whose opintojenlaajuusNumero is not in the range specified in koulutus" in {

    val opintokokonaisuusKoulutus    = kkOpintokokonaisuusKoulutus
    val opintokokonaisuusToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(opintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(opintokokonaisuusToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusNumero = Some(20)
            )
          )
        )
      )
    )

    failsModifyValidation(
      opintokokonaisuusKoulutus,
      opintokokonaisuusKoulutus,
      Seq(
        ValidationError(
          "metadata.opintojenLaajuusNumeroMax",
          invalidKoulutusOpintojenLaajuusNumeroIntegrity(5, 15, Seq(opintokokonaisuusToteutusOid))
        )
      )
    )
  }

  it should "fail if invalid metadata for luonnos 'muu' koulutus" in {
    failsValidation(
      muuKoulutusWithParams(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
        opintojenLaajuusNumeroMin = Some(-1.0),
        opintojenLaajuusNumeroMax = Some(-5.0),
      ).copy(oid = None),
      Seq(
        ValidationError("metadata.opintojenLaajuusNumeroMin", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMax", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMin", minmaxMsg(-1.0, -5.0))
      )
    )
  }

  it should "fail if julkaistu 'muu' koulutus has at least one julkaistu toteutus whose opintojenlaajuusMin is not in the range specified in koulutus" in {
    val muuKoulutus1   = muuKoulutusWithParams(opintojenLaajuusNumeroMin = Some(10), opintojenLaajuusNumeroMax = Some(20)).copy(tila = Julkaistu)
    val muuToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(muuKoulutus1.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuMuuToteutus.copy(
          oid = Some(muuToteutusOid),
          koulutusOid = muuKoulutus1.oid.get,
          metadata = Some(
            MuuToteutuksenMetatieto.copy(
              opintojenLaajuusNumeroMin = Some(9),
              opintojenLaajuusNumeroMax = Some(21)
            )
          )
        )
      )
    )

    failsModifyValidation(
      muuKoulutus1,
      muuKoulutus1,
      Seq(
        ValidationError(
          "metadata.opintojenLaajuusNumeroMin",
          invalidKoulutusOpintojenLaajuusNumeroIntegrity(10, 20, Seq(muuToteutusOid))
        ),
        ValidationError(
          "metadata.opintojenLaajuusNumeroMax",
          invalidKoulutusOpintojenLaajuusNumeroIntegrity(10, 20, Seq(muuToteutusOid))
        )
      )
    )
  }

  it should "fail if julkaistu kk-opintokokonaisuus koulutus has at least one julkaistu toteutus whose opintojenlaajuusyksikko is not the same as in koulutus" in {

    val opintokokonaisuusKoulutus    = kkOpintokokonaisuusKoulutus
    val opintokokonaisuusToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(opintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(opintokokonaisuusToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_5#1")
            )
          )
        )
      )
    )

    failsModifyValidation(
      opintokokonaisuusKoulutus,
      opintokokonaisuusKoulutus,
      Seq(
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          invalidKoulutusOpintojenLaajuusyksikkoIntegrity(
            "opintojenlaajuusyksikko_2#1",
            Seq(opintokokonaisuusToteutusOid)
          )
        )
      )
    )
  }

  it should "pass when julkaistu kk-opintokokonaisuus koulutus doesn't have laajuus range and has two toteutus with different laajuusNumero" in {
    val opintokokonaisuusKoulutus = kkOpintokokonaisuusKoulutus.copy(
      metadata = Some(
        KkOpintokokonaisuusKoulutuksenMetatieto.copy(
          opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
          opintojenLaajuusNumeroMin = None,
          opintojenLaajuusNumeroMax = None
        )
      )
    )

    when(toteutusDao.getByKoulutusOid(opintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
              opintojenLaajuusNumero = Some(20)
            )
          )
        ),
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
              opintojenLaajuusNumero = Some(40)
            )
          )
        )
      )
    )

    passesValidation(opintokokonaisuusKoulutus, opintokokonaisuusKoulutus)
  }

  it should "pass when julkaistu kk-opintokokonaisuus koulutus has one toteutus which has no laajuusyksikkö or laajuusNumero defined" in {
    when(toteutusDao.getByKoulutusOid(kkOpintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = kkOpintokokonaisuusKoulutus.oid.get
        ),
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = kkOpintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = None,
              opintojenLaajuusNumero = None
            )
          )
        )
      )
    )

    passesValidation(kkOpintokokonaisuusKoulutus, kkOpintokokonaisuusKoulutus)
  }

  it should "pass when saving kk-opintojakso with no toteutus and changed isAvoinKorkeakulutus" in {
    val oldOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true))
    val newOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(false))

    when(toteutusDao.getByKoulutusOid(newOpintojaksoKoulutus.oid.get, TilaFilter.onlyOlemassaolevat()))
      .thenReturn(
        Nil
      )

    passesValidation(newOpintojaksoKoulutus, oldOpintojaksoKoulutus)
  }

  it should "fail when kk-opintojakso has attached julkaistu toteutus and trying change isAvoinKorkeakoulutus" in {

    val oldOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true))
    val newOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(false))
    val opintojaksoToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(oldOpintojaksoKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())).thenReturn(
      Seq(
        JulkaistuKkOpintojaksoToteutus.copy(
          oid = Some(opintojaksoToteutusOid),
          koulutusOid = oldOpintojaksoKoulutus.oid.get,
          metadata = Some(
            KkOpintojaksoToteutuksenMetatieto.copy(
              isAvoinKorkeakoulutus = Some(true)
            )
          )
        )
      )
    )

    failsModifyValidation(
      newOpintojaksoKoulutus,
      oldOpintojaksoKoulutus,
      Seq(
        ValidationError(
          "metadata.isAvoinKorkeakoulutus",
          cannotChangeIsAvoinKorkeakoulutus
        )
      )
    )
  }

  it should "fail when trying to remove tarjoaja that is attached to a toteutus" in {
    val oldOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true), tarjoajat = List(HkiYoOid, YoOid))
    val newOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true), tarjoajat = List(YoOid))
    val opintojaksoToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(oldOpintojaksoKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())).thenReturn(
      Seq(
        JulkaistuKkOpintojaksoToteutus.copy(
          tarjoajat = List(ChildOid),
          oid = Some(opintojaksoToteutusOid),
          koulutusOid = oldOpintojaksoKoulutus.oid.get,
          metadata = Some(
            KkOpintojaksoToteutuksenMetatieto.copy(
              isAvoinKorkeakoulutus = Some(true)
            )
          )
        )
      )
    )
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(HkiYoOid)).thenReturn((Seq(HkiYoOid, OphOid, ChildOid), Seq(Yo)))

    failsModifyValidation(
      newOpintojaksoKoulutus,
      oldOpintojaksoKoulutus,
      Seq(
        ValidationError(
          "tarjoajat",
          cannotRemoveTarjoajaFromAvoinKorkeakoulutus(List(HkiYoOid))
        )
      )
    )
  }

  it should "pass when saving avoin kk-opintojakso with kansalaisopisto tarjoaja" in {
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(KuopionKansalaisopistoOid))
      .thenAnswer(
        Seq(KuopionKansalaisopistoOid),
        Seq(
          Amm,
          Lk,
          Muu,
          VapaaSivistystyoOpistovuosi,
          VapaaSivistystyoMuu,
          AikuistenPerusopetus
        )
      )

    when(
      organisaatioService.withoutOppilaitostyypit(
        Seq(KuopionKansalaisopistoOid),
        oppilaitostyypitForAvoinKorkeakoulutus
      )
    )
      .thenReturn(Seq())

    val oldOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true))
    val newOpintojaksoKoulutus =
      kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true), tarjoajat = List(KuopionKansalaisopistoOid))

    passesValidation(newOpintojaksoKoulutus, oldOpintojaksoKoulutus)
  }

  it should "fail when saving avoin kk-opintojakso with unauthorized tarjoaja" in {
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(HelsinginNormaalilyseoOid))
      .thenAnswer(
        Seq(HelsinginNormaalilyseoOid),
        Seq(Lk, Muu, Tuva, AikuistenPerusopetus)
      )

    when(
      organisaatioService.withoutOppilaitostyypit(
        Seq(HelsinginNormaalilyseoOid),
        oppilaitostyypitForAvoinKorkeakoulutus
      )
    )
      .thenReturn(Seq(HelsinginNormaalilyseoOid))

    val oldOpintojaksoKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true))
    val newOpintojaksoKoulutus =
      kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true), tarjoajat = List(HelsinginNormaalilyseoOid))

    failsModifyValidation(
      newOpintojaksoKoulutus,
      oldOpintojaksoKoulutus,
      Seq(
        ValidationError(
          "tarjoajat[0]",
          tarjoajaOidWoRequiredOppilaitostyyppi(HelsinginNormaalilyseoOid, oppilaitostyypitForAvoinKorkeakoulutus)
        )
      )
    )
  }

  it should "fail if invalid luonnos Erikoistumiskoulutus" in {
    failsValidation(
      ErikoistumisKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          ErikoistumiskoulutusMetadata(
            erikoistumiskoulutusKoodiUri = Some("puppu"),
            koulutusalaKoodiUrit = Seq("huttu"),
            opintojenLaajuusyksikkoKoodiUri = Some("höttö"),
            opintojenLaajuusNumeroMin = Some(-1.0),
            opintojenLaajuusNumeroMax = Some(-5.0)
          )
        )
      ),
      Seq(
        ValidationError("metadata.erikoistumiskoulutusKoodiUri", invalidErikoistumiskoulutusKoodiuri("puppu")),
        ValidationError("metadata.koulutusalaKoodiUrit[0]", invalidKoulutusAlaKoodiuri("huttu")),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuusyksikko_2"))
        ),
        ValidationError("metadata.opintojenLaajuusNumeroMin", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMax", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMin", minmaxMsg(-1.0, -5.0))
      )
    )
  }

  it should "fail if invalid julkaistu Erikoistumiskoulutus" in {
    failsValidation(
      ErikoistumisKoulutus.copy(metadata = Some(ErikoistumiskoulutusMetadata())),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.erikoistumiskoulutusKoodiUri", missingMsg)
      )
    )
  }

  it should "pass when saving kk-opintokokonaisuus with no toteutus and changed isAvoinKorkeakulutus" in {
    val oldOpintokokonaisuusKoulutus = kkOpintokokonaisuusWithParams(isAvoinKorkeakoulutus = Some(false))
    val newOpintokokonaisuusKoulutus = kkOpintokokonaisuusWithParams(isAvoinKorkeakoulutus = Some(true))

    when(toteutusDao.getByKoulutusOid(newOpintokokonaisuusKoulutus.oid.get, TilaFilter.onlyOlemassaolevat()))
      .thenReturn(
        Nil
      )

    passesValidation(newOpintokokonaisuusKoulutus, oldOpintokokonaisuusKoulutus)
  }

  it should "fail when kk-opintokokonaisuus has attached julkaistu toteutus and trying change isAvoinKorkeakoulutus" in {
    val oldOpintokokonaisuusKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(true))
    val newOpintokokonaisuusKoulutus = kkOpintojaksoWithParams(isAvoinKorkeakoulutus = Some(false))
    val opintokokonaisuusToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(oldOpintokokonaisuusKoulutus.oid.get, TilaFilter.onlyOlemassaolevat()))
      .thenReturn(
        Seq(
          JulkaistuKkOpintokokonaisuusToteutus.copy(
            oid = Some(opintokokonaisuusToteutusOid),
            koulutusOid = oldOpintokokonaisuusKoulutus.oid.get,
            metadata = Some(
              KkOpintokokonaisuusToteutuksenMetatieto.copy(
                isAvoinKorkeakoulutus = Some(true)
              )
            )
          )
        )
      )

    failsModifyValidation(
      newOpintokokonaisuusKoulutus,
      oldOpintokokonaisuusKoulutus,
      Seq(
        ValidationError(
          "metadata.isAvoinKorkeakoulutus",
          cannotChangeIsAvoinKorkeakoulutus
        )
      )
    )

  }

  val ammWithOid = AmmKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")))
  it should "fail if invalid ammatillinen koulutus" in {
    when(
      ammKoulutusServiceValidation.validate(
        ArgumentMatchers.eq[Koulutus](AmmKoulutus),
        any[Option[Koulutus]],
        any[ValidationContext]
      )
    ).thenAnswer(error("path", missingMsg))
    failsValidation(AmmKoulutus, "path", missingMsg)
  }

  val yoWithOid = yo.copy(oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")))
  it should "fail if invalid TaiteenPerusopetus -koulutus" in {
    failsValidation(
      TaiteenPerusopetusKoulutus.copy(
        koulutuksetKoodiUri = Seq("koulutus_201101#12"),
        metadata = Some(
          TaiteenPerusopetusKoulutusMetadata(
            kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
            linkkiEPerusteisiin = Map(Fi -> "puppua suomeksi"),
            isMuokkaajaOphVirkailija = Some(false)
          )
        )
      ),
      Seq(
        ValidationError(
          "koulutuksetKoodiUri",
          illegalValueForFixedValueSeqMsg("koulutus_999907#<versionumero>, esim. koulutus_999907#1")
        ),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppua suomeksi")),
        ValidationError("metadata.linkkiEPerusteisiin", kielistettyWoSvenskaError)
      )
    )
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(yoWithOid, yo.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(yoWithOid.copy(tila = Arkistoitu), yo)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(yoWithOid, yo.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(yoWithOid.copy(tila = Tallennettu), yo)
  }

  it should "succeed from tallennettu to poistettu when no existing toteutukset for koulutus" in {
    passesValidation(
      yo.copy(oid = Some(koulutusOid2), tila = Poistettu),
      yo.copy(tila = Tallennettu)
    )
  }

  it should "fail from tallennettu to arkistoitu" in {
    failsStageChangeValidation(
      yoWithOid.copy(tila = Arkistoitu),
      yo.copy(tila = Tallennettu),
      illegalStateChange("koulutukselle", Tallennettu, Arkistoitu)
    )
  }

  it should "fail from arkistoitu to tallennettu" in {
    failsStageChangeValidation(
      yoWithOid.copy(tila = Tallennettu),
      yo.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Tallennettu)
    )
  }

  it should "fail from julkaistu to poistettu" in {
    failsStageChangeValidation(
      yoWithOid.copy(tila = Poistettu),
      yo.copy(tila = Julkaistu),
      illegalStateChange("koulutukselle", Julkaistu, Poistettu)
    )
  }

  it should "fail from arkistoitu to poistettu" in {
    failsStageChangeValidation(
      yoWithOid.copy(tila = Poistettu),
      yo.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Poistettu)
    )
  }

  it should "fail from poistettu to tallennettu" in {
    failsStageChangeValidation(
      yoWithOid.copy(tila = Tallennettu),
      yo.copy(tila = Poistettu),
      illegalStateChange("koulutukselle", Poistettu, Tallennettu)
    )
  }

  it should "fail from tallennettu to poistettu when existing toteutukset for koulutus" in {
    failsStageChangeValidation(
      yoWithOid.copy(oid = Some(koulutusOid), tila = Poistettu),
      yo.copy(tila = Tallennettu),
      integrityViolationMsg("Koulutusta", "toteutuksia")
    )
  }
}
