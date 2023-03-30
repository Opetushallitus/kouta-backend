package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{AmmKoulutus, AmmMuuKoulutus, AmmOsaamisalaKoulutus, AmmTutkinnonOsaKoulutus, PelastusalanAmmKoulutus}
import fi.oph.kouta.client.KoodistoUtils.koodiUriFromString
import fi.oph.kouta.client._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations._
import org.scalatest.Assertion

class AmmatillinenKoulutusServiceValidationSpec extends BaseSubServiceValidationSpec[Koulutus] {
  val koodistoClient      = mock[CachedKoodistoClient]
  val ePerusteKoodiClient = mock[EPerusteKoodiClient]

  val amm: Koulutus   = AmmKoulutus
  val ammTk: Koulutus = AmmTutkinnonOsaKoulutus
  val ammOa: Koulutus = AmmOsaamisalaKoulutus

  val koulutusOid: Option[KoulutusOid] = Some(KoulutusOid("1.2.246.562.13.125"))

  override def validator = new AmmatillinenKoulutusServiceValidation(koodistoClient, ePerusteKoodiClient)

  override def beforeEach(): Unit = {
    super.beforeEach()

    acceptKoulutusKoodiUri(AmmatillisetKoulutusKoodit, "koulutus_371101#1")
    acceptKoulutusKoodiUri(AmmatillisetKoulutusKoodit, "koulutus_381501#1")
    when(koodistoClient.getKoodiUriVersionOrLatestFromCache("koulutus_371101#1"))
      .thenAnswer(Right(KoodiUri("koulutus_371101", 1, defaultName)))
    acceptKoulutusKoodiUri(AmmatillisetKoulutusKoodit, "koulutus_371101#12")
    when(koodistoClient.getKoodiUriVersionOrLatestFromCache("koulutus_371101#12"))
      .thenAnswer(Right(KoodiUri("koulutus_371101", 12, defaultName)))
    acceptKoulutusKoodiUri(AmmatillisetKoulutusKoodit, "koulutus_371666#1")
    when(koodistoClient.getKoodiUriVersionOrLatestFromCache("koulutus_371666#1"))
      .thenAnswer(Left(new RuntimeException("")))

    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L))
      .thenAnswer(Right(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371666", 1))))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(123L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_371101"))))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(111L)).thenAnswer(Right(Seq[KoodiUri]()))
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(200L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_371101"))))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L))
      .thenAnswer(Right(Seq(KoodiUri("osaamisala_01", 1, defaultName))))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(123L)).thenAnswer(Right(Seq[KoodiUri]()))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(123)))
      .thenAnswer(
        Right(
          Map(
            123L -> Seq(
              TutkinnonOsaServiceItem(1235L, 123L, Map(Fi -> "eri nimi")),
              TutkinnonOsaServiceItem(1234L, 122L, defaultName)
            )
          )
        )
      )
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(200))).thenAnswer(Right(Map(200L -> Seq())))
    val ePerusteFailure = KoodistoQueryException("url", 500, "ePerusteServiceFailed")
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(66L)).thenAnswer(Left(ePerusteFailure))
    when(ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(66L)).thenAnswer(Left(ePerusteFailure))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(66))).thenAnswer(Left(ePerusteFailure))

    when(
      koodistoClient.koodiUriExistsInKoodisto(
        KoulutusalaKoodisto,
        "kansallinenkoulutusluokitus2016koulutusalataso2_080#1"
      )
    )
      .thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6#1"))
      .thenAnswer(itemFound)
  }

  private def acceptKoulutusKoodiUri(filter: KoulutusKoodiFilter, koodiUri: String): Unit =
    when(
      koodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(filter.koulutusTyypit, koodiUri)
    ).thenAnswer(itemFound)

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
      koulutusalaKoodiUri: String = "kansallinenkoulutusluokitus2016koulutusalataso2_080#1",
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

  "Ammatillinen koulutus validation" should "succeed when new valid Amm koulutus" in {
    passesValidation(amm)
  }

  it should "succeed when koulutuskoodiUri has higher version than the matching koodiUri in ePeruste" in {
    passesValidation(amm.copy(koulutuksetKoodiUri = Seq("koulutus_371101#12")))
  }

  it should "succeed when new valid AmmOsaamisala koulutus" in {
    passesValidation(ammOa)
  }

  it should "succeed when new valid AmmTutkinnonOsa koulutus" in {
    passesValidation(ammTk)
  }

  it should "succeed when new valid AmmTutkinnonOsa without koulutusKoodiUri" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(124L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(124)))
      .thenAnswer(Right(Map(124L -> Seq(TutkinnonOsaServiceItem(1345L, 134L, defaultName)))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(124L), None, Some(1345L), Some(134L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with koulutusKoodiUri only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(125L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_123456"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(125)))
      .thenAnswer(Right(Map(125L -> Seq(TutkinnonOsaServiceItem(111111, 11111L, Map())))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(125L), Some("koulutus_123456#2"), None, None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(126L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(126)))
      .thenAnswer(Right(Map(126L -> Seq(TutkinnonOsaServiceItem(1346L, 11111L, Map())))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(126L), None, Some(1346L), None))
  }

  it should "succeed when new valid AmmTutkinnonOsa with tutkinnonosaViite only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(127L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(127)))
      .thenAnswer(Right(Map(127L -> Seq(TutkinnonOsaServiceItem(111111L, 135L, Map())))))
    passesValidation(ammTkWithTutkinnonOsaParams(Some(127L), None, None, Some(135L)))
  }

  it should "succeed when new valid AmmTutkinnonOsa with ePerusteId only" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(128L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(128)))
      .thenAnswer(Right(Map(128L -> Seq(TutkinnonOsaServiceItem(111111, 11111L, Map())))))
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
    passesValidation(AmmMuuKoulutus)
  }

  it should "succeed when new incomplete luonnos AmmMuu koulutus" in {
    passesValidation(AmmMuuKoulutus.copy(tila = Tallennettu, metadata = Some(AmmatillinenMuuKoulutusMetadata())))
  }

  it should "succeed when koulutuksetKoodiUri not changed in modify operation, even though unknown Uris" in {
    val ammKoulutus =
      amm.copy(oid = koulutusOid, koulutuksetKoodiUri = Seq("koulutus_000000#1"))
    passesModifyValidation(ammKoulutus, ammKoulutus)
  }

  it should "Succeed when ePerusteId not changed in modify operation, even though ePerusteId unknown" in {
    val nonChangedAmm = amm.copy(oid = koulutusOid, ePerusteId = Some(111L))
    passesModifyValidation(nonChangedAmm, nonChangedAmm)
    val nonChangedAmmOa = ammOa.copy(oid = koulutusOid, ePerusteId = Some(111L))
    passesModifyValidation(nonChangedAmmOa, nonChangedAmmOa)
  }

  it should "Succeed when ammTutkinnonosa not changed in modify operation, even though invalid parameters" in {
    val nonChangedAmmTk = ammTkWithTutkinnonOsaParams(Some(111L), Some("koulutus_12345#1"), Some(66L), Some(66L))
      .copy(oid = koulutusOid)
    passesModifyValidation(nonChangedAmmTk, nonChangedAmmTk)
  }

  it should "Succeed when koulutusalaKoodiUrit not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedAmmMuu =
      ammMuuKoulutusWithParameters("kansallinenkoulutusluokitus2016koulutusalataso1_66").copy(oid = koulutusOid)
    passesModifyValidation(nonChangedAmmMuu, nonChangedAmmMuu)
  }

  it should "Succeed when laajuusYksikkoKoodiUri not changed in modify operation, even though invalid koodiUrit" in {
    val nonChangedAmmMuu = ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "opintojenlaajuusyksikko_9#1")
      .copy(oid = koulutusOid)
    passesModifyValidation(nonChangedAmmMuu, nonChangedAmmMuu)
  }

  private def failValidationWithKoulutuksetKoodiUri(koulutus: Koulutus): Assertion = {
    val urit = Seq("koulutus_371101#1")
    failsSingleValidation(koulutus.copy(koulutuksetKoodiUri = urit), "koulutuksetKoodiUri", notEmptyMsg)
  }

  it should "fail if koulutuksetKoodiUri given for koulutustyyppi not using koulutusKoodit" in {
    failValidationWithKoulutuksetKoodiUri(ammTk)
    failValidationWithKoulutuksetKoodiUri(AmmMuuKoulutus)
  }

  private def failValidationWithePerusteId(koulutus: Koulutus): Assertion = {
    failsSingleValidation(koulutus.copy(ePerusteId = Some(11L)), "ePerusteId", notMissingMsg(Some(11L)))
  }

  it should "fail if ePerusteId given for koulutustyyppi not using ePeruste" in {
    failValidationWithePerusteId(ammTk)
    failValidationWithePerusteId(AmmMuuKoulutus)
  }

  it should "fail if invalid koulutusKoodiUris for ammatillinen koulutus" in {
    failsSingleValidation(amm.copy(koulutuksetKoodiUri = Seq()), "koulutuksetKoodiUri", missingMsg)
    failsSingleValidation(
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

  it should "fail if nimi not matching koulutus-koodisto name for ammatillinen koulutus" in {
    failsValidation(
      amm.copy(nimi = Map(Fi -> "eri nimi", Sv -> "eri nimi sv")),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "koulutuksessa koulutus_371101#1")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "koulutuksessa koulutus_371101#1"))
      )
    )
  }

  it should "fail if koodisto query failed when validating name of a ammatillinen koulutus" in {
    failsSingleValidation(
      amm.copy(koulutuksetKoodiUri = Seq("koulutus_371666#1")),
      "koulutuksetKoodiUri",
      koodistoServiceFailureMsg
    )
  }

  it should "fail if invalid ePeruste for ammatillinen koulutus" in {
    failsSingleValidation(ammOa.copy(ePerusteId = None), "ePerusteId", missingMsg)
    failsSingleValidation(ammOa.copy(ePerusteId = Some(-11L)), "ePerusteId", notNegativeMsg)
    failsSingleValidation(amm.copy(ePerusteId = Some(111L)), "ePerusteId", invalidEPerusteId(111L))
  }

  it should "fail if ePeruste-service query failed for ammatillinen koulutus" in {
    failsSingleValidation(amm.copy(ePerusteId = Some(66L)), "ePerusteId", ePerusteServiceFailureMsg)
    failsSingleValidation(ammOa.copy(ePerusteId = Some(66L)), "ePerusteId", ePerusteServiceFailureMsg)
  }

  it should "fail if invalid tutkinnonosat for AmmTutkinnonosa koulutus" in {
    failsSingleValidation(
      ammTk.copy(metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata())),
      "metadata.tutkinnonOsat",
      missingMsg
    )
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(
        koulutusKoodiUri = Some("puppu"),
        ePerusteId = Some(123L),
        tutkinnonOsaId = Some(122L),
        tutkinnonOsaViite = Some(1234L)
      ),
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

  it should "should succeed even if nimi is not matching tutkinnonosa for AmmTutkinnonosa koulutus" in {
    when(ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(124L))
      .thenAnswer(Right(Seq(koodiUriFromString("koulutus_000000"))))
    when(ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(124)))
      .thenAnswer(Right(Map(124L -> Seq(TutkinnonOsaServiceItem(1345L, 134L, defaultName)))))
    passesValidation(
      ammTkWithTutkinnonOsaParams(Some(124L), Some("koulutus_000000"), Some(1345L), Some(134L))
        .copy(nimi = Map(Fi -> "eri nimi", Sv -> "eri nimi sv"))
    )
  }

  it should "fail if invalid ePeruste for AmmTutkinnonosa koulutus" in {
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(-1L), tutkinnonOsaId = Some(1L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      notNegativeMsg
    )
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(111L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      invalidEPerusteId(111L)
    )
  }

  it should "fail if ePeruste-service query failed for AmmTutkinnonosa koulutus" in {
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(ePerusteId = Some(66L)),
      "metadata.tutkinnonOsat[0].ePerusteId",
      ePerusteServiceFailureMsg
    )
  }

  it should "fail if unknown tutkinnonOsa viite- and ID for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(200), Some("koulutus_371101"), Some(1234), Some(2345)),
      Seq(
        ValidationError(
          "metadata.tutkinnonOsat[0].tutkinnonosaViite",
          invalidTutkinnonOsaViiteForEPeruste(200, 2345)
        ),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaId", invalidTutkinnonOsaIdForEPeruste(200, 1234))
      )
    )
  }

  it should "fail if tutkinnonosaID did not match to tutkinnonosa found by ePeruste and viite for AmmTutkinnonosa koulutus" in {
    failsSingleValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), Some("koulutus_371101"), Some(1235L), Some(122L)),
      "metadata.tutkinnonOsat[0].tutkinnonosaId",
      invalidTutkinnonOsaIdForEPeruste(123L, 1235L)
    )
  }

  it should "fail if unknown tutkinnonosaViite for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), Some("koulutus_371101"), None, Some(130L)),
      Seq(
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaId", missingMsg),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaViite", invalidTutkinnonOsaViiteForEPeruste(123L, 130L))
      )
    )
  }

  it should "fail if unknown tutkinnonosaID for AmmTutkinnonosa koulutus" in {
    failsValidation(
      ammTkWithTutkinnonOsaParams(Some(123L), Some("koulutus_371101"), Some(1236L), None),
      Seq(
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaViite", missingMsg),
        ValidationError("metadata.tutkinnonOsat[0].tutkinnonosaId", invalidTutkinnonOsaIdForEPeruste(123L, 1236L))
      )
    )
  }

  it should "fail if invalid osaamisalat for AmmOsaamisala koulutus" in {
    failsSingleValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata())),
      "metadata.osaamisalaKoodiUri",
      missingMsg
    )
    failsSingleValidation(
      ammOa.copy(metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata(osaamisalaKoodiUri = Some("puppu")))),
      "metadata.osaamisalaKoodiUri",
      invalidOsaamisalaForEPeruste(11L, "puppu")
    )
    failsSingleValidation(
      ammOa.copy(ePerusteId = Some(123L)),
      "metadata.osaamisalaKoodiUri",
      invalidOsaamisalaForEPeruste(123L, "osaamisala_01")
    )
  }

  it should "fail if nimi not matching osaamisala for AmmOsaamisala koulutus" in {
    failsValidation(
      ammOa.copy(nimi = Map(Fi -> "eri nimi", Sv -> "eri nimi sv")),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "osaamisalassa osaamisala_01")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "osaamisalassa osaamisala_01"))
      )
    )
  }

  it should "fail if invalid koulutusalaKoodiUri for AmmMuu koulutus" in {
    failsSingleValidation(
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
    failsSingleValidation(
      ammMuuKoulutusWithParameters(opintojenlaajusyksikkoKoodiUri = "opintojenlaajuusyksikko_66#1"),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidOpintojenLaajuusyksikkoKoodiuri("opintojenlaajuusyksikko_66#1")
    )
    failsSingleValidation(
      ammMuuKoulutusWithParameters(opintojenLaajuusNumero = Some(-1)),
      "metadata.opintojenLaajuusNumero",
      notNegativeMsg
    )
  }

  it should "pass julkaistu pelastusalan koulutus" in {
    passesValidation(PelastusalanAmmKoulutus)
  }

  it should "fail pelastusalan koulutus with ePerusteId" in {
    failsSingleValidation(PelastusalanAmmKoulutus.copy(ePerusteId = Some(11L)), "ePerusteId", notMissingMsg(Some(11L)))
  }
}
