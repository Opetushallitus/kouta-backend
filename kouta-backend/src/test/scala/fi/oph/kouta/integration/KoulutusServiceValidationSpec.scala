package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodiUri, KoulutusKoodiClient}
import fi.oph.kouta.domain.oid.{HakukohdeOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{KoulutusServiceValidation, OrganisaatioService}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._
import org.scalatest.Assertion

import java.util.UUID

class KoulutusServiceValidationSpec extends BaseValidationSpec[Koulutus] {
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
      koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(tyypit, koodiUri)
    ).thenAnswer(true)

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(
      organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, EvilGrandChildOid, EvilCousin))
    ).thenAnswer(Set[OrganisaatioOid]())
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, UnknownOid, LonelyOid)))
      .thenAnswer(Set(UnknownOid, LonelyOid))
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1")).thenAnswer(true)
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_04#1")).thenAnswer(false)

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
      .thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"))
      .thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_080#1"))
      .thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#1"))
      .thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_v53#1")).thenAnswer(true)
    when(koulutusKoodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110#2")).thenAnswer(true)
    // ammatilliset
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#12")
    acceptKoulutusKoodiUri(ammatillisetKoulutustyypit, "koulutus_371101#12")
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(11L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(123L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(111L)).thenAnswer(Seq[KoodiUri]())
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(200L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPeruste(11L))
      .thenAnswer(Seq(koodiUriFromString("osaamisala_01")))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPeruste(123L)).thenAnswer(Seq[KoodiUri]())
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L))
      .thenAnswer(Seq((122L, 1234L), (123L, 1235L)))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(200L)).thenAnswer(Seq[(Long, Long)]())
    // korkeakoulu
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_201000#1")
    acceptKoulutusKoodiUri(yoKoulutustyypit, "koulutus_201111#1")
    acceptKoulutusKoodiUri(amkKoulutustyypit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(amkKoulutustyypit, "koulutus_201000#1")
    when(koulutusKoodiClient.koulutusKoodiUriExists(ammOpeErityisopeJaOpoKoulutusKoodiUrit, "koulutus_000002#12"))
      .thenAnswer(true)
    // lukio
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301101#1")).thenAnswer(true)
    // erikoislaakari
    when(koulutusKoodiClient.koulutusKoodiUriExists(erikoislaakariKoulutusKoodiUrit, "koulutus_775101#1"))
      .thenAnswer(true)
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
    passValidation(amm.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when incomplete luonnos" in {
    passValidation(min)
  }

  it should "succeed when koulutuskoodiUri has higher version than the matching koodiUri in ePeruste" in {
    passValidation(amm.copy(koulutuksetKoodiUri = Seq("koulutus_371101#12")))
  }

  it should "succeed when new valid AmmOsaamisala koulutus" in {
    passValidation(ammOa.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when new valid AmmTutkinnonOsa koulutus" in {
    passValidation(ammTk.copy(sorakuvausId = Some(sorakuvausId7)))
  }

  it should "succeed when new valid AmmTutkinnonOsa without koulutusKoodiUri" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(124L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(124L)).thenAnswer(Seq((134L, 1345L)))
    passValidation(ammTkWithTutkinnonOsaParams(Some(124L), None, Some(1345L), Some(134L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with koulutusKoodiUri only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(125L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_123456")))
    passValidation(ammTkWithTutkinnonOsaParams(Some(125L), Some("koulutus_123456#1"), None, None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(126L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(126L)).thenAnswer(Seq((11111L, 1346L)))
    passValidation(ammTkWithTutkinnonOsaParams(Some(126L), None, Some(1346L), None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaViite only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(127L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(127L)).thenAnswer(Seq((135L, 111111L)))
    passValidation(ammTkWithTutkinnonOsaParams(Some(127L), None, None, Some(135L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with ePerusteId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(128L))
      .thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    passValidation(ammTkWithTutkinnonOsaParams(Some(128L), None, None, None))
  }

  it should "succeed when new incomplete luonnos AmmTutkinnonOsa" in {
    passValidation(
      ammTk.copy(
        tila = Tallennettu,
        metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata())
      )
    )
    passValidation(
      ammTkWithTutkinnonOsaParams(None, Some("koulutus_371101#1"), Some(5L), Some(6L)).copy(
        tila = Tallennettu
      )
    )
  }

  it should "succeed when new incomplete luonnos AmmOsaamisala" in {
    passValidation(
      ammOa.copy(
        tila = Tallennettu,
        metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata())
      )
    )
    passValidation(
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
    passValidation(AmmMuuKoulutus.copy(tila = Tallennettu, metadata = Some(AmmatillinenMuuKoulutusMetadata())))
  }

  it should "succeed when new valid Yo koulutus" in {
    passValidation(yoKoulutusWithParameters())
  }

  it should "succeed when new incomplete luonnos Yo koulutus" in {
    passValidation(yo.copy(tila = Tallennettu, metadata = Some(YliopistoKoulutusMetadata())))
  }

  it should "succeed when new valid Yo koulutus and just one common koulutusKoodiUri with sorakuvaus" in {
    passValidation(yoKoulutusWithParameters().copy(koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201111#1")))
  }

  it should "succeed when new valid Amk koulutus" in {
    passValidation(amk)
  }

  it should "succeed when new valid AmmOpeErityisopeJaOpo koulutus" in {
    passValidation(AmmOpettajaKoulutus)
  }

  it should "succeed when new valid lukiokoulutus" in {
    passValidation(LukioKoulutus)
  }

  it should "succeed when new valid Tuva koulutus" in {
    passValidation(TuvaKoulutus)
  }

  it should "succeed when new incomplete luonnos Tuva koulutus" in {
    passValidation(TuvaKoulutus.copy(tila = Tallennettu, metadata = Some(TuvaKoulutusMetadata())))
  }

  it should "succeed when new valid Telma koulutus" in {
    passValidation(TelmaKoulutus)
  }

  it should "succeed when new valid Vapaa sivistystyö opistovuosi koulutus" in {
    passValidation(VapaaSivistystyoOpistovuosiKoulutus)
  }

  it should "succeed when new valid Vapaa sivistystyö muu koulutus" in {
    passValidation(VapaaSivistystyoMuuKoulutus)
  }

  it should "succeed when new incomplete luonnos Vapaa sivistystyö muu koulutus" in {
    passValidation(
      VapaaSivistystyoMuuKoulutus.copy(tila = Tallennettu, metadata = Some(VapaaSivistystyoMuuKoulutusMetadata()))
    )
  }

  it should "succeed when new valid Aikuisten perusopetus koulutus" in {
    passValidation(AikuistenPerusopetusKoulutus)
  }

  it should "succeed when new incomplete luonnos Aikuisten perusopetus koulutus" in {
    passValidation(
      AikuistenPerusopetusKoulutus.copy(tila = Tallennettu, metadata = Some(AikuistenPerusopetusKoulutusMetadata()))
    )
  }

  it should "succeed when new valid Kk-opintojakso koulutus" in {
    passValidation(KkOpintojaksoKoulutus)
  }

  it should "succeed when new incomplete luonnos Kk-opintojakso koulutus" in {
    passValidation(KkOpintojaksoKoulutus.copy(tila = Tallennettu, metadata = Some(KkOpintojaksoKoulutusMetadata())))
  }

  it should "Succeed when new valid Erikoislääkäri koulutus" in {
    passValidation(ErikoislaakariKoulutus)
  }

  it should "Fail with more than 1 koulutuksetKoodiUri" in {
    failValidation(
      ErikoislaakariKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_775101#1", "koulutus_775201#1")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
  }

  it should "Fail if unknown koulutusKoodiUri for Erikoislääkäri koulutus" in {
    failValidation(
      ErikoislaakariKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "fail if perustiedot is invalid" in {
    failValidation(
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

  it should "fail if invalid tarjoaja OIDs" in {
    failValidation(
      amm.copy(tarjoajat = List(OrganisaatioOid("1.2.3"), OrganisaatioOid("4.5.6"))),
      Seq(
        ValidationError("tarjoajat[0]", validationMsg("1.2.3")),
        ValidationError("tarjoajat[1]", validationMsg("4.5.6"))
      )
    )
  }

  it should "fail if unknown tarjoaja OIDs" in {
    failValidation(
      amm.copy(tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid)),
      Seq(
        ValidationError("tarjoajat[1]", unknownTarjoajaOid(UnknownOid)),
        ValidationError("tarjoajat[2]", unknownTarjoajaOid(LonelyOid))
      )
    )
  }

  it should "fail if invalid koulutuksetKoodiUri" in {
    failValidation(
      yo.copy(tila = Tallennettu, koulutuksetKoodiUri = Seq("puppua", "höttöä")),
      Seq(
        ValidationError("koulutuksetKoodiUri[0]", validationMsg("puppua")),
        ValidationError("koulutuksetKoodiUri[1]", validationMsg("höttöä"))
      )
    )
  }

  private def failSorakuvausValidation(koulutus: Koulutus): Assertion =
    failValidation(koulutus.copy(sorakuvausId = Some(sorakuvausId)), "sorakuvausId", notMissingMsg(Some(sorakuvausId)))

  it should "fail if sorakuvausId given for koulutustyyppi not accepting sorakuvaus" in {
    failSorakuvausValidation(AikuistenPerusopetusKoulutus)
    failSorakuvausValidation(AmmMuuKoulutus)
    failSorakuvausValidation(TuvaKoulutus)
    failSorakuvausValidation(TelmaKoulutus)
    failSorakuvausValidation(VapaaSivistystyoOpistovuosiKoulutus)
    failSorakuvausValidation(VapaaSivistystyoMuuKoulutus)
  }

  it should "fail if sorakuvaus doesn't exist" in {
    failValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId2)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId2)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus is not yet julkaistu" in {
    failValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId3)),
      "tila",
      notYetJulkaistu("Sorakuvausta", sorakuvausId3)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    failValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId4)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId4)
    )
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    failValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId5)),
      "koulutustyyppi",
      tyyppiMismatch("sorakuvauksen", sorakuvausId5)
    )
  }

  it should "fail to store koulutus if koulutusKoodit doesn't match sorakuvaus koulutuskoodit" in {
    failValidation(
      amm.copy(sorakuvausId = Some(sorakuvausId6)),
      "koulutuksetKoodiUri",
      valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
    )
  }

  def failTutkintoonjohtavuusValidation(koulutus: Koulutus, johtaaTutkintoon: Boolean): Unit =
    failValidation(
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
    failValidation(amm.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
  }

  it should "fail if invalid lisätieto" in {
    failValidation(
      ammWithLisatietoParams(koodiUri = "puppu"),
      "metadata.lisatiedot[0].otsikkoKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
      ammWithLisatietoParams(sv = ""),
      "metadata.lisatiedot[0].teksti",
      invalidKielistetty(Seq(Sv))
    )
    failValidation(
      ammWithLisatietoParams(koodiUri = "koulutuksenlisatiedot_04#1"),
      "metadata.lisatiedot[0].otsikkoKoodiUri",
      invalidLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_04#1")
    )
  }

  it should "fail if lisätieto given for koulutustyyppi not accepting lisätiedot" in {
    failValidation(
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
    failValidation(amm.copy(metadata = None), "metadata", missingMsg)
  }

  it should "fail if the tyyppi of the metadata differs from the tyyppi of the koulutus" in {
    failValidation(
      amm.copy(metadata = Some(AmmatillinenKoulutusMetadata(tyyppi = Yo))),
      "metadata.tyyppi",
      InvalidMetadataTyyppi
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with optional kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    passValidation(amm.copy(metadata = Some(AmmatillinenKoulutusMetadata())))
    failValidation(
      amm.copy(metadata = Some(AmmatillinenKoulutusMetadata(kuvaus = invalidKuvaus))),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  it should "fail if invalid metadata kuvaus for koulutustyyppi with mandatory kuvaus" in {
    val invalidKuvaus = Map(Fi -> "kuvaus vain suomeksi", Sv -> "")
    failValidation(
      TuvaKoulutus.copy(metadata =
        Some(TuvaKoulutusMetadata(opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1")))
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Fi, Sv))
    )
    failValidation(
      TuvaKoulutus.copy(metadata =
        Some(TuvaKoulutusMetadata(opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1"), kuvaus = invalidKuvaus))
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  private def failValidationWithKoulutuksetKoodiUri(koulutus: Koulutus): Assertion = {
    val urit = Seq("koulutus_371101#1")
    failValidation(koulutus.copy(koulutuksetKoodiUri = urit), "koulutuksetKoodiUri", notEmptyMsg)
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
    failValidation(koulutus.copy(ePerusteId = Some(11L)), "ePerusteId", notMissingMsg(Some(11L)))
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
  }

  it should "fail if invalid koulutusKoodiUris for ammatillinen koulutus" in {
    failValidation(amm.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failValidation(
      ammOa.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1", "koulutus_000001#1")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
    failValidation(
      amm.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      Seq(
        ValidationError("koulutuksetKoodiUri[0]", invalidKoulutuskoodiuri("koulutus_000000#1")),
        ValidationError("ePerusteId", invalidEPerusteIdForKoulutusKoodiUri(11L, "koulutus_000000#1"))
      )
    )
  }

  it should "fail if invalid ePeruste for ammatillinen koulutus" in {
    failValidation(ammOa.copy(ePerusteId = None), "ePerusteId", missingMsg)
    failValidation(ammOa.copy(ePerusteId = Some(-11L)), "ePerusteId", notNegativeMsg)
    failValidation(amm.copy(ePerusteId = Some(111L)), "ePerusteId", invalidEPerusteId(111L))
  }

  it should "fail if invalid tutkinnonosat for AmmTutkinnonosa koulutus" in {
    failValidation(
      ammTk.copy(metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata())),
      "metadata.tutkinnonOsat",
      missingMsg
    )
    failValidation(
      ammTkWithTutkinnonOsaParams(koulutusKoodiUri = Some("puppu")),
      "metadata.tutkinnonOsat[0].koulutusKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
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
    failValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L), tutkinnonOsaId = Some(1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(111L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      invalidEPerusteId(111L)
    )
  }

  it should "fail if unknown tutkinnonOsa viite- and ID for AmmTutkinnonosa koulutus" in {
    failValidation(
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
    failValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, Some(1235L), Some(122L)),
      "metadata.tutkinnonOsat[0].tutkinnonosaId",
      invalidTutkinnonOsaIdForEPeruste(123L, 1235L)
    )
  }

  it should "fail if unknown tutkinnonosaViite for AmmTutkinnonosa koulutus" in {
    failValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, None, Some(130L)),
      "metadata.tutkinnonOsat[0].tutkinnonosaViite",
      invalidTutkinnonOsaViiteForEPeruste(123L, 130L)
    )
  }

  it should "fail if unknown tutkinnonosaID for AmmTutkinnonosa koulutus" in {
    failValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), None, Some(1236L), None),
      "metadata.tutkinnonOsat[0].tutkinnonosaId",
      invalidTutkinnonOsaIdForEPeruste(123L, 1236L)
    )
  }

  it should "fail if invalid osaamisalat for AmmOsaamisala koulutus" in {
    failValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata())),
      "metadata.osaamisalaKoodiUri",
      missingMsg
    )
    failValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata(osaamisalaKoodiUri = Some("puppu")))),
      "metadata.osaamisalaKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
      ammOa.copy(ePerusteId = Some(123L)),
      "metadata.osaamisalaKoodiUri",
      invalidOsaamisalaForEPeruste(123L, "osaamisala_01")
    )
  }

  it should "fail if invalid koulutusalaKoodiUri for AmmMuu koulutus" in {
    failValidation(
      ammMuuKoulutusWithParameters("puppu"),
      "metadata.koulutusalaKoodiUrit[0]",
      validationMsg("puppu")
    )
    failValidation(
      ammMuuKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if invalid opintojenlaajuus for AmmMuu koulutus" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failValidation(
      AmmMuuKoulutus.copy(metadata = Some(AmmatillinenMuuKoulutusMetadata(kuvaus = kuvaus))),
      Seq(
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg)
      )
    )
    failValidation(
      ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "puppu"),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
      ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "opintojenlaajuusyksikko_66#1"),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
    )
    failValidation(
      ammMuuKoulutusWithParameters(opintojenLaajuusNumero = Some(-1)),
      "metadata.opintojenLaajuusNumero",
      notNegativeMsg
    )
  }

  it should "fail if invalid koulutusKoodiUri for Yo koulutus" in {
    failValidation(yo.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failValidation(
      yo.copy(sorakuvausId = None, koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_000000#1")
    )
  }

  it should "fail if unknown koulutusalaKoodiUri for Yo koulutus" in {
    failValidation(
      yoKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if invalid tutkintonimikeKoodiUri for Yo koulutus" in {
    failValidation(
      yoKoulutusWithParameters(tutkintonimikeKoodiUri = "puppu"),
      "metadata.tutkintonimikeKoodiUrit[0]",
      validationMsg("puppu")
    )
    failValidation(
      yoKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "metadata.tutkintonimikeKoodiUrit[0]",
      invalidTutkintoNimikeKoodiuri("tutkintonimikekk_110#70")
    )
  }

  it should "fail if invalid opintojenlaajuusKoodiUri for Yo koulutus" in {
    failValidation(
      yoKoulutusWithParameters(opintojenlaajuusKoodiUri = "puppu"),
      "metadata.opintojenLaajuusKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
      yoKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "metadata.opintojenLaajuusKoodiUri",
      invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")
    )
  }

  it should "fail if invalid koulutusKoodiUri for Amk koulutus" in {
    failValidation(amk.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failValidation(
      amk.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_000000#1")
    )
  }

  it should "fail if unknown koulutusalaKoodiUri for Amk koulutus" in {
    failValidation(
      amkKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "metadata.koulutusalaKoodiUrit[0]",
      invalidKoulutusAlaKoodiuri("kansallinenkoulutusluokitus2016koulutusalataso1_70")
    )
  }

  it should "fail if unknown tutkintonimikeKoodiUri for Amk koulutus" in {
    failValidation(
      amkKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "metadata.tutkintonimikeKoodiUrit[0]",
      invalidTutkintoNimikeKoodiuri("tutkintonimikekk_110#70")
    )
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Amk koulutus" in {
    failValidation(
      amkKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "metadata.opintojenLaajuusKoodiUri",
      invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")
    )
  }

  it should "fail if invalid koulutuksetKoodiUri for AmmOpeErityisopeJaOpo koulutus" in {
    failValidation(AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failValidation(
      AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1", "koulutus_222222#2")),
      "koulutuksetKoodiUri",
      tooManyKoodiUris
    )
    failValidation(
      AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "fail if invalid metadata for AmmOpeErityisopeJaOpo koulutus" in {
    failValidation(
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

  it should "fail if invalid koulutusKoodiUri for Lukio koulutus" in {
    failValidation(LukioKoulutus.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failValidation(
      LukioKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri[0]",
      invalidKoulutuskoodiuri("koulutus_111111#1")
    )
  }

  it should "fail if unknown invalid metadata for Lukio koulutus" in {
    failValidation(
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
    failValidation(
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
    failValidation(
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
    failValidation(
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
        ValidationError("metadata.opintojenLaajuusKoodiUri", validationMsg("puppu")),
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos vapaa sivistystyo opistovuosi koulutus" in {
    failValidation(
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
        ValidationError("metadata.opintojenLaajuusKoodiUri", validationMsg("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.fi", invalidUrl("puppu")),
        ValidationError("metadata.linkkiEPerusteisiin.sv", invalidUrl("puppu sv"))
      )
    )
  }

  it should "fail if invalid metadata for julkaistu vapaa sivistystyo opistovuosi koulutus" in {
    failValidation(
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
          validationMsg("puppu")
        ),
        ValidationError("metadata.opintojenLaajuusKoodiUri", invalidOpintojenLaajuusKoodiuri("opintojenlaajuus_40#70")),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos vapaa sivistystyo muu koulutus" in {
    failValidation(
      VapaaSivistystyoMuuKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          VapaaSivistystyoMuuKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
            linkkiEPerusteisiin = Map(Fi -> "puppu", Sv -> "puppu sv"),
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
            opintojenLaajuusNumero = Some(-1)
          )
        )
      ),
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
    failValidation(
      VapaaSivistystyoMuuKoulutus.copy(
        metadata = Some(
          VapaaSivistystyoMuuKoulutusMetadata(
            koulutusalaKoodiUrit = Seq("puppu"),
            linkkiEPerusteisiin = Map(Fi -> "http://www.vain.suomeksi.fi")
          )
        )
      ),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          validationMsg("puppu")
        ),
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid koulutusKoodiUri for Aikuisten perusopetus -koulutus" in {
    failValidation(
      AikuistenPerusopetusKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "koulutuksetKoodiUri",
      illegalValueForFixedValueSeqMsg("koulutus_201101#<versionumero>, esim. koulutus_201101#1")
    )
  }

  it should "fail if invalid metadata for luonnos Aikuisten perusopetus -koulutus" in {
    failValidation(
      AikuistenPerusopetusKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          AikuistenPerusopetusKoulutusMetadata(
            lisatiedot = Seq(Lisatieto1),
            linkkiEPerusteisiin = Map(Fi -> "puppu"),
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
            opintojenLaajuusNumero = Some(-1)
          )
        )
      ),
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
    failValidation(
      AikuistenPerusopetusKoulutus.copy(
        metadata = Some(
          AikuistenPerusopetusKoulutusMetadata(
            linkkiEPerusteisiin = Map(Fi -> "http://vain.suomeksi.fi")
          )
        )
      ),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", missingMsg),
        ValidationError("metadata.opintojenLaajuusNumero", missingMsg),
        ValidationError("metadata.linkkiEPerusteisiin", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid metadata for luonnos Kk-opintojakso koulutus" in {
    failValidation(
      KkOpintojaksoKoulutus.copy(
        tila = Tallennettu,
        metadata = Some(
          KkOpintojaksoKoulutusMetadata(
            opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1"),
            opintojenLaajuusNumero = Some(-1),
            koulutusalaKoodiUrit = Seq("puppu")
          )
        )
      ),
      Seq(
        ValidationError(
          "metadata.koulutusalaKoodiUrit[0]",
          validationMsg("puppu")
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
    failValidation(
      KkOpintojaksoKoulutus.copy(metadata = Some(KkOpintojaksoKoulutusMetadata())),
      Seq(
        ValidationError("metadata.kuvaus", invalidKielistetty(Seq(Fi, Sv)))
      )
    )
  }

  val ammWithOid = AmmKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.00000000000000000123")))
  "State change" should "succeed from tallennettu to julkaistu" in {
    passValidation(ammWithOid, AmmKoulutus.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passValidation(ammWithOid.copy(tila = Arkistoitu), AmmKoulutus)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passValidation(ammWithOid, AmmKoulutus.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passValidation(ammWithOid.copy(tila = Tallennettu), AmmKoulutus)
  }

  it should "succeed from tallennettu to poistettu when no existing toteutukset for koulutus" in {
    passValidation(
      AmmKoulutus.copy(oid = Some(koulutusOid2), tila = Poistettu),
      AmmKoulutus.copy(tila = Tallennettu)
    )
  }

  it should "fail from tallennettu to arkistoitu" in {
    failStageChangeValidation(
      ammWithOid.copy(tila = Arkistoitu),
      AmmKoulutus.copy(tila = Tallennettu),
      illegalStateChange("koulutukselle", Tallennettu, Arkistoitu)
    )
  }

  it should "fail from arkistoitu to tallennettu" in {
    failStageChangeValidation(
      ammWithOid.copy(tila = Tallennettu),
      AmmKoulutus.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Tallennettu)
    )
  }

  it should "fail from julkaistu to poistettu" in {
    failStageChangeValidation(
      ammWithOid.copy(tila = Poistettu),
      AmmKoulutus.copy(tila = Julkaistu),
      illegalStateChange("koulutukselle", Julkaistu, Poistettu)
    )
  }

  it should "fail from arkistoitu to poistettu" in {
    failStageChangeValidation(
      ammWithOid.copy(tila = Poistettu),
      AmmKoulutus.copy(tila = Arkistoitu),
      illegalStateChange("koulutukselle", Arkistoitu, Poistettu)
    )
  }

  it should "fail from poistettu to tallennettu" in {
    failStageChangeValidation(
      ammWithOid.copy(tila = Tallennettu),
      AmmKoulutus.copy(tila = Poistettu),
      illegalStateChange("koulutukselle", Poistettu, Tallennettu)
    )
  }

  it should "fail from tallennettu to poistettu when existing toteutukset for koulutus" in {
    failStageChangeValidation(
      ammWithOid.copy(oid = Some(koulutusOid), tila = Poistettu),
      AmmKoulutus.copy(tila = Tallennettu),
      integrityViolationMsg("Koulutusta", "toteutuksia")
    )
  }
}
