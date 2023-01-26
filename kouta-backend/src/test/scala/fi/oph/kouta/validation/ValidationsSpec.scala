package fi.oph.kouta.validation

import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient, CachedKoodistoClient}
import fi.oph.kouta.domain.{AmmatillisetKoulutusKoodit, En, Fi, LukioKoulutusKoodit, Sv, Tallennettu}
import fi.oph.kouta.validation.CrudOperations.create
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound, queryFailed}
import fi.oph.kouta.validation.Validations._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, equal}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util.UUID

class ValidationsSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val hakuKoodiClient      = mock[HakuKoodiClient]
  val cachedKoodistoClient  = mock[CachedKoodistoClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]
  val kielet               = Seq(Fi, Sv)
  val ataruId              = UUID.randomUUID()

  "findMissingKielet" should "return all kielet when kielistetty is an empty map" in {
    findMissingKielet(Seq(Fi, Sv), Map()) should contain theSameElementsAs Seq(Fi, Sv)
  }

  it should "return the missing kielet when there are some texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text")) should contain theSameElementsAs Seq(Sv)
  }

  it should "return the missing kielet when there are some empty texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text", Sv -> "")) should contain theSameElementsAs Seq(Sv)
  }

  "findNonAllowedKielet" should "return all kielet in kielistetty when kielivalinta is empty" in {
    findNonAllowedKielet(Seq(), Map(Fi -> "text", Sv -> "text sv")) should contain theSameElementsAs Seq(Fi, Sv)
  }

  it should "return non-allowed kielet when some texts empty" in {
    findNonAllowedKielet(Seq(), Map(Fi -> "text", Sv -> "", En -> null)) should contain theSameElementsAs Seq(Fi)
  }

  it should "return nothing when only allowed kielet populated" in {
    findNonAllowedKielet(Seq(Fi), Map(Fi -> "text", Sv -> "")) shouldEqual NoErrors
  }

  "validateKielistetty" should "return all kielet when kielistetty is an empty map" in {
    validateKielistetty(Seq(Fi, Sv), Map(), "test") should contain(
      ValidationError("test", invalidKielistetty(Seq(Fi, Sv)))
    )
  }

  it should "return the missing kielet when there are some texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text"), "test") should contain(
      ValidationError("test", invalidKielistetty(Seq(Sv)))
    )
  }

  it should "return the missing kielet when there are some empty texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text", Sv -> ""), "test") should contain(
      ValidationError("test", invalidKielistetty(Seq(Sv)))
    )
  }

  it should "return both missing and non-allowed kielet" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text", Sv -> "", En -> "something"), "test") should contain theSameElementsAs Seq(
      ValidationError("test", invalidKielistetty(Seq(Sv))),
      ValidationError("test", notAllowedKielistetty(Seq(En)))
    )
  }

  "assertValidEmail" should "accept an email with a plus" in {
    assertValidEmail("foo+bar@example.com", "test") shouldEqual NoErrors
  }

  it should "fail an email without a TLD" in {
    assertValidEmail("foo@bar", "test") should contain theSameElementsAs Seq(
      ValidationError("test", invalidEmail("foo@bar"))
    )
  }

  it should "accept an email with a funny TLD" in {
    assertValidEmail("foo@bar.pics", "test") shouldEqual NoErrors
  }

  "assertValidUrl" should "accept a valid url" in {
    assertValidUrl("https://www.google.fi", "url") shouldEqual NoErrors
  }

  it should "fail an invalid url" in {
    assertValidUrl("urli", "url") should contain theSameElementsAs Seq(ValidationError("url", invalidUrl("urli")))
  }

  it should "fail an url without the protocol" in {
    assertValidUrl("www.url.fi", "url") should contain theSameElementsAs Seq(
      ValidationError("url", invalidUrl("www.url.fi"))
    )
  }

  "assertNimiMatchExternal" should "accept matching nimi" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv -> "nimi sv"),
      Map(Fi -> "nimi", Sv -> "nimi sv", En -> "nimi en"),
      "nimi",
      "koulutuksessa"
    ) shouldEqual NoErrors
  }

  it should "not accept nimi if language not found from external" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv -> "nimi sv"),
      Map(Fi -> "nimi", En -> "nimi en"),
      "nimi",
      "koulutuksessa"
    ) should contain theSameElementsAs Seq(
      ValidationError("nimi.sv", nameNotAllowedForFixedlyNamedEntityMsg("koulutuksessa"))
    )
  }

  it should "ignore empty names" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv -> "", En -> null),
      Map(Fi -> "nimi"),
      "nimi",
      "koulutuksessa"
    ) shouldEqual NoErrors
  }

  it should "not accept nimi if single language item not matching" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv     -> "nimi sv"),
      Map(Fi -> "eri nimi", Sv -> "nimi sv", En -> "nimi en"),
      "nimi",
      "koulutuksessa"
    ) should contain theSameElementsAs Seq(
      ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("eri nimi", "koulutuksessa"))
    )
  }

  it should "not accept nimi if several language items not matching" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv     -> "nimi sv"),
      Map(Fi -> "eri nimi", Sv -> "eri nimi sv", En -> "nimi en"),
      "nimi",
      "koulutuksessa"
    ) should contain theSameElementsAs Seq(
      ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("eri nimi", "koulutuksessa")),
      ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("eri nimi sv", "koulutuksessa"))
    )
  }

  it should "not accept nimi when both invalid and non-allowed names" in {
    assertNimiMatchExternal(
      Map(Fi -> "nimi", Sv -> "nimi sv", En -> "nimi en"),
      Map(Fi -> "eri nimi", Sv -> "nimi sv"),
      "nimi",
      "koulutuksessa"
    ) should contain theSameElementsAs Seq(
      ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("eri nimi", "koulutuksessa")),
      ValidationError("nimi.en", nameNotAllowedForFixedlyNamedEntityMsg("koulutuksessa"))
    )
  }

  private def doAssertKoodistoQuery(
      validationContext: ValidationContext = ValidationContext(Tallennettu, kielet, create)
  ): IsValid =
    assertKoodistoQueryResult(
      "valintakokeentyyppi_1#1",
      hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
      "path",
      validationContext,
      invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_1#1")
    )

  "Koodisto validation" should "succeed when valid koodiUri" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    doAssertKoodistoQuery() should equal(NoErrors)
  }

  it should "fail when invalid koodiUri" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemNotFound)
    doAssertKoodistoQuery() should equal(error("path", invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_1#1")))
  }

  it should "fail when koodiUri query failed" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(queryFailed)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    doAssertKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    validationContext.setKoodistoServiceOk(false)
    doAssertKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertKoulutustyyppiKoodistoQuery(
      validationContext: ValidationContext = ValidationContext(Tallennettu, kielet, create)
  ): IsValid =
    assertKoulutuskoodiQueryResult(
      "koulutus_371101#1",
      AmmatillisetKoulutusKoodit,
      cachedKoodistoClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_371101#1")
    )

  "Koulutustyyppi-koodisto validation" should "succeed when valid koulutusKoodiUri for koulutustyyppi-list" in {
    when(cachedKoodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#1"))
      .thenAnswer(itemFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for koulutustyyppi-list" in {
    when(cachedKoodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_371101#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(cachedKoodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#1"))
      .thenAnswer(queryFailed)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    doAssertKoulutustyyppiKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(cachedKoodistoClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#1"))
      .thenAnswer(itemFound)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    validationContext.setKoodistoServiceOk(false)
    doAssertKoulutustyyppiKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertKoulutusKoodiUriQuery(
      validationContext: ValidationContext = ValidationContext(Tallennettu, kielet, create)
  ): IsValid =
    assertKoulutuskoodiQueryResult(
      "koulutus_301104#1",
      LukioKoulutusKoodit,
      cachedKoodistoClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_301104#1")
    )

  "KoulutusKoodiUri-validation" should "succeed when valid koulutusKoodiUri for filter-list" in {
    when(cachedKoodistoClient.koulutusKoodiUriExists(LukioKoulutusKoodit.koulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    doAssertKoulutusKoodiUriQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for filter-list" in {
    when(cachedKoodistoClient.koulutusKoodiUriExists(LukioKoulutusKoodit.koulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutusKoodiUriQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_301104#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(cachedKoodistoClient.koulutusKoodiUriExists(LukioKoulutusKoodit.koulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(queryFailed)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(cachedKoodistoClient.koulutusKoodiUriExists(LukioKoulutusKoodit.koulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    validationContext.setKoodistoServiceOk(false)
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertAtaruQuery(): IsValid =
    assertAtaruQueryResult(ataruId, hakemusPalveluClient, "path")

  "AtaruId-validation" should "succeed when valid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId)).thenAnswer(itemFound)
    doAssertAtaruQuery() should equal(NoErrors)
  }

  it should "fail when invalid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId)).thenAnswer(itemNotFound)
    doAssertAtaruQuery() should equal(error("path", unknownAtaruId(ataruId)))
  }

  it should "fail when Ataru-query failed" in {
    when(hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId)).thenAnswer(queryFailed)
    doAssertAtaruQuery() should equal(error("path", ataruServiceFailureMsg))
  }
}
