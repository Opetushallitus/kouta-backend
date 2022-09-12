package fi.oph.kouta.validation

import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain.{Fi, Sv, Tallennettu}
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
  val koulutusKoodiClient  = mock[KoulutusKoodiClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]
  val kielet               = Seq(Fi, Sv)
  val ataruId = UUID.randomUUID()

  "findMissingKielet" should "return all kielet when kielistetty is an empty map" in {
    findMissingKielet(Seq(Fi, Sv), Map()) should contain theSameElementsAs Seq(Fi, Sv)
  }

  it should "return the missing kielet when there are some texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text")) should contain theSameElementsAs Seq(Sv)
  }

  it should "return the missing kielet when there are some empty texts" in {
    findMissingKielet(Seq(Fi, Sv), Map(Fi -> "text", Sv -> "")) should contain theSameElementsAs Seq(Sv)
  }

  "validateKielistetty" should "return all kielet when kielistetty is an empty map" in {
    validateKielistetty(Seq(Fi, Sv), Map(), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Fi, Sv))))
  }

  it should "return the missing kielet when there are some texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text"), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Sv))))
  }

  it should "return the missing kielet when there are some empty texts" in {
    validateKielistetty(Seq(Fi, Sv), Map(Fi -> "text", Sv -> ""), "test") should contain(ValidationError("test", invalidKielistetty(Seq(Sv))))
  }

  "assertValidEmail" should "accept an email with a plus" in {
    assertValidEmail("foo+bar@example.com", "test") shouldEqual NoErrors
  }

  it should "fail an email without a TLD" in {
    assertValidEmail("foo@bar", "test") should contain theSameElementsAs Seq(ValidationError("test", invalidEmail("foo@bar")))
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
    assertValidUrl("www.url.fi", "url") should contain theSameElementsAs Seq(ValidationError("url", invalidUrl("www.url.fi")))
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
    assertKoulutustyyppiQueryResult(
      "koulutus_371101#1",
      ammatillisetKoulutustyypit,
      koulutusKoodiClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_371101#1")
    )

  "Koulutustyyppi-koodisto validation" should "succeed when valid koulutusKoodiUri for koulutustyyppi-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(itemFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for koulutustyyppi-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_371101#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(queryFailed)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    doAssertKoulutustyyppiKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
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
      lukioKoulutusKoodiUrit,
      koulutusKoodiClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_301104#1")
    )

  "KoulutusKoodiUri-validation" should "succeed when valid koulutusKoodiUri for filter-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    doAssertKoulutusKoodiUriQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for filter-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutusKoodiUriQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_301104#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(queryFailed)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    val validationContext = ValidationContext(Tallennettu, kielet, create)
    validationContext.setKoodistoServiceOk(false)
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertAtaruQuery(): IsValid =
    assertAtaruQueryResult(ataruId, hakemusPalveluClient, "path", unknownAtaruId(ataruId))

  "AtaruId-validation" should "succeed when valid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemFound)
    doAssertAtaruQuery() should equal(NoErrors)
  }

  it should "fail when invalid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemNotFound)
    doAssertAtaruQuery() should equal(error("path", unknownAtaruId(ataruId)))
  }

  it should "fail when Ataru-query failed" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(queryFailed)
    doAssertAtaruQuery() should equal(error("path", ataruServiceFailureMsg))
  }


}
