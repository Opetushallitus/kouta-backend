package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{AikuistenPerusopetusKoulutus, AmkKoulutus, AmmKoulutus, AmmMuuKoulutus, AmmOpettajaKoulutus, AmmOsaamisalaKoulutus, AmmTutkinnonOsaKoulutus, JulkaistuAmmToteutus, LukioKoulutus, TelmaKoulutus, TuvaKoulutus, VapaaSivistystyoMuuKoulutus, VapaaSivistystyoOpistovuosiKoulutus, YoKoulutus}
import fi.oph.kouta.TestOids.{ChildOid, EvilChildOid, EvilCousin, EvilGrandChildOid, GrandChildOid, LonelyOid, OtherOid, UnknownOid}
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodiUri, KoulutusKoodiClient}
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{AikuistenPerusopetusKoulutusMetadata, Amk, Amm, AmmOpeErityisopeJaOpo, AmmatillinenKoulutusMetadata, AmmatillinenMuuKoulutusMetadata, AmmatillinenTutkinnonOsaKoulutusMetadata, AmmattikorkeakouluKoulutusMetadata, Arkistoitu, Fi, Julkaistu, KorkeakoulutusKoulutusMetadata, Koulutus, Lisatieto, LukioKoulutusMetadata, Poistettu, Sv, Tallennettu, TelmaKoulutusMetadata, TilaFilter, Toteutus, TutkinnonOsa, TuvaKoulutusMetadata, VapaaSivistystyoMuuKoulutusMetadata, VapaaSivistystyoOpistovuosiKoulutusMetadata, YliopistoKoulutusMetadata, Yo}
import fi.oph.kouta.repository.{KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{KoulutusServiceValidation, KoutaValidationException, OrganisaatioService}
import fi.oph.kouta.validation.{amkKoulutustyypit, ammOpeErityisopeJaOpoKoulutusKoodiUrit, ammatillisetKoulutustyypit, lukioKoulutusKoodiUrit, yoKoulutustyypit}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec

import java.util.UUID
import scala.util.{Failure, Success, Try}

class KoulutusServiceValidationSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val koulutusKoodiClient = mock[KoulutusKoodiClient]
  val ePerusteKoodiClient = mock[EPerusteKoodiClient]
  var organisaatioService = mock[OrganisaatioService]
  val toteutusDao = mock[ToteutusDAO]
  val sorakuvausDao = mock[SorakuvausDAO]

  val sorakuvausId = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()
  val sorakuvausId3 = UUID.randomUUID()
  val sorakuvausId4 = UUID.randomUUID()
  val sorakuvausId5 = UUID.randomUUID()
  val sorakuvausId6 = UUID.randomUUID()
  val sorakuvausId7 = UUID.randomUUID()
  val koulutusOid = KoulutusOid("1.2.246.562.13.00000000000000000123")
  val koulutusOid2 = KoulutusOid("1.2.246.562.13.00000000000000000124")

  private def yoKoulutusWithParameters
  (
    koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
    tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
    opintojenlaajuusKoodiUri: String = "opintojenlaajuus_40#1"
  ) = YoKoulutus.copy(
    sorakuvausId = Some(sorakuvausId5), metadata =
    Some(YoKoulutus.metadata.get.asInstanceOf[YliopistoKoulutusMetadata].copy(
      koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
      tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
      opintojenLaajuusKoodiUri = Some(opintojenlaajuusKoodiUri))))

  private def amkKoulutusWithParameters
  (
    koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_020#1",
    tutkintonimikeKoodiUri: String = "tutkintonimikekk_110#2",
    opintojenlaajuusKoodiUri: String = "opintojenlaajuus_40#1"
  ) = AmkKoulutus.copy(metadata =
    Some(AmkKoulutus.metadata.get.asInstanceOf[AmmattikorkeakouluKoulutusMetadata].copy (
      koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
      tutkintonimikeKoodiUrit = Seq(tutkintonimikeKoodiUri),
      opintojenLaajuusKoodiUri = Some(opintojenlaajuusKoodiUri))))

  private def ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(
    ePerusteId: Option[Long],
    koulutusKoodiUri: Option[String],
    tutkinnonOsaId: Option[Long],
    tutkinnonOsaViite: Option[Long]) =
  AmmTutkinnonOsaKoulutus.copy(
    tila = Tallennettu,
    metadata =
      Some(AmmTutkinnonOsaKoulutus.metadata.get.asInstanceOf[AmmatillinenTutkinnonOsaKoulutusMetadata].copy(
        tutkinnonOsat = Seq(TutkinnonOsa(ePerusteId, koulutusKoodiUri, tutkinnonOsaId, tutkinnonOsaViite))))
  )

  private def ammMuuKoulutusWithParameters(
    koulutusalaKoodiUri: String,
    opintojenlaajusyksikkoKoodiUri: String): Koulutus =
  AmmMuuKoulutus.copy(metadata =
    Some(AmmMuuKoulutus.metadata.get.asInstanceOf[AmmatillinenMuuKoulutusMetadata].copy(
      koulutusalaKoodiUrit = Seq(koulutusalaKoodiUri),
      opintojenLaajuusyksikkoKoodiUri = Some(opintojenlaajusyksikkoKoodiUri))))


  var validator =
    new KoulutusServiceValidation(koulutusKoodiClient, ePerusteKoodiClient, organisaatioService, toteutusDao, sorakuvausDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, EvilGrandChildOid, EvilCousin))).thenAnswer(Set[OrganisaatioOid]())
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(GrandChildOid, UnknownOid, LonelyOid))).thenAnswer(Set(UnknownOid, LonelyOid))
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1")).thenAnswer(true)
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_04#1")).thenAnswer(false)

    // sorakuvaukset
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId)).thenAnswer((Some(Julkaistu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId2)).thenAnswer((None, None, None))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId3)).thenAnswer((Some(Tallennettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId4)).thenAnswer((Some(Poistettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId5)).thenAnswer((Some(Julkaistu), Some(Yo), Some(Seq("koulutus_371101#1", "koulutus_201000#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId6)).thenAnswer((Some(Julkaistu), Some(Amm), Some(Seq("koulutus_000001#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId7)).thenAnswer((Some(Julkaistu), Some(Amm), None))

    // yleiset metadatat
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso1_01")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso1_001#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_080#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60#1")).thenAnswer(true)
    when(koulutusKoodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_v53#1")).thenAnswer(true)
    when(koulutusKoodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110#2")).thenAnswer(true)
    // ammatilliset
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371101#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371101#12")).thenAnswer(true)
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(11L)).thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(123L)).thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(111L)).thenAnswer(Seq[KoodiUri]())
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(200L)).thenAnswer(Seq(koodiUriFromString("koulutus_371101")))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPeruste(11L)).thenAnswer(Seq(koodiUriFromString("osaamisala_01")))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPeruste(123L)).thenAnswer(Seq[KoodiUri]())
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L)).thenAnswer(Seq((122L, 1234L), (123L, 1235L)))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(200L)).thenAnswer(Seq[(Long, Long)]())
    // korkeakoulu
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_371101#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_201000#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_201111#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(amkKoulutustyypit, "koulutus_371101#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExists(amkKoulutustyypit, "koulutus_201000#1")).thenAnswer(true)
    when(koulutusKoodiClient.koulutusKoodiUriExists(ammOpeErityisopeJaOpoKoulutusKoodiUrit, "koulutus_000002#12")).thenAnswer(true)
    // lukio
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301101#1")).thenAnswer(true)
    // toteutukset
    when(toteutusDao.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq(
      JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
      JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000124")))
    ))
    when(toteutusDao.getByKoulutusOid(koulutusOid2, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[Toteutus]())
  }

  "Validation" should "succeed when new valid koulutus" in {
    validator.withValidation(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId)), None)(koulutus => koulutus)
  }

  it should "succeed when koulutuskoodiUri has higher version than the matching koodiUri in ePeruste" in {
    validator.withValidation(AmmKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_371101#12")), None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmOsaamisala koulutus" in {
    validator.withValidation(AmmOsaamisalaKoulutus.copy(sorakuvausId = Some(sorakuvausId)), None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa koulutus" in {
    validator.withValidation(AmmTutkinnonOsaKoulutus.copy(sorakuvausId = Some(sorakuvausId7)), None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa without koulutusKoodiUri" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(124L)).thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(124L)).thenAnswer(Seq((134L, 1345L)))
    validator.withValidation(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(124L), None, Some(1345L), Some(134L)),
      None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa with koulutusKoodiUri only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(125L)).thenAnswer(Seq(koodiUriFromString("koulutus_123456")))
    validator.withValidation(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(125L), Some("koulutus_123456#1"), None, None),
      None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(126L)).thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(126L)).thenAnswer(Seq((11111L, 1346L)))
    validator.withValidation(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(126L), None, Some(1346L), None),
      None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaViite only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(127L)).thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    when(ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(127L)).thenAnswer(Seq((135L, 111111L)))
    validator.withValidation(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(127L), None, None, Some(135L)),
      None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmTutkinnonOsa with ePerusteId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(128L)).thenAnswer(Seq(koodiUriFromString("koulutus_000000")))
    validator.withValidation(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(128L), None, None, None),
      None)(koulutus => koulutus)
  }

  it should "succeed when new julkaisematon AmmTutkinnonOsa without tutkinnonosat" in {
    validator.withValidation(
      AmmTutkinnonOsaKoulutus.copy(
        tila = Tallennettu,
        metadata =
          Some(AmmTutkinnonOsaKoulutus.metadata.get.asInstanceOf[AmmatillinenTutkinnonOsaKoulutusMetadata].copy(
            tutkinnonOsat = Seq[TutkinnonOsa]()))), None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmMuu koulutus" in {
    validator.withValidation(AmmMuuKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Yo koulutus" in {
    validator.withValidation(yoKoulutusWithParameters(), None)(koulutus => koulutus)
  }

  it should "succeed when new valid Yo koulutus and just one common koulutusKoodiUri with sorakuvaus" in {
    validator.withValidation(yoKoulutusWithParameters().copy(
      koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201111#1")), None)(koulutus => koulutus)
  }

  it should "succeed when new valid Amk koulutus" in {
    validator.withValidation(AmkKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid AmmOpeErityisopeJaOpo koulutus" in {
    validator.withValidation(AmmOpettajaKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid lukiokoulutus" in {
    validator.withValidation(LukioKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Tuva koulutus" in {
    validator.withValidation(TuvaKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Telma koulutus" in {
    validator.withValidation(TelmaKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Vapaa sivistystyö opistovuosi koulutus" in {
    validator.withValidation(VapaaSivistystyoOpistovuosiKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Vapaa sivistystyö muu koulutus" in {
    validator.withValidation(VapaaSivistystyoMuuKoulutus, None)(koulutus => koulutus)
  }

  it should "succeed when new valid Aikuisten perusopetus koulutus" in {
    validator.withValidation(AikuistenPerusopetusKoulutus, None)(koulutus => koulutus)
  }

  private def expectValidationErrors(koulutus: Koulutus, errorCodes: String*): Unit =
    expectValidationErrors(koulutus, None, errorCodes)

  private def expectValidationErrors(koulutus: Koulutus, oldKoulutus: Option[Koulutus], errorCodes: Seq[String]): Unit = {
    Try(validator.withValidation(koulutus, oldKoulutus)(koulutus => koulutus)) match {
      case Success(_) => throw new RuntimeException("Oletettuja validointivirheitä ei tunnistettu")
      case Failure(exp: KoutaValidationException) => {
        val expectedErrorCodes = errorCodes.toSet
        val actualErrorCodes = exp.errorMessages.map(_.errorType).toSet
        assert(expectedErrorCodes == actualErrorCodes)
      }
    }
  }

  it should "fail if unknown tarjoaja OIDs" in {
    expectValidationErrors(AmmKoulutus.copy(tarjoajat = List(GrandChildOid, UnknownOid, LonelyOid)), errorCodes = "unknownTajoajaOids")
  }
  it should "fail if unknown lisätieto-otsikkoUri" in {
    expectValidationErrors(AmmKoulutus.copy(metadata =
      Some(AmmKoulutus.metadata.get.asInstanceOf[AmmatillinenKoulutusMetadata].copy(
        lisatiedot = Seq(
          Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_04#1",
            teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv")))
        ))), "invalidLisatietoOtsikkoKoodiuri")
  }

  it should "fail if sorakuvaus doesn't exist" in {
    expectValidationErrors(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId2)), "nonExistent")
  }

  it should "fail to store julkaistu koulutus if sorakuvaus is not yet julkaistu" in {
    expectValidationErrors(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId3)), "notYetJulkaistu")
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    expectValidationErrors(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId4)), "nonExistent")
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    expectValidationErrors(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId5)), "tyyppiMismatch")
  }

  it should "fail to store koulutus if koulutusKoodit doesn't match sorakuvaus koulutuskoodit" in {
    expectValidationErrors(AmmKoulutus.copy(sorakuvausId = Some(sorakuvausId6)), "valuesDontMatch")
  }

  it should "fail if invalid koulutusKoodiUri for ammatillinen koulutus" in {
    expectValidationErrors(AmmKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "invalidKoulutuskoodiuri", "invalidEPerusteIdForKoulutus")
  }

  it should "fail if ePeruste not found for ammatillinen koulutus" in {
    expectValidationErrors(AmmKoulutus.copy(ePerusteId = Some(111L)), "invalidEPerusteId")
  }

  it should "fail if unknown ePeruste for AmmTutkinnonosa koulutus" in {
    expectValidationErrors(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(111L), None, None, None), "invalidEPerusteId")
  }

  it should "fail if unknown tutkinnonOsa viite- and ID for AmmTutkinnonosa koulutus" in {
    expectValidationErrors(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(200L), None, Some(1234L), Some(2345L)),
      "invalidTukinnonosaViiteForEPeruste", "invalidTukinnonosaIdForEPeruste")
  }

  it should "fail if tutkinnonosaID did not match to tutkinnonosa found by ePeruste and viite for AmmTutkinnonosa koulutus" in {
    expectValidationErrors(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(123L), None, Some(1235L), Some(122L)),
      "invalidTukinnonosaIdForEPeruste")
  }

  it should "fail if unknown tutkinnonosaViite for AmmTutkinnonosa koulutus" in {
    expectValidationErrors(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(123L), None, None, Some(130L)),
      "invalidTukinnonosaViiteForEPeruste")
  }

  it should "fail if unknown tutkinnonosaID for AmmTutkinnonosa koulutus" in {
    expectValidationErrors(
      ammTutkinnonOsaKoulutusWithTutkinnonOsaParameters(Some(123L), None, Some(1236L), None),
      "invalidTukinnonosaIdForEPeruste")
  }

  it should "fail if no ePerusteId for AmmOsaamisala koulutus" in {
    expectValidationErrors(AmmOsaamisalaKoulutus.copy(ePerusteId = None), "missingMsg")
  }

  it should "fail if no osaamisalat found with ePerusteId for AmmOsaamisala koulutus" in {
    expectValidationErrors(AmmOsaamisalaKoulutus.copy(ePerusteId = Some(123L)), "invalidOsaamisalaForEPeruste")
  }

  it should "fail if unknown koulutusalaKoodiUri for AmmMuu koulutus" in {
    expectValidationErrors(ammMuuKoulutusWithParameters(
      "kansallinenkoulutusluokitus2016koulutusalataso1_70",
      "opintojenlaajuusyksikko_6#1"), "invalidKoulutusalaKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusyksikkoKoodiUri for AmmMuu koulutus" in {
    expectValidationErrors(ammMuuKoulutusWithParameters(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01",
      "opintojenlaajuusyksikko_66#1"), "invalidOpintojenLaajuusyksikkoKoodiuri")
  }

  it should "fail if unknown koulutusKoodiUri for Yo koulutus" in {
    expectValidationErrors(yoKoulutusWithParameters().copy(sorakuvausId = None, koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "invalidKoulutuskoodiuri")
  }

  it should "fail if unknown koulutusalaKoodiUri for Yo koulutus" in {
    expectValidationErrors(yoKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "invalidKoulutusalaKoodiuri")
  }

  it should "fail if unknown tutkintonimikeKoodiUri for Yo koulutus" in {
    expectValidationErrors(yoKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "invalidTutkintoNimikeKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Yo koulutus" in {
    expectValidationErrors(yoKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown koulutusKoodiUri for Amk koulutus" in {
    expectValidationErrors(amkKoulutusWithParameters().copy(koulutuksetKoodiUri = Seq("koulutus_000000#1")),
      "invalidKoulutuskoodiuri")
  }

  it should "fail if unknown koulutusalaKoodiUri for Amk koulutus" in {
    expectValidationErrors(amkKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_70"),
      "invalidKoulutusalaKoodiuri")
  }

  it should "fail if unknown tutkintonimikeKoodiUri for Amk koulutus" in {
    expectValidationErrors(amkKoulutusWithParameters(tutkintonimikeKoodiUri = "tutkintonimikekk_110#70"),
      "invalidTutkintoNimikeKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Amk koulutus" in {
    expectValidationErrors(amkKoulutusWithParameters(opintojenlaajuusKoodiUri = "opintojenlaajuus_40#70"),
      "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown koulutusKoodiUri for AmmOpeErityisopeJaOpo koulutus" in {
    expectValidationErrors(AmmOpettajaKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "invalidKoulutuskoodiuri")
  }

  it should "fail if unknown koulutusKoodiUri for Lukio koulutus" in {
    expectValidationErrors(LukioKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_111111#1")),
      "invalidKoulutuskoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Lukio koulutus" in {
    expectValidationErrors(LukioKoulutus.copy(metadata =
      Some(LukioKoulutus.metadata.get.asInstanceOf[LukioKoulutusMetadata].copy(opintojenLaajuusKoodiUri =
        Some("opintojenlaajuus_40#70")))), "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Tuva koulutus" in {
    expectValidationErrors(TuvaKoulutus.copy(metadata =
      Some(TuvaKoulutus.metadata.get.asInstanceOf[TuvaKoulutusMetadata].copy(opintojenLaajuusKoodiUri =
        Some("opintojenlaajuus_40#70")))), "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Telma koulutus" in {
    expectValidationErrors(TelmaKoulutus.copy(metadata =
      Some(TelmaKoulutus.metadata.get.asInstanceOf[TelmaKoulutusMetadata].copy(opintojenLaajuusKoodiUri =
        Some("opintojenlaajuus_40#70")))), "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown koulutusalaKoodiUri for Vapaa sivistystyo koulutus" in {
    expectValidationErrors(VapaaSivistystyoOpistovuosiKoulutus.copy(metadata =
      Some(VapaaSivistystyoOpistovuosiKoulutus.metadata.get.asInstanceOf[VapaaSivistystyoOpistovuosiKoulutusMetadata].copy(
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_70")))),
      "invalidKoulutusalaKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusKoodiUri for Vapaa sivistystyo koulutus" in {
    expectValidationErrors(VapaaSivistystyoMuuKoulutus.copy(metadata =
      Some(VapaaSivistystyoMuuKoulutus.metadata.get.asInstanceOf[VapaaSivistystyoMuuKoulutusMetadata].copy(
        opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#70")))),
      "invalidOpintojenLaajuusKoodiuri")
  }

  it should "fail if unknown opintojenlaajuusyksikkoKoodiUri for Aikuisten perusopetus -koulutus" in {
    expectValidationErrors(AikuistenPerusopetusKoulutus.copy(metadata =
      Some(AikuistenPerusopetusKoulutus.metadata.get.asInstanceOf[AikuistenPerusopetusKoulutusMetadata].copy(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_66#1")))),
      "invalidOpintojenLaajuusyksikkoKoodiuri")
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    validator.withValidation(AmmKoulutus, Some(AmmKoulutus.copy(tila = Tallennettu)))(koulutus => koulutus)
  }

  it should "succeed from julkaistu to arkistoitu" in {
    validator.withValidation(AmmKoulutus.copy(tila = Arkistoitu), Some(AmmKoulutus))(koulutus => koulutus)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    validator.withValidation(AmmKoulutus, Some(AmmKoulutus.copy(tila = Arkistoitu)))(koulutus => koulutus)
  }

  it should "succeed from julkaistu to tallennettu" in {
    validator.withValidation(AmmKoulutus.copy(tila = Tallennettu), Some(AmmKoulutus))(koulutus => koulutus)
  }

  it should "succeed from tallennettu to poistettu when no existing toteutukset for koulutus" in {
    validator.withValidation(AmmKoulutus.copy(oid = Some(koulutusOid2), tila = Poistettu),
      Some(AmmKoulutus.copy(tila = Tallennettu)))(koulutus => koulutus)
  }

  it should "fail from tallennettu to arkistoitu" in {
    expectValidationErrors(AmmKoulutus.copy(tila = Arkistoitu), Some(AmmKoulutus.copy(tila = Tallennettu)), Seq("illegalStateChange"))
  }

  it should "fail from arkistoitu to tallennettu" in {
    expectValidationErrors(AmmKoulutus.copy(tila = Tallennettu), Some(AmmKoulutus.copy(tila = Arkistoitu)), Seq("illegalStateChange"))
  }

  it should "fail from julkaistu to poistettu" in {
    expectValidationErrors(AmmKoulutus.copy(tila = Poistettu), Some(AmmKoulutus.copy(tila = Julkaistu)), Seq("illegalStateChange"))
  }

  it should "fail from arkistoitu to poistettu" in {
    expectValidationErrors(AmmKoulutus.copy(tila = Poistettu), Some(AmmKoulutus.copy(tila = Arkistoitu)), Seq("illegalStateChange"))
  }

  it should "fail from poistettu to tallennettu" in {
    expectValidationErrors(AmmKoulutus.copy(tila = Tallennettu), Some(AmmKoulutus.copy(tila = Poistettu)), Seq("illegalStateChange"))
  }

  it should "fail from tallennettu to poistettu when existing toteutukset for koulutus" in {
    expectValidationErrors(AmmKoulutus.copy(oid = Some(koulutusOid), tila = Poistettu),
      Some(AmmKoulutus.copy(tila = Tallennettu)), Seq("integrityViolation"))
  }
}
