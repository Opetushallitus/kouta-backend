package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodiUri, KoodistoQueryException, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{KoulutusServiceValidation, OrganisaatioService}
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._
import org.scalatest.Assertion

import java.util.UUID

class KoulutusServiceValidationSpec extends BaseServiceValidationSpec[Koulutus] {
  val koulutusKoodiClient = mock[KoulutusKoodiClient]
  val ePerusteKoodiClient = mock[EPerusteKoodiClient]
  var organisaatioService = mock[OrganisaatioService]
  val toteutusDao         = mock[ToteutusDAO]
  val sorakuvausDao       = mock[SorakuvausDAO]

  val sorakuvausId  = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()
  val sorakuvausId3 = UUID.randomUUID()
  val sorakuvausId4 = UUID.randomUUID()
  val sorakuvausId5 = UUID.randomUUID()
  val sorakuvausId6 = UUID.randomUUID()
  val sorakuvausId7 = UUID.randomUUID()
  val koulutusOid   = KoulutusOid("1.2.246.562.13.00000000000000000123")
  val koulutusOid2  = KoulutusOid("1.2.246.562.13.00000000000000000124")

  val amm: Koulutus   = AmmKoulutus
  val yo: Koulutus    = YoKoulutus
  val amk: Koulutus   = AmkKoulutus
  val min: Koulutus   = MinKoulutus
  val ammTk: Koulutus = AmmTutkinnonOsaKoulutus
  val ammOa: Koulutus = AmmOsaamisalaKoulutus

  val kkOpintokokonaisuusKoulutus = KkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.133")))

  private def yoKoulutusWithParameters(
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
      tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
      opintojenlaajuusKoodiUri: String = "opintojenlaajuus_40#1"
  ) = yo.copy(
    sorakuvausId = Some(sorakuvausId5),
    metadata = Some(
      yo.metadata.get
        .asInstanceOf[YliopistoKoulutusMetadata]
        .copy(
          koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
          tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
          opintojenLaajuusKoodiUri = Some(opintojenlaajuusKoodiUri)
        )
    )
  )

  private def amkKoulutusWithParameters(
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
      tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
      opintojenlaajuusKoodiUri: String = "opintojenlaajuus_40#1"
  ) = amk.copy(metadata =
    Some(
      amk.metadata.get
        .asInstanceOf[AmmattikorkeakouluKoulutusMetadata]
        .copy(
          koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
          tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
          opintojenLaajuusKoodiUri = Some(opintojenlaajuusKoodiUri)
        )
    )
  )

  private def ammTkWithTutkinnonOsaParams(
      ePerusteId: Option[Long] = None,
      koulutusKoodiUri: Option[String] = None,
      tutkinnonOsaId: Option[Long] = None,
      tutkinnonOsaViite: Option[Long] = None
  ) =
    ammTk.copy(
      tila = Tallennettu,
      metadata = Some(
        ammTk.metadata.get
          .asInstanceOf[AmmatillinenTutkinnonOsaKoulutusMetadata]
          .copy(tutkinnonOsat = Seq(TutkinnonOsa(ePerusteId, koulutusKoodiUri, tutkinnonOsaId, tutkinnonOsaViite)))
      )
    )

  private def ammMuuKoulutusWithParameters(
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso1_01",
      opintojenlaajusyksikkoKoodiUri: String = "opintojenlaajuusyksikko_6#1",
      opintojenLaajuusNumero: Option[Double] = Some(10)
  ): Koulutus =
    AmmMuuKoulutus.copy(metadata =
      Some(
        AmmMuuKoulutus.metadata.get
          .asInstanceOf[AmmatillinenMuuKoulutusMetadata]
          .copy(
            koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
            opintojenLaajuusyksikkoKoodiUri = Some(opintojenlaajusyksikkoKoodiUri),
            opintojenLaajuusNumero = opintojenLaajuusNumero
          )
      )
    )

  private def vstMuuWithParams(
      koulutusalaKoodiUrit: Seq[String] = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_6#1"),
      linkkiEPerusteisiin: Kielistetty = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
      opintojenLaajuusNumero: Option[Double] = Some(12)
  ): Koulutus =
    VapaaSivistystyoMuuKoulutus.copy(
      metadata = Some(
        VapaaSivistystyoMuuKoulutusMetadata(
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          linkkiEPerusteisiin = linkkiEPerusteisiin,
          opintojenLaajuusNumero = opintojenLaajuusNumero
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu
    )

  private def kkOpintoJaksoWithParams(
      koulutusalaKoodiUrit: Seq[String] = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      opintojenLaajuusyksikkoKoodiUri: Option[String] = Some("opintojenlaajuusyksikko_6#1"),
      opintojenLaajuusNumero: Option[Double] = Some(12)
  ): Koulutus =
    KkOpintojaksoKoulutus.copy(
      metadata = Some(
        KkOpintojaksoKoulutusMetadata(
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          opintojenLaajuusyksikkoKoodiUri = opintojenLaajuusyksikkoKoodiUri,
          opintojenLaajuusNumero = opintojenLaajuusNumero
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
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

  private def ammWithLisatietoParams(
      koodiUri: String = "koulutuksenlisatiedot_03#1",
      fi: String = "Opintojen lisatiedot fi",
      sv: String = "Opintojen lisatiedot sv"
  ): Koulutus =
    amm.copy(metadata =
      Some(AmmatillinenKoulutusMetadata(lisatiedot = Seq(Lisatieto(koodiUri, Map(Fi -> fi, Sv -> sv)))))
    )

  override val validator =
    new KoulutusServiceValidation(
      koulutusKoodiClient,
      ePerusteKoodiClient,
      organisaatioService,
      toteutusDao,
      sorakuvausDao
    )

  private def acceptKoulutusKoodiUri(tyypit: Seq[String], koodiUri: String): Unit =
    when(
      koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(tyypit, koodiUri)
    ).thenAnswer(itemFound)

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(
      organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, EvilGrandChildOid, EvilCousin))
    ).thenAnswer(Right(Set[OrganisaatioOid]()))
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, UnknownOid, LonelyOid)))
      .thenAnswer(Right(Set(UnknownOid, LonelyOid)))
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(LonelyOid, UnknownOid)))
      .thenAnswer(Left(new RuntimeException()))
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_04#1")).thenAnswer(itemNotFound)

    // sorakuvaukset
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId))
      .thenAnswer((Some(Julkaistu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId2)).thenAnswer((None, None, None))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId3))
      .thenAnswer((Some(Tallennettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId4))
      .thenAnswer((Some(Poistettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId5))
      .thenAnswer((Some(Julkaistu), Some(Yo), Some(Seq("koulutus_371101#1", "koulutus_201000#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId6))
      .thenAnswer((Some(Julkaistu), Some(Amm), Some(Seq("koulutus_000001#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId7)).thenAnswer((Some(Julkaistu), Some(Amm), None))

    // yleiset metadatat
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso1_01"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_080#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#1"))
      .thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_5#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_v53#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110#2")).thenAnswer(itemFound)

    // ammatilliset
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#12")
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#12")
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_371101"))))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(123L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_371101"))))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(111L)).thenAnswer(Right(Seq[KoodiUri]()))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(200L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_371101"))))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L))
      .thenAnswer(Right(Seq(koodiUriFromString("osaamisala_01"))))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(123L)).thenAnswer(Right(Seq[KoodiUri]()))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(123L))
      .thenAnswer(Right(Seq((122L, 1234L), (123L, 1235L))))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(200L)).thenAnswer(Right(Seq[(Long, Long)]()))
    val ePerusteFailure = KoodistoQueryException("url", 500, "ePerusteServiceFailed")
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(66L)).thenAnswer(Left(ePerusteFailure))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPeruste(66L)).thenAnswer(Left(ePerusteFailure))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(66L)).thenAnswer(Left(ePerusteFailure))

    // korkeakoulu
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_201000#1")
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_201111#1")
    acceptKoulutusKoodiUri(amkKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(amkKoulutustyypit, "koulutus_201000#1")
    when(koulutusKoodiClient.koulutusKoodiUriExists(ammOpeErityisopeJaOpoKoulutusKoodiUrit, "koulutus_000002#12"))
      .thenAnswer(itemFound)
    // lukio
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301101#1")).thenAnswer(itemFound)
    // erikoislaakari
    when(koulutusKoodiClient.koulutusKoodiUriExists(erikoislaakariKoulutusKoodiUrit, "koulutus_775101#1"))
      .thenAnswer(itemFound)
    // toteutukset
    when(toteutusDao.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(
      Seq(
        JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
        JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000124")))
      )
    )
    when(toteutusDao.getByKoulutusOid(koulutusOid2, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[Toteutus]())
  }

  "Validation" should "succeed when new valid koulutus" in {
    passesValidation(amm.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when incomplete luonnos" in {
    passesValidation(min)
  }

  it should "succeed when koulutuskoodiUri has higher version than the matching koodiUri in ePeruste" in {
    passesValidation(amm.copy(koulutuksetKoodiUri = Seq("koulutus_371101#12")))
  }

  it should "succeed when new valid AmmOsaamisala koulutus" in {
    passesValidation(ammOa.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when new valid AmmTutkinnonOsa koulutus" in {
    passesValidation(ammTk.copy(sorakuvausId = Some(sorakuvausId7)))
  }

  it should "succeed when new valid AmmTutkinnonOsa without koulutusKoodiUri" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(124L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(124L)).thenAnswer(Right(Seq((134L, 1345L))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(124L), None, Some(1345L), Some(134L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with koulutusKoodiUri only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(125L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_123456"))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(125L), Some("koulutus_123456#1"), None, None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(126L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(126L)).thenAnswer(Right(Seq((11111L, 1346L))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(126L), None, Some(1346L), None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaViite only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(127L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(127L)).thenAnswer(Right(Seq((135L, 111111L))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(127L), None, None, Some(135L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with ePerusteId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(128L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(128L), None, None, None))
  }

  it should "succeed when new incomplete luonnos AmmTutkinnonOsa" in {
    passesValidation(
      ammTk.copy(
        tila = Tallennettu,
        metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata())
      )
    )
    passesValidation(
      ammTkWithTutkinnonOsaParams(None, Some("koulutus_371101#1"), Some(5L), Some(6L)).copy(
        tila = Tallennettu
      )
    )
  }

  it should "succeed when new incomplete luonnos AmmOsaamisala" in {
    passesValidation(
      ammOa.copy(
        tila = Tallennettu,
        metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata())
      )
    )
    passesValidation(
      ammOa.copy(
        ePerusteId = None,
        tila = Tallennettu,
        metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata(osaamisalaKoodiUri = Some("osaamisala_01")))
      )
    )
  }

  it should "succeed when new valid AmmMuu koulutus" in {
    validator.withValidation(AmmMuuKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new incomplete luonnos AmmMuu koulutus" in {
    passesValidation(AmmMuuKoulutus.copy(tila = Tallennettu, metadata = Some(AmmatillinenMuuKoulutusMetadata())))
  }

  it should "succeed when new valid Yo koulutus" in {
    passesValidation(yoKoulutusWithParameters())
  }

  it should "succeed when new incomplete luonnos Yo koulutus" in {
    passesValidation(yo.copy(tila = Tallennettu, metadata = Some(YliopistoKoulutusMetadata())))
  }

  it should "succeed when new valid Yo koulutus and just one common koulutusKoodiUri with sorakuvaus" in {
    passesValidation(yoKoulutusWithParameters().copy(koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201111#1")))
  }

  it should "succeed when new valid Amk koulutus" in {
    passesValidation(amk)
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

  it should "succeed when new valid Tuva koulutus" in {
    passesValidation(TuvaKoulutus)
  }

  it should "succeed when new incomplete luonnos Tuva koulutus" in {
    passesValidation(TuvaKoulutus.copy(tila = Tallennettu, metadata = Some(TuvaKoulutusMetadata())))
  }

  it should "succeed when new valid Telma koulutus" in {
    passesValidation(TelmaKoulutus)
  }

  it should "succeed when new valid Vapaa sivistystyö opistovuosi koulutus" in {
    passesValidation(VapaaSivistystyoOpistovuosiKoulutus)
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
    passesValidation(KkOpintojaksoKoulutus.copy(tila = Tallennettu, metadata = Some(KkOpintojaksoKoulutusMetadata())))
  }

  it should "Succeed when new valid Erikoislääkäri koulutus" in {
    passesValidation(ErikoislaakariKoulutus)
  }

  it should "succeed when tarjoajat not changed in modify operation, even though unknown tarjoajat" in {
    val koulutus =
      amm.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid))
    passesValidation(koulutus, koulutus)
  }

  it should "succeed when koulutuksetKoodiUri not changed in modify operation, even though unknown Uris" in {
    val ammKoulutus =
      amm.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), koulutuksetKoodiUri = Seq("koulutus_000000#1"))
    passesValidation(ammKoulutus, ammKoulutus)
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
    val koulutus = ammWithLisatietoParams(koodiUri = "koulutuksenlisatiedot_XX#1")
      .copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(koulutus, koulutus)
  }

  it should "Succeed when ePerusteId not changed in modify operation, even though ePerusteId unknown" in {
    val nonChangedAmm = amm.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), ePerusteId = Some(111L))
    passesValidation(nonChangedAmm, nonChangedAmm)
    val nonChangedAmmOa = ammOa.copy(oid = Some(KoulutusOid("1.2.246.562.13.125")), ePerusteId = Some(111L))
    passesValidation(nonChangedAmmOa, nonChangedAmmOa)
  }

  it should "Succeed when ammTutkinnonosa not changed in modify operation, even though invalid parameters" in {
    val nonChangedAmmTk = ammTkWithTutkinnonOsaParams(Some(111L), Some("koulutus_12345#1"), Some(66L), Some(66L))
      .copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(nonChangedAmmTk, nonChangedAmmTk)
  }

  it should "Succeed when koulutusalaKoodiUrit not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedAmmMuu = ammMuuKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_66").copy(oid =
      Some(KoulutusOid("1.2.246.562.13.125"))
    )
    passesValidation(nonChangedAmmMuu, nonChangedAmmMuu)
    val nonChangedYo = yoKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_66").copy(oid =
      Some(KoulutusOid("1.2.246.562.13.125"))
    )
    passesValidation(nonChangedYo, nonChangedYo)
    val nonChangedVst =
      vstMuuWithParams(koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_66"))
    passesValidation(nonChangedVst, nonChangedVst)
    val nonChangedKkOj =
      kkOpintoJaksoWithParams(koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_66"))
    passesValidation(nonChangedKkOj, nonChangedKkOj)
  }

  it should "Succeed when laajuusKoodiUri not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedYo = yoKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_99#1")
      .copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(nonChangedYo, nonChangedYo)
    val nonChangedLk = LukioKoulutus.copy(
      metadata = Some(
        LukioKoulutusMetadata(
          opintojenLaajuusKoodiUri = Some("opintojenlaajuus_99#1"),
          koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_00#1")
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125"))
    )
    passesValidation(nonChangedLk, nonChangedLk)
    val nonChangedTuva = TuvaKoulutus.copy(
      metadata = Some(
        TuvaKoulutusMetadata(
          opintojenLaajuusKoodiUri = Some("opintojenlaajuus_99#1")
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125")),
      tila = Tallennettu
    )
    passesValidation(nonChangedTuva, nonChangedTuva)
    val nonChangedVst = VapaaSivistystyoOpistovuosiKoulutus.copy(
      metadata = Some(
        VapaaSivistystyoOpistovuosiKoulutusMetadata(
          opintojenLaajuusKoodiUri = Some("opintojenlaajuus_99#1"),
          kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv")
        )
      ),
      oid = Some(KoulutusOid("1.2.246.562.13.125"))
    )
    passesValidation(nonChangedVst, nonChangedVst)
  }

  it should "Succeed when laajuusYksikkoKoodiUri not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedAmmMuu = ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "opintojenlaajuusyksikko_9#1")
      .copy(oid = Some(KoulutusOid("1.2.246.562.13.125")))
    passesValidation(nonChangedAmmMuu, nonChangedAmmMuu)
    val nonChangedVst = vstMuuWithParams(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_9#1"))
    passesValidation(nonChangedVst, nonChangedVst)
    val nonChangedAikPer =
      aikuistenPerusopetusWithParams(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_9#1"))
    passesValidation(nonChangedAikPer, nonChangedAikPer)
    val nonChangedKkOj =
      kkOpintoJaksoWithParams(opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_9#1"))
    passesValidation(nonChangedKkOj, nonChangedKkOj)
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
      KkOpintokokonaisuusKoulutus.copy(tila = Tallennettu, metadata = Some(KkOpintokokonaisuusKoulutusMetadata()))
    )
  }

  it should "fail if perustiedot is invalid" in {
    failsValidation(
      amm.copy(oid = Some(KoulutusOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(KoulutusOid("1.2.3"))))
      )
    )
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(amm.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(
      min.copy(organisaatioOid = OrganisaatioOid("1.2.3")),
      "organisaatioOid",
      validationMsg("1.2.3")
    )
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }

  it should "fail if koulutustyyppi changed in modify operation" in {
    failsModifyValidation(amm.copy(oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")), johtaaTutkintoon = false, koulutustyyppi = AmmOsaamisala), amm, Seq(
      ValidationError("koulutustyyppi", notModifiableMsg("koulutustyyppiä", "koulutukselle")),
      ValidationError("metadata.tyyppi", InvalidMetadataTyyppi)
    ))
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(amm, amm, Seq(ValidationError("oid", missingMsg)))
  }

  it should "fail if invalid tarjoaja OIDs" in {
    failsValidation(
      amm.copy(tarjoajat = List(OrganisaatioOid("1.2.3"), OrganisaatioOid("4.5.6"))),
      Seq(
        ValidationError("tarjoajat[0]", validationMsg("1.2.3")),
        ValidationError("tarjoajat[1]", validationMsg("4.5.6"))
      )
    )
  }

  it should "fail if unknown tarjoaja OIDs" in {
    failsValidation(
      amm.copy(tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid)),
      Seq(
        ValidationError("tarjoajat[1]", unknownTarjoajaOid(UnknownOid)),
        ValidationError("tarjoajat[2]", unknownTarjoajaOid(LonelyOid))
      )
    )
  }

  it should "fail if organisaatio-service not working when checking tarjoajat" in {
    failsValidation(amm.copy(tarjoajat = List(LonelyOid, UnknownOid)), "tarjoajat", organisaatioServiceFailureMsg)
  }

  private def failSorakuvausValidation(koulutus: Koulutus): Assertion =
    failsValidation(koulutus.copy(sorakuvausId = Some(sorakuvausId)), "sorakuvausId", notMissingMsg(Some(sorakuvausId)))

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
      amm.copy(sorakuvausId = Some(sorakuvausId2)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId2)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus is not yet julkaistu" in {
    failsValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId3)),
      "tila",
      notYetJulkaistu("Sorakuvausta", sorakuvausId3)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    failsValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId4)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId4)
    )
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    failsValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId5)),
      "koulutustyyppi",
      tyyppiMismatch("sorakuvauksen", sorakuvausId5)
    )
  }

  it should "fail to store koulutus if koulutusKoodit doesn't match sorakuvaus koulutuskoodit" in {
    failsValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId6)),
      "koulutuksetKoodiUri",
      valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
    )
  }

  def failTutkintoonjohtavuusValidation(koulutus: Koulutus, johtaaTutkintoon: Boolean): Unit =
    failsValidation(
      koulutus.copy(johtaaTutkintoon = johtaaTutkintoon),
      "johtaaTutkintoon",
      invalidTutkintoonjohtavuus(koulutus.koulutustyyppi.toString)
    )

  it should "fail if wrong tutkintoonJohtavuus" in {
    failTutkintoonjohtavuusValidation(amm, false)
    failTutkintoonjohtavuusValidation(yo, false)
    failTutkintoonjohtavuusValidation(ammTk, true)
    failTutkintoonjohtavuusValidation(ammOa, true)
  }

  it should "fail if invalid teemakuva" in {
    failsValidation(amm.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
  }

  it should "fail if invalid lisätieto" in {
    failsValidation(
      ammWithLisatietoParams(koodiUri = "koulutuksenlisatiedot_04#1"),
      "metadata.lisatiedot[0].otsikkoKoodiUri",
      invalidLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_04#1")
    )
  }

  it should "fail if lisätieto given for koulutustyyppi not accepting lisätiedot" in {
    failsValidation(
      AmmMuuKoulutus.copy(metadata =
        Some(
          AmmMuuKoulutus.metadata.get
            .asInstanceOf[AmmatillinenMuuKoulutusMetadata]
            .copy(
              lisatiedot = Seq(Lisatieto1)
            )
        )
      ),
      "metadata.lisatiedot",
      notEmptyMsg
    )
  }

  it should "fail if metadata missing from julkaistu koulutus" in {
    failsValidation(amm.copy(metadata = None), "metadata", missingMsg)
  }

  it should "fail if the tyyppi of the metadata differs from the tyyppi of the koulutus" in {
    failsValidation(
      amm.copy(metadata = Some(AmmatillinenKoulutusMetadata(tyyppi = Yo))),
      "metadata.tyyppi",
      InvalidMetadataTyyppi
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with optional kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    passesValidation(amm.copy(metadata = Some(AmmatillinenKoulutusMetadata())))
    failsValidation(
      amm.copy(metadata = Some(AmmatillinenKoulutusMetadata(kuvaus = invalidKuvaus))),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with mandatory kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    failsValidation(
      TuvaKoulutus.copy(metadata =
        Some(TuvaKoulutusMetadata(opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1")))
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Fi, Sv))
    )
    failsValidation(
      TuvaKoulutus.copy(metadata =
        Some(TuvaKoulutusMetadata(opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1"), kuvaus = invalidKuvaus))
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  private def failValidationWithKoulutuksetKoodiUri(koulutus: Koulutus): Assertion = {
    val urit = Seq("koulutus_371101#1")
    failsValidation(koulutus.copy(koulutuksetKoodiUri = urit), "koulutuksetKoodiUri", notEmptyMsg)
  }

  it should "fail if koulutuksetKoodiUri given for koulutustyyppi not using koulutusKoodit" in {
    failValidationWithKoulutuksetKoodiUri(ammTk)
    failValidationWithKoulutuksetKoodiUri(AmmMuuKoulutus)
    failValidationWithKoulutuksetKoodiUri(TuvaKoulutus)
    failValidationWithKoulutuksetKoodiUri(TelmaKoulutus)
    failValidationWithKoulutuksetKoodiUri(VapaaSivistystyoOpistovuosiKoulutus)
    failValidationWithKoulutuksetKoodiUri(VapaaSivistystyoMuuKoulutus)
    failValidationWithKoulutuksetKoodiUri(KkOpintojaksoKoulutus)
  }

  private def failValidationWithePerusteId(koulutus: Koulutus): Assertion = {
    failsValidation(koulutus.copy(ePerusteId = Some(11L)), "ePerusteId", notMissingMsg(Some(11L)))
  }

  it should "fail if ePerusteId given for koulutustyyppi not using ePeruste" in {
    failValidationWithePerusteId(ammTk)
    failValidationWithePerusteId(yo)
    failValidationWithePerusteId(amk)
    failValidationWithePerusteId(AmmOpettajaKoulutus)
    failValidationWithePerusteId(LukioKoulutus)
    failValidationWithePerusteId(AmmMuuKoulutus)
    failValidationWithePerusteId(TuvaKoulutus)
    failValidationWithePerusteId(TelmaKoulutus)
    failValidationWithePerusteId(VapaaSivistystyoOpistovuosiKoulutus)
    failValidationWithePerusteId(VapaaSivistystyoMuuKoulutus)
    failValidationWithePerusteId(AikuistenPerusopetusKoulutus)
    failValidationWithePerusteId(KkOpintojaksoKoulutus)
    failValidationWithePerusteId(KkOpintokokonaisuusKoulutus)
  }

  it should "fail if invalid koulutusKoodiUris for ammatillinen koulutus" in {
    failsValidation(amm.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsValidation(
      ammOa.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1", "koulutus_000001#1")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
    failsValidation(
      amm.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      Seq(
        ValidationError("koulutuksetKoodiUri[0]", invalidKoulutuskoodiuri("koulutus_000000#1")),
        ValidationError("ePerusteId", invalidEPerusteIdForKoulutusKoodiUri(11L, "koulutus_000000#1"))
      )
    )
  }

  it should "fail if invalid ePeruste for ammatillinen koulutus" in {
    failsValidation(ammOa.copy(ePerusteId = None), "ePerusteId", missingMsg)
    failsValidation(ammOa.copy(ePerusteId = Some(-11L)), "ePerusteId", notNegativeMsg)
    failsValidation(amm.copy(ePerusteId = Some(111L)), "ePerusteId", invalidEPerusteId(111L))
  }

  it should "fail if ePeruste-service query failed for ammatillinen koulutus" in {
    failsValidation(amm.copy(ePerusteId = Some(66L)), "ePerusteId", ePerusteServiceFailureMsg)
    failsValidation(ammOa.copy(ePerusteId = Some(66L)), "ePerusteId", ePerusteServiceFailureMsg)
  }

  it should "fail if invalid tutkinnonosat for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTk.copy(metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata())),
      "metadata.tutkinnonOsat",
      missingMsg
    )
    failsValidation(
      ammTkWithTutkinnonOsaParams(koulutusKoodiUri = Some("puppu"), ePerusteId = Some(123L), tutkinnonOsaId = Some(122L), tutkinnonOsaViite = Some(1234L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      invalidEPerusteIdForKoulutusKoodiUri(123L, "puppu")
    )
    failsValidation(
      ammTkWithTutkinnonOsaParams().copy(tila = Julkaistu),
      Seq(
        ValidationError("metadata.tutkinnonOsat[0].ePerusteId", missingMsg),
        ValidationError("metadata.tutkinnonOsat[0].koulutusKoodiUri", missingMsg),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaId", missingMsg),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaViite", missingMsg)
      )
    )
  }

  it should "fail if invalid ePeruste for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failsValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L), tutkinnonOsaId = Some(1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failsValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(111L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      invalidEPerusteId(111L)
    )
  }

  it should "fail if ePeruste-service query failed for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(66L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      ePerusteServiceFailureMsg
    )
  }

  it should "fail if unknown tutkinnonOsa viite- and ID for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(200L), None, Some(1234L), Some(2345L)),
      Seq(
        ValidationError(
          "metadata.tutkinnonOsat[0].tutkinnonosaViite",
          invalidTutkinnonOsaViiteForEPeruste(200L, 2345L)
        ),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaId", invalidTutkinnonOsaIdForEPeruste(200L, 1234L))
      )
    )
  }

  it should "fail if tutkinnonosaID did not match to tutkinnonosa found by ePeruste and viite for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, Some(1235L), Some(122L)),
      "metadata.tutkinnonOsat[0].tutkinnonosaId",
      invalidTutkinnonOsaIdForEPeruste(123L, 1235L)
    )
  }

  it should "fail if unknown tutkinnonosaViite for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, None, Some(130L)),
      "metadata.tutkinnonOsat[0].tutkinnonosaViite",
      invalidTutkinnonOsaViiteForEPeruste(123L, 130L)
    )
  }

  it should "fail if unknown tutkinnonosaID for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, Some(1236L), None),
      "metadata.tutkinnonOsat[0].tutkinnonosaId",
      invalidTutkinnonOsaIdForEPeruste(123L, 1236L)
    )
  }

  it should "fail if invalid osaamisalat for AmmOsaamisala koulutus" in {
    failsValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata())),
      "metadata.osaamisalaKoodiUri",
      missingMsg
    )
    failsValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata(osaamisalaKoodiUri = Some("puppu")))),
      "metadata.osaamisalaKoodiUri",
      invalidOsaamisalaForEPeruste(11L, "puppu")
    )
    failsValidation(
      ammOa.copy(ePerusteId = Some(123L)),
      "metadata.osaamisalaKoodiUri",
      invalidOsaamisalaForEPeruste(123L, "osaamisala_01")
    )
  }

  it should "fail if invalid koulutusalaKoodiUri for AmmMuu koulutus" in {
    failsValidation(
      ammMuuKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if invalid opintojenlaajuus for AmmMuu koulutus" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failsValidation(
      AmmMuuKoulutus.copy(metadata = Some(AmmatillinenMuuKoulutusMetadata(kuvaus = kuvaus))),
      Seq(
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg)
      )
    )
    failsValidation(
      ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "opintojenlaajuusyksikko_66#1"),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
    )
    failsValidation(
      ammMuuKoulutusWithParameters(opintojenLaajuusNumero = Some(-1)),
      "metadata.opintojenLaajuusNumero",
      notNegativeMsg
    )
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

  it should "fail if invalid opintojenlaajuusKoodiUri for Yo koulutus" in {
    failsValidation(
      yoKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "metadata.opintojenLaajuusKoodiUri",
      invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")
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

  it should "fail if unknown opintojenlaajuusKoodiUri for Amk koulutus" in {
    failsValidation(
      amkKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "metadata.opintojenLaajuusKoodiUri",
      invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")
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
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01"),
            opintojenLaajuusKoodiUri = Some("puppu")
          )
        )
      ),
      Seq(
        ValidationError("metadata.tutkintonimikeKoodiUrit", notEmptyMsg),
        ValidationError(
          "metadata.opintojenLaajuusKoodiUri",
          illegalValueForFixedValueMsg("opintojenlaajuus_60#<versionumero>, esim. opintojenlaajuus_60#1")
        ),
        ValidationError(
          "metadata.koulutusalaKoodiUrit",
          illegalValueForFixedValueSeqMsg(
            "kansallinenkoulutusluokitus2016koulutusalataso1_01#<versionumero>, esim. kansallinenkoulutusluokitus2016koulutusalataso1_01#1"
          )
        )
      )
    )
  }

  it should "fail if invalid Opettajien Pedagogiset Opinnot -koulutus" in {
    failsValidation(YoOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1"), metadata = Some(OpePedagOpinnotKoulutusMetadata(
      tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
      koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01"),
      opintojenLaajuusKoodiUri = Some("puppu")
    ))), Seq(
      ValidationError("koulutuksetKoodiUri", illegalValueForFixedValueSeqMsg(koodiUriTipText("koulutus_919999"))),
      ValidationError("metadata.tutkintonimikeKoodiUrit", notEmptyMsg),
      ValidationError(
        "metadata.opintojenLaajuusKoodiUri",
        illegalValueForFixedValueMsg(koodiUriTipText("opintojenlaajuus_60"))
      ),
      ValidationError(
        "metadata.koulutusalaKoodiUrit",
        illegalValueForFixedValueSeqMsg(koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso1_01"))
      )
    ))
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
            opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#70"),
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalatasoXX_01")
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
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70"))
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
            linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv"),
            opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#70")
          )
        )
      ),
      Seq(
        ValidationError("metadata.lisatiedot", notEmptyMsg),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv")),
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70"))
      )
    )
  }

  it should "fail if invalid metadata for julkaistu Tuva koulutus" in {
    failsValidation(
      TuvaKoulutus.copy(metadata =
        Some(TuvaKoulutusMetadata(linkkiEPerusteisiin = Map(Fi -> "http://www.vain.suomeksi.fi")))
      ),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.opintojenLaajuusKoodiUri", missingMsg),
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
            opintojenLaajuusKoodiUri = Some("puppu")
          )
        )
      ),
      Seq(
        ValidationError("metadata.lisatiedot", notEmptyMsg),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("puppu")),
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
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
            opintojenLaajuusKoodiUri = Some("puppu"),
            linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv")
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
        ),
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv"))
      )
    )
  }

  it should "fail if invalid metadata for julkaistu vapaa sivistystyo opistovuosi koulutus" in {
    failsValidation(
      VapaaSivistystyoOpistovuosiKoulutus.copy(
        metadata = Some(
          VapaaSivistystyoOpistovuosiKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("puppu"),
            opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#70"),
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
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
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
      kkOpintoJaksoWithParams(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
        opintojenLaajuusNumero = Some(-1),
        koulutusalaKoodiUrit = Seq("puppu")
      ).copy(oid = None),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          invalidKoulutusAlaKoodiuri("puppu")
        ),
        ValidationError(
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
        ),
        ValidationError("metadata.opintojenLaajuusNumero", notNegativeMsg)
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

  val ammWithOid = AmmKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")))
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
            koulutusalaKoodiUrit = Seq("puppu")
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
          invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
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
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
      )
    )
  }

  it should "fail if julkaistu kk-opintokokonaisuus koulutus has at least one julkaistu toteutus whose opintojenlaajuusNumero is not in the range specified in koulutus" in {

    val opintokokonaisuusKoulutus = kkOpintokokonaisuusKoulutus
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

  it should "fail if julkaistu kk-opintokokonaisuus koulutus has at least one julkaistu toteutus whose opintojenlaajuusyksikko is not the same as in koulutus" in {

    val opintokokonaisuusKoulutus = kkOpintokokonaisuusKoulutus
    val opintokokonaisuusToteutusOid = randomToteutusOid

    when(toteutusDao.getByKoulutusOid(opintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(opintokokonaisuusToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_5#1"),
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
          invalidKoulutusOpintojenLaajuusyksikkoIntegrity("opintojenlaajuusyksikko_6#1", Seq(opintokokonaisuusToteutusOid)),
        )
      )
    )
  }

  it should "pass when julkaistu kk-opintokokonaisuus koulutus doesn't have laajuus data and has two toteutus with different laajuusyksikko and laajuusNumero" in {
    val opintokokonaisuusKoulutus = kkOpintokokonaisuusKoulutus.copy(
      metadata = Some(
        KkOpintokokonaisuusKoulutuksenMetatieto.copy(
          opintojenLaajuusyksikkoKoodiUri = None,
          opintojenLaajuusNumeroMin = None,
          opintojenLaajuusNumeroMax = None
        )
      ),
    )

    when(toteutusDao.getByKoulutusOid(opintokokonaisuusKoulutus.oid.get, TilaFilter.onlyJulkaistut())).thenAnswer(
      Seq(
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
              opintojenLaajuusNumero = Some(20)
            )
          )
        ),
        JulkaistuKkOpintokokonaisuusToteutus.copy(
          oid = Some(randomToteutusOid),
          koulutusOid = opintokokonaisuusKoulutus.oid.get,
          metadata = Some(
            KkOpintokokonaisuusToteutuksenMetatieto.copy(
              opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_5#1"),
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

  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(ammWithOid, AmmKoulutus.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(ammWithOid.copy(tila = Arkistoitu), AmmKoulutus)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(ammWithOid, AmmKoulutus.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(ammWithOid.copy(tila = Tallennettu), AmmKoulutus)
  }

  it should "succeed from tallennettu to poistettu when no existing toteutukset for koulutus" in {
    passesValidation(
      AmmKoulutus.copy(oid = Some(koulutusOid2), tila = Poistettu),
      AmmKoulutus.copy(tila = Tallennettu)
    )
  }

  it should "fail from tallennettu to arkistoitu" in {
    failsStageChangeValidation(
      ammWithOid.copy(tila = Arkistoitu),
      AmmKoulutus.copy(tila = Tallennettu),
      illegalStateChange("koulutukselle", Tallennettu, Arkistoitu)
    )
  }

  it should "fail from arkistoitu to tallennettu" in {
    failsStageChangeValidation(
      ammWithOid.copy(tila = Tallennettu),
      AmmKoulutus.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Tallennettu)
    )
  }

  it should "fail from julkaistu to poistettu" in {
    failsStageChangeValidation(
      ammWithOid.copy(tila = Poistettu),
      AmmKoulutus.copy(tila = Julkaistu),
      illegalStateChange("koulutukselle", Julkaistu, Poistettu)
    )
  }

  it should "fail from arkistoitu to poistettu" in {
    failsStageChangeValidation(
      ammWithOid.copy(tila = Poistettu),
      AmmKoulutus.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Poistettu)
    )
  }

  it should "fail from poistettu to tallennettu" in {
    failsStageChangeValidation(
      ammWithOid.copy(tila = Tallennettu),
      AmmKoulutus.copy(tila = Poistettu),
      illegalStateChange("koulutukselle", Poistettu, Tallennettu)
    )
  }

  it should "fail from tallennettu to poistettu when existing toteutukset for koulutus" in {
    failsStageChangeValidation(
      ammWithOid.copy(oid = Some(koulutusOid), tila = Poistettu),
      AmmKoulutus.copy(tila = Tallennettu),
      integrityViolationMsg("Koulutusta", "toteutuksia")
    )
  }
}
